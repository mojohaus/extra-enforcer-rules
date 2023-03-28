package org.codehaus.mojo.extraenforcer.model;

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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * This rule enforces the presence of an URL and optionally matches the URL against a regex
 *
 * @since 1.0-beta-4
 */
@Named("requireProjectUrl")
public class RequireProjectUrl extends AbstractEnforcerRule {
    /**
     * The regex that the url must match. Default is a non-empty URL
     */
    private String regex = "^.+$";

    private final MavenProject project;

    @Inject
    RequireProjectUrl(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        try {
            if (project.getUrl() == null) {
                throw new EnforcerRuleException("The project URL is not defined");
            }
            Matcher matcher = Pattern.compile(regex).matcher(project.getUrl());
            if (!matcher.matches()) {
                throw new EnforcerRuleException(
                        "The project URL " + project.getUrl() + " does not match the required regex: " + regex);
            }
        } catch (PatternSyntaxException e) {
            throw new EnforcerRuleError("Invalid regex \"" + regex + "\": " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.format("RequireProjectUrl[regex=%s]", regex);
    }
}
