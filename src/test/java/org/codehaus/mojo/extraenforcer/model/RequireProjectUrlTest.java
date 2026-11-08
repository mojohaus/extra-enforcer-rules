package org.codehaus.mojo.extraenforcer.model;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The default regex; the {@code require-project-url-regex} integration test covers a configured one.
 */
class RequireProjectUrlTest {

    @Test
    void failsWithoutUrl() {
        RequireProjectUrl rule = new RequireProjectUrl(new MavenProject());

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertEquals("The project URL is not defined", e.getMessage());
    }

    @Test
    void passesWithUrl() {
        MavenProject project = new MavenProject();
        project.setUrl("http://example.org");

        assertDoesNotThrow(new RequireProjectUrl(project)::execute);
    }
}
