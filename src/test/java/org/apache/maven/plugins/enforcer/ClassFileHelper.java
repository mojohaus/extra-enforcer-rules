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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.junit.rules.TemporaryFolder;

/**
 * Test utility to make writing tests with {@link ClassFile}s easier.
 */
public class ClassFileHelper
{
    private static int uniqueId = 0;

    private final TemporaryFolder temporaryFolder;

    public ClassFileHelper( TemporaryFolder temporaryFolder )
    {
        this.temporaryFolder = temporaryFolder;
    }

    public ClassFile createWithContent( String pathToClassFile, String fileContents ) throws IOException
    {
        uniqueId++;
        String uniqueIdStr = Integer.toString( uniqueId );

        File tempDirectory = createTempDirectory( uniqueIdStr );
        createClassFile( tempDirectory, pathToClassFile, fileContents );

        Artifact artifact = ArtifactBuilder.newBuilder()
            .withFileOrDirectory( tempDirectory )
            .withVersion( uniqueIdStr )
            .withType( "some type that isn't 'jar' so our code assumes it's a directory" )
            .build();

        return new ClassFile( pathToClassFile, artifact );
    }

    public ClassFile createJarWithContent( String jarFileName, String pathToClassFile, String fileContents )
        throws IOException
    {
        uniqueId++;
        String uniqueIdStr = Integer.toString( uniqueId );

        File tempDirectory = createTempDirectory( uniqueIdStr );
        File tempJarFile = new File( tempDirectory, jarFileName );

        try ( JarOutputStream outStream = new JarOutputStream( new FileOutputStream( tempJarFile ) ) )
        {
            outStream.putNextEntry( new JarEntry( pathToClassFile ) );
            outStream.write( fileContents.getBytes( StandardCharsets.UTF_8 ) );
        }

        Artifact artifact = ArtifactBuilder.newBuilder()
            .withFileOrDirectory( tempJarFile )
            .withVersion( uniqueIdStr )
            .withType( "jar" )
            .build();

        return new ClassFile( pathToClassFile, artifact );
    }

    private File createTempDirectory( String uniqueIdStr )
    {
        try
        {
            return temporaryFolder.newFolder( uniqueIdStr );
        }
        catch ( IOException exception )
        {
            throw new RuntimeException( "unable to create temporary folder", exception );
        }
    }

    private void createClassFile( File directory, String pathToClassFile, String fileContents ) throws IOException
    {
        File file = new File( directory, pathToClassFile );

        boolean madeDirs = file.getParentFile().mkdirs();
        if ( !madeDirs )
        {
            throw new RuntimeException( "unable to create parent directories for " + file );
        }

        file.createNewFile();

        try ( FileWriter writer = new FileWriter( file ) )
        {
            writer.write( fileContents );
        }
    }
}
