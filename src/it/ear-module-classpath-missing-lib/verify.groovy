def log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EarModuleClasspath failed with message:')
assert log.contains('Did not find shared library lib/missing-1.0.jar in manifest of ejb-1.0.jar')
