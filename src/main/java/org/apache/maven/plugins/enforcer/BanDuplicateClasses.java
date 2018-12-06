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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.mojo.enforcer.Dependency;
import org.codehaus.plexus.util.FileUtils;

import static org.apache.maven.plugins.enforcer.JarUtils.isJarFile;

/**
 * Bans duplicate classes on the classpath.
 */
public class BanDuplicateClasses
    extends AbstractResolveDependencies
{

    /**
     * Default ignores which are needed for JDK 9, cause in JDK 9 and above the <code>module-info.class</code> will be
     * duplicated in any jar file. Furthermore in use cases for multi release jars the <code>module-info.class</code> is
     * also contained several times.
     */
    private static final String[] DEFAULT_CLASSES_IGNORES = { "module-info", "META-INF/versions/*/module-info" };

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

    /**
     * List of dependencies for which you want to ignore specific classes.
     */
    private List<Dependency> dependencies;
    
    /**
     * Only verify dependencies with one of these scopes
     */
    private List<String> scopes;

    /**
     * If {@code true} do not fail the build when duplicate classes exactly match each other. In other words, ignore
     * duplication if the bytecode in the class files match. Default is {@code false}.
     */
    private boolean ignoreWhenIdentical;

    @Override
    protected void handleArtifacts( Set<Artifact> artifacts ) throws EnforcerRuleException
    {
        List<IgnorableDependency> ignorableDependencies = new ArrayList<IgnorableDependency>();

        IgnorableDependency ignoreableClasses = new IgnorableDependency();
        ignoreableClasses.applyIgnoreClasses( DEFAULT_CLASSES_IGNORES, false );
        if ( ignoreClasses != null )
        {
            ignoreableClasses.applyIgnoreClasses( ignoreClasses, false );
        }
        ignorableDependencies.add( ignoreableClasses );

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
                ignorableDependency.applyIgnoreClasses( dependency.getIgnoreClasses(), true );
                ignorableDependencies.add( ignorableDependency );
            }
        }

        Map<String, ClassesWithSameName> classesSeen = new HashMap<String, ClassesWithSameName>();
        Set<String> duplicateClassNames = new HashSet<String>();
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
                    for ( String name : FileUtils.getFileNames( file, null, null, false ) )
                    {
                        getLog().debug( "  " + name );
                        checkAndAddName( o, name, classesSeen, duplicateClassNames, ignorableDependencies );
                    }
                }
                catch ( IOException e )
                {
                    throw new EnforcerRuleException(
                        "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(), e );
                }
            }
            else if ( isJarFile( o ) )
            {
                try
                {
                    //@todo use UnArchiver as defined per type
                    JarFile jar = new JarFile( file );
                    try
                    {
                        for ( JarEntry entry : Collections.<JarEntry>list( jar.entries() ) )
                        {
                            String fileName = entry.getName();
                            checkAndAddName( o, fileName, classesSeen, duplicateClassNames, ignorableDependencies );
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
        if ( !duplicateClassNames.isEmpty() )
        {
            Map<Set<Artifact>, List<String>> inverted = new HashMap<Set<Artifact>, List<String>>();
            for ( String className : duplicateClassNames )
            {
                ClassesWithSameName classesWithSameName = classesSeen.get( className );
                Set<Artifact> artifactsOfDuplicateClass = classesWithSameName.getAllArtifactsThisClassWasFoundIn();

                List<String> s = inverted.get( artifactsOfDuplicateClass );
                if ( s == null )
                {
                    s = new ArrayList<String>();
                }
                s.add( classesWithSameName.toOutputString( ignoreWhenIdentical ) );
                inverted.put( artifactsOfDuplicateClass, s );
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
                for ( String classNameWithDuplicationInfo : entry.getValue() )
                {
                    buf.append( "\n    " );
                    buf.append( classNameWithDuplicationInfo );
                }
                buf.append( '\n' );
            }
            throw new EnforcerRuleException( buf.toString() );
        }

    }

    private void checkAndAddName( Artifact artifact, String pathToClassFile, Map<String,
                                  ClassesWithSameName> classesSeen, Set<String> duplicateClasses,
                                  Collection<IgnorableDependency> ignores )
        throws EnforcerRuleException
    {
        if ( !pathToClassFile.endsWith( ".class" ) )
        {
            return;
        }

        for ( IgnorableDependency c : ignores )
        {
            if ( c.matchesArtifact( artifact ) && c.matches( pathToClassFile ) )
            {
                if ( classesSeen.containsKey( pathToClassFile ) )
                {
                    getLog().debug( "Ignoring excluded class " + pathToClassFile );
                }
                return;
            }
        }

        ClassesWithSameName classesWithSameName = classesSeen.get( pathToClassFile );
        boolean isFirstTimeSeeingThisClass = ( classesWithSameName == null );
        ClassFile classFile = new ClassFile( pathToClassFile, artifact );

        if ( isFirstTimeSeeingThisClass )
        {
            classesSeen.put( pathToClassFile, new ClassesWithSameName( getLog(), classFile ) );
            return;
        }

        classesWithSameName.add( classFile );

        if ( !classesWithSameName.hasDuplicates( ignoreWhenIdentical ) )
        {
            return;
        }

        if ( findAllDuplicates )
        {
            duplicateClasses.add( pathToClassFile );
        }
        else
        {
            Artifact previousArtifact = classesWithSameName.previous().getArtifactThisClassWasFoundIn();

            StringBuilder buf = new StringBuilder( message == null ? "Duplicate class found:" : message );
            buf.append( '\n' );
            buf.append( "\n  Found in:" );
            buf.append( "\n    " );
            buf.append( previousArtifact );
            buf.append( "\n    " );
            buf.append( artifact );
            buf.append( "\n  Duplicate classes:" );
            buf.append( "\n    " );
            buf.append( pathToClassFile );
            buf.append( '\n' );
            buf.append( "There may be others but <findAllDuplicates> was set to false, so failing fast" );
            throw new EnforcerRuleException( buf.toString() );
        }
    }
}
