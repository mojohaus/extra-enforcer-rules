package org.apache.maven.plugins.enforcer;

import org.apache.maven.artifact.Artifact;
import org.junit.Test;

import static org.apache.maven.plugins.enforcer.ArtifactBuilder.newBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JarUtilsTest {
    /**
     * "Sunny day" test: the method should return true for a jar artifact.
     */
    @Test
    public void isJarFileShouldReturnTrueForJarFile() {
        Artifact artifact = newBuilder().withType("jar").build();
        assertTrue(JarUtils.isJarFile(artifact));
    }

    /**
     * The method should return false when the artifact is a directory (for example:
     * a folder with a bunch of packages/class files in it).
     */
    @Test
    public void isJarFileShouldReturnFalseForDirectory() {
        Artifact artifact = newBuilder().withType("jar").withAnyDirectory().build();
        assertFalse(JarUtils.isJarFile(artifact));
    }

    /**
     * The method should return false whenever we're passed an artifact who's type is
     * not "jar". For example: a war or a zip file.
     */
    @Test
    public void isJarFileShouldReturnFalseWhenArtifactTypeIsNotJar() {
        Artifact artifact = newBuilder().withType("war").build();
        assertFalse(JarUtils.isJarFile(artifact));
    }
}
