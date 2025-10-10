File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

// only direct dependency
assert text.contains('[ERROR] Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EnforceManagedDepsRule(requireManagedDeps) failed with message:')
assert text.contains('[ERROR] The following 1 dependencies are NOT using a managed version:')






