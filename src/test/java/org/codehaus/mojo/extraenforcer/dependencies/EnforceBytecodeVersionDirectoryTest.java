package org.codehaus.mojo.extraenforcer.dependencies;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Directory artifacts, such as {@code target/classes} of a module in the same reactor.
 */
class EnforceBytecodeVersionDirectoryTest {

    @TempDir
    Path tempDir;

    private EnforceBytecodeVersion rule;

    private EnforcerLogger log;

    @BeforeEach
    void setUp() {
        rule = new EnforceBytecodeVersion(null, null);
        log = mock(EnforcerLogger.class);
        rule.setLog(log);
    }

    @Test
    void failsForClassNewerThanAllowedInDirectory() throws Exception {
        File classes = compile("11", "com/example/Newer.java", "package com.example; public class Newer {}");
        rule.setMaxJavaMajorVersionNumber(52);

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> check(classes));
        assertTrue(e.getMessage().contains("Found Banned Dependency"), e.getMessage());
        verify(log)
                .info(argThat((String m) ->
                        m.contains("Restricted to JDK 8") && m.contains("com/example/Newer.class targeted to JDK 11")));
    }

    @Test
    void acceptsClassWithinAllowedVersionInDirectory() throws Exception {
        File classes = compile("8", "com/example/Older.java", "package com.example; public class Older {}");
        rule.setMaxJavaMajorVersionNumber(52);

        assertDoesNotThrow(() -> check(classes));
    }

    @Test
    void ignoredClassInDirectoryIsSkipped() throws Exception {
        File classes = compile("11", "com/example/Newer.java", "package com.example; public class Newer {}");
        rule.setMaxJavaMajorVersionNumber(52);
        rule.setIgnoreClasses(new String[] {"com.example.Newer"});

        assertDoesNotThrow(() -> check(classes));
    }

    @Test
    void emptyDirectoryAndMissingDirectoryPass() throws Exception {
        rule.setMaxJavaMajorVersionNumber(52);
        File empty = Files.createDirectory(tempDir.resolve("empty")).toFile();

        assertDoesNotThrow(() -> check(empty));
        assertDoesNotThrow(() -> check(new File(tempDir.toFile(), "classes-not-compiled-yet")));
    }

    @Test
    void nestedMultiReleaseEntryIsSkippedUnlessStrict() throws Exception {
        File classes = compile("11", "com/example/Newer.java", "package com.example; public class Newer {}");
        Path nested = classes.toPath().resolve("org/gradle/internal/impldep/META-INF/versions/9/com/example");
        Files.createDirectories(nested);
        Files.move(classes.toPath().resolve("com/example/Newer.class"), nested.resolve("Newer.class"));
        rule.setMaxJavaMajorVersionNumber(52);

        assertDoesNotThrow(() -> check(classes));

        rule.setStrict(true);
        assertThrows(EnforcerRuleException.class, () -> check(classes));
    }

    @Test
    void rootMultiReleaseEntryStillWarnsWhenNewerThanItsVersion() throws Exception {
        File classes = compile("11", "com/example/Newer.java", "package com.example; public class Newer {}");
        Path root = classes.toPath().resolve("META-INF/versions/9/com/example");
        Files.createDirectories(root);
        Files.move(classes.toPath().resolve("com/example/Newer.class"), root.resolve("Newer.class"));
        rule.setMaxJavaMajorVersionNumber(52);

        assertDoesNotThrow(() -> check(classes));
        verify(log).warn(argThat((String m) -> m.contains("Invalid bytecodeVersion")));
    }

    private void check(File fileOrDirectory) throws EnforcerRuleException {
        Artifact artifact = ArtifactBuilder.newBuilder()
                .withType("jar")
                .withFileOrDirectory(fileOrDirectory)
                .build();
        rule.handleArtifacts(Collections.singleton(artifact));
    }

    private File compile(String release, String sourcePath, String source) throws IOException {
        assumeTrue(
                !System.getProperty("java.specification.version").startsWith("1."),
                "javac --release needs JDK 9 or newer");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "no system java compiler");

        Path src = tempDir.resolve("src-" + release).resolve(sourcePath);
        Files.createDirectories(src.getParent());
        Files.write(src, source.getBytes("UTF-8"));
        Path out = Files.createDirectories(tempDir.resolve("classes-" + release));

        int result = compiler.run(null, null, null, "--release", release, "-d", out.toString(), src.toString());
        assertEquals(0, result, "javac failed");
        return out.toFile();
    }
}
