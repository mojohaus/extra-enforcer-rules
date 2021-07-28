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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.maven.plugins.enforcer.JarUtils.isJarFile;

/**
 * Utility class to generate hashes/checksums for binary files.
 * Typically used to generate a hashes for .class files to compare
 * those files for equality.
 */
public class Hasher
{
    /** the path to the .class file. Example: org/apache/maven/Stuff.class */
    private final String classFilePath;

    /**
     * Constructor.
     * @param classFilePath The path to the .class file. This is the file we'll generate a hash for.
     *                      Example: org/apache/maven/Stuff.class
     */
    public Hasher( String classFilePath )
    {
        this.classFilePath = classFilePath;
    }

    /**
     * @param artifact The artifact (example: jar file) which contains the {@link #classFilePath}.
     *                 We'll generate a hash for the class file inside this artifact.
     * @return generate a hash/checksum for the .class file in the provided artifact.
     */
    public String generateHash( Artifact artifact )
    {
        File artifactFile = artifact.getFile();
        try
        {
            if ( artifactFile.isDirectory() )
            {
                return hashForFileInDirectory( artifactFile );
            }
            else if ( isJarFile( artifact ) )
            {
                return hashForFileInJar( artifactFile );
            }
            else
            {
                throw new IllegalArgumentException(
                  "Expected either a directory or a jar file, but instead received: " + artifactFile );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Problem calculating hash for " + artifact + " " + classFilePath, e );
        }
    }

    private String hashForFileInDirectory( File artifactFile ) throws IOException
    {
        try ( InputStream inputStream = new FileInputStream( new File( artifactFile, classFilePath ) ) )
        {
            return sha256Hex( inputStream );
        }
      }

    private String hashForFileInJar( File artifactFile ) throws IOException
    {
        try( JarFile jar = new JarFile( artifactFile );
        InputStream inputStream = jar.getInputStream( jar.getEntry( classFilePath )))
        {
            return sha256Hex( inputStream );
        }
    }

}
