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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.artifact.filter.AbstractStrictPatternArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.codehaus.plexus.util.IOUtil;

/**
 * Enforcer rule that will check the bytecode version of each class of each dependency.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Java_class_file#General_layout">Java class file general layout</a>
 * @since 1.0-alpha-4
 */
public class EnforceBytecodeVersion extends AbstractResolveDependencies
{
    private static final Map<String, Integer> JDK_TO_MAJOR_VERSION_NUMBER_MAPPING = new HashMap<String, Integer>();
    static
    {
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.1", 45 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.2", 46 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.3", 47 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.4", 48 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.5", 49 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.6", 50 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.7", 51 );
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put( "1.8", 52 );
    }

    static String renderVersion( int major, int minor )
    {
        if ( minor == 0 )
        {
            for ( Map.Entry<String, Integer> entry : JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.entrySet() )
            {
                if ( major == entry.getValue() )
                {
                    return "JDK " + entry.getKey();
                }
            }
        }
        return major + "." + minor;
    }
    
    private String message;

    /**
     * JDK version as used for example in the maven-compiler-plugin: 1.5, 1.6 and so on. If in need of more precise
     * configuration please see {@link #maxJavaMajorVersionNumber} and {@link #maxJavaMinorVersionNumber} Mandatory if
     * {@link #maxJavaMajorVersionNumber} not specified.
     */
    private String maxJdkVersion;

    /**
     * If unsure, don't use that parameter. Better look at {@link #maxJdkVersion}. Mandatory if {@link #maxJdkVersion}
     * is not specified. see http://en.wikipedia.org/wiki/Java_class_file#General_layout
     */
    int maxJavaMajorVersionNumber = -1;

    /**
     * This parameter is here for potentially advanced use cases, but it seems like it is actually always 0.
     * 
     * @see #maxJavaMajorVersionNumber
     * @see http://en.wikipedia.org/wiki/Java_class_file#General_layout
     */
    int maxJavaMinorVersionNumber = 0;

    /** Specify if transitive dependencies should be searched (default) or only look at direct dependencies. */
    private boolean searchTransitive = true;

    /** @see AbstractStrictPatternArtifactFilter */
    private List<String> includes, excludes;

    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    /**
     * Optional list of dependency scopes to ignore.
     * {@code test} and {@code provided} make sense here.
     */
    private String[] ignoredScopes;
    
    private List<IgnorableDependency> ignorableDependencies = new ArrayList<IgnorableDependency>();

    @Override
    protected void handleArtifacts( Set<Artifact> artifacts )
        throws EnforcerRuleException
    {
        computeParameters();

        // look for banned dependencies
        Set<Artifact> foundExcludes = checkDependencies( filterArtifacts( artifacts ), getLog() );

        // if any are found, fail the check but list all of them
        if ( foundExcludes != null && !foundExcludes.isEmpty() )
        {
            StringBuilder buf = new StringBuilder();
            if ( message != null )
            {
                buf.append( message + "\n" );
            }
            for ( Artifact artifact : foundExcludes )
            {
                buf.append( getErrorMessage( artifact ) );
            }
            message = buf.toString() + "Use 'mvn dependency:tree' to locate the source of the banned dependencies.";

            throw new EnforcerRuleException( message );
        }
    }
    
    @Override
    protected boolean isSearchTransitive()
    {
        return searchTransitive;
    }

    protected CharSequence getErrorMessage( Artifact artifact )
    {
        return "Found Banned Dependency: " + artifact.getId() + "\n";
    }

    private void computeParameters()
        throws EnforcerRuleException
    {
        if ( maxJdkVersion != null && maxJavaMajorVersionNumber != -1 )
        {
            throw new IllegalArgumentException( "Only maxJdkVersion or maxJavaMajorVersionNumber "
                + "configuration parameters should be set. Not both." );
        }
        if ( maxJdkVersion == null && maxJavaMajorVersionNumber == -1 )
        {
            throw new IllegalArgumentException( "Exactly one of maxJdkVersion or "
                + "maxJavaMajorVersionNumber options should be set." );
        }
        if ( maxJdkVersion != null )
        {
            Integer needle = JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.get( maxJdkVersion );
            if ( needle == null )
            {
                throw new IllegalArgumentException( "Unknown JDK version given. Should be something like \"1.7\"" );
            }
            maxJavaMajorVersionNumber = needle;
        }
        if ( maxJavaMajorVersionNumber == -1 )
        {
            throw new EnforcerRuleException( "maxJavaMajorVersionNumber must be set in the plugin configuration" );
        }
        if ( ignoreClasses != null )
        {
            IgnorableDependency ignorableDependency = new IgnorableDependency();
            ignorableDependency.applyIgnoreClasses( ignoreClasses, false );
            ignorableDependencies.add( ignorableDependency );
        }
    }

