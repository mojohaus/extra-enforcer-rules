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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 * This rule verifies that the {@code Class-Path} entries in the manifests of the modules of an EAR refer to
 * libraries in the EAR.
 *
 * <p>The rule reads the built EAR, so bind it to the {@code package} phase or later.
 *
 * @since 1.12.2
 */
@Named("earModuleClasspath")
public class EarModuleClasspath extends AbstractEnforcerRule {

    private static final String DEFAULT_LIB_DIR = "lib";

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
     * Returns one message per Class-Path entry that does not point to a library in the EAR.
     */
    List<String> verify(File earFile) throws IOException, EnforcerRuleException {
        List<String> problems = new ArrayList<>();
        try (ZipFile ear = new ZipFile(earFile)) {
            ApplicationXmlHandler appXml = readApplicationXml(ear);
            Set<String> sharedLibs = new HashSet<>();
            Map<String, Set<String>> moduleClasspaths = new LinkedHashMap<>();

            String libFolderName = appXml.getLibDir() + '/';
            Enumeration<? extends ZipEntry> entries = ear.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (appXml.getEjbModules().contains(entryName)) {
                    moduleClasspaths.put(entryName, readClassPath(ear, entry));
                } else if (entryName.startsWith(libFolderName) && entryName.endsWith(".jar")) {
                    sharedLibs.add(entryName);
                }
            }

            for (Map.Entry<String, Set<String>> module : moduleClasspaths.entrySet()) {
                for (String clEntry : module.getValue()) {
                    if (!sharedLibs.contains(clEntry) && !sharedLibs.contains(libFolderName + clEntry)) {
                        problems.add(module.getKey() + " refers to " + clEntry + " in its Class-Path, but " + clEntry
                                + " is not in the EAR");
                    }
                }
            }
        }
        return problems;
    }

    private Set<String> readClassPath(ZipFile ear, ZipEntry module) throws IOException {
        Set<String> classPath = new LinkedHashSet<>();
        try (ZipInputStream zip = new ZipInputStream(ear.getInputStream(module))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName())) {
                    String value = new Manifest(zip).getMainAttributes().getValue("Class-Path");
                    if (value != null && !value.trim().isEmpty()) {
                        classPath.addAll(java.util.Arrays.asList(value.trim().split("\\s+")));
                    }
                    break;
                }
            }
        }
        return classPath;
    }

    private ApplicationXmlHandler readApplicationXml(ZipFile ear) throws IOException, EnforcerRuleException {
        ZipEntry applicationXml = ear.getEntry("META-INF/application.xml");
        ApplicationXmlHandler handler = new ApplicationXmlHandler();
        if (applicationXml == null) {
            return handler;
        }
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
            return handler;
        } catch (ParserConfigurationException | SAXException e) {
            throw new EnforcerRuleException("Cannot parse META-INF/application.xml: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "EarModuleClasspath";
    }

    private static final class ApplicationXmlHandler extends DefaultHandler {
        private final Set<String> ejbModules = new LinkedHashSet<>();
        private final Deque<String> elements = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();
        private String libDir = DEFAULT_LIB_DIR;

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
            if ("ejb".equals(localName) && "module".equals(parent)) {
                ejbModules.add(value);
            } else if ("library-directory".equals(localName)) {
                libDir = value;
            }
            text.setLength(0);
        }

        Set<String> getEjbModules() {
            return ejbModules;
        }

        String getLibDir() {
            return libDir;
        }
    }
}
