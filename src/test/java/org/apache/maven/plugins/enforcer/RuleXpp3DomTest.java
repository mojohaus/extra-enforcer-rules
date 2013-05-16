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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author mfriedenhagen
 */
public class RuleXpp3DomTest
{

    final RequirePropertyDiverges sut1 = new RequirePropertyDiverges();
    final RequirePropertyDiverges sut2 = new RequirePropertyDiverges();

    @Test
    public void checkRuleWithoutRegex()
    {
        sut1.setProperty( "foo" );
        sut2.setProperty( "foo" );
        checkEquals();
    }

    @Test
    public void checkRuleWithoutRegexButMessage()
    {
        sut1.setProperty( "foo" );
        sut1.message = "Oops";
        sut2.setProperty( "foo" );
        sut2.message = "Oops";
        checkEquals();
    }

    @Test
    public void checkRuleWithoutRegexDiverges()
    {
        sut1.setProperty( "foo" );
        sut2.setProperty( "foo2" );
        checkDiverges();
    }

    @Test
    public void checkRuleWithRegex()
    {
        sut1.setProperty( "foo" );
        sut1.setRegex( "http://company/wiki/company-parent-pom.*" );
        sut2.setProperty( "foo" );
        sut2.setRegex( "http://company/wiki/company-parent-pom.*" );
        checkEquals();
    }

    @Test
    public void checkRuleWithRegexDiverges()
    {
        sut1.setProperty( "foo" );
        sut1.setRegex( "http://company/wiki/company-parent-pom.*" );
        sut2.setProperty( "foo" );
        sut2.setRegex( "http://company/wiki/company-project1" );
        checkDiverges();
    }

    void checkEquals()
    {
        Xpp3Dom ruleDom1 = sut1.createInvokingRuleDom();
        Xpp3Dom ruleDom2 = sut2.createInvokingRuleDom();
        assertTrue( ruleDom1.equals( ruleDom2 ) );
    }

    void checkDiverges()
    {
        Xpp3Dom ruleDom1 = sut1.createInvokingRuleDom();
        Xpp3Dom ruleDom2 = sut2.createInvokingRuleDom();
        assertFalse( ruleDom1.equals( ruleDom2 ) );
    }
}
