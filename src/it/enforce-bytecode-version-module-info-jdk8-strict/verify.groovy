File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert ! text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( 'Found Banned Dependency: org.ow2.asm:asm:jar:6.0' )

return true;
