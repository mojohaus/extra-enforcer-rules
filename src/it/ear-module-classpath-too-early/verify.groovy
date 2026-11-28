def log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EarModuleClasspath failed with message:')
assert log.contains('Bind the earModuleClasspath rule to the package phase or later.')
