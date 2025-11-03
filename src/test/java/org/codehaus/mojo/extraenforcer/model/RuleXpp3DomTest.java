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

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 *
 * @author mfriedenhagen
 */
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class RuleXpp3DomTest {

    @Mock
    private MavenProject project;

    @Mock
    private ExpressionEvaluator evaluator;

    @InjectMocks
    private RequirePropertyDiverges sut1;

    @InjectMocks
    private RequirePropertyDiverges sut2;

    @Test
    void checkRuleWithoutRegex() {
        sut1.setProperty("foo");
        sut2.setProperty("foo");
        checkEquals();
    }

    @Test
    void checkRuleWithoutRegexButMessage() {
        sut1.setProperty("foo");
        sut1.setMessage("Oops");
        sut2.setProperty("foo");
        sut2.setMessage("Oops");
        checkEquals();
    }

    @Test
    void checkRuleWithoutRegexDiverges() {
        sut1.setProperty("foo");
        sut2.setProperty("foo2");
        checkDiverges();
    }

    @Test
    void checkRuleWithRegex() {
        sut1.setProperty("foo");
        sut1.setRegex("http://company/wiki/company-parent-pom.*");
        sut2.setProperty("foo");
        sut2.setRegex("http://company/wiki/company-parent-pom.*");
        checkEquals();
    }

    @Test
    void checkRuleWithRegexDiverges() {
        sut1.setProperty("foo");
        sut1.setRegex("http://company/wiki/company-parent-pom.*");
        sut2.setProperty("foo");
        sut2.setRegex("http://company/wiki/company-project1");
        checkDiverges();
    }

    void checkEquals() {
        Xpp3Dom ruleDom1 = sut1.createInvokingRuleDom();
        Xpp3Dom ruleDom2 = sut2.createInvokingRuleDom();
        assertEquals(ruleDom1, ruleDom2);
    }

    void checkDiverges() {
        Xpp3Dom ruleDom1 = sut1.createInvokingRuleDom();
        Xpp3Dom ruleDom2 = sut2.createInvokingRuleDom();
        assertNotEquals(ruleDom1, ruleDom2);
    }
}
