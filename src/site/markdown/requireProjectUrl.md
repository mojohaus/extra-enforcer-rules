# Require Project URL

This rule checks that the project defines a `url` and, optionally, that the URL matches a regular expression.
It is useful in a parent POM that wants every project to point at its own site, because Maven appends the
`artifactId` to an inherited `url`.

The following parameters are supported by this rule:

* **regex** - a regular expression the whole URL must match. Default `^.+$`, which accepts any non-empty URL.

Sample Plugin Configuration:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>
      <id>enforce-project-url</id>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <requireProjectUrl>
            <regex>^https://example\.com/.*</regex>
          </requireProjectUrl>
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

