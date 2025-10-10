File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

// only direct dependency
assert text.contains('[INFO] Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EnforceManagedDepsRule(requireManagedDeps) passed')




