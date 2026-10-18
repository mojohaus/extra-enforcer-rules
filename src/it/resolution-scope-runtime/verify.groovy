String text = new File(basedir, 'build.log').getText('UTF-8')
def builds = text.split(/(?m)^\[INFO\] Building resolution-scope-runtime 1\.0-SNAPSHOT[ \t]*$/).drop(1)
assert builds.size() == 5 : 'Expected five scope-resolution invocations'

// Do not use legacy dependency:build-classpath as the oracle: it exhibits MNG-8041 itself.
[0, 4].each { index ->
    assert builds[index].contains('Restricted to JDK 1.7 yet commons-io:commons-io:jar:2.11.0:compile') : 'Missing transitive compile dependency'
    assert builds[index].contains('[ERROR] Found Banned Dependency: commons-io:commons-io:jar:2.11.0') : 'An excluded test dependency must not eclipse the required version'
    assert !builds[index].contains('[ERROR] Found Banned Dependency: commons-io:commons-io:jar:2.16.1') : 'The test version must not be scanned'
}
[1, 2].each { index ->
    assert builds[index].contains('BUILD SUCCESS') : 'Scan filtering should exclude Commons IO'
    assert !builds[index].contains('[ERROR] Found Banned Dependency:') : 'Unexpected bytecode failure'
}
assert builds[3].contains('Restricted to JDK 1.7 yet commons-io:commons-io:jar:2.16.1:test') : 'Missing test dependency bytecode failure'
assert builds[3].contains('[ERROR] Found Banned Dependency: commons-io:commons-io:jar:2.16.1') : 'Test resolution must scan the direct version'
assert !builds[3].contains('[ERROR] Found Banned Dependency: commons-io:commons-io:jar:2.11.0') : 'The transitive version must lose in test resolution'
return true
