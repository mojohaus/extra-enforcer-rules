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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Bans duplicate classes on the classpath.
 */
public class BanDuplicateClasses
    implements EnforcerRule
{

    private transient DependencyTreeBuilder treeBuilder;
    
    private transient ArtifactResolver resolver;
    
    private transient ArtifactRepository localRepository;
    
    private transient List<ArtifactRepository> remoteRepositories;
    
    private transient EnforcerRuleHelper helper;

    // Configuration properties
    
    /**
     * The failure message
     */
    private String message;
    
    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    /**
     * If {@code false} then the rule will fail at the first duplicate, if {@code true} then the rule will fail at
     * the end.
     */
    private boolean findAllDuplicates;
    
    private List<Dependency> dependencies;
    
    /**
     * Only verify dependencies with one of these scopes
     */
    private List<String> scopes;

    
    /**
     * Convert a wildcard into a regex.
     *
     * @param wildcard the wildcard to convert.
     * @return the equivalent regex.
     */
    private static String asRegex( String wildcard )
    {
        StringBuilder result = new StringBuilder( wildcard.length() );
        result.append( '^' );
        for ( int index = 0; index < wildcard.length(); index++ )
        {
            char character = wildcard.charAt( index );
            switch ( character )
            {
                case '*':
                    result.append( ".*" );
                    break;
                case '?':
                    result.append( "." );
                    break;
                case '$':
                case '(':
                case ')':
                case '.':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                    result.append( "\\" );
                default:
                    result.append( character );
                    break;
            }
        }
        result.append( "(\\.class)?" );
        result.append( '$' );
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        this.helper = helper;
        
        List<IgnorableDependency> ignorableDependencies = new ArrayList<IgnorableDependency>();
        if ( ignoreClasses != null )
        {
            IgnorableDependency ignorableDependency = new IgnorableDependency();
            applyIgnoreClasses( ignorableDependency, ignoreClasses, false );
            ignorableDependencies.add( ignorableDependency );
        }
        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                getLog().info( "Adding ignorable dependency: " + dependency );
                IgnorableDependency ignorableDependency = new IgnorableDependency();
                if ( dependency.getGroupId() != null )
                {
                    ignorableDependency.groupId = Pattern.compile( asRegex( dependency.getGroupId() ) );
                }
                if ( dependency.getArtifactId() != null )
                {
                    ignorableDependency.artifactId = Pattern.compile( asRegex( dependency.getArtifactId() ) );
                }
                if ( dependency.getType() != null )
                {
                    ignorableDependency.type = Pattern.compile( asRegex( dependency.getType() ) );
                }
                if ( dependency.getClassifier() != null )
                {
                    ignorableDependency.classifier = Pattern.compile( asRegex( dependency.getClassifier() ) );
                }
                applyIgnoreClasses( ignorableDependency, dependency.getIgnoreClasses(), true );
                ignorableDependencies.add( ignorableDependency );
            }
        }
        
        try
        {
            resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            
            treeBuilder = (DependencyTreeBuilder) helper.getComponent( DependencyTreeBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
                throw new EnforcerRuleException( "Unable to lookup DependencyTreeBuilder: ", e );
        }
        
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            
            localRepository = (ArtifactRepository) helper.evaluate( "${localRepository}" );
            
            remoteRepositories = (List<ArtifactRepository>) helper.evaluate( "${project.remoteArtifactRepositories}" );

            Set<Artifact> artifacts = getDependenciesToCheck( project, localRepository );

            Map<String, Artifact> classNames = new HashMap<String, Artifact>();
            Map<String, Set<Artifact>> duplicates = new HashMap<String, Set<Artifact>>();
            for ( Artifact o : artifacts )
            {
                if( scopes != null && !scopes.contains( o.getScope() ) )
                {
                    if( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "Skipping " + o.toString() + " due to scope" );
                    }
                    continue;
                }
                File file = o.getFile();
                getLog().debug( "Searching for duplicate classes in " + file );
                if ( file == null || !file.exists() )
                {
                    getLog().warn( "Could not find " + o + " at " + file );
                }
                else if ( file.isDirectory() )
                {
                    try
                    {
                        for ( String name : (List<String>) FileUtils.getFileNames( file, null, null, false ) )
                        {
                            getLog().debug( "  " + name );
                            checkAndAddName( o, name, classNames, duplicates, ignorableDependencies );
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new EnforcerRuleException(
                            "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(), e );
                    }
                }
                else if ( file.isFile() && "jar".equals( o.getType() ) )
                {
                    try
                    {
                        //@todo use UnArchiver as defined per type
                        JarFile jar = new JarFile( file );
                        try
                        {
                            for ( JarEntry entry : Collections.<JarEntry>list( jar.entries() ) )
                            {
                                checkAndAddName( o, entry.getName(), classNames, duplicates, ignorableDependencies );
                            }
                        }
                        finally
                        {
                            try
                            {
                                jar.close();
                            }
                            catch ( IOException e )
                            {
                                // ignore
                            }
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new EnforcerRuleException(
                            "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(), e );
                    }
                }
            }
            if ( !duplicates.isEmpty() )
            {
                Map<Set<Artifact>, List<String>> inverted = new HashMap<Set<Artifact>, List<String>>();
                for ( Map.Entry<String, Set<Artifact>> entry : duplicates.entrySet() )
                {
                    List<String> s = inverted.get( entry.getValue() );
                    if ( s == null )
                    {
                        s = new ArrayList<String>();
                    }
                    s.add( entry.getKey() );
                    inverted.put( entry.getValue(), s );
                }
                StringBuilder buf = new StringBuilder( message == null ? "Duplicate classes found:" : message );
                buf.append( '\n' );
                for ( Map.Entry<Set<Artifact>, List<String>> entry : inverted.entrySet() )
                {
                    buf.append( "\n  Found in:" );
                    for ( Artifact a : entry.getKey() )
                    {
                        buf.append( "\n    " );
                        buf.append( a );
                    }
                    buf.append( "\n  Duplicate classes:" );
                    for ( String className : entry.getValue() )
                    {
                        buf.append( "\n    " );
                        buf.append( className );
                    }
                    buf.append( '\n' );
                }
                throw new EnforcerRuleException( buf.toString() );
            }

        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
    }

    private void applyIgnoreClasses( IgnorableDependency ignorableDependency, String[] ignores, boolean indent )
    {
        String prefix = indent ? "  " : "";
        for ( String ignore : ignores )
        {
            getLog().info( prefix + "Adding ignore: " + ignore );
            ignore = ignore.replace( '.', '/' );
            String pattern = asRegex( ignore );
            getLog().debug( prefix + "Ignore: " + ignore + " maps to regex " + pattern );
            ignorableDependency.ignores.add( Pattern.compile( pattern ) );
        }
    }

    private void checkAndAddName( Artifact artifact, String name, Map<String, Artifact> classNames,
                                  Map<String, Set<Artifact>> duplicates, Collection<IgnorableDependency> ignores )
        throws EnforcerRuleException
    {
        if ( !name.endsWith( ".class" ) )
        {
            return;
        }
        
        for ( IgnorableDependency c : ignores )
        {
            if ( matchesArtifact( artifact, c ) )
            {
                for ( Pattern p : c.ignores )
                {
                    if ( p.matcher( name ).matches() )
                    {
                        if( classNames.containsKey( name ) )
                        {
                            getLog().debug( "Ignoring excluded class " + name );
                        }
                        return;
                    }
                }
            }
        }

        if ( classNames.containsKey( name ) )
        {
            Artifact dup = classNames.put( name, artifact );
            if ( !( findAllDuplicates && duplicates.containsKey( name ) ) )
            {
                for ( IgnorableDependency c : ignores )
                {
                    if ( matchesArtifact( artifact, c ) )
                    {
                        for ( Pattern p : c.ignores )
                        {
                            if ( p.matcher( name ).matches() )
                            {
                                getLog().debug( "Ignoring duplicate class " + name );
                                return;
                            }
                        }
                    }
                }
            }
            
            if ( findAllDuplicates )
            {
                Set<Artifact> dups = duplicates.get( name );
                if ( dups == null )
                {
                    dups = new LinkedHashSet<Artifact>();
                    dups.add( dup );
                }
                dups.add( artifact );
                duplicates.put( name, dups );
            }
            else
            {
                StringBuilder buf = new StringBuilder( message == null ? "Duplicate class found:" : message );
                buf.append( '\n' );
                buf.append( "\n  Found in:" );
                buf.append( "\n    " );
                buf.append( dup );
                buf.append( "\n    " );
                buf.append( artifact );
                buf.append( "\n  Duplicate classes:" );
                buf.append( "\n    " );
                buf.append( name );
                buf.append( '\n' );
                buf.append( "There may be others but <findAllDuplicates> was set to false, so failing fast" );
                throw new EnforcerRuleException( buf.toString() );
            }
        }
        else 
        {
            classNames.put( name, artifact );
        }
    }

    private boolean matchesArtifact( Artifact dup, IgnorableDependency c )
    {
        return ( c.artifactId == null || c.artifactId.matcher( dup.getArtifactId() ).matches() )
            && ( c.groupId == null || c.groupId.matcher( dup.getGroupId() ).matches() )
            && ( c.classifier == null || c.classifier.matcher( dup.getClassifier() ).matches() )
            && ( c.type == null || c.type.matcher( dup.getType() ).matches() );
    }
    
    protected Set<Artifact> getDependenciesToCheck( MavenProject project, ArtifactRepository localRepository )
    {
        Set<Artifact> dependencies = null;
        try
        {
            DependencyNode node = treeBuilder.buildDependencyTree( project, localRepository, null );
            dependencies  = getAllDescendants( node );
        }
        catch ( DependencyTreeBuilderException e )
        {
            // otherwise we need to change the signature of this protected method
            throw new RuntimeException( e );
        }
        return dependencies;
    }
    
    private Set<Artifact> getAllDescendants( DependencyNode node )
    {
        Set<Artifact> children = null; 
        if( node.getChildren() != null )
        {
            children = new HashSet<Artifact>();
            for( DependencyNode depNode : node.getChildren() )
            {
                try
                {
                    Artifact artifact = depNode.getArtifact();

                    resolver.resolve( artifact, remoteRepositories, localRepository );
                    
                    children.add( artifact );

                    Set<Artifact> subNodes = getAllDescendants( depNode );
                    
                    if( subNodes != null )
                    {
                        children.addAll( subNodes );
                    }
                }
                catch ( ArtifactResolutionException e )
                {
                    getLog().warn( e.getMessage() );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLog().warn( e.getMessage() );
                }
            }
        }
        return children;
    }
    
    private Log getLog()
    {
        return helper.getLog();
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
    
    /**
     * 
     */
    private class IgnorableDependency
    {
        private Pattern groupId;
        private Pattern artifactId;
        private Pattern classifier;
        private Pattern type;
        private List<Pattern> ignores = new ArrayList<Pattern>();
    }
}