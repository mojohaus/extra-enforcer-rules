package org.codehaus.mojo.extraenforcer.encoding;

import java.io.File;
import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void regexInIncludesIsNotSplitAtPipes() throws Exception {
        configureUtf8();

        rule.setIncludes("%regex[(ascii|utf8)\\.txt]");

        assertThrows(EnforcerRuleException.class, () -> rule.execute());
    }

    @Test
    void regexInExcludesIsNotSplitAtPipes() throws Exception {
        configureUtf8();

        rule.setIncludes("*.txt");
        rule.setExcludes("%regex[(ascii|iso88591)\\.txt]");

        assertDoesNotThrow(() -> rule.execute());
    }

    @Test
    void globAndRegexCanBeMixed() throws Exception {
        configureUtf8();

        rule.setIncludes("utf8.txt,%regex[ascii\\.txt]");

        assertThrows(EnforcerRuleException.class, () -> rule.execute());
    }

    @Test
    void splitPatternsKeepsBracketedSeparators() {
        assertArrayEquals(new String[] {"a", "b", "c"}, RequireEncoding.splitPatterns("a,b|c"));
        assertArrayEquals(
                new String[] {"*.txt", "%regex[.*\\.(a|b)]", "x"},
                RequireEncoding.splitPatterns("*.txt,%regex[.*\\.(a|b)]|x"));
        assertArrayEquals(new String[] {"%regex[[a,b]|c]"}, RequireEncoding.splitPatterns("%regex[[a,b]|c]"));
        assertArrayEquals(new String[] {"a\\,b", "c"}, RequireEncoding.splitPatterns("a\\,b,c"));
        assertArrayEquals(new String[] {"a", "", "b"}, RequireEncoding.splitPatterns("a,,b,"));
        assertArrayEquals(new String[0], RequireEncoding.splitPatterns(""));
    }

    private void configureUtf8() {
        when(project.getBasedir()).thenReturn(new File("src/test/resources").getAbsoluteFile());

        Properties properties = new Properties();
        properties.put("project.build.sourceEncoding", "UTF-8");
        when(project.getProperties()).thenReturn(properties);
    }
}
