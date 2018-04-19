package org.apache.maven.plugins.enforcer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

/**
 * Represents one or more class files that have the same exact name.
 *
 * In this case the class name is a relative, file system path to the
 * class file. For example:  org/apache/maven/Stuff.class
 *
 * Example of how we can have two of the same class:
 *   - mockito-core-1.9.5.jar contains org/mockito/Mockito.class
 *   - mockito-all-1.9.5.jar contains org/mockito/Mockito.class
 *
 * With that example you're not supposed to have both on the classpath. Typically
 * you'd choose the maven way (mockito-core) or the convenient-for-non-maven-users
 * way (mockito-all) but not both.
 */
public class ClassesWithSameName
{
    private final Log log;
    /** the path to the .class file. Example: org/apache/maven/Stuff.class */
    private final String classFilePath;
    private final List<ClassFile> list = new ArrayList<ClassFile>();

    /**
     * @param log (required) the logger
     * @param initialClassFile (required) we require at least one class file. Splitting this param from the
     *                         next one lets us require at least one at compile time (instead of runtime).
     * @param additionalClassFiles (optional) additional class files
     */
    public ClassesWithSameName( Log log, ClassFile initialClassFile, ClassFile... additionalClassFiles )
    {
        this.log = log;
        classFilePath = initialClassFile.getClassFilePath();
        list.add( initialClassFile );

        for ( ClassFile classFile : additionalClassFiles )
        {
            throwIfClassNameDoesNotMatch( classFile, classFilePath );
            list.add( classFile );
        }
    }

    public ClassFile previous()
    {
        if ( list.size() > 1 )
        {
            int lastIndex = list.size() - 2;
            return list.get( lastIndex );
        }
        else
        {
            throw new IllegalArgumentException( "there was only " + list.size() +
                " element(s) in the list, so there is no 2nd-to-last element to retrieve " );
        }
    }

    public void add( ClassFile classFile )
    {
        throwIfClassNameDoesNotMatch( classFile, classFilePath );
        list.add( classFile );
    }

    /**
     * @return Return a Set rather than a List so we can use this as the key in another Map.
     *         List.of(3,2,1) doesn't equal List.of(1,2,3) but Set.of(3,2,1) equals Set.of(1,2,3)
     */
    public Set<Artifact> getAllArtifactsThisClassWasFoundIn()
    {
        Set<Artifact> result = new HashSet<Artifact>();

        for ( ClassFile classFile : list )
        {
            result.add( classFile.getArtifactThisClassWasFoundIn() );
        }

        return result;
    }

    public boolean hasDuplicates( boolean ignoreWhenIdentical )
    {
        boolean compareJustClassNames = !ignoreWhenIdentical;
        if ( compareJustClassNames )
        {
            return list.size() > 1;
        }

        if ( list.size() <= 1 )
        {
            return false;
        }

        String previousHash = list.get( 0 ).getHash();
        for ( int i = 1; i < list.size(); i++ )
        {
            String currentHash = list.get( i ).getHash();
            if ( !previousHash.equals( currentHash ) )
            {
                return true;
            }
        }

        log.debug( "ignoring duplicates of class " + classFilePath + " since the bytecode matches exactly" );

        return false;
    }

    public String toOutputString( boolean ignoreWhenIdentical )
    {
        String result = classFilePath;

        if ( list.size() >= 2 && ignoreWhenIdentical )
        {
            StringBuilder duplicationInfo = new StringBuilder();
            for ( Set<Artifact> groupedArtifacts : groupArtifactsWhoseClassesAreExactMatch().values() )
            {
                if ( groupedArtifacts.size() <= 1 )
                {
                    continue;
                }

                if ( duplicationInfo.length() == 0 )
                {
                    duplicationInfo.append( "  -- the bytecode exactly matches in these: " );
                }
                else
                {
                    duplicationInfo.append( "; and more exact matches in these: " );
                }

                duplicationInfo.append( joinWithSeparator( groupedArtifacts, " and " ) );
            }

            result += duplicationInfo.toString();
        }

        return result;
    }

    private static void throwIfClassNameDoesNotMatch( ClassFile classFile, String otherClassFilePath )
    {
        if ( !classFile.getClassFilePath().equals( otherClassFilePath ) )
        {
            throw new IllegalArgumentException( "Expected class " + otherClassFilePath +
                " but got " + classFile.getClassFilePath() );
        }
    }

    private String joinWithSeparator( Set<Artifact> artifacts, String separator )
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for ( Artifact artifact : artifacts )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                result.append( separator );
            }

            result.append( artifact );
        }

        return result.toString();
    }

    private Map<String, Set<Artifact>> groupArtifactsWhoseClassesAreExactMatch()
    {
        Map<String, Set<Artifact>> groupedArtifacts = new LinkedHashMap<String, Set<Artifact>>();

        for ( ClassFile classFile : list )
        {
            Set<Artifact> artifacts = groupedArtifacts.get( classFile.getHash() );
            if ( artifacts == null )
            {
                artifacts = new LinkedHashSet<Artifact>();
            }
            artifacts.add( classFile.getArtifactThisClassWasFoundIn() );

            groupedArtifacts.put( classFile.getHash(), artifacts );
        }

        return groupedArtifacts;
    }
}
