package org.codehaus.mojo.extraenforcer.dependencies;

import java.io.File;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EarModuleClasspathTest {

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

    @Test
    void skipsProjectsThatAreNotEars() {
        assertDoesNotThrow(rule("jar", null)::execute);
    }

    @Test
    void failsWithAHintWhenTheEarIsNotBuiltYet() {
        EnforcerRuleException e =
                assertThrows(EnforcerRuleException.class, rule("ear", new File("target/missing.ear"))::execute);
        assertTrue(e.getMessage().contains("package phase or later"), e.getMessage());
    }
}
