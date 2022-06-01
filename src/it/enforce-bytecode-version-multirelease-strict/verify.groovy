File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert ! text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( 'Found Banned Dependency: org.apache.logging.log4j:log4j-api:jar:2.17.2' )

return true;
