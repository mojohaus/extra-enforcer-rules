package org.apache.maven.plugins.enforcer;

import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EarModuleClasspathTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private EnforcerRuleHelper helper;
    private MavenProject project;
    private Artifact artifact;
    private Build build;
    private EarModuleClasspath rule;
    private static int javaVersion;

    @BeforeClass
    public static void initClass() {
        javaVersion = javaMinor(); 
    }
    
    @Before
    public void initFields() {
        helper = mock(EnforcerRuleHelper.class);
        project = mock(MavenProject.class);
        artifact = mock(Artifact.class);
        build = mock(Build.class);
        rule = new EarModuleClasspath();
    }
    
    @Test
    public void testExecuteWithWrongProjectType() throws Exception {
        
        when(helper.evaluate("${project}")).thenReturn(project);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("jar");
        
        exception.expectMessage("EarModuleClasspath rule can only be applied in EAR projects");
        rule.execute(helper);
    }
    
    @Test
    public void testWithMissingEar() throws Exception {
        
        when(helper.evaluate("${project}")).thenReturn(project);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("ear");
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/test-classes");
        when(build.getFinalName()).thenReturn("missing-ear");
        
        exception.expect(EnforcerRuleException.class);
        // java 1.5 and 1.6 throw a ZipException instead of a FileNotFoundException
        if (javaVersion > 6) {
            exception.expectCause(isA(FileNotFoundException.class));
        } else {
            exception.expectCause(isA(ZipException.class));
        }
        rule.execute(helper);
    }
    
    /**
     * This EAR file has no classpath in hte EJB's manifest.
     * The test should succeed.
     */
    @Test
    public void testEarWithoutLibFolder() throws Exception {
        when(helper.evaluate("${project}")).thenReturn(project);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("ear");
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/test-classes");
        when(build.getFinalName()).thenReturn("test-ear-no-lib");
        
        rule.execute(helper);
    }
    
    /**
     * This EAR's EJB have a Class-Path manifest entry  
     * @throws Exception
     */
    @Test
    public void testEarWithLibInClasspath() throws Exception {
        when(helper.evaluate("${project}")).thenReturn(project);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("ear");
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/test-classes");
        when(build.getFinalName()).thenReturn("test-ear-lib-in-classpath");
        
        rule.execute(helper);
    }

    /**
     * This EAR's EJB have a Class-Path manifest entry  
     * @throws Exception
     */
    @Test
    public void testEarNoLibInClasspath() throws Exception {
        when(helper.evaluate("${project}")).thenReturn(project);
        when(project.getArtifact()).thenReturn(artifact);
        when(artifact.getType()).thenReturn("ear");
        when(project.getBuild()).thenReturn(build);
        when(build.getDirectory()).thenReturn("target/test-classes");
        when(build.getFinalName()).thenReturn("test-ear-no-lib-in-classpath");
        
        rule.execute(helper);
    }
    
    private static int javaMinor() {
        String specString = System.getProperty("java.specification.version");
        Pattern versionPattern = Pattern.compile("\\d+\\.(\\d+)");
        Matcher m = versionPattern.matcher(specString);
        if (m.matches()) {
            return Integer.valueOf(m.group(1));
        }
        
        throw new IllegalStateException("Cannot parse Java version");
    }
}
