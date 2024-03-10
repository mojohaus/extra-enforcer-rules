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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;

/**
 * Bans circular dependencies on the classpath.
 *
 * @since 1.0-alpha-4
 */
@Named("banCircularDependencies")
public class BanCircularDependencies extends AbstractResolveDependencies {

    private final MavenProject project;

    private String message;

    @Inject
    public BanCircularDependencies(MavenSession session, RepositorySystem repositorySystem) {
        super(session, repositorySystem);
        project = session.getCurrentProject();
    }

    @Override
    protected void handleArtifacts(Set<Artifact> artifacts) throws EnforcerRuleException {
        for (Artifact artifact : artifacts) {
            getLog().debug("groupId: " + artifact.getGroupId() + project.getGroupId());
            if (artifact.getGroupId().equals(project.getGroupId())) {
                getLog().debug("artifactId: " + artifact.getArtifactId() + " " + project.getArtifactId());
                if (artifact.getArtifactId().equals(project.getArtifactId())) {
                    throw new EnforcerRuleException(getErrorMessage() + "\n  " + artifact.getGroupId() + ":"
                            + artifact.getArtifactId() + "\n ");
                }
            }
        }
    }

    private String getErrorMessage() {
        if (message == null) {
            return "Circular Dependency found. Your project's groupId:artifactId combination "
                    + "must not exist in the list of direct or transitive dependencies.";
        }
        return message;
    }
}
