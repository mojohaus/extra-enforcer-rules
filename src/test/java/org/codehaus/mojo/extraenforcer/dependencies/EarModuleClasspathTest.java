package org.codehaus.mojo.extraenforcer.dependencies;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Builds small EARs in memory; the {@code ear-module-classpath-*} integration tests cover the Maven wiring and an
 * EAR built by the maven-ear-plugin.
 */
class EarModuleClasspathTest {

    @TempDir
    Path dir;

    private final Map<String, byte[]> entries = new LinkedHashMap<>();

    private static String applicationXml(String modules) {
        return "<application xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"7\">" + modules + "</application>";
    }

    private static String ejb(String name) {
        return "<module><ejb>" + name + "</ejb></module>";
    }

    private static byte[] jar(String classPath) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (classPath != null) {
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JarOutputStream(out, manifest).close();
        return out.toByteArray();
    }

    private EarModuleClasspathTest add(String name, String content) {
        entries.put(name, content.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    private EarModuleClasspathTest module(String name, String classPath) throws IOException {
        entries.put(name, jar(classPath));
        return this;
    }

    private EarModuleClasspathTest descriptor(String modules) {
        return add("META-INF/application.xml", applicationXml(modules));
    }

    private File build() throws IOException {
        File ear = dir.resolve("app.ear").toFile();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(ear.toPath()))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return ear;
    }

    private static EarModuleClasspath rule(String packaging, File file) {
        MavenProject project = new MavenProject();
        project.setArtifactId("app");
        project.getModel().setPackaging(packaging);
        DefaultArtifact artifact =
                new DefaultArtifact("g", "app", "1", "compile", packaging, null, new DefaultArtifactHandler(packaging));
        artifact.setFile(file);
        project.setArtifact(artifact);
        EarModuleClasspath rule = new EarModuleClasspath(project);
        rule.setLog(mock(EnforcerLogger.class));
        return rule;
    }

    private String failureOf(File ear) {
        return assertThrows(EnforcerRuleException.class, rule("ear", ear)::execute)
                .getMessage();
    }

    @Test
    void skipsProjectsThatAreNotEars() {
        assertDoesNotThrow(rule("jar", null)::execute);
    }

    @Test
    void failsWithAHintWhenTheEarIsNotBuiltYet() {
        String message = failureOf(new File("target/missing.ear"));
        assertTrue(message.contains("package phase or later"), message);
    }

    @Test
    void toStringNamesTheRule() {
        assertEquals("EarModuleClasspath", rule("ear", null).toString());
    }

    @Test
    void passesWhenTheClassPathPointsToALibrary() throws Exception {
        descriptor(ejb("ejb.jar"))
                .module("ejb.jar", "lib/a.jar lib/b.jar")
                .module("lib/a.jar", null)
                .module("lib/b.jar", null);

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void passesWithoutClassPath() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", null);

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void failsForAMissingLibraryAndNamesModuleAndEntry() throws Exception {
        descriptor(ejb("ejb.jar"))
                .module("ejb.jar", "lib/a.jar lib/missing.jar")
                .module("lib/a.jar", null);

        String message = failureOf(build());

        assertTrue(message.contains("ejb.jar"), message);
        assertTrue(message.contains("lib/missing.jar"), message);
        assertTrue(message.contains("not in the EAR"), message);
        assertTrue(!message.contains("lib/a.jar"), message);
    }

    @Test
    void reportsEveryUnresolvedEntryOfEveryModule() throws Exception {
        descriptor(ejb("one.jar") + ejb("two.jar"))
                .module("one.jar", "lib/x.jar lib/y.jar")
                .module("two.jar", "lib/z.jar");

        String message = failureOf(build());

        assertTrue(message.contains("lib/x.jar"), message);
        assertTrue(message.contains("lib/y.jar"), message);
        assertTrue(message.contains("lib/z.jar"), message);
    }

    @Test
    void resolvesAnEntryRelativeToTheModuleNotToTheLibDirectory() throws Exception {
        // by the JAR specification a.jar next to ejb.jar is /a.jar, not /lib/a.jar
        descriptor(ejb("ejb.jar")).module("ejb.jar", "a.jar").module("lib/a.jar", null);
        assertTrue(failureOf(build()).contains("a.jar"));

        entries.put("a.jar", jar(null));
        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void resolvesAnEntryOfANestedModuleFromItsDirectory() throws Exception {
        descriptor(ejb("ejbs/x.jar")).module("ejbs/x.jar", "lib/a.jar").module("lib/a.jar", null);
        String message = failureOf(build());
        assertTrue(message.contains("ejbs/lib/a.jar"), message);

        entries.put("ejbs/x.jar", jar("../lib/a.jar"));
        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void failsForAnEntryThatLeavesTheEar() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", "../outside.jar");

        assertTrue(failureOf(build()).contains("outside the EAR"));
    }

    @Test
    void ignoresAbsoluteUrls() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", "http://example.org/a.jar /abs/b.jar");

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void toleratesExtraWhitespaceInTheClassPath() throws Exception {
        descriptor(ejb("ejb.jar"))
                .module("ejb.jar", "  lib/a.jar   lib/b.jar  ")
                .module("lib/a.jar", null);
        entries.put("lib/b.jar", jar(null));

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void decodesPercentEscapes() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", "lib/my%20lib.jar").module("lib/my lib.jar", null);

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void acceptsADirectoryThatExistsOnlyImplicitly() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", "conf/").module("conf/app.properties", null);

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void failsForAMissingDirectory() throws Exception {
        descriptor(ejb("ejb.jar")).module("ejb.jar", "conf/");

        assertTrue(failureOf(build()).contains("conf/"));
    }

    @Test
    void checksApplicationClientAndWebModules() throws Exception {
        descriptor("<module><java>client.jar</java></module>"
                        + "<module><web><web-uri>app.war</web-uri><context-root>/</context-root></web></module>")
                .module("client.jar", "lib/client-missing.jar")
                .module("app.war", "lib/web-missing.jar");

        String message = failureOf(build());

        assertTrue(message.contains("lib/client-missing.jar"), message);
        assertTrue(message.contains("lib/web-missing.jar"), message);
    }

    @Test
    void doesNotCheckLibrariesInTheLibraryDirectory() throws Exception {
        descriptor(ejb("ejb.jar") + "<library-directory/>")
                .module("ejb.jar", null)
                .module("lib/a.jar", "missing.jar");

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void readsModuleNamesSplitIntoSeveralTextEvents() throws Exception {
        // &#46; makes the SAX parser deliver "ejb", ".", "jar" in separate characters() calls
        descriptor(ejb("ejb&#46;jar")).module("ejb.jar", "lib/missing.jar");

        assertTrue(failureOf(build()).contains("lib/missing.jar"));
    }

    @Test
    void readsPrefixedElementNames() throws Exception {
        add(
                        "META-INF/application.xml",
                        "<j2ee:application xmlns:j2ee=\"http://java.sun.com/xml/ns/j2ee\" version=\"1.4\">"
                                + "<j2ee:module><j2ee:ejb>ejb.jar</j2ee:ejb></j2ee:module></j2ee:application>")
                .module("ejb.jar", "lib/missing.jar");

        assertTrue(failureOf(build()).contains("lib/missing.jar"));
    }

    @Test
    void checksTopLevelArchivesWithoutDescriptor() throws Exception {
        module("ejb.jar", "lib/missing.jar");

        assertTrue(failureOf(build()).contains("lib/missing.jar"));
    }

    @Test
    void skipsADeclaredModuleThatIsNotInTheEar() throws Exception {
        descriptor(ejb("gone.jar"));

        assertDoesNotThrow(rule("ear", build())::execute);
    }

    @Test
    void failsOnAMalformedDescriptor() throws Exception {
        add("META-INF/application.xml", "<application><module>").module("ejb.jar", null);

        String message = failureOf(build());

        assertTrue(message.contains("META-INF/application.xml"), message);
    }

    @Test
    void doesNotFetchTheDtdNorExternalEntities() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(500);
            String url = "http://127.0.0.1:" + server.getLocalPort();
            add(
                            "META-INF/application.xml",
                            "<?xml version=\"1.0\"?>\n"
                                    + "<!DOCTYPE application PUBLIC \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\""
                                    + " \"" + url + "/application_1_3.dtd\" [<!ENTITY x SYSTEM \"" + url + "/x\">]>\n"
                                    + "<application><module><ejb>&x;ejb.jar</ejb></module></application>")
                    .module("ejb.jar", "lib/missing.jar");

            // parsing proceeds: the module is still inspected and its dangling entry is reported
            assertTrue(failureOf(build()).contains("lib/missing.jar"));

            try (Socket connection = server.accept()) {
                throw new AssertionError("The parser connected to " + connection);
            } catch (SocketTimeoutException expected) {
                // nothing connected
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "ejb.jar|lib/a.jar|lib/a.jar",
                "ejbs/x.jar|lib/a.jar|ejbs/lib/a.jar",
                "ejbs/x.jar|../lib/a.jar|lib/a.jar",
                "ejbs/x.jar|./a.jar|ejbs/a.jar",
                "a/b/x.jar|../../c.jar|c.jar",
                "ejb.jar|conf/|conf/",
                "ejb.jar|.|''",
            })
    void resolve(String module, String path, String expected) {
        assertEquals("''".equals(expected) ? "" : expected, EarModuleClasspath.resolve(module, path));
    }

    @Test
    void resolveRejectsPathsAboveTheRoot() {
        assertNull(EarModuleClasspath.resolve("ejb.jar", "../a.jar"));
        assertNull(EarModuleClasspath.resolve("a/x.jar", "../../a.jar"));
    }
}
