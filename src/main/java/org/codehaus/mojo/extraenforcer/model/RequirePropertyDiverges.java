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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * This rule checks that certain properties are set and diverge from the ones given in another project.
 * This allows to enforce that a property is e.g. overridden in a child project.
 *
 * @author Mirko Friedenhagen
 * @since 1.0-alpha-3
 */
@Named("requirePropertyDiverges")
public class RequirePropertyDiverges extends AbstractEnforcerRule {
    private String message;

    static final String MAVEN_ENFORCER_PLUGIN = "org.apache.maven.plugins:maven-enforcer-plugin";

    /**
     * Specify the required property. Must be given.
     */
    private String property = null;

    /**
     * Match the property value to a given regular expression. Defaults to value of defining project.
     */
    private String regex = null;

    private String reference = "DEFINING";

    private static final String RULE_NAME =
            StringUtils.lowercaseFirstLetter(RequirePropertyDiverges.class.getSimpleName());

    private final MavenProject project;

    private final ExpressionEvaluator evaluator;

    @Inject
    public RequirePropertyDiverges(MavenProject project, ExpressionEvaluator evaluator) {
        this.project = project;
        this.evaluator = evaluator;
    }

    /**
     * Execute the rule.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute() throws EnforcerRuleException {

        Object propValue = getPropertyValue();
        checkPropValueNotBlank(propValue);

        ParentReference parentReference = getParentReference();

        getLog().debug(() -> ruleName() + ": checking property '" + property + "' for project " + project);

        final MavenProject parent = findParent(project, parentReference);

        // fail fast if the reference parent could not be found due to a bug in the rule
        if (parent == null) {
            throw new IllegalStateException("Failed to find reference POM which defines the current value");
        }

        if (project.equals(parent)) {
            getLog().debug(() ->
                    ruleName() + ": skip for property '" + property + "' as " + project + " defines rule.");
        } else {
            getLog().debug(() -> "Check configuration defined in " + parent);
            if (regex == null) {
                checkAgainstParentValue(project, parent, propValue);
            } else {
                checkAgainstRegex(propValue);
            }
        }
    }

    /**
     * Checks the value of the project against the one given in the reference ancestor project.
     *
     * @param project
     * @param parent
     * @param propValue
     * @throws EnforcerRuleException
     */
    void checkAgainstParentValue(final MavenProject project, final MavenProject parent, Object propValue)
            throws EnforcerRuleException {
        final StringBuilder parentHierarchy = new StringBuilder("project.");
        MavenProject needle = project;
        while (!needle.equals(parent)) {
            parentHierarchy.append("parent.");
            needle = needle.getParent();
        }
        final String propertyNameInParent = property.replace("project.", parentHierarchy.toString());
        Object parentValue = getPropertyValue(propertyNameInParent);
        if (propValue.equals(parentValue)) {
            final String errorMessage = createResultingErrorMessage(String.format(
                    "Property '%s' evaluates to '%s'. This does match '%s' from parent %s",
                    property, propValue, parentValue, parent));
            throw new EnforcerRuleException(errorMessage);
        }
    }

    /**
     * Checks the value of the project against the given regex.
     *
     * @param propValue
     * @throws EnforcerRuleException
     */
    void checkAgainstRegex(Object propValue) throws EnforcerRuleException {
        // Check that the property does not match the regex.
        if (propValue.toString().matches(regex)) {
            final String errorMessage = createResultingErrorMessage(String.format(
                    "Property '%s' evaluates to '%s'. This does match the regular expression '%s'",
                    property, propValue, regex));
            throw new EnforcerRuleException(errorMessage);
        }
    }

    /**
     * Finds the ancestor project which defines the reference value.
     *
     * @param project to inspect
     * @param reference project to diverge from
     * @return the defining ancestor project.
     */
    final MavenProject findParent(final MavenProject project, ParentReference reference) {
        switch (reference) {
            case BASE:
                return findBaseParent(project);
            case DEFINING:
                return findDefiningParent(project);
            case PARENT:
                return findDirectParent(project);
            default:
                throw new IllegalArgumentException("Unhandled ParentReference: " + reference.name());
        }
    }

