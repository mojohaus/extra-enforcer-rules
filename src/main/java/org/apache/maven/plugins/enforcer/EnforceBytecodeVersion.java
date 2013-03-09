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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.AbstractBanDependencies;

/**
 * Enforcer rule that will check the bytecode version of each class of each dependency. FIXME : Special jar like
 * Hibernate, that embeds .class files with many different compilers, but are only loaded under right condition, is
 * gonna difficult to handle here. Maybe a solution would be to introduce some kind of exclusion.
 * 
 * @see http://en.wikipedia.org/wiki/Java_class_file#General_layout
 */
public class EnforceBytecodeVersion
    extends AbstractBanDependencies
{
    private static final Map<String, Integer> jdkToMajorVersionNumberMapping = new HashMap<String, Integer>();
    static
    {
        jdkToMajorVersionNumberMapping.put( "1.1", 45 );
        jdkToMajorVersionNumberMapping.put( "1.2", 46 );
        jdkToMajorVersionNumberMapping.put( "1.3", 47 );
        jdkToMajorVersionNumberMapping.put( "1.4", 48 );
        jdkToMajorVersionNumberMapping.put( "1.5", 49 );
        jdkToMajorVersionNumberMapping.put( "1.6", 50 );
        jdkToMajorVersionNumberMapping.put( "1.7", 51 );
        jdkToMajorVersionNumberMapping.put( "1.8", 52 );
    }

    /**
     * JDK version as used for example in the maven-compiler-plugin : 1.5, 1.6 and so on. If in need of more precise
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

    private EnforcerRuleHelper helper;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        computeParameters();

        this.helper = helper;
        super.execute( helper );
    }

    private void computeParameters()
        throws EnforcerRuleException
    {
        if ( maxJdkVersion != null && maxJavaMajorVersionNumber != -1 )
        {
            throw new IllegalArgumentException(
                                                "Only maxJdkVersion or maxJavaMajorVersionNumber configuration parameters should be set. Not both." );
        }
        if ( maxJdkVersion == null && maxJavaMajorVersionNumber == -1 )
        {
            throw new IllegalArgumentException(
                                                "Exactly one of maxJdkVersion or maxJavaMajorVersionNumber options should be set." );
        }
        if ( maxJdkVersion != null )
        {
            Integer needle = jdkToMajorVersionNumberMapping.get( maxJdkVersion );
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
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected Set<Artifact> checkDependencies( Set/* <Artifact> */dependencies, Log log )
        throws EnforcerRuleException
    {
        Set<Artifact> problematic = new LinkedHashSet<Artifact>();
        for ( Iterator<Artifact> it = dependencies.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            getLog().debug( "Analyzing artifact " + artifact );
            if ( isBadArtifact( artifact ) )
            {
                getLog().info( "Artifact " + artifact + " contains .class compiled with incorrect version" );
                problematic.add( artifact );
            }
        }
        return problematic;
    }

    private boolean isBadArtifact( Artifact a )
        throws EnforcerRuleException
    {
        File f = a.getFile();
        if ( !f.getName().endsWith( ".jar" ) )
        {
            return false;
        }
        try
        {
            JarFile jarFile = new JarFile( f );
            getLog().debug( f.getName() + " => " + f.getPath() );
            byte[] magicAndClassFileVersion = new byte[8];
            for ( Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); )
            {
                JarEntry entry = e.nextElement();
                if ( !entry.isDirectory() && entry.getName().endsWith( ".class" ) )
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append( "\t" ).append( entry.getName() ).append( " => " );
                    InputStream is = jarFile.getInputStream( entry );
                    int read = is.read( magicAndClassFileVersion );
                    is.close();
                    assert read != 8;

                    int minor = ( magicAndClassFileVersion[4] << 8 ) + magicAndClassFileVersion[5];
                    int major = ( magicAndClassFileVersion[6] << 8 ) + magicAndClassFileVersion[7];
                    builder.append( "major=" ).append( major ).append( ",minor=" ).append( minor );
                    getLog().debug( builder.toString() );

                    if ( ( major > maxJavaMajorVersionNumber )
                        || ( major == maxJavaMajorVersionNumber && minor > maxJavaMinorVersionNumber ) )
                    {
                        return true;
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "Error while reading jar file", e );
        }
        return false;
    }

    private Log getLog()
    {
        return helper.getLog();
    }

    public void setMaxJavaMajorVersionNumber( int maxJavaMajorVersionNumber )
    {
        this.maxJavaMajorVersionNumber = maxJavaMajorVersionNumber;
    }

    public void setMaxJavaMinorVersionNumber( int maxJavaMinorVersionNumber )
    {
        this.maxJavaMinorVersionNumber = maxJavaMinorVersionNumber;
    }
}
