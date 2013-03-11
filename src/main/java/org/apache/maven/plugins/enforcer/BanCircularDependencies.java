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

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Bans circular dependencies on the classpath.
 * 
 * @since 1.0-alpha-4
 */
public class BanCircularDependencies
    extends AbstractStandardEnforcerRule
{
    /**
     * {@inheritDoc}
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Log log = helper.getLog();
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            Set<Artifact> artifacts = project.getArtifacts();

            if ( artifacts != null )
            {
                for ( Artifact artifact : artifacts )
                {
                    log.debug( "groupId " + artifact.getGroupId() + project.getGroupId() );
                    if ( artifact.getGroupId().equals( project.getGroupId() ) )
                    {
                        log.debug( "artifactId" + artifact.getArtifactId() + " " + project.getArtifactId() );
                        if ( artifact.getArtifactId().equals( project.getArtifactId() ) )
                        {
                            StringBuilder buf = new StringBuilder( getErrorMessage() );
                            buf.append( "\n  " ).
                                append( artifact.getGroupId() ).append( ":" ).append( artifact.getArtifactId() ).
                                append( "\n " );
                            throw new EnforcerRuleException( buf.toString() );
                        }
                    }
                }
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            log.error( "Error checking for circular dependencies" );
            e.printStackTrace();
        }
    }

    private String getErrorMessage()
    {
        if ( message == null )
            return "Circular Dependency found. Your project's groupId:artifactId combination " +
            		"must not exist in the list of direct or transitive dependencies.";
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isResultValid( EnforcerRule enforcerRule )
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheId()
    {
        return "Does not matter as not cacheable";
    }
}