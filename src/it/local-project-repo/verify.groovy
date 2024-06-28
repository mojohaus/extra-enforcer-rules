File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText( "utf-8" );

assert text.contains( '[DEBUG] Analyzing artifact dumy:dumy-local-repo:pom:1.0' )

return true;
