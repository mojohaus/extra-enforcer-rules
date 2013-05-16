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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * This rule checks that certain properties are set and diverge from the ones given in another project.
 *
 * This allows to enforce that a property is e.g. overridden in a child project.
 *
 * @author Mirko Friedenhagen
 * @since 1.0-alpha-3
 */
public class RequirePropertyDiverges extends AbstractNonCacheableEnforcerRule
{

    static final String MAVEN_ENFORCER_PLUGIN = "org.apache.maven.plugins:maven-enforcer-plugin";
    /**
     * Specify the required property. Must be given.
     */
    private String property = null;
    /**
     * Match the property value to a given regular expression. Defaults to value of defining project.
     */
    private String regex = null;
    private final String ruleName = StringUtils.lowercaseFirstLetter( getClass().getSimpleName() );

    /**
     * Execute the rule.
     *
     * @param helper the helper
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        final Log log = helper.getLog();

        Object propValue = getPropertyValue( helper );
        checkPropValueNotBlank( propValue );

        final MavenProject project = getMavenProject( helper );
        log.debug( getRuleName() + ": checking property '" + property + "' for project " + project );

        final MavenProject parent = findDefiningParent( project );

        // fail fast if the defining parent could not be found due to a bug in the rule
        if ( parent == null )
        {
            throw new IllegalStateException( "Failed to find parent POM which defines the current rule" );
        }

        if ( project.equals( parent ) )
        {
            log.debug( getRuleName() + ": skip for property '" + property + "' as " + project + " defines rule." );
        }
        else
        {
            log.debug( "Check configuration defined in " + parent );
            if ( regex == null )
            {
                checkAgainstParentValue( project, parent, helper, propValue );
            }
            else
            {
                checkAgainstRegex( propValue );
            }
        }
    }

    /**
     * Checks the value of the project against the one given in the defining ancestor project.
     *
     * @param project
     * @param parent
     * @param helper
     * @param propValue
     * @throws EnforcerRuleException
     */
    void checkAgainstParentValue( final MavenProject project, final MavenProject parent, EnforcerRuleHelper helper,
            Object propValue ) throws EnforcerRuleException
    {
        final StringBuilder parentHierarchy = new StringBuilder( "project." );
        MavenProject needle = project;
        while ( !needle.equals( parent ) )
        {
            parentHierarchy.append( "parent." );
            needle = needle.getParent();
        }
        final String propertyNameInParent = property.replace( "project.", parentHierarchy.toString() );
        Object parentValue = getPropertyValue( helper, propertyNameInParent );
        if ( propValue.equals( parentValue ) )
        {
            final String errorMessage = createResultingErrorMessage( String.format(
                    "Property '%s' evaluates to '%s'. This does match '%s' from parent %s",
                    property, propValue, parentValue, parent ) );
            throw new EnforcerRuleException( errorMessage );
        }
    }

    /**
     * Checks the value of the project against the given regex.
     *
     *
     * @param propValue
     * @throws EnforcerRuleException
     */
    void checkAgainstRegex( Object propValue ) throws EnforcerRuleException
    {
        // Check that the property does not match the regex.
        if ( propValue.toString().matches( regex ) )
        {
            final String errorMessage = createResultingErrorMessage(
                    String.format(
                    "Property '%s' evaluates to '%s'. This does match the regular expression '%s'",
                    property, propValue, regex ) );
            throw new EnforcerRuleException( errorMessage );
        }
    }

