package org.codehaus.mojo.extraenforcer.model;

import java.lang.reflect.Field;

import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The default regex and a configured one; the {@code require-project-url-regex} integration test covers reading the
 * rule and its {@code regex} from a POM.
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

    @Test
    void failsWhenUrlDoesNotMatchTheConfiguredRegex() throws Exception {
        MavenProject project = new MavenProject();
        project.setUrl("http://example.org");
        RequireProjectUrl rule = new RequireProjectUrl(project);
        setRegex(rule, "^http://localhost/.*");

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertEquals(
                "The project URL http://example.org does not match the required regex: ^http://localhost/.*",
                e.getMessage());
    }

    @Test
    void passesWhenUrlMatchesTheConfiguredRegex() throws Exception {
        MavenProject project = new MavenProject();
        project.setUrl("http://localhost/project");
        RequireProjectUrl rule = new RequireProjectUrl(project);
        setRegex(rule, "^http://localhost/.*");

        assertDoesNotThrow(rule::execute);
    }

    @Test
    void rejectsAnInvalidRegexAsAnError() throws Exception {
        MavenProject project = new MavenProject();
        project.setUrl("http://example.org");
        RequireProjectUrl rule = new RequireProjectUrl(project);
        setRegex(rule, "[");

        assertThrows(EnforcerRuleError.class, rule::execute);
    }

    private static void setRegex(RequireProjectUrl rule, String regex) throws Exception {
        Field field = RequireProjectUrl.class.getDeclaredField("regex");
        field.setAccessible(true);
        field.set(rule, regex);
    }
}
