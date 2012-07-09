package org.apache.maven.plugins.enforcer;

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
import java.util.HashMap;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

/**
 *
 * @author mirko
 */
public class RequirePropertyDivergesTest
{

    final EnforcerRuleHelper helper = Mockito.mock( EnforcerRuleHelper.class );
    final RequirePropertyDiverges instance = new RequirePropertyDiverges();

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInChild() throws EnforcerRuleException
    {
        RequirePropertyDiverges mockInstance = createMockRule();
        final MavenProject project = createMavenProject( "company", "child" );
        setUpHelper( project, "childValue" );
        mockInstance.execute( helper );
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInParent() throws EnforcerRuleException
    {
        RequirePropertyDiverges mockInstance = createMockRule();
        final MavenProject project = createMavenProject( "company", "company-parent-pom" );
        final Build build = new Build();
        build.setPluginManagement( new PluginManagement() );
        final Plugin plugin = new Plugin();
        plugin.setGroupId( "org.apache.maven.plugins" );
        plugin.setArtifactId( "maven-enforcer-plugin" );
        final Xpp3Dom configuration = createPluginConfiguration();
        plugin.setConfiguration( configuration );
        build.addPlugin( plugin );
        project.getOriginalModel().setBuild( build );
        setUpHelper( project, "parentValue" );
        mockInstance.execute( helper );
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test
    public void testExecuteInParentWithConfigurationInPluginManagement() throws EnforcerRuleException
    {
        RequirePropertyDiverges mockInstance = createMockRule();
        final MavenProject project = createMavenProject( "company", "company-parent-pom" );
        final Build build = new Build();
        // create pluginManagement
        final Plugin pluginInManagement = new Plugin();
        pluginInManagement.setGroupId( "org.apache.maven.plugins" );
        pluginInManagement.setArtifactId( "maven-enforcer-plugin" );
        final Xpp3Dom configuration = createPluginConfiguration();
        pluginInManagement.setConfiguration( configuration );
        final PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.addPlugin( pluginInManagement );
        build.setPluginManagement( pluginManagement );
        // create plugins
        final Plugin pluginInPlugins = new Plugin();
        pluginInPlugins.setGroupId( "org.apache.maven.plugins" );
        pluginInPlugins.setArtifactId( "maven-enforcer-plugin" );
        build.addPlugin( pluginInPlugins );
        // add build
        project.getOriginalModel().setBuild( build );
        //project.getOriginalModel().setBuild( build );
        setUpHelper( project, "parentValue" );
        mockInstance.execute( helper );
    }

    @Test
    public void testProjectWithoutEnforcer()
    {
        final Build build = new Build();
        //build.setPluginManagement( new PluginManagement() );
        instance.getRuleConfigurations( build );
    }

    /**
     * Test of execute method, of class RequirePropertyDiverges.
     */
    @Test( expected = EnforcerRuleException.class )
    public void testExecuteInChildShouldFail() throws EnforcerRuleException
    {
        RequirePropertyDiverges mockInstance = createMockRule();
        final MavenProject project = createMavenProject( "company", "child" );
        setUpHelper( project, "parentValue" );
        mockInstance.execute( helper );
    }

    /**
     * Test of checkPropValueNotBlank method, of class RequirePropertyDiverges.
     */
    @Test
    public void testCheckPropValueNotBlank() throws Exception
    {
        instance.setProperty( "checkedProperty" );
        instance.checkPropValueNotBlank( "propertyValue" );
    }

    /**
     * Test of checkPropValueNotBlank method, of class RequirePropertyDiverges.
     */
    @Test( expected = EnforcerRuleException.class )
    public void testCheckPropValueNotBlankNull() throws EnforcerRuleException
    {
        instance.setProperty( "checkedProperty" );
        instance.checkPropValueNotBlank( null );
    }

    @Test
    public void testGetRuleName()
    {
        assertEquals( "requirePropertyDiverges", instance.getRuleName() );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testGetPropertyValueFail() throws ExpressionEvaluationException, EnforcerRuleException
    {
        when( helper.evaluate( "${checkedProperty}" ) ).thenThrow( ExpressionEvaluationException.class );
        instance.setProperty( "checkedProperty" );
        instance.getPropertyValue( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testGetProjectFail() throws ExpressionEvaluationException, EnforcerRuleException
    {
        when( helper.evaluate( "${project}" ) ).thenThrow( ExpressionEvaluationException.class );
        instance.getMavenProject( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testCheckAgainstParentValueFailing() throws EnforcerRuleException, ExpressionEvaluationException
    {
        testCheckAgainstParentValue( "company.parent-pom", "company.parent-pom" );
    }

    @Test
    public void testCheckAgainstParentValue() throws EnforcerRuleException, ExpressionEvaluationException
    {
        testCheckAgainstParentValue( "company.parent-pom", "company.project1" );
    }

    void testCheckAgainstParentValue( final String parentGroupId, final String childGroupId ) throws ExpressionEvaluationException, EnforcerRuleException
    {
        instance.setProperty( "project.groupId" );
        MavenProject parent = createMavenProject( parentGroupId, "parent-pom" );
        MavenProject project = createMavenProject( childGroupId, "child" );
        project.setParent( parent );
        when( helper.evaluate( "${project.parent.groupId}" ) ).thenReturn( parentGroupId );
        instance.checkAgainstParentValue( project, parent, helper, childGroupId );
    }

    static RequirePropertyDiverges createMockRule()
    {
        RequirePropertyDiverges instance = new RequirePropertyDiverges();
        instance.setRegex( "parentValue" );
        instance.setProperty( "checkedProperty" );
        return instance;

    }

    static MavenProject createMavenProject( final String groupId, final String artifactId )
    {
        final MavenProject project = new MavenProject();
        project.setGroupId( groupId );
        project.setArtifactId( artifactId );
        project.setOriginalModel( new Model() );
        return project;
    }

    void setUpHelper( final MavenProject project, final String propertyValue ) throws RuntimeException
    {
        try
        {
            Mockito.when( helper.evaluate( "${project}" ) ).thenReturn( project );
            Mockito.when( helper.evaluate( "${checkedProperty}" ) ).thenReturn( propertyValue );
        }
        catch ( ExpressionEvaluationException ex )
        {
            throw new RuntimeException( ex );
        }
        Mockito.when( helper.getLog() ).thenReturn( Mockito.mock( Log.class ) );
    }

    Xpp3Dom createPluginConfiguration()
    {
        final Xpp3Dom configuration = new Xpp3Dom( "configuration" );
        final Xpp3Dom rules = new Xpp3Dom( "rules" );
        final Xpp3Dom rule = new Xpp3Dom( instance.getRuleName() );
        rules.addChild( rule );
        final Xpp3Dom property = new Xpp3Dom( "property" );
        property.setValue( "checkedProperty" );
        rule.addChild( property );
        final Xpp3Dom regex = new Xpp3Dom( "regex" );
        regex.setValue( "parentValue" );
        rule.addChild( regex );
        configuration.addChild( rules );
        return configuration;
    }
}
