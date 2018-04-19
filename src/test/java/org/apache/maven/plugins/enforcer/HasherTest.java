package org.apache.maven.plugins.enforcer;

import org.apache.maven.artifact.Artifact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;

public class HasherTest
{
    private static final String PATH_TO_CLASS_FILE = HasherTest.class.getName().replace( '.', '/' ) + ".class";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    private ClassFileHelper classFileHelper;

    @Before
    public void beforeEachTest()
    {
        classFileHelper = new ClassFileHelper( temporaryFolder );
    }

    @Test
    public void generateHashReturnsCorrectHashForFileInDirectory() throws Exception
    {
        ClassFile classFile = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "this is the file's contents" );
        Artifact artifact = classFile.getArtifactThisClassWasFoundIn();
        Hasher hasher = new Hasher( PATH_TO_CLASS_FILE );

        String hash = hasher.generateHash( artifact );

        assertEquals( "ae23844bc5db9bfad3fbbe5426d89dd3", hash );
    }

    @Test
    public void generateHashReturnsCorrectHashForFileInJar() throws Exception
    {
        ClassFile classFile = classFileHelper.createJarWithContent( "temp.jar", PATH_TO_CLASS_FILE, "this is the file's contents" );
        Artifact artifact = classFile.getArtifactThisClassWasFoundIn();
        Hasher hasher = new Hasher( PATH_TO_CLASS_FILE );

        String hash = hasher.generateHash( artifact );

        assertEquals( "ae23844bc5db9bfad3fbbe5426d89dd3", hash );
    }
}
