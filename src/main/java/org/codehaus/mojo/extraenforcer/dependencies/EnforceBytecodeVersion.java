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

import javax.inject.Inject;
import javax.inject.Named;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.artifact.filter.AbstractStrictPatternArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.eclipse.aether.RepositorySystem;

/**
 * Enforcer rule that will check the bytecode version of each class of each dependency.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Java_class_file#General_layout">Java class file general layout</a>
 * @since 1.0-alpha-4
 */
@Named("enforceBytecodeVersion")
public class EnforceBytecodeVersion extends AbstractResolveDependencies {
    private static final Map<String, Integer> JDK_TO_MAJOR_VERSION_NUMBER_MAPPING = new LinkedHashMap<>();
    /**
     * Default ignores when validating against jdk < 9 because <code>module-info.class</code> will always have level 1.9.
     */
    private static final String[] DEFAULT_CLASSES_IGNORE_BEFORE_JDK_9 = {"module-info"};

    private static final Pattern MULTIRELEASE = Pattern.compile("META-INF/versions/(\\d+)/.*");

    static {
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.1", 45);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.2", 46);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.3", 47);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.4", 48);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.5", 49);
        // Java6
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.6", 50);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("6", 50);
        // Java7
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.7", 51);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("7", 51);
        // Java8
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("8", 52);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.8", 52);
        // Java9
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("9", 53);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.9", 53);

        // Java10
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("10", 54);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.10", 54);

        // Java11
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("11", 55);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.11", 55);

        // Java 12
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("12", 56);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.12", 56);

        // Java 13
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("13", 57);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.13", 57);

        // Java 14
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("14", 58);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.14", 58);

        // Java 15
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("15", 59);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.15", 59);

        // Java 16
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("16", 60);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.16", 60);

        // Java 17
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("17", 61);
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("1.17", 61);

        // Java 18
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("18", 62);

        // Java 19
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("19", 63);

        // Java 20
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("20", 64);

        // Java 21
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("21", 65);

        // Java 22
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("22", 66);

        // Java 23
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("23", 67);

        // Java 24
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("24", 68);

        // Java 25
        JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.put("25", 69);
    }

    @Inject
    protected EnforceBytecodeVersion(MavenSession session, RepositorySystem repositorySystem) {
        super(session, repositorySystem);
    }

    static String renderVersion(int major, int minor) {
        if (minor == 0) {
            for (Map.Entry<String, Integer> entry : JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.entrySet()) {
                if (major == entry.getValue()) {
                    return "JDK " + entry.getKey();
                }
            }
        }
        return major + "." + minor;
    }

    private String message;

    /**
     * JDK version as used for example in the maven-compiler-plugin: 8, 11 and so on. If in need of more precise
     * configuration please see {@link #maxJavaMajorVersionNumber} and {@link #maxJavaMinorVersionNumber} Mandatory if
     * {@link #maxJavaMajorVersionNumber} not specified.
     */
    private String maxJdkVersion;

    /**
     * If unsure, don't use that parameter. Better look at {@link #maxJdkVersion}. Mandatory if {@link #maxJdkVersion}
     * is not specified. see http://en.wikipedia.org/wiki/Java_class_file#General_layout
     */
    int maxJavaMajorVersionNumber = -1;

    /**
     * This parameter is here for potentially advanced use cases, but it seems like it is actually always 0.
     *
     * @see #maxJavaMajorVersionNumber
     * @see <a href="https://en.wikipedia.org/wiki/Java_class_file#General_layout">Java class file general layout</a>
     */
    int maxJavaMinorVersionNumber = 0;

    /**
     * @see AbstractStrictPatternArtifactFilter
     */
    private List<String> includes, excludes;

    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    /**
     * Process module-info and Multi-Release JAR classes if true
     */
    private boolean strict = false;

    private List<IgnorableDependency> ignorableDependencies = new ArrayList<>();

    @Override
    protected void handleArtifacts(Set<Artifact> artifacts) throws EnforcerRuleException {
        computeParameters();

        // look for banned dependencies
        Set<Artifact> foundExcludes = checkDependencies(filterArtifacts(artifacts));

        // if any are found, fail the check but list all of them
        if (foundExcludes != null && !foundExcludes.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            if (message != null) {
                buf.append(message).append("\n");
            }
            for (Artifact artifact : foundExcludes) {
                buf.append(getErrorMessage(artifact));
            }
            message = buf + "Use 'mvn dependency:tree' to locate the source of the banned dependencies.";

            throw new EnforcerRuleException(message);
        }
    }

    protected CharSequence getErrorMessage(Artifact artifact) {
        return "Found Banned Dependency: " + artifact.getId() + "\n";
    }

    private void computeParameters() throws EnforcerRuleException {
        if (maxJdkVersion != null && maxJavaMajorVersionNumber != -1) {
            throw new IllegalArgumentException("Only maxJdkVersion or maxJavaMajorVersionNumber "
                    + "configuration parameters should be set. Not both.");
        }
        if (maxJdkVersion == null && maxJavaMajorVersionNumber == -1) {
            throw new IllegalArgumentException(
                    "Exactly one of maxJdkVersion or maxJavaMajorVersionNumber options should be set.");
        }
        if (maxJdkVersion != null) {
            Integer needle = JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.get(maxJdkVersion);
            if (needle == null) {
                throw new IllegalArgumentException(
                        "Unknown JDK version given. Should be something like \"8\", \"11\", \"17\", \"21\" ..");
            }
            maxJavaMajorVersionNumber = needle;
            if (!strict && needle < 53) {
                IgnorableDependency ignoreModuleInfoDependency = new IgnorableDependency();
                ignoreModuleInfoDependency.applyIgnoreClasses(DEFAULT_CLASSES_IGNORE_BEFORE_JDK_9, false);
                ignorableDependencies.add(ignoreModuleInfoDependency);
            }
        }
        if (maxJavaMajorVersionNumber == -1) {
            throw new EnforcerRuleException("maxJavaMajorVersionNumber must be set in the plugin configuration");
        }
        if (ignoreClasses != null) {
            IgnorableDependency ignorableDependency = new IgnorableDependency();
            ignorableDependency.applyIgnoreClasses(ignoreClasses, false);
            ignorableDependencies.add(ignorableDependency);
        }
    }

    protected Set<Artifact> checkDependencies(Set<Artifact> dependencies) throws EnforcerRuleException {
        long beforeCheck = System.currentTimeMillis();
        Set<Artifact> problematic = new LinkedHashSet<>();
        for (Artifact artifact : dependencies) {
            getLog().debug("Analyzing artifact " + artifact);
            String problem = isBadArtifact(artifact);
            if (problem != null) {
                getLog().info(problem);
                problematic.add(artifact);
            }
        }
        getLog().debug("Bytecode version analysis took " + (System.currentTimeMillis() - beforeCheck) + " ms");
        return problematic;
    }

    private String isBadArtifact(Artifact a) throws EnforcerRuleException {
        File f = a.getFile();
        getLog().debug("isBadArtifact() a:" + a + " Artifact getFile():" + a.getFile());
        if (f == null) {
            // This happens if someone defines dependencies instead of dependencyManagement in a pom file
            // which packaging type is pom.
            return null;
        }
        if (!f.getName().endsWith(".jar")) {
            return null;
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(f);
            getLog().debug(f.getName() + " => " + f.getPath());
            byte[] magicAndClassFileVersion = new byte[8];
            JAR:
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    for (IgnorableDependency i : ignorableDependencies) {
                        if (i.matches(entry.getName())) {
                            continue JAR;
                        }
                    }

                    try (InputStream is = jarFile.getInputStream(entry)) {
                        int total = magicAndClassFileVersion.length;
                        while (total > 0) {
                            int read =
                                    is.read(magicAndClassFileVersion, magicAndClassFileVersion.length - total, total);
                            if (read == -1) {
                                throw new EOFException(f.toString());
                            }
                            total -= read;
                        }
                    }

                    int minor = (magicAndClassFileVersion[4] << 8) + magicAndClassFileVersion[5];
                    int major = (magicAndClassFileVersion[6] << 8) + magicAndClassFileVersion[7];

                    // Assuming regex match is more expensive, verify bytecode versions first

                    if ((major > maxJavaMajorVersionNumber)
                            || (major == maxJavaMajorVersionNumber && minor > maxJavaMinorVersionNumber)) {

                        Matcher matcher = MULTIRELEASE.matcher(entry.getName());

                        if (!strict && matcher.matches()) {
                            Integer maxExpectedMajor = JDK_TO_MAJOR_VERSION_NUMBER_MAPPING.get(matcher.group(1));

                            if (maxExpectedMajor == null) {
                                getLog().warn("Unknown bytecodeVersion for " + a + " : " + entry.getName() + ": got "
                                        + maxExpectedMajor + " class-file-version");
                            } else if (major > maxExpectedMajor) {
                                getLog().warn("Invalid bytecodeVersion for " + a + " : " + entry.getName()
                                        + ": expected lower or equal to " + maxExpectedMajor + ", but was " + major);
                            }
                        } else {
                            return "Restricted to "
                                    + renderVersion(maxJavaMajorVersionNumber, maxJavaMinorVersionNumber)
                                    + " yet " + a + " contains " + entry.getName() + " targeted to "
                                    + renderVersion(major, minor);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new EnforcerRuleException("IOException while reading " + f, e);
        } catch (IllegalArgumentException e) {
            throw new EnforcerRuleException("Error while reading " + f, e);
        } finally {
            closeQuietly(jarFile);
        }
        return null;
    }

    private void closeQuietly(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException ioe) {
                getLog().warn("Exception catched while closing " + jarFile.getName() + ": " + ioe.getMessage());
            }
        }
    }

    public void setMaxJavaMajorVersionNumber(int maxJavaMajorVersionNumber) {
        this.maxJavaMajorVersionNumber = maxJavaMajorVersionNumber;
    }

    public void setMaxJavaMinorVersionNumber(int maxJavaMinorVersionNumber) {
        this.maxJavaMinorVersionNumber = maxJavaMinorVersionNumber;
    }

    /**
     * Process module-info and Multi-Release JAR classes if true
     * @param strict the strictness to set
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    // copied from RequireReleaseDeps
    /*
     * Filter the dependency artifacts according to the includes and excludes If includes and excludes are both null,
     * the original set is returned.
     * @param dependencies the list of dependencies to filter
     * @return the resulting set of dependencies
     */
    private Set<Artifact> filterArtifacts(Set<Artifact> dependencies) {
        if (includes == null && excludes == null) {
            return dependencies;
        }

        AndArtifactFilter filter = new AndArtifactFilter();
        if (includes != null) {
            filter.add(new StrictPatternIncludesArtifactFilter(includes));
        }
        if (excludes != null) {
            filter.add(new StrictPatternExcludesArtifactFilter(excludes));
        }

        Set<Artifact> result = new HashSet<>();
        for (Artifact artifact : dependencies) {
            if (filter.include(artifact)) {
                result.add(artifact);
            }
        }
        return result;
    }
}
