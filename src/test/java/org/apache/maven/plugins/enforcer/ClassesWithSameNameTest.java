package org.apache.maven.plugins.enforcer;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassesWithSameNameTest
{
    /** logging thresholds are: DEBUG=0, INFO=1, WARNING=2, ERROR=3, FATAL ERROR=4, DISABLED=5 */
    private static final int LOGGING_THRESHOLD = 5;
    private static final String PATH_TO_CLASS_FILE = ClassesWithSameNameTest.class.getName().replace( '.', '/' ) + ".class";

    /** this is an alias to make the code read better */
    private static final boolean DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE = true;

    /** this is an alias to make the code read better */
    private static final boolean DETERMINE_DUPLICATES_BY_NAME = false;

    private static final Log log = new DefaultLog( new ConsoleLogger( LOGGING_THRESHOLD, "test" ) );

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassFileHelper classFileHelper;

    @Before
    public void beforeEachTest()
    {
        classFileHelper = new ClassFileHelper( temporaryFolder );
    }

    /**
     * Verify the method returns true when there's a simple duplication (meaning, the names match).
     * This check is only concerned if we found a duplicate. It should still fail even if the two *.class
     * files are exactly the same.
     */
    @Test
    public void hasDuplicatesShouldReturnTrueWhenClassNameIsDuplicate() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        boolean result = classesWithSameName.hasDuplicates( DETERMINE_DUPLICATES_BY_NAME );

        assertTrue( result );
    }

    @Test
    public void hasDuplicatesShouldReturnFalseWhenClassNameIsDuplicateButBytecodeIsIdentical() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "content matches in both" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "content matches in both" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        boolean result = classesWithSameName.hasDuplicates( DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE );

        assertFalse( result );
    }

    @Test
    public void hasDuplicatesShouldReturnFalseWhenClassHasNoDuplicates() throws Exception
    {
        ClassFile classFile = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile );

        boolean result = classesWithSameName.hasDuplicates( DETERMINE_DUPLICATES_BY_NAME );

        assertFalse( result );
    }

    /**
     * This test compares two files with the same exact relative path (so they look like the same file)
     * but they exist in two different folders and their bytecode doesn't match. This should be considered
     * a duplicate.
     *
     * We set the test up so it fails if it finds the same class name/path twice (meaning, it does not compare
     * bytecode).
     */
    @Test
    public void hasDuplicatesShouldReturnTrueWhenClassNameIsDuplicateButBytecodeDiffers() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "1" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "2" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        boolean result = classesWithSameName.hasDuplicates( DETERMINE_DUPLICATES_BY_NAME );

        assertTrue( result );
    }

    /**
     * This test compares two files with the same exact relative path (so they look like the same file)
     * but they exist in two different folders and their bytecode doesn't match. This should be considered
     * a duplicate.
     *
     * We set the test up so it finds duplicates only if the bytecode differs.
     */
    @Test
    public void hasDuplicatesShouldReturnFalseWhenClassNameIsDuplicateAndBytecodeDiffers() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "1" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "2" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        boolean result = classesWithSameName.hasDuplicates( DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE );

        assertTrue( result );
    }

    /**
     * This tests the normal condition where we just output the class file path.
     */
    @Test
    public void toOutputStringOutputsPlainArtifactWhenJustNamesAreDuplicate() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        String actualOutput = classesWithSameName.toOutputString( DETERMINE_DUPLICATES_BY_NAME );

        assertEquals( PATH_TO_CLASS_FILE, actualOutput );
    }

    /**
     * Verify the output string contains all the information, specifically, it should list which artifacts
     * were an exact match (meaning, the bytecode of the .class files were identical). This helps users
     * determine which artifacts they can ignore when fix the BanDuplicateClasses error.
     */
    @Test
    public void toOutputStringOutputsTwoArtifactsWhereBytecodeIsExactMatch() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "content matches in both" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "content matches in both" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        String actualOutput = classesWithSameName.toOutputString( DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE );

        String expectedOutput = PATH_TO_CLASS_FILE + "  -- the bytecode exactly matches in these: " +
            classFile1.getArtifactThisClassWasFoundIn() + " and " + classFile2.getArtifactThisClassWasFoundIn();
        assertEquals( expectedOutput, actualOutput );
    }

    /**
     * This verifies the output string contains all the information, specifically, it should list
     * which artifacts were an exact match. In this case we have 4 artifacts: 1, 2, 3, and 4.
     * The bytecode of 1 and 2 match each other, the bytecode of 3 and 4 match each other, but
     * 1 and 2 don't match 3 and 4.
     */
    @Test
    public void toOutputStringOutputsFourArtifactsWhereBytecodeIsExactMatchInTwoAndExactMatchInOtherTwo() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 1 and 2" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 1 and 2" );
        ClassFile classFile3 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 3 and 4" );
        ClassFile classFile4 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 3 and 4" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2, classFile3, classFile4 );

        String actualOutput = classesWithSameName.toOutputString( DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE );

        String expectedOutput = PATH_TO_CLASS_FILE + "  -- the bytecode exactly matches in these: " +
            classFile1.getArtifactThisClassWasFoundIn() + " and " + classFile2.getArtifactThisClassWasFoundIn() +
            "; and more exact matches in these: " +
            classFile3.getArtifactThisClassWasFoundIn() + " and " + classFile4.getArtifactThisClassWasFoundIn();
        assertEquals( expectedOutput, actualOutput );
    }

    /**
     * The method should return the 2nd-to-last element in the last, but if there's only 1 element
     * there's no 2nd-to-last element to return.
     */
    @Test( expected = IllegalArgumentException.class )
    public void previousShouldThrowIfOnlyOneArtifact() throws Exception
    {
        ClassFile classFile = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 1 and 2" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile );

        classesWithSameName.previous();
    }

    @Test
    public void previousShouldReturn2ndToLastElement() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 1 and 2" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content of 1 and 2" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );

        ClassFile previous = classesWithSameName.previous();

        assertEquals( classFile1, previous );
    }

    @Test
    public void addShouldAddArtifact() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1 );

        assertEquals( 1, classesWithSameName.getAllArtifactsThisClassWasFoundIn().size() );
        classesWithSameName.add( classFile2 );
        assertEquals( 2, classesWithSameName.getAllArtifactsThisClassWasFoundIn().size() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void addShouldThrowWhenClassNameDoesNotMatch() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( "some/other/path.class", "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1 );

        classesWithSameName.add( classFile2 );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorShouldThrowWhenClassNameDoesNotMatch() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( "some/other/path.class", "" );

        new ClassesWithSameName( log, classFile1, classFile2 );
    }

    @Test
    public void getAllArtifactsThisClassWasFoundInShouldReturnAllArtifacts() throws Exception
    {
        ClassFile classFile1 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassFile classFile2 = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "" );
        ClassesWithSameName classesWithSameName = new ClassesWithSameName( log, classFile1, classFile2 );
        Artifact artifact1 = classFile1.getArtifactThisClassWasFoundIn();
        Artifact artifact2 = classFile2.getArtifactThisClassWasFoundIn();

        Set<Artifact> result = classesWithSameName.getAllArtifactsThisClassWasFoundIn();

        assertEquals( 2, result.size() );
        assertTrue( result.contains( artifact1 ) );
        assertTrue( result.contains( artifact2 ) );
    }
}
