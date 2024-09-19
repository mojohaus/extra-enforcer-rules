package org.codehaus.mojo.extraenforcer.dependencies;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassFileTest {
    private static final String PATH_TO_CLASS_FILE =
            ClassFileTest.class.getName().replace('.', '/') + ".class";

    private ClassFileHelper classFileHelper;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        classFileHelper = new ClassFileHelper(tempFolder);
    }

    @Test
    void getHashComputesHashOfFile() throws Exception {
        ClassFile classFile = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "the content of the file");

        assertEquals("7b7f48e1c0e847133d8881d5743d253756bf44e490e2252556ad4816a0a77b67", classFile.getHash());
    }

    @Test
    void getHashReturnsConsistentHashWhenInvokedTwice() throws Exception {
        ClassFile classFile = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content");

        String hash1 = classFile.getHash();
        String hash2 = classFile.getHash();

        assertEquals("e0ac3601005dfa1864f5392aabaf7d898b1b5bab854f1acb4491bcd806b76b0c", hash1);
        assertEquals(hash1, hash2);
    }
}
