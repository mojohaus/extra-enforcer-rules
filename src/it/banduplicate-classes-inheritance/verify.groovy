// Verify that the build succeeded
File buildLog = new File(basedir, "build.log")
assert buildLog.exists()

String log = buildLog.text

// The build should succeed because we're ignoring both Log and LogFactory
// which are the duplicate classes between commons-logging and jcl-over-slf4j
assert !log.contains("Duplicate class found")
assert !log.contains("BUILD FAILURE")

println "Test passed: banDuplicateClasses correctly combined ignoreClasses from parent and child POMs"