    /**
     * Finds the ancestor project which defines the rule.
     *
     * @param project to inspect
     * @return the defining ancestor project.
     */
    final MavenProject findDefiningParent(final MavenProject project) {
        final Xpp3Dom invokingRule = createInvokingRuleDom();
        MavenProject parent = project;
        while (parent != null) {
            final Model model = parent.getOriginalModel();
            final Build build = model.getBuild();
            if (build != null) {
                final List<Xpp3Dom> rules = getRuleConfigurations(build);
                if (isDefiningProject(rules, invokingRule)) {
                    break;
                }
            }
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * Finds the direct ancestor project.
     *
     * @param project to inspect
     * @return the top-most local project or current project if it has no parent.
     *
     */
    final MavenProject findDirectParent(MavenProject project) {
        MavenProject parent = project.getParent();
        return parent == null ? project : parent;
    }

    /**
     * Finds the top-most local ancestor project.
     *
     * @param project to inspect
     * @return the top-most local project.
     */
    final MavenProject findBaseParent(final MavenProject project) {
        MavenProject current = project;
        while (current.getParentFile() != null) {
            current = project.getParent();
        }
        return current;
    }

    /**
     * Creates a {@link Xpp3Dom} which corresponds to the configuration of the invocation.
     *
     * @return dom of the invoker.
     */
    Xpp3Dom createInvokingRuleDom() {
        return new CreateInvokingRuleDom(this).getRuleDom();
    }

    /**
     * Checks whether ruleDom is in the list of rules from the model.
     *
     * @param rulesFromModel
     * @param invokingRule
     * @return true when the rules contain the invoking rule.
     */
    final boolean isDefiningProject(final List<Xpp3Dom> rulesFromModel, final Xpp3Dom invokingRule) {
        for (final Xpp3Dom rule : rulesFromModel) {
            // compare string representation as with Maven4 3ba8f2d equals does not work as before.
            if (rule.toString().equals(invokingRule.toString())) {
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
    static String ruleName() {
        return RULE_NAME;
    }

    /**
     * Returns the rule configurations from the <tt>pluginManagement</tt> as well
     * as the <tt>plugins</tt> section.
     *
     * @param build the build to inspect.
     * @return configuration of the rules, may be an empty list.
     */
    final List<Xpp3Dom> getRuleConfigurations(final Build build) {
        final Map<String, Plugin> plugins = build.getPluginsAsMap();
        final List<Xpp3Dom> ruleConfigurationsForPlugins = getRuleConfigurations(plugins);
        final PluginManagement pluginManagement = build.getPluginManagement();
        if (pluginManagement != null) {
            final Map<String, Plugin> pluginsFromManagementAsMap = pluginManagement.getPluginsAsMap();
            List<Xpp3Dom> ruleConfigurationsFromManagement = getRuleConfigurations(pluginsFromManagementAsMap);
            ruleConfigurationsForPlugins.addAll(ruleConfigurationsFromManagement);
        }
        return ruleConfigurationsForPlugins;
    }

    /**
     * Returns the list of <tt>requirePropertyDiverges</tt> configurations from the map of plugins.
     *
     * @param plugins
     * @return list of requirePropertyDiverges configurations.
     */
    List<Xpp3Dom> getRuleConfigurations(final Map<String, Plugin> plugins) {
        if (plugins.containsKey(MAVEN_ENFORCER_PLUGIN)) {
            final List<Xpp3Dom> ruleConfigurations = new ArrayList<>();

            final Plugin enforcer = plugins.get(MAVEN_ENFORCER_PLUGIN);
            final Xpp3Dom configuration = (Xpp3Dom) enforcer.getConfiguration();

            // add rules from plugin configuration
            addRules(configuration, ruleConfigurations);

            // add rules from all plugin execution configurations
            for (PluginExecution execution : enforcer.getExecutions()) {
                addRules((Xpp3Dom) execution.getConfiguration(), ruleConfigurations);
            }

            return ruleConfigurations;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Add the rules found in the given configuration to the list of rule configurations.
     *
     * @param configuration      configuration from which the rules are copied. May be <code>null</code>.
     * @param ruleConfigurations List to which the rules will be added.
     */
    private void addRules(final Xpp3Dom configuration, final List<Xpp3Dom> ruleConfigurations) {
        // may be null when rules are defined in pluginManagement during invocation
        // for plugin section and vice versa.
        if (configuration != null) {
            final Xpp3Dom rules = configuration.getChild("rules");
            if (rules != null) {
                final List<Xpp3Dom> originalListFromPom = Arrays.asList(rules.getChildren(ruleName()));
                ruleConfigurations.addAll(createRuleListWithNameSortedChildren(originalListFromPom));
            }
        }
    }

    /**
     * As Xpp3Dom is very picky about the order of children while comparing, create a new list where the children
     * are added in alphabetical order. See <a href="https://jira.codehaus.org/browse/MOJO-1931">MOJO-1931</a>.
     *
     * @param originalListFromPom order not specified
     * @return a list where children's member are alphabetically sorted.
     */
    private List<Xpp3Dom> createRuleListWithNameSortedChildren(final List<Xpp3Dom> originalListFromPom) {
        final List<Xpp3Dom> listWithSortedEntries = new ArrayList<>(originalListFromPom.size());
        for (Xpp3Dom unsortedXpp3Dom : originalListFromPom) {
            final Xpp3Dom sortedXpp3Dom = new Xpp3Dom(ruleName());
            final SortedMap<String, Xpp3Dom> childrenMap = new TreeMap<>();
            final Xpp3Dom[] children = unsortedXpp3Dom.getChildren();
            for (Xpp3Dom child : children) {
                childrenMap.put(child.getName(), child);
            }
            for (Xpp3Dom entry : childrenMap.values()) {
                sortedXpp3Dom.addChild(entry);
            }
            listWithSortedEntries.add(sortedXpp3Dom);
        }
        return listWithSortedEntries;
    }

    /**
     * Extracted for easier testability.
     *
     * @return the value of the property.
     * @throws EnforcerRuleException
     */
    Object getPropertyValue() throws EnforcerRuleException {
        return getPropertyValue(property);
    }

    /**
     * Extracted for easier testability.
     *
     * @param propertyName name of the property to extract.
     * @return the value of the property.
     * @throws EnforcerRuleException
     */
    Object getPropertyValue(final String propertyName) throws EnforcerRuleException {
        try {
            return evaluator.evaluate("${" + propertyName + "}");
        } catch (ExpressionEvaluationException eee) {
            throw new EnforcerRuleError("Unable to evaluate property: " + propertyName, eee);
        }
    }

    /**
     * Checks that the property is not null or empty string
     *
     * @param propValue value of the property from the project.
     * @throws EnforcerRuleException
     */
    void checkPropValueNotBlank(Object propValue) throws EnforcerRuleException {

        if (propValue == null || StringUtils.isBlank(propValue.toString())) {
            throw new EnforcerRuleException(String.format(
                    "Property '%s' is required for this build and not defined in hierarchy at all.", property));
        }
    }

    /**
     * Extracted for easier testability.
     *
     * @return the ParentReference constant.
     * @throws EnforcerRuleException if null or no ParentReference matches (case-insensitively)
     */
    ParentReference getParentReference() throws EnforcerRuleException {
        return getParentReference(reference);
    }

    /**
     * Extracted for easier testability.
     *
     * @param parentReferenceName name of the ParentReference.
     * @return the ParentReference enum constant.
     * @throws EnforcerRuleException if null or no ParentReference matches (case-insensitively)
     */
    ParentReference getParentReference(String parentReferenceName) throws EnforcerRuleException {
        try {
            return ParentReference.valueOf(StringUtils.upperCase(StringUtils.strip(parentReferenceName)));
        } catch (IllegalArgumentException iae) {
            throw new EnforcerRuleError("Unknown parent reference value: " + parentReferenceName, iae);
        }
    }

    /**
     * Either return the submitted errorMessage or replace it with the custom message set in the rule extended
     * by the property name.
     *
     * @param errorMessage
     * @return
     */
    String createResultingErrorMessage(String errorMessage) {
        if (StringUtils.isNotEmpty(message)) {
            return "Property '" + property + "' must be overridden:\n" + message;
        } else {
            return errorMessage;
        }
    }

    // HELPER methods for unittests.

    /**
     * @param property the property to set
     */
    void setProperty(String property) {
        this.property = property;
    }

    /**
     * @param regex the regex to set
     */
    void setRegex(String regex) {
        this.regex = regex;
    }

    /**
     * @param message the message to set
     */
    void setMessage(String message) {
        this.message = message;
    }

    /**
     * @param reference the reference to set
     */
    void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Creates the DOM of the invoking rule, but returns the children alphabetically sorted.
     */
    private static class CreateInvokingRuleDom {

        private final Xpp3Dom ruleDom;

        private final SortedMap<String, Xpp3Dom> map = new TreeMap<>();

        /** Real work is done in the constructor */
        CreateInvokingRuleDom(RequirePropertyDiverges rule) {
            ruleDom = new Xpp3Dom(ruleName());
            addToMapWhenNotNull(rule.property, "property");
            addToMapWhenNotNull(rule.message, "message");
            addToMapWhenNotNull(rule.regex, "regex");
            addChildrenToRuleDom();
        }

        /**
         * Readily prepared in constructor.
         *
         * @return the ruleDom
         */
        public Xpp3Dom getRuleDom() {
            return ruleDom;
        }

        private void addToMapWhenNotNull(String member, final String memberName) {
            if (member != null) {
                final Xpp3Dom memberDom = new Xpp3Dom(memberName);
                memberDom.setValue(member);
                map.put(memberName, memberDom);
            }
        }

        private void addChildrenToRuleDom() {
            for (Xpp3Dom entry : map.values()) {
                ruleDom.addChild(entry);
            }
        }
    }

    enum ParentReference {
        /**
         * The top-most local project (i.e.: the highest parent whose POM does not come from a repository)
         */
        BASE,
        /**
         * The parent where the current requirePropertyDiverges rule is defined
         */
        DEFINING,
        /**
         * The immediate parent (same as ${project.parent})
         */
        PARENT
    }
}