    protected Set<Artifact> checkDependencies( Set<Artifact> dependencies, Log log )
        throws EnforcerRuleException
    {
        long beforeCheck = System.currentTimeMillis();
        Set<Artifact> problematic = new LinkedHashSet<Artifact>();
        for ( Iterator<Artifact> it = dependencies.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            getLog().debug( "Analyzing artifact " + artifact );
            String problem = isBadArtifact( artifact );
            if ( problem != null )
            {
                getLog().info( problem );
                problematic.add( artifact );
            }
        }
        getLog().debug( "Bytecode version analysis took " + ( System.currentTimeMillis() - beforeCheck ) + " ms" );
        return problematic;
    }

    private String isBadArtifact( Artifact a )
        throws EnforcerRuleException
    {
        File f = a.getFile();
        if ( !f.getName().endsWith( ".jar" ) )
        {
            return null;
        }
        JarFile jarFile = null;
        try
        {
            jarFile = new JarFile( f );
            getLog().debug( f.getName() + " => " + f.getPath() );
            byte[] magicAndClassFileVersion = new byte[8];
            JAR:
            for ( Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); )
            {
                JarEntry entry = e.nextElement();
                if ( !entry.isDirectory() && entry.getName().endsWith( ".class" ) )
                {
                    for ( IgnorableDependency i : ignorableDependencies )
                    {
                        if ( i.matches( entry.getName() ) )
                        {
                            continue JAR;
                        }
                    }

                    InputStream is = null;
                    try
                    {
                        is = jarFile.getInputStream( entry );
                        int total = magicAndClassFileVersion.length;
                        while ( total > 0 )
                        {
                            int read = is.read( magicAndClassFileVersion, magicAndClassFileVersion.length - total,
                                    total );

                            if ( read == -1 )
                            {
                                throw new EOFException( f.toString() );
                            }

                            total -= read;
                        }

                        is.close();
                        is = null;
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }

                    int minor = ( magicAndClassFileVersion[4] << 8 ) + magicAndClassFileVersion[5];
                    int major = ( magicAndClassFileVersion[6] << 8 ) + magicAndClassFileVersion[7];

                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "\t" + entry.getName() + " => major=" + major + ",minor=" + minor );
                    }

                    if ( ( major > maxJavaMajorVersionNumber )
                        || ( major == maxJavaMajorVersionNumber && minor > maxJavaMinorVersionNumber ) )
                    {
                        return "Restricted to " + renderVersion( maxJavaMajorVersionNumber, maxJavaMinorVersionNumber )
                            + " yet " + a + " contains " + entry.getName() + " targeted to "
                            + renderVersion( major, minor );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "IOException while reading " + jarFile.getName(), e );
        }
        finally
        {
            closeQuietly( jarFile );
        }
        return null;
    }

    private void closeQuietly( JarFile jarFile )
    {
        if ( jarFile != null )
        {
            try
            {
                jarFile.close();
            }
            catch ( IOException ioe )
            {
                getLog().warn( "Exception catched while closing " + jarFile.getName(), ioe );
            }
        }
    }

    public void setMaxJavaMajorVersionNumber( int maxJavaMajorVersionNumber )
    {
        this.maxJavaMajorVersionNumber = maxJavaMajorVersionNumber;
    }

    public void setMaxJavaMinorVersionNumber( int maxJavaMinorVersionNumber )
    {
        this.maxJavaMinorVersionNumber = maxJavaMinorVersionNumber;
    }
    
    /**
     * Sets the search transitive.
     *
     * @param theSearchTransitive the searchTransitive to set
     */
    public void setSearchTransitive( boolean theSearchTransitive )
    {
        this.searchTransitive = theSearchTransitive;
    }

    // copied from RequireReleaseDeps
    /*
     * Filter the dependency artifacts according to the includes and excludes
     * If includes and excludes are both null, the original set is returned.
     *
     * @param dependencies the list of dependencies to filter
     * @return the resulting set of dependencies
     */
    private Set<Artifact> filterArtifacts( Set<Artifact> dependencies )
    {
        if ( includes == null && excludes == null && ignoredScopes == null )
        {
            return dependencies;
        }

        AndArtifactFilter filter = new AndArtifactFilter( );
        if ( includes != null )
        {
            filter.add( new StrictPatternIncludesArtifactFilter( includes ) );
        }
        if ( excludes != null )
        {
            filter.add( new StrictPatternExcludesArtifactFilter( excludes ) );
        }

        Set<Artifact> result = new HashSet<Artifact>();
        for ( Artifact artifact : dependencies )
        {
            if ( ignoredScopes != null && Arrays.asList( ignoredScopes ).contains( artifact.getScope() ) )
            {
                continue;
            }
            if ( filter.include( artifact ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

}
