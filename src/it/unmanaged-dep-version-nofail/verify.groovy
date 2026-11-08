File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

// level WARN reports the unmanaged version and lets the build pass
assert text.contains('[WARNING] The following 1 dependencies are NOT using a managed version:')
assert text.contains('  - junit:junit:jar')
assert text.contains('[INFO] Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EnforceManagedDepsRule(requireManagedDeps) passed')
