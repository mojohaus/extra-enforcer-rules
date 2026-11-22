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

# Ban Circular Dependencies

This rule fails the build when the `groupId:artifactId` of the project is also one of its own direct or
transitive dependencies.

The following parameters are supported by this rule:

* **message** - an optional message to use instead of the default one when a circular dependency is found.

* **resolutionScope**, **scopes**, **ignoredScopes**, **ignoreOptionals**, **searchTransitive** - select the
  dependencies to check, with the same meaning as for [banDuplicateClasses](./banDuplicateClasses.html).

Sample Plugin Configuration:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>
      <id>enforce-no-circular-dependencies</id>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <banCircularDependencies/>
        </rules>
      </configuration>
    </execution>
  </executions>
  <dependencies>
    <dependency>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>extra-enforcer-rules</artifactId>
    </dependency>
  </dependencies>
</plugin>
```

