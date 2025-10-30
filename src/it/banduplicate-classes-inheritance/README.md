# Test: banDuplicateClasses Configuration Inheritance

This integration test demonstrates how to properly inherit and extend `banDuplicateClasses` configuration from a parent POM.

## Problem

When defining `ignoreClasses` or `dependencies` in a parent POM, child POMs cannot simply add more entries without using special Maven configuration combination attributes. By default, Maven replaces arrays/lists during POM inheritance rather than merging them.

## Solution

Use the `combine.children="append"` attribute on collection elements (`<ignoreClasses>` and `<dependencies>`) in the child POM to append to the parent's configuration.

### Parent POM Example

```xml
<plugin>
  <artifactId>maven-enforcer-plugin</artifactId>
  <configuration>
    <rules>
      <banDuplicateClasses>
        <ignoreClasses>
          <ignoreClass>org.apache.commons.logging.*</ignoreClass>
        </ignoreClasses>
        <dependencies>
          <dependency>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
            <ignoreClasses>
              <ignoreClass>org.apache.commons.logging.impl.WeakHashtable*</ignoreClass>
            </ignoreClasses>
          </dependency>
        </dependencies>
      </banDuplicateClasses>
    </rules>
  </configuration>
</plugin>
```

### Child POM Example

```xml
<plugin>
  <artifactId>maven-enforcer-plugin</artifactId>
  <configuration>
    <rules>
      <banDuplicateClasses>
        <ignoreClasses combine.children="append">
          <ignoreClass>some.other.Class</ignoreClass>
        </ignoreClasses>
        <dependencies combine.children="append">
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>jcl-over-slf4j</artifactId>
            <ignoreClasses>
              <ignoreClass>org.slf4j.*</ignoreClass>
            </ignoreClasses>
          </dependency>
        </dependencies>
      </banDuplicateClasses>
    </rules>
  </configuration>
</plugin>
```

### Result

The child POM's effective configuration will include entries from both parent and child:

**ignoreClasses:**
- `org.apache.commons.logging.*` (from parent)
- `some.other.Class` (from child)

**dependencies:**
- commons-logging configuration (from parent)
- jcl-over-slf4j configuration (from child)

## Maven Configuration Combination

The `combine.children="append"` attribute is a standard Maven feature for controlling how configurations are merged during POM inheritance.

**Important Notes:**
- The attribute must be placed on the collection element in the **child** POM (not the parent)
- Works for both `<ignoreClasses>` and `<dependencies>` elements
- Without this attribute, child configuration will **replace** parent configuration

See [Maven POM Reference](https://maven.apache.org/pom.html#Plugins) for more details on configuration combination.
