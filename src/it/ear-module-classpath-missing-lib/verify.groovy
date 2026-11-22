def log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EarModuleClasspath failed with message:')
assert log.contains('ejb-1.0.jar: the Class-Path entry lib/missing-1.0.jar resolves to lib/missing-1.0.jar, which is not in the EAR')
assert !log.contains('lib/log4j-api-2.17.2.jar resolves')
