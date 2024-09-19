package org.codehaus.mojo.extraenforcer.dependencies;

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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ClassesWithSameNameTest {
    /** logging thresholds are: DEBUG=0, INFO=1, WARNING=2, ERROR=3, FATAL ERROR=4, DISABLED=5 */
    private static final int LOGGING_THRESHOLD = 5;

    private static final String PATH_TO_CLASS_FILE =
            ClassesWithSameNameTest.class.getName().replace('.', '/') + ".class";

    /** this is an alias to make the code read better */
    private static final boolean DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE = true;

    /** this is an alias to make the code read better */
    private static final boolean DETERMINE_DUPLICATES_BY_NAME = false;

    private static final EnforcerLogger LOG = mock(EnforcerLogger.class);

    private ClassFileHelper classFileHelper;

    @BeforeEach
    void beforeEachTest(@TempDir Path temporaryFolder) {
        classFileHelper = new ClassFileHelper(temporaryFolder);
    }

    /**
     * Verify the method returns true when there's a simple duplication (meaning, the names match).
     * This check is only concerned if we found a duplicate. It should still fail even if the two *.class
     * files are exactly the same.
     */
    @Test
    void hasDuplicatesShouldReturnTrueWhenClassNameIsDuplicate() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        boolean result = classesWithSameName.hasDuplicates(DETERMINE_DUPLICATES_BY_NAME);

        assertTrue(result);
    }

    @Test
    void hasDuplicatesShouldReturnFalseWhenClassNameIsDuplicateButBytecodeIsIdentical() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "content matches in both");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "content matches in both");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        boolean result = classesWithSameName.hasDuplicates(DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE);

        assertFalse(result);
    }

    @Test
    void hasDuplicatesShouldReturnFalseWhenClassHasNoDuplicates() throws Exception {
        ClassFile classFile = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile);

        boolean result = classesWithSameName.hasDuplicates(DETERMINE_DUPLICATES_BY_NAME);

        assertFalse(result);
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
    void hasDuplicatesShouldReturnTrueWhenClassNameIsDuplicateButBytecodeDiffers() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "1");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "2");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        boolean result = classesWithSameName.hasDuplicates(DETERMINE_DUPLICATES_BY_NAME);

        assertTrue(result);
    }

    /**
     * This test compares two files with the same exact relative path (so they look like the same file)
     * but they exist in two different folders and their bytecode doesn't match. This should be considered
     * a duplicate.
     *
     * We set the test up so it finds duplicates only if the bytecode differs.
     */
    @Test
    void hasDuplicatesShouldReturnFalseWhenClassNameIsDuplicateAndBytecodeDiffers() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "1");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "2");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        boolean result = classesWithSameName.hasDuplicates(DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE);

        assertTrue(result);
    }

    /**
     * This tests the normal condition where we just output the class file path.
     */
    @Test
    void toOutputStringOutputsPlainArtifactWhenJustNamesAreDuplicate() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        String actualOutput = classesWithSameName.toOutputString(DETERMINE_DUPLICATES_BY_NAME);

        assertEquals(PATH_TO_CLASS_FILE, actualOutput);
    }

    /**
     * Verify the output string contains all the information, specifically, it should list which artifacts
     * were an exact match (meaning, the bytecode of the .class files were identical). This helps users
     * determine which artifacts they can ignore when fix the BanDuplicateClasses error.
     */
    @Test
    void toOutputStringOutputsTwoArtifactsWhereBytecodeIsExactMatch() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "content matches in both");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "content matches in both");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        String actualOutput = classesWithSameName.toOutputString(DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE);

        String expectedOutput = PATH_TO_CLASS_FILE + "  -- the bytecode exactly matches in these: "
                + classFile1.getArtifactThisClassWasFoundIn() + " and " + classFile2.getArtifactThisClassWasFoundIn();
        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * This verifies the output string contains all the information, specifically, it should list
     * which artifacts were an exact match. In this case we have 4 artifacts: 1, 2, 3, and 4.
     * The bytecode of 1 and 2 match each other, the bytecode of 3 and 4 match each other, but
     * 1 and 2 don't match 3 and 4.
     */
    @Test
    void toOutputStringOutputsFourArtifactsWhereBytecodeIsExactMatchInTwoAndExactMatchInOtherTwo() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 1 and 2");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 1 and 2");
        ClassFile classFile3 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 3 and 4");
        ClassFile classFile4 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 3 and 4");
        ClassesWithSameName classesWithSameName =
                new ClassesWithSameName(LOG, classFile1, classFile2, classFile3, classFile4);

        String actualOutput = classesWithSameName.toOutputString(DETERMINE_DUPLICATES_BY_NAME_AND_BYTECODE);

        String expectedOutput = PATH_TO_CLASS_FILE + "  -- the bytecode exactly matches in these: "
                + classFile1.getArtifactThisClassWasFoundIn()
                + " and " + classFile2.getArtifactThisClassWasFoundIn() + "; and more exact matches in these: "
                + classFile3.getArtifactThisClassWasFoundIn()
                + " and " + classFile4.getArtifactThisClassWasFoundIn();
        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * The method should return the 2nd-to-last element in the last, but if there's only 1 element
     * there's no 2nd-to-last element to return.
     */
    @Test
    void previousShouldThrowIfOnlyOneArtifact() throws IOException {
        ClassFile classFile = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 1 and 2");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile);
        assertThrows(IllegalArgumentException.class, classesWithSameName::previous);
    }

    @Test
    void previousShouldReturn2ndToLastElement() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 1 and 2");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "file content of 1 and 2");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);

        ClassFile previous = classesWithSameName.previous();

        assertEquals(classFile1, previous);
    }

    @Test
    void addShouldAddArtifact() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1);

        assertEquals(1, classesWithSameName.getAllArtifactsThisClassWasFoundIn().size());
        classesWithSameName.add(classFile2);
        assertEquals(2, classesWithSameName.getAllArtifactsThisClassWasFoundIn().size());
    }

    @Test
    void addShouldThrowWhenClassNameDoesNotMatch() throws IOException {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent("some/other/path.class", "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1);
        assertThrows(IllegalArgumentException.class, () -> classesWithSameName.add(classFile2));
    }

    @Test
    void constructorShouldThrowWhenClassNameDoesNotMatch() throws IOException {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent("some/other/path.class", "");
        assertThrows(IllegalArgumentException.class, () -> {
            new ClassesWithSameName(LOG, classFile1, classFile2);
        });
    }

    @Test
    void getAllArtifactsThisClassWasFoundInShouldReturnAllArtifacts() throws Exception {
        ClassFile classFile1 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassFile classFile2 = classFileHelper.createWithContent(PATH_TO_CLASS_FILE, "");
        ClassesWithSameName classesWithSameName = new ClassesWithSameName(LOG, classFile1, classFile2);
        Artifact artifact1 = classFile1.getArtifactThisClassWasFoundIn();
        Artifact artifact2 = classFile2.getArtifactThisClassWasFoundIn();

        Set<Artifact> result = classesWithSameName.getAllArtifactsThisClassWasFoundIn();

        assertEquals(2, result.size());
        assertTrue(result.contains(artifact1));
        assertTrue(result.contains(artifact2));
    }
}
