package org.apache.maven.plugins.enforcer;

import org.apache.maven.artifact.Artifact;

public class JarUtils
{
    public static boolean isJarFile( Artifact artifact )
    {
        return artifact.getFile().isFile() && "jar".equals( artifact.getType() );
    }

}
