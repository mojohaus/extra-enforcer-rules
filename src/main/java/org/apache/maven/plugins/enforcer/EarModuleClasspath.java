package org.apache.maven.plugins.enforcer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This rule verifies that the EJB's Classpath entry in the manifest refers to a libray in the shared library path.
 * 
 * The rule assumes that all EJB are located at the root of the EAR and the lib folder contains all shared libraries.
 *  
 * @author Martin Goldhahn <martin.goldhahn@tieto.com>
 * @author Stig Tore Johannesen <stigtore.johannesen@tieto.com>
 */
public class EarModuleClasspath implements EnforcerRule {

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {

        try {
            MavenProject project = (MavenProject) helper.evaluate("${project}");
            String projectType = project.getArtifact().getType();
            if (!"ear".equals(projectType)) { 
                throw new EnforcerRuleException("EarModuleClasspath rule can only be applied in EAR projects"); 
            }
            File outputDir = new File(project.getBuild().getDirectory());
            String finalName = project.getBuild().getFinalName();
            File earFile = new File(outputDir, finalName + '.' + projectType);

            verifyManifests(earFile);
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Unable to lookup expression " + e.getLocalizedMessage(), e);
        } catch (IOException ex) {
            throw new EnforcerRuleException(ex.getMessage(), ex);
        }
    }

    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid(EnforcerRule cachedRule) {
        return false;
    }

    public String getCacheId() {
        return "";
    }

    private void verifyManifests(File earFile) throws EnforcerRuleException, IOException {
        Set<String> sharedLibs = new HashSet<String>();
        Map<String, Set<String>> ejbClasspaths = new HashMap<String, Set<String>>();

        ApplicationXmlHandler appXml = readApplicationXml(earFile);
        
        extractSharedLibsAndEjbClasspath(earFile, appXml, sharedLibs, ejbClasspaths);

        checkClassPaths(sharedLibs, ejbClasspaths, appXml);
    }

    private void extractSharedLibsAndEjbClasspath(File earFile, ApplicationXmlHandler appXml, Set<String> sharedLibs,
        Map<String, Set<String>> ejbClasspaths) throws IOException {
        
        String libFolderName = appXml.getLibDir() + '/';
        ZipFile ear = null;
        try {
            ear = new ZipFile(earFile);
            Enumeration<? extends ZipEntry> entries = ear.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (appXml.getEjbModules().contains(entryName)) {
                    Set<String> cl = extractClassPathFromManifest(ear, entry);
                    ejbClasspaths.put(entryName, cl);
                } else if (appXml.getWebModules().contains(entryName)) {
                    // web modules are ignored
                } else if (entryName.startsWith(libFolderName) && entryName.endsWith(".jar")) {
                    // this must be a shared library
                    sharedLibs.add(entryName);
                }
            }
        } finally {
            if (ear != null) {
                ear.close();
            }
        }
    }

    private Set<String> extractClassPathFromManifest(ZipFile ear, ZipEntry entry) throws IOException {
        
        ZipInputStream zipStream = null;
        Set<String> classPath = new HashSet<String>();
        try {
            zipStream = new ZipInputStream(ear.getInputStream(entry));
           
            ZipEntry moduleEntry;
            while ((moduleEntry = zipStream.getNextEntry()) != null) {
                if (moduleEntry.getName().equals(JarFile.MANIFEST_NAME)) {
                    ByteArrayOutputStream sb = new ByteArrayOutputStream();
                    byte[] buffer = new byte[0xFFFF];
                    int read;
                    while ((read = zipStream.read(buffer)) >= 0) {
                        sb.write(buffer, 0, read);
                    }
                    Manifest mf = new Manifest(new ByteArrayInputStream(sb.toByteArray()));
                    String cl = mf.getMainAttributes().getValue("Class-Path");
                    if (cl != null) {
                        for (String clEntry : cl.split(" ")) {
                            classPath.add(clEntry);
                        }
                    }
                    break;
                }
            }
            return classPath;
        } finally {
            if (zipStream != null) {
                zipStream.close();
            }
        }
    }

    /**
     * Read the META-INF/application.xml from the EAR.
     */
    private ApplicationXmlHandler readApplicationXml(File earFile) throws IOException {
        ZipFile ear = null;
        try {
            ear = new ZipFile(earFile);
            ApplicationXmlHandler appXml = new ApplicationXmlHandler();
            ZipEntry applicationXmlEntry = ear.getEntry("META-INF/application.xml");
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(ear.getInputStream(applicationXmlEntry), appXml);
            return appXml;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } finally {
            if (ear != null) {
                ear.close();
            }
        }
    }

    /**
     * The libraries in the classpath entry of the manifest can include the library path or not. 
     */
    private void checkClassPaths(Set<String> sharedLibs, Map<String, Set<String>> ejbClasspaths, ApplicationXmlHandler appXml) throws EnforcerRuleException {
        for (Map.Entry<String, Set<String>> entry : ejbClasspaths.entrySet()) {
            for (String clEntry : entry.getValue()) {
                if (!sharedLibs.contains(clEntry) && !sharedLibs.contains(appXml.getLibDir() + '/' + clEntry)) {
                    throw new EnforcerRuleException("Did not find shared library " + clEntry + " in manifest of " + entry.getKey());
                }
            }
        }
    }

    private static class ApplicationXmlHandler extends DefaultHandler {
        private Set<String> ejbModules = new HashSet<String>();
        private Set<String> webModules = new HashSet<String>();
        private String libDir;
        
        private Stack<String> elementStack = new Stack<String>();
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            elementStack.push(qName);
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            elementStack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String value = new String(ch, start, length).trim();
            
            String currentElement = elementStack.peek();
            if ("ejb".equals(currentElement)) {
                ejbModules.add(value);
            } else if ("web-uri".equals(currentElement)) {
                webModules.add(value);
            } else if ("library-directory".equals(currentElement)) {
                libDir = value;
            }
        }

        public Set<String> getEjbModules() {
            return ejbModules;
        }
        
        public Set<String> getWebModules() {
            return webModules;
        }

        public String getLibDir() {
            return libDir;
        }
    }
}
