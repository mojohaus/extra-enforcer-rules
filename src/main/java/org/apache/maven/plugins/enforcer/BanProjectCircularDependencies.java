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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Bans circular dependencies within the overall project.
 *
 * @since 1.0-alpha-4
 */
public class BanProjectCircularDependencies
    implements EnforcerRule
{

    private transient DependencyGraphBuilder graphBuilder;

    private String message;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        warnIfMaven2( helper );

        Log log = helper.getLog();

        try
        {
            graphBuilder = (DependencyGraphBuilder) helper.getComponent( DependencyGraphBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
            // real cause is probably that one of the Maven3 graph builder could not be initiated and fails with a ClassNotFoundException
            try
            {
                graphBuilder = (DependencyGraphBuilder) helper.getComponent( DependencyGraphBuilder.class.getName(), "maven2" );
            }
            catch ( ComponentLookupException e1 )
            {
                throw new EnforcerRuleException( "Unable to lookup DependencyGraphBuilder: ", e );
            }
        }

        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            Set<SimpleArtifact> projectArtifacts = new HashSet<SimpleArtifact>();
            for (Object subproject : project.getCollectedProjects()) {
              if (subproject instanceof MavenProject) {
                MavenProject mavenSubproject = (MavenProject)subproject;
                projectArtifacts.add(new SimpleArtifact(mavenSubproject.getGroupId(), mavenSubproject.getArtifactId()));
              }
            }
            // Iterate through the sub-projects this project builds
            for (Object subproject : project.getCollectedProjects()) {
              if (subproject instanceof MavenProject) {
                MavenProject mavenSubproject = (MavenProject)subproject;
                Set<SimpleArtifact> dependencyArtifacts = getDependenciesToCheck( mavenSubproject, log, projectArtifacts );
                for ( SimpleArtifact artifact : dependencyArtifacts )
                {
                  log.debug("Artifact " + artifact.getGroupId() + " : " + artifact.getArtifactId());
                  if (projectArtifacts.contains(artifact))
                  {
                    StringBuilder buf = new StringBuilder( getErrorMessage() );
                    buf.append( "\n  " )
                    .append( artifact.getGroupId() )
                    .append( ":" )
                    .append( artifact.getArtifactId() )
                    .append( " found in transitive dependencies of " )
                    .append( mavenSubproject.getGroupId() )
                    .append( ":" )
                    .append( mavenSubproject.getArtifactId() )
                    .append( "\n " );
                    throw new EnforcerRuleException( buf.toString() );
                  }
                }
              }
            }


        }
        catch ( ExpressionEvaluationException e )
        {
            log.error( "Error checking for circular dependencies", e );
            e.printStackTrace();
        }
    }

    private void warnIfMaven2( EnforcerRuleHelper helper )
    {
        final Log log = helper.getLog();
        RuntimeInformation rti;
        try
        {
            rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );
            ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
            log.debug( "Detected Maven Version: " + detectedMavenVersion );
            if ( detectedMavenVersion.getMajorVersion() == 2 )
            {
                log.warn( "Circular dependencies cannot exist with Maven 2. "
                    + "So that rule is of no use for that Maven version. " + "See rule documentation at "
                    + "http://mojo.codehaus.org/extra-enforcer-rules/banCircularDependencies.html" );
            }
        }
        catch ( ComponentLookupException e )
        {
            log.warn( "Unable to detect Maven version. Please report this issue to the mojo@codehaus project" );
        }
    }

    protected Set<SimpleArtifact> getDependenciesToCheck( MavenProject project, Log log, Set<SimpleArtifact> topLevelArtifactsToIgnore )
    {
        Set<Artifact> dependencies = null;
        try
        {
            DependencyNode node = graphBuilder.buildDependencyGraph( project, null );
            dependencies  = getAllDescendants( node, topLevelArtifactsToIgnore );
        }
        catch ( DependencyGraphBuilderException e )
        {
            // otherwise we need to change the signature of this protected method
            throw new RuntimeException( e );
        }
        Set<SimpleArtifact> simpleDependencies = new HashSet<SimpleArtifact>();
        if (dependencies != null)
        {
          for (Artifact artifact : dependencies) {
            simpleDependencies.add(new SimpleArtifact(artifact.getGroupId(), artifact.getArtifactId()));
          }
        }

        return simpleDependencies;
    }

    private Set<Artifact> getAllDescendants( DependencyNode node, Set<SimpleArtifact> topLevelArtifactsToIgnore )
    {
        Set<Artifact> children = null;
        if( node.getChildren() != null )
        {
            children = new HashSet<Artifact>();
            for( DependencyNode depNode : node.getChildren() )
            {
                if (topLevelArtifactsToIgnore != null) {
                  SimpleArtifact childArtifact = new SimpleArtifact(depNode.getArtifact().getGroupId(), depNode.getArtifact().getArtifactId());
                  if (topLevelArtifactsToIgnore.contains(childArtifact)) {
                    continue; // skip modules in the project
                  }
                }
                children.add( depNode.getArtifact() );
                Set<Artifact> subNodes = getAllDescendants( depNode, null ); // only skip modules where they are direct descendants
                if( subNodes != null )
                {
                    children.addAll( subNodes );
                }
            }
        }
        return children;
    }


    private String getErrorMessage()
    {
        if ( message == null )
            return "Circular Project Dependency found. No modules groupId:artifactId combination in your project "
                + "must exist in the list of transitive dependencies for any module in the project.";
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResultValid( EnforcerRule enforcerRule )
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheId()
    {
        return "Does not matter as not cacheable";
    }


    private static class SimpleArtifact {

      private final String groupId;
      private final String artifactId;

      SimpleArtifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
      }

      public String getGroupId() {
        return groupId;
      }

      public String getArtifactId() {
        return artifactId;
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (artifactId == null ? 0 : artifactId.hashCode());
        result = prime * result + (groupId == null ? 0 : groupId.hashCode());
        return result;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SimpleArtifact other = (SimpleArtifact) obj;
        if (artifactId == null) {
          if (other.artifactId != null) return false;
        } else if (!artifactId.equals(other.artifactId)) return false;
        if (groupId == null) {
          if (other.groupId != null) return false;
        } else if (!groupId.equals(other.groupId)) return false;
        return true;
      }
    }
}