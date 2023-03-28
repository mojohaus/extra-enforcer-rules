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
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Test helper for working with {@link Artifact}s.
 */
public class ArtifactBuilder {
    private String groupId = "groupId";
    private String artifactId = "artifactId";
    private VersionRange versionRange = VersionRange.createFromVersion("1.0");
    private String scope = "scope";
    private String type = "type";
    private String classifier = "classifier";
    private File fileOrDirectory = getAnyFile();

    public static ArtifactBuilder newBuilder() {
        return new ArtifactBuilder();
    }

    public ArtifactBuilder withVersion(String version) {
        versionRange = VersionRange.createFromVersion(version);
        return this;
    }

    public ArtifactBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public ArtifactBuilder withAnyDirectory() {
        fileOrDirectory = getAnyDirectory();
        return this;
    }

    public ArtifactBuilder withFileOrDirectory(File directory) {
        fileOrDirectory = directory;
        return this;
    }

    public Artifact build() {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, classifier, null);
        artifact.setFile(fileOrDirectory);

        return artifact;
    }

    private static File getAnyFile() {
        // the actual file isn't important, just so long as it exists
        URL url = ArtifactBuilder.class.getResource("/utf8.txt");
        try {
            return new File(url.toURI());
        } catch (URISyntaxException exception) {
            throw new RuntimeException(exception);
        }
    }

    private File getAnyDirectory() {
        return getAnyFile().getParentFile();
    }
}
