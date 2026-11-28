# Ear Module Classpath

This rule checks that every `Class-Path` entry in the manifests of the modules of an EAR points to a file or
directory in that EAR. A module that refers to a library the EAR does not contain fails at class loading time on the
application server, not at build time, so the rule moves the error into the build.

The rule is available since 1.12.2.

## What the rule checks

The rule reads the built EAR, not the project model, and inspects the manifest of these modules:

* EJB modules (`<ejb>` in `META-INF/application.xml`),
* application client modules (`<java>`), and
* web modules (`<web-uri>`).

Without a `META-INF/application.xml`, it inspects the archives at the top level of the EAR that end in `.jar` or
`.war`. It does not look at connector (`.rar`) modules, or at the libraries in the library directory.

It resolves each `Class-Path` entry as the JAR specification describes: a relative URL, taken relative to the
location of the module in the EAR. For `ejbs/orders.jar`, the entry `lib/a.jar` means `ejbs/lib/a.jar` and
`../lib/a.jar` means `lib/a.jar`. Percent escapes such as `%20` are decoded, and an entry that ends in a slash must
name a directory. The rule fails for every entry whose target is not in the EAR, including one that points above the
root of the EAR, and reports all of them at once. It ignores absolute URLs, because they do not name a file in the
EAR.

The maven-ear-plugin 3.x already rewrites the `Class-Path` entries that come from the dependencies of a module so
that they point to the libraries in the EAR, so those entries pass. The rule finds the remaining case: an entry that
you add by hand, for example with `manifestEntries`, or that a mapping change leaves dangling.

The rule does not check that a library is used, or that the `Class-Path` of a module is complete.

## Parameters and phase

This rule has no parameters.

It reads the EAR file, which exists after the `package` phase, so bind it to the `package` phase or later. The
default phase of the `enforce` goal is `validate`, where the rule fails with a message that names the phase. In a
project whose packaging is not `ear`, the rule does nothing, so you can configure it in a parent POM.

Sample Plugin Configuration:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>
      <id>enforce-ear-module-classpath</id>
      <phase>package</phase>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <earModuleClasspath/>
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

A failing build reports each unresolved entry:

```text
ejb-1.0.jar: the Class-Path entry lib/missing-1.0.jar resolves to lib/missing-1.0.jar, which is not in the EAR
```

