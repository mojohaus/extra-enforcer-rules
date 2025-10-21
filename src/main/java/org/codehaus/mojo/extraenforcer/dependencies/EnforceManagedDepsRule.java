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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;

/**
 * Enforcer rule that will check that all dependencies are managed.
 */
@Named("requireManagedDeps")
public class EnforceManagedDepsRule extends AbstractEnforcerRule {
    private boolean checkProfiles = true;

    private boolean failOnViolation = true;

    private Pattern[] regexIgnoredPatterns;

    private final MavenProject project;

    @Inject
    public EnforceManagedDepsRule(final MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        final Model model = project.getOriginalModel();

        final Set<Dependency> failed = new HashSet<>();

        getLog().debug("Checking model...");

        check(model, failed);

        if (checkProfiles) {
            getLog().debug("Checking profiles...");
            final List<Profile> profiles = project.getOriginalModel().getProfiles();
            if (profiles != null && !profiles.isEmpty()) {
                for (final Profile profile : profiles) {
                    check(profile, failed);
                }
            }
        }

        final String message = buildFailureMessage(failed);

        if (message != null) {
            if (this.failOnViolation) {
                throw new EnforcerRuleException(message);
            } else {
                getLog().warn(message);
            }
        }
    }

    private String buildFailureMessage(final Set<Dependency> failed) {
        if (failed == null || failed.isEmpty()) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("The following ").append(failed.size()).append(" dependencies are NOT using a managed version:\n");

        for (final Dependency d : failed) {
            sb.append("\n  - ").append(d.getManagementKey());
        }

        return sb.toString();
    }

    private void check(final ModelBase src, final Set<Dependency> failed) {
        final List<Dependency> dependencies = src.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            for (final Dependency dependency : dependencies) {
                getLog().debug("Check dependency: " + dependency.getArtifactId() + ", version: "
                        + dependency.getVersion());
                if (!checkRegex(dependency) && dependency.getVersion() != null) {
                    failed.add(dependency);
                }
            }
        }
    }

    private boolean checkRegex(Dependency dependency) {
        boolean result = false;

        if (regexIgnoredPatterns != null) {

            getLog().debug("Check if dependency is ignored, groupId: " + dependency.getGroupId() + ", artifactId: "
                    + dependency.getArtifactId());

            for (Pattern p : regexIgnoredPatterns) {
                if (p.matcher(dependency.getGroupId()).find()
                        || p.matcher(dependency.getArtifactId()).find()) {
                    result = true;
                    getLog().debug("Found ignored dependency, groupId: " + dependency.getGroupId() + ", artifactId: "
                            + dependency.getArtifactId());
                    break;
                }
            }
        }
        return result;
    }

    public void setCheckProfiles(final boolean checkProfiles) {
        this.checkProfiles = checkProfiles;
    }

    public void setFailOnViolation(final boolean failOnViolation) {
        this.failOnViolation = failOnViolation;
    }

    public void setRegexIgnored(String[] regexIgnored) {
        if (regexIgnored != null) {
            int index = 0;
            regexIgnoredPatterns = new Pattern[regexIgnored.length];
            for (String r : regexIgnored) {
                Pattern p = Pattern.compile(r);
                getLog().debug("Prepared pattern from regexIgnored: " + r);
                regexIgnoredPatterns[index] = p;
                index++;
            }
        }
    }
}
