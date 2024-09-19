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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Mirko Friedenhagen
 */
class RequireRolesTest {

    private MavenProject mavenProject;

    @Test
    void shouldSucceedBecauseArchitectAsDeveloperAndBusinessEngineerAsContributorArePresent()
            throws EnforcerRuleException {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireDeveloperRoles("architect" /*required role*/, null /* valid roles not needed */)
                .execute();
        newRequireContributorRoles("business engineer" /*required role*/, "*" /* valid roles */)
                .execute();
    }

    @Test
    void shouldFailBecauseContributorWithRoleQualityManagerIsMissing() {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        assertThrows(EnforcerRuleException.class, () -> newRequireContributorRoles(
                        "business engineer, quality manager", null)
                .execute());
    }

    @Test
    void shouldFailBecauseDeveloperWithRoleCodeMonkeyIsMissing() {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        assertThrows(
                EnforcerRuleException.class,
                () -> newRequireDeveloperRoles("codemonkey" /* required but not in project */, null)
                        .execute());
    }

    @Test
    void shouldFailBecauseContributorRoleBusinessEngineerIsInvalid() {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        assertThrows(
                EnforcerRuleException.class,
                () -> newRequireContributorRoles(null /* no required roles needed */, "hacker" /* only valid role */)
                        .execute());
    }

    @Test
    void shouldFailBecauseNoContributorRolesAtAllAreValid() {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        assertThrows(EnforcerRuleException.class, () -> newRequireContributorRoles(
                        null /* no required roles needed */, "" /*but no role is valid at all */)
                .execute());
    }

    @Test
    void shouldSucceedAsNoRolesAreRequiredAndAllAreAccepted() throws Exception {
        addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper();
        newRequireContributorRoles(null /* no required roles */, "*" /* any role is valid */)
                .execute();
        newRequireContributorRoles(null /* no required roles */, null /* any role is valid */)
                .execute();
    }

    /**
     * Test of getRolesFromString method, of class AbstractRequireRoles.
     */
    @Test
    void getRolesFromString() {
        HashSet<String> expResult = new HashSet<>(Arrays.asList("architect", "codemonkey", "business engineer"));
        final RequireContributorRoles sut = new RequireContributorRoles(mavenProject);
        Set<String> result = sut.getRolesFromString(" architect,  business engineer   , codemonkey ");
        assertEquals(expResult, result);
    }

    /**
     * Test of getRolesFromMaven method, of class AbstractRequireRoles.
     */
    @Test
    void getRolesFromMaven() {
        HashSet<String> expResult =
                new HashSet<>(Arrays.asList("quality manager", "product owner", "business engineer"));
        final Contributor singleHero = new Contributor();
        singleHero.addRole("quality manager");
        singleHero.addRole("business engineer");
        singleHero.addRole("product owner");
        List<Contributor> listFromMaven = Collections.singletonList(singleHero);
        final HashSet<String> result = new HashSet<>();
        for (final Contributor contributor : listFromMaven) {
            @SuppressWarnings("unchecked")
            List<String> roles = contributor.getRoles();
            result.addAll(roles);
        }
        assertEquals(expResult, result);
    }

    private void addProjectHavingAnArchitectAsDeveloperAndABusinessEngineerAsContributorToHelper() {
        mavenProject = new MavenProject();
        final Developer developer = new Developer();
        developer.addRole("architect");
        mavenProject.addDeveloper(developer);
        final Contributor contributor = new Contributor();
        contributor.addRole("business engineer");
        mavenProject.addContributor(contributor);
    }

    private RequireDeveloperRoles newRequireDeveloperRoles(
            final String commaSeparatedRequiredRoles, final String commaSeparatedValidRoles) {
        final RequireDeveloperRoles sut = new RequireDeveloperRoles(mavenProject);
        if (commaSeparatedRequiredRoles != null) {
            sut.setRequiredRoles(commaSeparatedRequiredRoles);
        }

        if (commaSeparatedValidRoles != null) {
            sut.setValidRoles(commaSeparatedValidRoles);
        }
        return sut;
    }

    private RequireContributorRoles newRequireContributorRoles(
            final String commaSeparatedRequiredRoles, final String commaSeparatedValidRoles) {
        final RequireContributorRoles sut = new RequireContributorRoles(mavenProject);
        if (commaSeparatedRequiredRoles != null) {
            sut.setRequiredRoles(commaSeparatedRequiredRoles);
        }

        if (commaSeparatedValidRoles != null) {
            sut.setValidRoles(commaSeparatedValidRoles);
        }
        return sut;
    }
}
