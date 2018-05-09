File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains( '[DEBUG] log4j-api-2.9.0.jar => ' )

return true;
