package org.codehaus.mojo.extraenforcer.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mirko
 */
@RunWith(MockitoJUnitRunner.class)
public class RequirePropertyDivergesTest {

    @Mock
    private ExpressionEvaluator evaluator;

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInChild() throws EnforcerRuleException {
        final MavenProject project = createMavenProject("company", "child");
        final MavenProject parent = createParentProject();
        project.setParent(parent);

        RequirePropertyDiverges mockInstance = createMockRule(project);
        setUpHelper(project, "childValue");
        mockInstance.execute();
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInParent() throws EnforcerRuleException {
        final MavenProject project = createParentProject();
        RequirePropertyDiverges mockInstance = createMockRule(project);
        setUpHelper(project, "parentValue");
        mockInstance.execute();
    }

    private MavenProject createParentProject() {
        final MavenProject project = createMavenProject("company", "company-parent-pom");
        final Build build = new Build();
        build.setPluginManagement(new PluginManagement());
        final Plugin plugin = newPlugin("org.apache.maven.plugins", "maven-enforcer-plugin", "1.0");
        final Xpp3Dom configuration = createPluginConfiguration();
        plugin.setConfiguration(configuration);
        build.addPlugin(plugin);
        project.getOriginalModel().setBuild(build);
        return project;
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInParentWithConfigurationInPluginManagement() throws EnforcerRuleException {
        final MavenProject project = createMavenProject("company", "company-parent-pom");
        RequirePropertyDiverges mockInstance = createMockRule(project);
        final Build build = new Build();
        // create pluginManagement
        final Plugin pluginInManagement = newPlugin("org.apache.maven.plugins", "maven-enforcer-plugin", "1.0");
        final Xpp3Dom configuration = createPluginConfiguration();
        pluginInManagement.setConfiguration(configuration);
        final PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.addPlugin(pluginInManagement);
        build.setPluginManagement(pluginManagement);
        // create plugins
        final Plugin pluginInPlugins = newPlugin("org.apache.maven.plugins", "maven-enforcer-plugin", "1.0");
        build.addPlugin(pluginInPlugins);
        // add build
        project.getOriginalModel().setBuild(build);
        // project.getOriginalModel().setBuild( build );
        setUpHelper(project, "parentValue");
        mockInstance.execute();
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInParentWithConfigurationInExecution() throws EnforcerRuleException {
        final MavenProject project = createMavenProject("company", "company-parent-pom");
        RequirePropertyDiverges mockInstance = createMockRule(project);
        final Build build = new Build();
        build.setPluginManagement(new PluginManagement());
        final Plugin plugin = newPlugin("org.apache.maven.plugins", "maven-enforcer-plugin", "1.0");
        final Xpp3Dom configuration = createPluginConfiguration();
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setConfiguration(configuration);
        plugin.addExecution(pluginExecution);
        build.addPlugin(plugin);
        project.getOriginalModel().setBuild(build);
        setUpHelper(project, "parentValue");
        mockInstance.execute();
    }

    @Test
    public void testProjectWithoutEnforcer() {
        final Build build = new Build();
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));

        // build.setPluginManagement( new PluginManagement() );
        instance.getRuleConfigurations(build);
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test(expected = EnforcerRuleException.class)
    public void testExecuteInChildShouldFail() throws EnforcerRuleException {
        final MavenProject project = createMavenProject("company", "child");
        RequirePropertyDiverges mockInstance = createMockRule(project);
        final MavenProject parent = createParentProject();
        project.setParent(parent);
        setUpHelper(project, "parentValue");
        mockInstance.execute();
    }

    /**
     * Test of checkPropValueNotBlank method, of class RequirePropertyDiverges.
     */
    @Test
    public void testCheckPropValueNotBlank() throws Exception {
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));
        instance.setProperty("checkedProperty");
        instance.checkPropValueNotBlank("propertyValue");
    }

