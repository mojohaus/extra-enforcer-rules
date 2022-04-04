File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( '[DEBUG] log4j-api-2.17.2.jar => ' )

return true;
