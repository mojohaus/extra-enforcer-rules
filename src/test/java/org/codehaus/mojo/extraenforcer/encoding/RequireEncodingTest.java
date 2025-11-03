package org.codehaus.mojo.extraenforcer.encoding;

import java.io.File;
import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequireEncodingTest {

    private RequireEncoding rule;

    private MavenProject project;

    @BeforeEach
    void initFields() {
        project = mock(MavenProject.class);
        rule = new RequireEncoding(project);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void failUTF8() throws Exception {

        when(project.getBasedir()).thenReturn(new File("src/test/resources").getAbsoluteFile());

        Properties properties = new Properties();
        properties.put("project.build.sourceEncoding", "UTF-8");
        when(project.getProperties()).thenReturn(properties);

        rule.setIncludes("ascii.txt");

        assertThrows(EnforcerRuleException.class, () -> rule.execute());
    }
}
