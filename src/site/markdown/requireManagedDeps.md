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

# Require Managed Deps

This rule checks that the dependencies declared in the project's POM do not set a version of their
own. A dependency that has a `version` element fails the rule, so every version has to come from
`dependencyManagement`. The rule reads the original model, before inheritance and interpolation,
and by default also checks the dependencies in profiles.

The following parameters are supported by this rule:

* **checkProfiles** - a boolean, if `true` (default) the dependencies declared in profiles are checked as well.

* **regexIgnored** - a list of regular expressions. A dependency whose `groupId` or `artifactId` matches one of them is not checked.

Sample Plugin Configuration:

```xml
<project>
  [...]
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version><!-- latest release --></version> <!-- find the latest version at https://maven.apache.org/plugins/maven-enforcer-plugin/ -->
        <executions>
          <execution>
            <id>enforce-managed-deps</id>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <requireManagedDeps>
                  <checkProfiles>false</checkProfiles>
                  <regexIgnored>
                    <regexIgnored>^com\.example\.legacy$</regexIgnored>
                  </regexIgnored>
                </requireManagedDeps>
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
    </plugins>
  </build>
  [...]
</project>
```