    /**
     * Test of checkPropValueNotBlank method, of class RequirePropertyDiverges.
     */
    @Test(expected = EnforcerRuleException.class)
    public void testCheckPropValueNotBlankNull() throws EnforcerRuleException {
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));
        instance.setProperty("checkedProperty");
        instance.checkPropValueNotBlank(null);
    }

    @Test
    public void testCreateResultingErrorMessageReturningCustomMessage() {
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));
        instance.setProperty("checkedProperty");
        instance.setMessage("This is needed for foo.");
        final String actual = instance.createResultingErrorMessage("default message");
        final String expected = "Property 'checkedProperty' must be overridden:\nThis is needed for foo.";
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateResultingErrorMessageReturningDefaultMessage() {
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));

        instance.setProperty("checkedProperty");
        instance.setMessage(null);
        String actual = instance.createResultingErrorMessage("default message");
        String expected = "default message";
        assertEquals(expected, actual);

        instance.setMessage("");
        actual = instance.createResultingErrorMessage("default message");
        expected = "default message";
        assertEquals(expected, actual);
    }

    @Test
    public void testGetRuleName() {
        assertEquals("requirePropertyDiverges", RequirePropertyDiverges.getRuleName());
    }

    @Test(expected = EnforcerRuleException.class)
    public void testGetPropertyValueFail() throws ExpressionEvaluationException, EnforcerRuleException {
        RequirePropertyDiverges instance = createMockRule(mock(MavenProject.class));

        when(evaluator.evaluate("${checkedProperty}")).thenThrow(ExpressionEvaluationException.class);
        instance.setProperty("checkedProperty");
        instance.getPropertyValue();
    }

    @Test(expected = EnforcerRuleException.class)
    public void testCheckAgainstParentValueFailing() throws EnforcerRuleException, ExpressionEvaluationException {
        testCheckAgainstParentValue("company.parent-pom", "company.parent-pom");
    }

    @Test
    public void testCheckAgainstParentValue() throws EnforcerRuleException, ExpressionEvaluationException {
        testCheckAgainstParentValue("company.parent-pom", "company.project1");
    }

    void testCheckAgainstParentValue(final String parentGroupId, final String childGroupId)
            throws ExpressionEvaluationException, EnforcerRuleException {
        MavenProject project = createMavenProject(childGroupId, "child");
        RequirePropertyDiverges instance = createMockRule(project);

        instance.setProperty("project.groupId");
        MavenProject parent = createMavenProject(parentGroupId, "parent-pom");
        project.setParent(parent);
        when(evaluator.evaluate("${project.parent.groupId}")).thenReturn(parentGroupId);
        instance.checkAgainstParentValue(project, parent, childGroupId);
    }

    private RequirePropertyDiverges createMockRule(MavenProject project) {
        RequirePropertyDiverges instance = new RequirePropertyDiverges(project, evaluator);
        instance.setRegex("parentValue");
        instance.setProperty("checkedProperty");
        instance.setLog(mock(EnforcerLogger.class));
        return instance;
    }

    static MavenProject createMavenProject(final String groupId, final String artifactId) {
        final MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setOriginalModel(new Model());
        return project;
    }

    void setUpHelper(final MavenProject project, final String propertyValue) throws RuntimeException {
        //            when(helper.evaluate("${project}")).thenReturn(project);
        try {
            when(evaluator.evaluate("${checkedProperty}")).thenReturn(propertyValue);
        } catch (ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    Xpp3Dom createPluginConfiguration() {
        final Xpp3Dom configuration = new Xpp3Dom("configuration");
        final Xpp3Dom rules = new Xpp3Dom("rules");
        final Xpp3Dom rule = new Xpp3Dom(RequirePropertyDiverges.getRuleName());
        rules.addChild(rule);
        final Xpp3Dom property = new Xpp3Dom("property");
        property.setValue("checkedProperty");
        rule.addChild(property);
        final Xpp3Dom regex = new Xpp3Dom("regex");
        regex.setValue("parentValue");
        rule.addChild(regex);
        configuration.addChild(rules);
        return configuration;
    }

    static Plugin newPlugin(String groupId, String artifactId, String version) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        return plugin;
    }
}
