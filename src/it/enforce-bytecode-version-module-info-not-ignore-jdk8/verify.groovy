File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert !text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( '[INFO] Restricted to JDK 8 yet org.ow2.asm:asm:jar:6.0:runtime contains module-info.class targeted to JDK 9' )

return true;
