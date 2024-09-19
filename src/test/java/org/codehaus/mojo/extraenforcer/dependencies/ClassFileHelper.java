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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.artifact.Artifact;

/**
 * Test utility to make writing tests with {@link ClassFile}s easier.
 */
public class ClassFileHelper {
    private static int uniqueId = 0;

    private final Path temporaryFolder;

    public ClassFileHelper(Path temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    public ClassFile createWithContent(String pathToClassFile, String fileContents) throws IOException {
        uniqueId++;
        String uniqueIdStr = Integer.toString(uniqueId);

        Path tempDirectory = Files.createTempDirectory(temporaryFolder, uniqueIdStr);
        createClassFile(tempDirectory.toFile(), pathToClassFile, fileContents);

        Artifact artifact = ArtifactBuilder.newBuilder()
                .withFileOrDirectory(tempDirectory)
                .withVersion(uniqueIdStr)
                .withType("some type that isn't 'jar' so our code assumes it's a directory")
                .build();

        return new ClassFile(
                pathToClassFile, artifact, () -> Files.newInputStream(tempDirectory.resolve(pathToClassFile)));
    }

    private void createClassFile(File directory, String pathToClassFile, String fileContents) throws IOException {
        File file = new File(directory, pathToClassFile);

        boolean madeDirs = file.getParentFile().mkdirs();
        if (!madeDirs) {
            throw new RuntimeException("unable to create parent directories for " + file);
        }

        file.createNewFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fileContents);
        }
    }
}
