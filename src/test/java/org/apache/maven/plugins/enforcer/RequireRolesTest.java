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
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Mirko Friedenhagen
 */
public class RequireRolesTest
{

    final EnforcerRuleHelper helper = Mockito.mock( EnforcerRuleHelper.class );

    @Test
    public void shouldSucceedBecauseArchitectAsDeveloperAndBusinessEngineerAsContributorArePresent() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireDeveloperRoles( "architect" /*required role*/, null /* valid roles not needed */).execute( helper );
        newRequireContributorRoles( "business engineer" /*required role*/, "*" /* valid roles */).execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void shouldFailBecauseContributorWithRoleQualityManagerIsMissing() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireContributorRoles( "business engineer, quality manager", null ).execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void shouldFailBecauseDeveloperWithRoleCodeMonkeyIsMissing() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireDeveloperRoles( "codemonkey" /* required but not in project */, 
                                  null ).execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void shouldFailBecauseContributorRoleBusinessEngineerIsInvalid() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireContributorRoles( null /* no required roles needed */, 
                                    "hacker" /* only valid role */).execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void shouldFailBecauseNoContributorRolesAtAllAreValid() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireContributorRoles( null /* no required roles needed */, 
                                    "" /*but no role is valid at all */).execute( helper );
    }

    @Test
    public void shouldSucceedAsNoRolesAreRequiredAndAllAreAccepted() throws Exception
    {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireContributorRoles( null /* no required roles */, 
                                    "*" /* any role is valid */).execute( helper );
        newRequireContributorRoles( null /* no required roles */, 
                                    null /* any role is valid */).execute( helper );
    }

    /**
     * Test of getRolesFromString method, of class AbstractRequireRoles.
     */
    @Test
    public void testGetRolesFromString()
    {
        HashSet<String> expResult = new HashSet<String>( Arrays.asList( "architect", "codemonkey", "business engineer" ) );
        final RequireContributorRoles sut = new RequireContributorRoles();
        Set<String> result = sut.getRolesFromString( " architect,  business engineer   , codemonkey " );
        assertEquals( expResult, result );
    }

    /**
     * Test of getRolesFromMaven method, of class AbstractRequireRoles.
     */
    @Test
    public void testGetRolesFromMaven()
    {
        HashSet<String> expResult = new HashSet<String>( Arrays.asList( 
                "quality manager", "product owner", "business engineer" ) );
        final Contributor singleHero = new Contributor();
        singleHero.addRole( "quality manager" );
        singleHero.addRole( "business engineer" );
        singleHero.addRole( "product owner" );
        List<Contributor> listFromMaven = Arrays.asList( singleHero );
        final HashSet<String> result = new HashSet<String>();
        for ( final Contributor contributor : listFromMaven )
        {
            @SuppressWarnings( "unchecked" )
            List<String> roles = contributor.getRoles();
            for ( String role : roles )
            {
                result.add( role );
            }
        }
        assertEquals( expResult, result );
    }

    private void addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper() throws Exception
    {
        final MavenProject mavenProject = new MavenProject();
        final Developer developer = new Developer();
        developer.addRole( "architect" );
        mavenProject.addDeveloper( developer );
        final Contributor contributor = new Contributor();
        contributor.addRole( "business engineer" );
        mavenProject.addContributor( contributor );
        Mockito.when( helper.evaluate( "${project}" ) ).thenReturn( mavenProject );
    }

    
    private RequireDeveloperRoles newRequireDeveloperRoles(
            final String commaSeparatedRequiredRoles,
            final String commaSeparatedValidRoles )
    {
        final RequireDeveloperRoles sut = new RequireDeveloperRoles();
        if ( commaSeparatedRequiredRoles != null )
        {
            sut.setRequiredRoles( commaSeparatedRequiredRoles );
        }

        if ( commaSeparatedValidRoles != null )
        {
            sut.setValidRoles( commaSeparatedValidRoles );
        }
        return sut;
    }
    
    private RequireContributorRoles newRequireContributorRoles(
            final String commaSeparatedRequiredRoles,
            final String commaSeparatedValidRoles )
    {
        final RequireContributorRoles sut = new RequireContributorRoles();
        if ( commaSeparatedRequiredRoles != null )
        {
            sut.setRequiredRoles( commaSeparatedRequiredRoles );
        }

        if ( commaSeparatedValidRoles != null )
        {
            sut.setValidRoles( commaSeparatedValidRoles );
        }
        return sut;
    }}