    /**
     * Finds the ancestor project which defines the rule.
     *
     * @param project to inspect
     * @return the defining ancestor project.
     */
    final MavenProject findDefiningParent( final MavenProject project )
    {
        final Xpp3Dom invokingRule = createInvokingRuleDom();
        MavenProject parent = project;
        while ( parent != null )
        {
            final Model model = parent.getOriginalModel();
            final Build build = model.getBuild();
            if ( build != null )
            {
                final List<Xpp3Dom> rules = getRuleConfigurations( build );
                if ( isDefiningProject( rules, invokingRule ) )
                {
                    break;
                }
            }
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * Creates a {@link Xpp3Dom} which corresponds to the configuration of the invocation.
     *
     * @return dom of the invoker.
     */
    Xpp3Dom createInvokingRuleDom()
    {
        Xpp3Dom ruleDom = new Xpp3Dom( getRuleName() );
        final Xpp3Dom propertyDom = new Xpp3Dom( "property" );
        propertyDom.setValue( property );
        ruleDom.addChild( propertyDom );
        if ( regex != null )
        {
            final Xpp3Dom regexDom = new Xpp3Dom( "regex" );
            regexDom.setValue( regex );
            ruleDom.addChild( regexDom );
        }
        if ( message != null ) 
        {
            final Xpp3Dom messageDom = new Xpp3Dom( "message" );
            messageDom.setValue( message );
            ruleDom.addChild( messageDom );
        }
        return ruleDom;
    }

    /**
     * Checks whether ruleDom is in the list of rules from the model.
     *
     * @param rulesFromModel
     * @param invokingRule
     * @return true when the rules contain the invoking rule.
     */
    final boolean isDefiningProject( final List<Xpp3Dom> rulesFromModel, final Xpp3Dom invokingRule )
    {
        for ( final Xpp3Dom rule : rulesFromModel )
        {
            if ( rule.equals( invokingRule ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the configuration name of the rule.
     *
     * @return configuration name.
     */
    final String getRuleName()
    {
        return ruleName;
    }

    /**
     * Returns the rule configurations from the <tt>pluginManagement</tt> as well
     * as the <tt>plugins</tt> section.
     *
     * @param build the build to inspect.
     * @return configuration of the rules, may be an empty list.
     */
    final List<Xpp3Dom> getRuleConfigurations( final Build build )
    {
        @SuppressWarnings( "unchecked" )
        final Map<String, Plugin> plugins = build.getPluginsAsMap();
        final List<Xpp3Dom> ruleConfigurationsForPlugins = getRuleConfigurations( plugins );
        final PluginManagement pluginManagement = build.getPluginManagement();
        if ( pluginManagement != null )
        {
            @SuppressWarnings( "unchecked" )
            final Map<String, Plugin> pluginsFromManagementAsMap = pluginManagement.getPluginsAsMap();
            List<Xpp3Dom> ruleConfigurationsFromManagement = getRuleConfigurations( pluginsFromManagementAsMap );
            ruleConfigurationsForPlugins.addAll( ruleConfigurationsFromManagement );
        }
        return ruleConfigurationsForPlugins;
    }

    /**
     * Returns the list of <tt>requirePropertyDiverges</tt> configurations from the map of plugins.
     *
     * @param plugins
     * @return list of requirePropertyDiverges configurations.
     */
    List<Xpp3Dom> getRuleConfigurations( final Map<String, Plugin> plugins )
    {
        if ( plugins.containsKey( MAVEN_ENFORCER_PLUGIN ) )
        {
            final List<Xpp3Dom> ruleConfigurations = new ArrayList<Xpp3Dom>();

            final Plugin enforcer = plugins.get( MAVEN_ENFORCER_PLUGIN );
            final Xpp3Dom configuration = ( Xpp3Dom ) enforcer.getConfiguration();

            // add rules from plugin configuration
            addRules( configuration, ruleConfigurations );

            // add rules from all plugin execution configurations
            for ( Object execution : enforcer.getExecutions() )
            {
                addRules( ( Xpp3Dom ) ( ( PluginExecution ) execution ).getConfiguration(), ruleConfigurations );
            }

            return ruleConfigurations;
        }
        else
        {
            return Collections.emptyList();
        }
    }

    /**
     * Add the rules found in the given configuration to the list of rule configurations.
     *
     * @param configuration
     *            configuration from which the rules are copied. May be <code>null</code>.
     * @param ruleConfigurations
     *            List to which the rules will be added.
     */
    private void addRules(final Xpp3Dom configuration, final List<Xpp3Dom> ruleConfigurations)
    {
        // may be null when rules are defined in pluginManagement during invocation
        // for plugin section and vice versa.
        if ( configuration != null )
        {
            final Xpp3Dom rules = configuration.getChild( "rules" );
            if ( rules != null )
            {
                ruleConfigurations.addAll( Arrays.asList( rules.getChildren( getRuleName() ) ) );
            }
        }
    }

    /**
     * Extracted for easier testability.
     *
     * @param helper
     * @return the value of the property.
     *
     * @throws EnforcerRuleException
     */
    Object getPropertyValue( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        return getPropertyValue( helper, property );
    }

    /**
     * Extracted for easier testability.
     *
     * @param helper
     * @param propertyName name of the property to extract.
     * @return the value of the property.
     * @throws EnforcerRuleException
     */
    Object getPropertyValue( EnforcerRuleHelper helper, final String propertyName ) throws EnforcerRuleException
    {
        try
        {
            return helper.evaluate( "${" + propertyName + "}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to evaluate property: " + propertyName, eee );
        }
    }

    /**
     * Extracted for easier testability.
     *
     * @param helper
     * @return the MavenProject enforcer is running on.
     *
     * @throws EnforcerRuleException
     */
    MavenProject getMavenProject( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        try
        {
            return ( MavenProject ) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to get project.", eee );
        }
    }

    /**
     * Checks that the property is not null or empty string
     *
     * @param propValue value of the property from the project.
     * @throws EnforcerRuleException
     */
    void checkPropValueNotBlank( Object propValue ) throws EnforcerRuleException
    {

        if ( propValue == null || StringUtils.isBlank( propValue.toString() ) )
        {
            throw new EnforcerRuleException( String.format(
                    "Property '%s' is required for this build and not defined in hierarchy at all.", property ) );
        }
    }

    /**
     * Either return the submitted errorMessage or replace it with the custom message set in the rule extended
     * by the property name.
     *
     * @param errorMessage
     * @return
     */
    String createResultingErrorMessage( String errorMessage )
    {
        if ( StringUtils.isNotEmpty( message ) )
        {
            return "Property '" + property + "' must be overridden:\n" + message;
        }
        else
        {
            return errorMessage;
        }
    }

    // HELPER methods for unittests.
    /**
     * @param property the property to set
     */
    void setProperty( String property )
    {
        this.property = property;
    }

    /**
     * @param regex the regex to set
     */
    void setRegex( String regex )
    {
        this.regex = regex;
    }
}
