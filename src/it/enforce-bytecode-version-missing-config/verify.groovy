File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains("Exactly one of maxJdkVersion or maxJavaMajorVersionNumber options should be set.")

return true;
