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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Bans circular dependencies on the classpath.
 *
 * @since 1.0-alpha-4
 */
public class BanCircularDependencies extends AbstractMojoHausEnforcerRule {

    private transient DependencyGraphBuilder graphBuilder;

    private String message;

    /**
     * {@inheritDoc}
     */
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        Log log = helper.getLog();

        try {
            graphBuilder = helper.getComponent(DependencyGraphBuilder.class);
        } catch (ComponentLookupException e) {
            throw new EnforcerRuleException("Unable to lookup DependencyGraphBuilder: ", e);
        }

        try {
            MavenProject project = (MavenProject) helper.evaluate("${project}");
            MavenSession session = (MavenSession) helper.evaluate("${session}");

            ProjectBuildingRequest buildingRequest =
                    new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);

            Set<Artifact> artifacts = getDependenciesToCheck(buildingRequest);
            if (artifacts != null) {
                for (Artifact artifact : artifacts) {
                    log.debug("groupId: " + artifact.getGroupId() + project.getGroupId());
                    if (artifact.getGroupId().equals(project.getGroupId())) {
                        log.debug("artifactId: " + artifact.getArtifactId() + " " + project.getArtifactId());
                        if (artifact.getArtifactId().equals(project.getArtifactId())) {
                            throw new EnforcerRuleException(getErrorMessage() + "\n  " + artifact.getGroupId() + ":"
                                    + artifact.getArtifactId() + "\n ");
                        }
                    }
                }
            }
        } catch (ExpressionEvaluationException e) {
            log.error("Error checking for circular dependencies", e);
            e.printStackTrace();
        }
    }

    protected Set<Artifact> getDependenciesToCheck(ProjectBuildingRequest buildingRequest) {
        Set<Artifact> dependencies;
        try {
            DependencyNode node = graphBuilder.buildDependencyGraph(buildingRequest, null);
            dependencies = getAllDescendants(node);
        } catch (DependencyGraphBuilderException e) {
            // otherwise we need to change the signature of this protected method
            throw new RuntimeException(e);
        }
        return dependencies;
    }

    private Set<Artifact> getAllDescendants(DependencyNode node) {
        Set<Artifact> children = null;
        if (node.getChildren() != null) {
            children = new HashSet<>();
            for (DependencyNode depNode : node.getChildren()) {
                children.add(depNode.getArtifact());
                Set<Artifact> subNodes = getAllDescendants(depNode);
                if (subNodes != null) {
                    children.addAll(subNodes);
                }
            }
        }
        return children;
    }

    private String getErrorMessage() {
        if (message == null) {
            return "Circular Dependency found. Your project's groupId:artifactId combination "
                    + "must not exist in the list of direct or transitive dependencies.";
        }
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isResultValid(EnforcerRule enforcerRule) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheId() {
        return "Does not matter as not cacheable";
    }
}
