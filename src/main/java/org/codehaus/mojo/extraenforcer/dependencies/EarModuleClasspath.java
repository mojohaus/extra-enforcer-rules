package org.codehaus.mojo.extraenforcer.dependencies;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This rule verifies that the {@code Class-Path} entries in the manifests of the modules of an EAR, that is its
 * EJB, application client and web modules, point to files or directories in the EAR. Each entry is resolved
 * relative to the location of the module in the EAR, as the JAR specification describes.
 *
 * <p>The rule reads the built EAR, so bind it to the {@code package} phase or later.
 *
 * @since 1.12.2
 */
@Named("earModuleClasspath")
public class EarModuleClasspath extends AbstractEnforcerRule {

    private final MavenProject project;

    @Inject
    EarModuleClasspath(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        if (!"ear".equals(project.getPackaging())) {
            getLog().debug("Skipping " + project.getArtifactId() + ": packaging is " + project.getPackaging());
            return;
        }
        File earFile =
                project.getArtifact() == null ? null : project.getArtifact().getFile();
        if (earFile == null || !earFile.isFile()) {
            throw new EnforcerRuleException("The EAR of " + project.getArtifactId() + " does not exist yet (" + earFile
                    + "). Bind the earModuleClasspath rule to the package phase or later.");
        }
        try {
            List<String> problems = verify(earFile);
            if (!problems.isEmpty()) {
                throw new EnforcerRuleException(String.join(System.lineSeparator(), problems));
            }
        } catch (IOException e) {
            throw new EnforcerRuleException("Cannot read " + earFile + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns one message per Class-Path entry that does not point to a file or directory in the EAR.
     */
    List<String> verify(File earFile) throws IOException, EnforcerRuleException {
        List<String> problems = new ArrayList<>();
        try (ZipFile ear = new ZipFile(earFile)) {
            Set<String> names = new HashSet<>();
            List<String> topLevelArchives = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = ear.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                names.add(name);
                // a directory may exist only implicitly, as the parent of an entry
                for (int slash = name.indexOf('/'); slash >= 0; slash = name.indexOf('/', slash + 1)) {
                    names.add(name.substring(0, slash + 1));
                }
                if (name.indexOf('/') < 0 && (name.endsWith(".jar") || name.endsWith(".war"))) {
                    topLevelArchives.add(name);
                }
            }

            Collection<String> modules = readModules(ear, topLevelArchives);
            for (String module : modules) {
                ZipEntry moduleEntry = ear.getEntry(module);
                if (moduleEntry == null || moduleEntry.isDirectory()) {
                    getLog().debug("Module " + module + " is declared but not in the EAR, skipping it");
                    continue;
                }
                for (String clEntry : readClassPath(ear, moduleEntry)) {
                    String problem = check(module, clEntry, names);
                    if (problem != null) {
                        problems.add(problem);
                    }
                }
            }
        }
        return problems;
    }

    /**
     * Resolves one Class-Path entry against the location of the module in the EAR, as the JAR specification
     * describes, and returns a message if the target is not in the EAR.
     */
    private String check(String module, String clEntry, Set<String> names) {
        URI uri;
        try {
            uri = new URI(clEntry);
        } catch (URISyntaxException e) {
            return module + ": the Class-Path entry " + clEntry + " is not a valid relative URL";
        }
        if (uri.isAbsolute()
                || uri.getAuthority() != null
                || uri.getPath() == null
                || uri.getPath().startsWith("/")) {
            getLog().debug("Ignoring " + clEntry + " in the Class-Path of " + module + ": it is not a relative URL");
            return null;
        }
        String target = resolve(module, uri.getPath());
        if (target == null) {
            return module + ": the Class-Path entry " + clEntry + " points outside the EAR";
        }
        if (!target.isEmpty() && !names.contains(target)) {
            return module + ": the Class-Path entry " + clEntry + " resolves to " + target
                    + ", which is not in the EAR";
        }
        return null;
    }

    /**
     * Returns the path inside the EAR that {@code path} names when read relative to the directory of
     * {@code module}, or {@code null} if it leaves the EAR. The result of a directory ends with a slash, and the
     * root of the EAR is the empty string.
     */
    static String resolve(String module, String path) {
        String base = module.substring(0, module.lastIndexOf('/') + 1);
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : (base + path).split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    return null;
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        String resolved = String.join("/", segments);
        return path.endsWith("/") && !resolved.isEmpty() ? resolved + '/' : resolved;
    }

    private Set<String> readClassPath(ZipFile ear, ZipEntry module) throws IOException {
        Set<String> classPath = new LinkedHashSet<>();
        try (ZipInputStream zip = new ZipInputStream(ear.getInputStream(module))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName())) {
                    String value = new Manifest(zip).getMainAttributes().getValue("Class-Path");
                    if (value != null && !value.trim().isEmpty()) {
                        classPath.addAll(Arrays.asList(value.trim().split("\\s+")));
                    }
                    break;
                }
            }
        }
        return classPath;
    }

    /**
     * Returns the EJB, application client and web modules of the EAR. Without an {@code application.xml} the
     * archives at the top level of the EAR are the modules.
     */
    private Collection<String> readModules(ZipFile ear, List<String> topLevelArchives)
            throws IOException, EnforcerRuleException {
        ZipEntry applicationXml = ear.getEntry("META-INF/application.xml");
        if (applicationXml == null) {
            getLog().debug("The EAR has no META-INF/application.xml, checking its top-level archives");
            return topLevelArchives;
        }
        ApplicationXmlHandler handler = new ApplicationXmlHandler();
        try (InputStream in = ear.getInputStream(applicationXml)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            SAXParser parser = factory.newSAXParser();
            parser.getXMLReader().setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            parser.parse(in, handler);
            return handler.getModules();
        } catch (ParserConfigurationException | SAXException e) {
            throw new EnforcerRuleException("Cannot parse META-INF/application.xml: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "EarModuleClasspath";
    }

    private static final class ApplicationXmlHandler extends DefaultHandler {
        private final Set<String> modules = new LinkedHashSet<>();
        private final Deque<String> elements = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            elements.push(localName);
            text.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            elements.pop();
            String parent = elements.peek();
            String value = text.toString().trim();
            boolean module = ("ejb".equals(localName) || "java".equals(localName)) && "module".equals(parent);
            if ((module || "web-uri".equals(localName) && "web".equals(parent)) && !value.isEmpty()) {
                modules.add(value.startsWith("/") ? value.substring(1) : value);
            }
            text.setLength(0);
        }

        Set<String> getModules() {
            return modules;
        }
    }
}
