package org.apache.maven.plugins.enforcer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class ClassFileTest
{
    private static final String PATH_TO_CLASS_FILE = ClassFileTest.class.getName().replace( '.', '/' ) + ".class";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final ClassFileHelper classFileHelper = new ClassFileHelper( tempFolder );

    @Test
    public void getHashComputesHashOfFile() throws Exception
    {
        ClassFile classFile = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "the content of the file" );

        assertEquals( "7e47820975c51a762e63caa95cc76e45", classFile.getHash() );
    }

    @Test
    public void getHashReturnsConsistentHashWhenInvokedTwice() throws Exception
    {
        ClassFile classFile = classFileHelper.createWithContent( PATH_TO_CLASS_FILE, "file content" );

        String hash1 = classFile.getHash();
        String hash2 = classFile.getHash();

        assertEquals( "d10b4c3ff123b26dc068d43a8bef2d23", hash1 );
        assertEquals( hash1, hash2 );
    }

}
