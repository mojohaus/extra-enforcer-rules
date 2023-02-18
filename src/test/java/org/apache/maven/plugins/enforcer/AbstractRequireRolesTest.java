package org.apache.maven.plugins.enforcer;

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
import java.util.Set;

import org.junit.Test;

public class AbstractRequireRolesTest {
    private static final String CSV_TO_SPLIT = "a,b,c";
    private static final String CSV_WITH_SPACES_TO_SPLIT = " a, b ,c ";

    @Test
    public void testCsvSplitSize() {
        Set<String> values = AbstractRequireRoles.splitCsvToSet(CSV_TO_SPLIT);

        assert values.size() == 3;
    }

    @Test
    public void testCsvSplitExpectedElements() {
        Set<String> values = AbstractRequireRoles.splitCsvToSet(CSV_TO_SPLIT);

        assert values.contains("a");
        assert values.contains("b");
        assert values.contains("c");
    }

    @Test
    public void testCsvSplitTrimsValues() {
        Set<String> values = AbstractRequireRoles.splitCsvToSet(CSV_WITH_SPACES_TO_SPLIT);

        assert values.contains("a");
        assert values.contains("b");
        assert values.contains("c");
    }
}
