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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that certain roles are filled.
 *
 * @author Mirko Friedenhagen
 * @since 1.0-alpha-3
 */
abstract class AbstractRequireRoles<T extends Contributor> extends AbstractNonCacheableEnforcerRule
{

    /**
     * Specify the required roles as comma separated list.
     */
    private String requiredRoles = "";

    /**
     * Specify the allowed roles as comma separated list. These are combined with the requiredRoles.
     */
    private String validRoles = "*";

    /**
     * Execute the rule.
     * 
     * @param helper the helper
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        MavenProject mavenProject = getMavenProject( helper );

        // Trying to prevent side-effects with unmodifiable sets (already got burned)
        final Set<String> requiredRolesSet = Collections.unmodifiableSet( getRolesFromString( requiredRoles ) );
        final Set<String> rolesFromProject = Collections.unmodifiableSet(getRolesFromProject( mavenProject ));

        checkRequiredRoles( requiredRolesSet, rolesFromProject );
        checkValidRoles( requiredRolesSet, rolesFromProject );
    }

    private void checkRequiredRoles( final Set<String> requiredRolesSet, final Set<String> rolesFromProject )
        throws EnforcerRuleException
    {
        final Set<String> copyOfRequiredRolesSet = new LinkedHashSet<String>( requiredRolesSet );
        copyOfRequiredRolesSet.removeAll( rolesFromProject );
        if ( copyOfRequiredRolesSet.size() > 0 )
        {
            final String message =
                String.format( "Found no %s representing role(s) '%s'", getRoleName(), copyOfRequiredRolesSet );
            throw new EnforcerRuleException( message );
        }
    }

    private void checkValidRoles( final Set<String> requiredRolesSet, final Set<String> rolesFromProject )
        throws EnforcerRuleException
    {
        final Set<String> copyOfRolesFromProject = new LinkedHashSet<String>(rolesFromProject); 
        final Set<String> allowedRoles = getRolesFromString( validRoles );
        if ( !allowedRoles.contains( "*" ) )
        {
            allowedRoles.addAll( requiredRolesSet );

            // results in invalid roles
            copyOfRolesFromProject.removeAll( allowedRoles );
            if ( copyOfRolesFromProject.size() > 0 )
            {
                final String message = String.format( "Found invalid %s role(s) '%s'", getRoleName(), copyOfRolesFromProject );
                throw new EnforcerRuleException( message );
            }
        }
    }

    /**
     * Returns the roles from the POM.
     *
     * @param mavenProject
     * @return roles from POM.
     */
    @SuppressWarnings( "unchecked" )
    final Set<String> getRolesFromProject( MavenProject mavenProject )
    {
        final Set<String> result = new HashSet<String>();
        for ( final T roleFromPom : getRoles( mavenProject ) )
        {
            List<String> roles = roleFromPom.getRoles();
            for ( String role : roles )
            {
                result.add( role );
            }
        }
        return result;
    }


    /**
     * Returns the rolename.
     *
     * @return rolename.
     */
    protected abstract String getRoleName();

    /**
     * Returns the roles from the POM.
     *
     * @param mavenProject
     * @return the list of {@link Contributor}s or {@link Developer}s.
     */
    protected abstract List<T> getRoles( final MavenProject mavenProject );

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
    
    void setValidRoles( String validRoles )
    {
        this.validRoles = validRoles;
    }
}