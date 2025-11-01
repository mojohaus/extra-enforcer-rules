// Verify that the build succeeded
File buildLog = new File(basedir, "build.log")
assert buildLog.exists()

String log = buildLog.text

// The build should succeed
assert !log.contains("BUILD FAILURE"), "Build should have succeeded"

// Verify that the child's configuration includes both parent and child ignoreClasses
// Look for the child module's configuration section
def childConfig = log.find(/(?s)Building Test banDuplicateClasses inheritance - Child.*?end configuration/)
assert childConfig != null, "Could not find child configuration in build log"

// Check that both ignore patterns are present in the child's effective configuration
assert childConfig.contains("org.apache.commons.logging.*"), "Parent's ignoreClass pattern should be inherited"
assert childConfig.contains("some.other.Class"), "Child's ignoreClass pattern should be present"

// Check that dependencies from both parent and child are present
assert childConfig.contains("commons-logging"), "Parent's dependency configuration should be inherited"
assert childConfig.contains("jcl-over-slf4j"), "Child's dependency configuration should be present"

println "Test passed: banDuplicateClasses correctly combined configuration from parent and child POMs"
println "  - Parent's ignoreClass pattern 'org.apache.commons.logging.*' was inherited"
println "  - Child's ignoreClass pattern 'some.other.Class' was added"
println "  - Parent's dependency configuration for 'commons-logging' was inherited"
println "  - Child's dependency configuration for 'jcl-over-slf4j' was added"
