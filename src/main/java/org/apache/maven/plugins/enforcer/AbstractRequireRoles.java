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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that certain roles are defined for developers and contributors.
 *
 * @author Mirko Friedenhagen
 */
abstract class AbstractRequireRoles extends AbstractNonCacheableEnforcerRule
{

    /**
     * Specify the required roles for developers as comma separated list.
     */
    private String requiredRoles = "";

    /**
     * Execute the rule.
     *
     * @param helper the helper
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {

        MavenProject mavenProject = getMavenProject( helper );

        final Set<String> rolesFromProperties = getRolesFromString( requiredRoles );
        final Set<String> rolesFromMaven = rolesFromMaven( mavenProject );
        rolesFromProperties.removeAll( rolesFromMaven );

        if ( rolesFromProperties.size() > 0 )
        {
            final String message = String.format(
                    "Found undeclared role(s) '%s' for %s",
                    rolesFromProperties, getRoleName() );
            throw new EnforcerRuleException( message );
        }

    }

    /**
     * Returns the roles from Maven.
     *
     * @param mavenProject
     * @return roles from Maven.
     */
    protected abstract Set<String> rolesFromMaven( final MavenProject mavenProject );

    /**
     * Returns the rolename.
     *
     * @return rolename.
     */
    protected abstract String getRoleName();

    /**
     * Returns the set of required roles from the property.
     *
     * @param toSet
     * @return
     */
    Set<String> getRolesFromString( final String toSet )
    {
        final List<String> asList = Arrays.asList( StringUtils.split( toSet, "," ) );
        final Set<String> result = new HashSet<String>();
        for ( String role : asList )
        {
            result.add( role.trim() );
        }
        return result;
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

    // HELPER methods for unittests.
    /**
     * @param requiredRoles the requiredRoles to set.
     */
    void setRequiredRoles( String requiredRoles )
    {
        this.requiredRoles = requiredRoles;
    }
}