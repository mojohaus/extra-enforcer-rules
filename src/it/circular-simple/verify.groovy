File file = new File(basedir, "build.log");
assert file.exists();

String text = file.getText("utf-8");


assert text.contains('ERROR] Rule 0: org.codehaus.mojo.extraenforcer.dependencies.BanCircularDependencies failed with message:')
assert text.contains('[ERROR] Circular Dependency found. Your project\'s groupId:artifactId combination must not exist in the list of direct or transitive dependencies.')

return true;
