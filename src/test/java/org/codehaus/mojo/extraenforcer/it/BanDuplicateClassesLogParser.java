package org.codehaus.mojo.extraenforcer.it;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * LogParser for BanDuplicateClasses Enforcer Rule used for integration test verification by parsing the messages from the BanDuplicateClasses rule.
 */
public class BanDuplicateClassesLogParser {
    private static final String DUPLICATE_START_LINE =
            "[ERROR] Rule 0: org.codehaus.mojo.extraenforcer.dependencies.BanDuplicateClasses failed with message:";

    private final File logFile;

    public BanDuplicateClassesLogParser(File logFile) {
        this.logFile = logFile;
    }

    /**
     * Parse out the violations from BanDuplicateClasses in a log file.
     *
     * @return A map where the keys are sets of jars which contain duplicate classes, and the values are sets of classes
     *         which are duplicated in those jars.
     * @throws IOException if the reader for the log file throws one
     */
    public Map<Set<String>, Set<String>> parse() throws IOException {
        Map<Set<String>, Set<String>> duplicates = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (DUPLICATE_START_LINE.equals(line.trim())) {
                    break;
                }
            }
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[INFO] ---")) {
                    break;
                }
                if (line.equals("  Found in:")) {
                    Set<String> jars = readFoundInJars(reader);
                    Set<String> classes = readDuplicateClasses(reader);
                    duplicates.put(jars, classes);
                }
            }
        }
        return duplicates;
    }

    private static Set<String> readFoundInJars(BufferedReader reader) throws IOException {
        Set<String> jars = new TreeSet<>();
        for (String line = reader.readLine();
                line != null && !"  Duplicate classes:".equals(line);
                line = reader.readLine()) {
            jars.add(line.trim());
        }
        return jars;
    }

    private static Set<String> readDuplicateClasses(BufferedReader reader) throws IOException {
        Set<String> classes = new TreeSet<>();
        for (String line = reader.readLine(); line != null && line.length() > 0; line = reader.readLine()) {
            classes.add(line.trim());
        }
        return classes;
    }
}
