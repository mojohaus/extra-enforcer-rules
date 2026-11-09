package org.codehaus.mojo.extraenforcer.dependencies;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EarModuleClasspathTest {

    private MavenProject project;
    private Artifact artifact;
    private Build build;
    private EarModuleClasspath rule;
    private static int javaVersion;

    @BeforeAll
    public static void initClass() {
        javaVersion = javaMinor();
    }

    @BeforeEach
    public void initFields() {
        MavenSession session = mock(MavenSession.class);
        project = mock(MavenProject.class);
        when(session.getCurrentProject()).thenReturn(project);
        artifact = mock(Artifact.class);
        build = mock(Build.class);
        rule = new EarModuleClasspath(session);
    }

    @Test
    public void testExecuteWithWrongProjectType() throws Exception {

        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("jar");

        EnforcerRuleException exception = Assertions.assertThrows(EnforcerRuleException.class, () -> {
            rule.execute();
        });
        assertEquals("EarModuleClasspath rule can only be applied in EAR projects", exception.getMessage());
    }

    @Test
    public void testWithMissingEar() throws Exception {

        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("ear");
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/test-classes");
        when(build.getFinalName()).thenReturn("missing-ear");

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        Class<? extends Throwable> expectedCause =
                javaVersion == 8 ? FileNotFoundException.class : NoSuchFileException.class;
        assertEquals(expectedCause, exception.getCause().getClass());
    }

    private static int javaMinor() {
        String specString = System.getProperty("java.specification.version");
        Pattern versionPattern = Pattern.compile("(?<major>\\d+\\.)?(?<minor>\\d+)");
        Matcher m = versionPattern.matcher(specString);
        if (m.matches()) {
            return Integer.valueOf(m.group("minor"));
        }

        throw new IllegalStateException("Cannot parse Java version");
    }
}
