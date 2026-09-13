String log = new File(basedir, 'build.log').getText('UTF-8')
def builds = log.split(/(?m)^\[INFO\] Building managed-app 1\.0-SNAPSHOT[ \t]*$/)
assert builds.size() == 4 : 'Expected three managed-scope invocations'
String text = builds[0]

// Invoker checks each invocation's exit status; also check the reasons for failure.
int duplicateFailures = text.readLines().count { it == '[ERROR] Duplicate classes found:' }
assert duplicateFailures == 5 : 'Expected duplicates after classpath selection and transitive mediation'
assert text.contains('[ERROR]     test.resolution-scope:api:jar:1.0-SNAPSHOT:compile') : 'Missing runtime API after direct-scope filtering'
assert text.contains('test.resolution-scope:api:jar:1.0-SNAPSHOT:provided') : 'Unexpected scope-resolution log output'
assert text.contains('test.resolution-scope:container:jar:1.0-SNAPSHOT:compile') : 'Unexpected scope-resolution log output'
assert text.contains('example/Api.class') : 'Unexpected scope-resolution log output'
assert text.contains('[ERROR]     test.resolution-scope:api:jar:2.0-SNAPSHOT:compile') : 'The nearer transitive API must win mediation'
assert text.contains('[ERROR]     test.resolution-scope:api:jar:2.0-SNAPSHOT:provided') : 'The runtime child must inherit provided scope'
assert text.contains("Invalid resolutionScope 'complie': expected compile, runtime, or test.") : 'Unexpected scope-resolution log output'

builds[1..2].each { build ->
    assert build.contains('[ERROR] Duplicate classes found:') : 'Missing managed-scope duplicate failure'
    assert build.contains('[ERROR]     test.resolution-scope:api:jar:2.0-SNAPSHOT:compile') : 'Missing managed compile API'
    assert build.contains('[ERROR]     test.resolution-scope:container:jar:1.0-SNAPSHOT:compile') : 'Missing duplicate container'
    assert build.contains('example/Api.class') : 'Missing duplicate class'
}
assert builds[3].contains('BUILD SUCCESS') : 'The managed runtime dependency should not be scanned'

String classpath = new File(basedir, 'mediated-app/target/compile-classpath.txt').getText('UTF-8').trim()
def apiJars = classpath.split(java.util.regex.Pattern.quote(File.pathSeparator)).findAll {
    new File(it).name.startsWith('api-')
}
assert apiJars.size() == 1 : 'Expected exactly one API on the Maven compile classpath'
assert new File(apiJars[0]).name == 'api-2.0-SNAPSHOT.jar' : 'Maven should select the nearer API version'
String mediatedLog = log.substring(log.lastIndexOf('[INFO] Building mediated-app 1.0-SNAPSHOT'))
        .split(/(?m)^\[INFO\] Building app 1\.0-SNAPSHOT[ \t]*$/)[0]
assert mediatedLog.contains('Searching for duplicate classes in ' + apiJars[0]) : 'The rule must scan Maven\'s selected API'
assert !mediatedLog.contains('api-1.0-SNAPSHOT.jar') : 'The discarded API must not be scanned'
assert !mediatedLog.contains('[ERROR] Duplicate classes found:') : 'The duplicate only exists in the discarded API'

// MNG-8041: legacy dependency:build-classpath omits this API, so it cannot be the oracle.
// Check each invocation for the duplicate in the required transitive dependency instead.
def appBuilds = log.split(/(?m)^\[INFO\] Building app 1\.0-SNAPSHOT[ \t]*$/).drop(1)
assert appBuilds.size() == 15 : 'Expected fifteen app rule invocations'
[4: 'runtime-provided', 5: 'runtime-test', 14: 'compile-test'].each { index, scenario ->
    String build = appBuilds[index]
    assert build.contains('[ERROR] Duplicate classes found:') : 'Missing duplicate failure for ' + scenario
    assert build.contains('[ERROR]     test.resolution-scope:api:jar:1.0-SNAPSHOT:compile') : 'Missing transitive compile API for ' + scenario
    assert build.contains('[ERROR]     test.resolution-scope:container:jar:1.0-SNAPSHOT:compile') : 'Missing duplicate container for ' + scenario
    assert build.contains('example/Api.class') : 'Missing duplicate class for ' + scenario
    assert !build.contains('[ERROR]     test.resolution-scope:api:jar:1.0-SNAPSHOT:provided') : 'The provided dependency must not be scanned for ' + scenario
    assert !build.contains('[ERROR]     test.resolution-scope:api:jar:1.0-SNAPSHOT:test') : 'The test dependency must not be scanned for ' + scenario
}
return true
