# Extra Enforcer Rules

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/extra-enforcer-rules.svg?label=License)](https://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/extra-enforcer-rules.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.mojo/extra-enforcer-rules)
[![GitHub CI](https://github.com/mojohaus/extra-enforcer-rules/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/mojohaus/extra-enforcer-rules/actions/workflows/maven.yml?query=branch%3Amaster)

Extra rules for the [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/)
that are not part of its [standard rules](https://maven.apache.org/enforcer/enforcer-rules/index.html):
banning duplicate classes and circular dependencies, limiting the bytecode version of dependencies,
requiring developer and contributor roles, a project URL, a source encoding or managed dependency
versions, and checking that a property differs from the one in another project.

The rules are not a Maven plugin. Add the artifact as a dependency of `maven-enforcer-plugin`, as
described on the [usage page](https://www.mojohaus.org/extra-enforcer-rules/usage.html). Building
the project needs Maven 3.9.0 or later and Java 8 or later (the rules themselves target Java 8); the
[site](https://www.mojohaus.org/extra-enforcer-rules/) documents each rule.

Contributor and agent notes are in [AGENTS.md](AGENTS.md).
