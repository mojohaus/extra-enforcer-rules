<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Require Encoding

This rule checks that the files below the project directory are encoded in the required encoding. The encoding of a file is detected from its content; a file whose encoding cannot be detected is not reported.

## Parameters

* **encoding** - the encoding the files must have. Defaults to the `project.build.sourceEncoding` property.

* **includes** - a comma or pipe separated list of patterns for the files to check, relative to the project directory.

* **excludes** - a comma or pipe separated list of patterns for the files to skip.

* **useDefaultExcludes** - a boolean, if `true` (default) the default excludes for SCM files, such as `.git/**`, are applied.

* **failFast** - a boolean, if `true` (default) the rule fails at the first file with a wrong encoding, otherwise it reports all of them.

* **acceptAsciiSubset** - a boolean, if `true` a file that only contains US-ASCII characters is also accepted when the required encoding is UTF-8, ISO-8859-1 or ISO-8859-15. Default `false`.

* **acceptIso8859Subset** - a boolean, if `true` an ISO-8859-1 file is also accepted when the required encoding is ISO-8859-15. Default `false`.

A pattern is an Ant style glob such as `src/**/*.java`. To match with a regular expression instead, write it as `%regex[...]`; commas and pipes inside the brackets belong to the expression and do not separate patterns. A regular expression has to match the whole path relative to the project directory.

```xml
<requireEncoding>
  <encoding>UTF-8</encoding>
  <includes>src/**/*.java,%regex[src/main/resources/.*\.(properties|txt)]</includes>
</requireEncoding>
```

