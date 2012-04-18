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

    /**
     * Test of execute method, of class AbstractRequireRoles.
     */
    @Test
    public void testExecuteSuccess() throws ExpressionEvaluationException, EnforcerRuleException
    {
        setUpMavenProject();
        {
            final RequireDeveloperRoles instance = new RequireDeveloperRoles();
            instance.setRequiredRoles( "lead developer" );
            instance.execute( helper );
        }
        {
            final RequireContributorRoles instance = new RequireContributorRoles();
            instance.setRequiredRoles( "business engineer" );
            instance.execute( helper );
        }
    }

    /**
     * Test of execute method, of class RequireContributorRoles.
     */
    @Test( expected = EnforcerRuleException.class )
    public void testExecuteMissingContributorQa() throws ExpressionEvaluationException, EnforcerRuleException
    {
        setUpMavenProject();
        final RequireContributorRoles instance = new RequireContributorRoles();
        instance.setRequiredRoles( "business engineer, quality manager" );
        instance.execute( helper );
    }

    /**
     * Test of execute method, of class RequireContributorRoles.
     */
    @Test( expected = EnforcerRuleException.class )
    public void testExecuteMissingDeveloperLead() throws ExpressionEvaluationException, EnforcerRuleException
    {
        setUpMavenProject();
        final RequireDeveloperRoles instance = new RequireDeveloperRoles();
        instance.setRequiredRoles( "developer" );
        instance.execute( helper );
    }

    void setUpMavenProject() throws ExpressionEvaluationException
    {
        final MavenProject mavenProject = new MavenProject();
        final Developer developer = new Developer();
        developer.addRole( "lead developer" );
        mavenProject.addDeveloper( developer );
        final Contributor contributor = new Contributor();
        contributor.addRole( "business engineer" );
        mavenProject.addContributor( contributor );
        Mockito.when( helper.evaluate( "${project}" ) ).thenReturn( mavenProject );
    }

    /**
     * Test of getRolesFromString method, of class AbstractRequireRoles.
     */
    @Test
    public void testGetRolesFromString()
    {
        HashSet<String> expResult = new HashSet<String>();
        expResult.add( "lead developer" );
        expResult.add( "business engineer" );
        final RequireContributorRoles instance = new RequireContributorRoles();
        Set<String> result = instance.getRolesFromString( "lead developer, business engineer" );
        assertEquals( expResult, result );
    }

    /**
     * Test of getRolesFromMaven method, of class AbstractRequireRoles.
     */
    @Test
    public void testGetRolesFromMaven()
    {
        HashSet<String> expResult = new HashSet<String>();
        expResult.add( "lead developer" );
        expResult.add( "business engineer" );
        final Contributor singleHero = new Contributor();
        singleHero.addRole( "lead developer" );
        singleHero.addRole( "business engineer" );
        List<Contributor> listFromMaven = Arrays.asList( singleHero );
        final RequireContributorRoles instance = new RequireContributorRoles();
        final HashSet<String> result1 = new HashSet<String>();
        for ( final Contributor contributor : listFromMaven )
        {
            @SuppressWarnings( "unchecked" )
            List<String> roles = contributor.getRoles();
            for ( String role : roles )
            {
                result1.add( role );
            }
        }
        HashSet<String> result = result1;
        assertEquals( expResult, result );
    }
}
