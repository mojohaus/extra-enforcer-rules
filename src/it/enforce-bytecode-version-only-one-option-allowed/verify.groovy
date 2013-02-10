File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains("Only maxJdkVersion or maxJavaMajorVersionNumber configuration parameters should be set. Not both.")

return true;
