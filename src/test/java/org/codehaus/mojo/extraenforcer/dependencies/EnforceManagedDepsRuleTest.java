package org.codehaus.mojo.extraenforcer.dependencies;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * The rule reads the original model, so profiles count whether or not they are active. The
 * {@code unmanaged-dep-version-nofail} integration test covers {@code level=WARN}, which only Maven can configure.
 */
class EnforceManagedDepsRuleTest {

    private final Model model = new Model();

    @Test
    void passesWhenTheVersionIsManaged() {
        manage("junit", "junit", "3.8.1");
        model.addDependency(dependency("junit", "junit", null));

        assertDoesNotThrow(rule(false)::execute);
    }

    @Test
    void passesWhenAProfileDependencyIsManaged() {
        manage("junit", "junit", "3.8.1");
        model.addDependency(dependency("junit", "junit", null));
        model.addProfile(profile(dependency("junit", "junit", null)));

        assertDoesNotThrow(rule(true)::execute);
    }

    @Test
    void failsOnADirectVersion() {
        manage("junit", "junit", "3.8.1");
        model.addDependency(dependency("junit", "junit", "3.8.1"));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule(false)::execute);
        assertEquals(
                "The following 1 dependencies are NOT using a managed version:" + System.lineSeparator()
                        + System.lineSeparator() + "  - junit:junit:jar",
                e.getMessage());
    }

    @Test
    void failsOnAVersionInAnInactiveProfile() {
        manage("junit", "junit", "3.8.1");
        model.addDependency(dependency("junit", "junit", null));
        model.addProfile(profile(dependency("localhost", "commons-lang", "2.5")));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule(true)::execute);
        assertEquals(
                "The following 1 dependencies are NOT using a managed version:" + System.lineSeparator()
                        + System.lineSeparator() + "  - localhost:commons-lang:jar",
                e.getMessage());
    }

    @Test
    void ignoresProfilesWhenToldTo() {
        model.addProfile(profile(dependency("localhost", "commons-lang", "2.5")));

        assertDoesNotThrow(rule(false)::execute);
    }

    @Test
    void ignoresDependenciesMatchingRegexIgnored() {
        model.addDependency(dependency("junit", "junit", "3.8.1"));
        EnforceManagedDepsRule rule = rule(false);
        rule.setRegexIgnored(new String[] {"junit"});

        assertDoesNotThrow(rule::execute);
    }

    private EnforceManagedDepsRule rule(boolean checkProfiles) {
        MavenProject project = new MavenProject();
        project.setOriginalModel(model);
        EnforceManagedDepsRule rule = new EnforceManagedDepsRule(project);
        rule.setLog(mock(EnforcerLogger.class));
        rule.setCheckProfiles(checkProfiles);
        return rule;
    }

    private void manage(String groupId, String artifactId, String version) {
        DependencyManagement management = new DependencyManagement();
        management.addDependency(dependency(groupId, artifactId, version));
        model.setDependencyManagement(management);
    }

    private static Profile profile(Dependency dependency) {
        Profile profile = new Profile();
        profile.setId("never-used");
        profile.addDependency(dependency);
        return profile;
    }

    private static Dependency dependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }
}
