// Verify that the build succeeded
// This means banDuplicateClasses did not report false duplicates
// when a direct 'provided' dependency overrides a transitive 'compile' dependency

def buildLog = new File(basedir, 'build.log')
assert buildLog.exists()
assert buildLog.text.contains('BUILD SUCCESS')

// Make sure the enforcer plugin actually ran
assert buildLog.text.contains('maven-enforcer-plugin')

return true
