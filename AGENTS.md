# Agent guide for extra-enforcer-rules

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Layout

- One jar module with Maven Enforcer rules in `org.codehaus.mojo.extraenforcer`: `dependencies`
  (`BanDuplicateClasses`, `BanCircularDependencies`, `EnforceBytecodeVersion`,
  `EnforceManagedDepsRule`), `encoding` (`RequireEncoding`) and `model` (the `Require*` rules for
  roles, project URL and diverging properties). `sisu-maven-plugin` generates the component index,
  so a new rule needs the Sisu `@Named` annotation like the existing ones.
- It is not a Maven plugin, so there is no plugin descriptor. The plugin report is disabled in the
  POM, and the rules are documented in the `apt` pages in `src/site`.
- The enforcer version the rules build and test against is the `maven-enforcer-plugin.version`
  property. The build bans `org.apache.maven.enforcer:enforcer-rules` (MOJO-1949): depend on
  `enforcer-api` only.

## Build gates

- Main code must use the Java 8 API: mojo-parent sets `maven.compiler.release` to 8.
- Spotless runs on JDK 17 and later only. It applies the formatting in `process-sources` on a local
  build and checks it when the `CI` environment variable is set. It formats Java with
  palantir-java-format and also reformats Markdown, so run `mvn spotless:apply` after editing the
  README or this file.
- The rules compile against Maven 3.9.16 (`mavenVersion`); building the project needs Maven 3.9
  (`minimalMavenBuildVersion`). There is no `<prerequisites>` element, because the artifact is not a
  plugin.
- `.github/workflows/maven.yml` calls the shared `apache/maven-gh-actions-shared`
  `maven-verify.yml@v5` workflow. Branches in this repository build under the push event.

## Unit tests

- JUnit 5 and Mockito. The rules are tested directly, with `ArtifactBuilder` and `ClassFileHelper`
  in `dependencies` for building artifacts and class files, and the files in `src/test/resources`
  for the encoding rule.
- Prefer a unit test. Add an integration test only for what needs a real Maven build: the rule
  configuration read from a POM, resolved dependencies, or multi-module reactors.

## Integration tests

- They live in `src/it`, one directory per test, and run the goal `enforcer:enforce` (set in the
  invoker configuration) through the `run-its` profile. A mock repository (`mrm-maven-plugin`)
  serves `src/it-repo` to them through `src/it/settings.xml`; the invoker plugin installs the rules
  into `target/local-repo`, and `addTestClassPath` makes the test classes available to the
  `verify.groovy` scripts, such as `BanDuplicateClassesLogParser`.
- Run a subset:

  ```shell
  mvn -B -P run-its verify -Dinvoker.test=require-encoding-utf8,require-project-url-present
  ```
- They run in parallel, one per CPU core (`invoker.parallelThreads` 1C), so a test must not depend
  on another test's output.
- The forked builds get `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -XX:+UseSerialGC`, so every JVM
  prints a `Picked up JAVA_TOOL_OPTIONS` line on stderr.
- Write `verify.groovy` as `assert` statements. Invoker fails a script whose last expression is
  anything other than `true` or `null`, so a trailing `list.each { }` fails the test; use a `for`
  loop.

## Dependencies

- Dependabot excludes `src/it`, `src/it-repo` and `src/test/resources` from version updates
  (`exclude-paths`), so the fixtures keep their old versions on purpose. Security updates can still
  propose them; close those pull requests.
- Deliberate pins in `.github/dependabot.yml`: every `org.apache.maven` artifact is ignored, so
  Maven and Enforcer versions change by hand through `mavenVersion` and
  `maven-enforcer-plugin.version`; Mockito stays below 5 because it needs Java 11 and the build
  still runs on JDK 8.

## Pull requests

- Pull requests are squash- or rebase-merged; merge commits are disabled. Release Drafter builds
  the release notes from the titles and labels, so the title should describe the change for a
  user.

