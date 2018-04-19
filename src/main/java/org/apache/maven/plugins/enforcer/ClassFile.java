package org.apache.maven.plugins.enforcer;

import org.apache.maven.artifact.Artifact;

/**
 * This class represents a binary class file.
 *
 * The path to the class file should be a relative, file system path to the
 * actual file. Examples:
 *
 *   - CORRECT: org/apache/maven/Stuff.class
 *   - NO:  /org/apache/maven/Stuff.class
 *   - NO:  org.apache.maven.Stuff
 *   - NO:  maven.jar!org.apache.maven.Stuff
 *   - NO:  maven.jar!/org/apache/maven/Stuff.class
 *   - NO:  /path/to/some/directory/org.apache.maven.Stuff
 *   - NO:  /path/to/some/directory/org/apache/maven/Stuff.class
 *
 * The file must exist in either a directory or a jar file, but the path
 * of the directory/jar is not included in the class file path. Rather,
 * it's included in the Artifact. See {@link Artifact#getFile()}
 */
public class ClassFile
{
    /** the path to the .class file. Example: org/apache/maven/Stuff.class */
    private final String classFilePath;
    private final Artifact artifactThisClassWasFoundIn;
    private final Hasher hasher;
    private String lazilyComputedHash;

    public ClassFile( String classFilePath, Artifact artifactThisClassWasFoundIn )
    {
        this.classFilePath = classFilePath;
        this.artifactThisClassWasFoundIn = artifactThisClassWasFoundIn;
        this.hasher = new Hasher( classFilePath );
    }

    /**
     * The path to the .class file. Example: org/apache/maven/Stuff.class
     */
    public String getClassFilePath()
    {
        return classFilePath;
    }

    public Artifact getArtifactThisClassWasFoundIn()
    {
        return artifactThisClassWasFoundIn;
    }

    public String getHash()
    {
        if ( lazilyComputedHash == null )
        {
            lazilyComputedHash = hasher.generateHash( artifactThisClassWasFoundIn );
        }

        return lazilyComputedHash;
    }

}
