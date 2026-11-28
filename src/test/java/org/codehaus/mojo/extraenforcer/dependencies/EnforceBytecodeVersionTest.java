/*
 * Copyright 2014 Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.extraenforcer.dependencies;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EnforceBytecodeVersionTest {

    static Stream<Arguments> renderVersion() {
        return Stream.of(
                arguments("44.0", 44, 0),
                arguments("JDK 1.5", 49, 0),
                arguments("JDK 1.7", 51, 0),
                arguments("51.3", 51, 3),
                arguments("JDK 8", 52, 0),
                arguments("JDK 11", 55, 0),
                arguments("JDK 12", 56, 0),
                arguments("JDK 21", 65, 0),
                arguments("JDK 26", 70, 0),
                arguments("JDK 57", 101, 0));
    }

    @ParameterizedTest
    @MethodSource
    void renderVersion(String expected, int major, int minor) {
        assertEquals(expected, EnforceBytecodeVersion.renderVersion(major, minor));
    }

    public static Stream<Arguments> decodeMajorVersion() {
        return Stream.of(
                arguments("1.1", 45),
                arguments("1.2", 46),
                arguments("1.3", 47),
                arguments("1.4", 48),
                arguments("1.5", 49),
                arguments("1.6", 50),
                arguments("1.7", 51),
                arguments("1.8", 52),
                arguments("8", 52),
                arguments("1.9", 53),
                arguments("9", 53),
                arguments("11", 55),
                arguments("12", 56),
                arguments("21", 65),
                arguments("26", 70),
                arguments("57", 101));
    }

    @ParameterizedTest
    @MethodSource
    void decodeMajorVersion(String version, int expectedMajor) {
        assertEquals(expectedMajor, EnforceBytecodeVersion.decodeMajorVersion(version));
    }

    @Test
    void failsWithoutAnyVersionOption() {
        EnforceBytecodeVersion rule = new EnforceBytecodeVersion(null, null);
        rule.setLog(mock(EnforcerLogger.class));

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> rule.handleArtifacts(Collections.emptySet()));
        assertEquals(
                "Exactly one of maxJdkVersion or maxJavaMajorVersionNumber options should be set.", e.getMessage());
    }

    @Test
    void failsWhenBothVersionOptionsAreSet() throws Exception {
        EnforceBytecodeVersion rule = new EnforceBytecodeVersion(null, null);
        rule.setLog(mock(EnforcerLogger.class));
        Field maxJdkVersion = EnforceBytecodeVersion.class.getDeclaredField("maxJdkVersion");
        maxJdkVersion.setAccessible(true);
        maxJdkVersion.set(rule, "1.2");
        rule.setMaxJavaMajorVersionNumber(46);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> rule.handleArtifacts(Collections.emptySet()));
        assertEquals(
                "Only maxJdkVersion or maxJavaMajorVersionNumber configuration parameters should be set. Not both.",
                e.getMessage());
    }

    @Test
    void warnsThatTheRuleIsDeprecated() throws Exception {
        EnforceBytecodeVersion rule = new EnforceBytecodeVersion(null, null);
        EnforcerLogger log = mock(EnforcerLogger.class);
        rule.setLog(log);
        rule.setMaxJavaMajorVersionNumber(52);

        rule.handleArtifacts(Collections.emptySet());

        verify(log).warn(EnforceBytecodeVersion.DEPRECATION_WARNING);
    }
}
