package org.apache.maven.plugins.enforcer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.artifact.Artifact;

import static org.apache.maven.plugins.enforcer.JarUtils.isJarFile;

public class Hasher
{
    /** the path to the .class file. Example: org/apache/maven/Stuff.class */
    private final String classFilePath;

    public Hasher( String classFilePath )
    {
        this.classFilePath = classFilePath;
    }

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

    private String hashForFileInDirectory( File artifactFile ) throws IOException {
        File classFile = new File( artifactFile, classFilePath );
        InputStream inputStream = new FileInputStream( classFile );
        try
        {
            return DigestUtils.md5Hex( inputStream );
        }
        finally
        {
            closeAll( inputStream );
        }
      }

    private String hashForFileInJar( File artifactFile ) throws IOException {
        JarFile jar = new JarFile( artifactFile );
        InputStream inputStream = jar.getInputStream( jar.getEntry( classFilePath ) );
        try
        {
            return DigestUtils.md5Hex( inputStream );
        }
        finally
        {
            closeAll( inputStream, jar );
        }
    }

    private void closeAll( Closeable... closeables ) throws IOException
    {
        IOException firstException = null;

        for ( Closeable closeable : closeables )
        {
            if ( closeable != null )
            {
                try
                {
                    closeable.close();
                }
                catch ( IOException exception )
                {
                    if ( firstException == null )
                    {
                        firstException = exception;
                    }
                }
            }
        }

        if ( firstException != null )
        {
            throw firstException;
        }
    }
}
