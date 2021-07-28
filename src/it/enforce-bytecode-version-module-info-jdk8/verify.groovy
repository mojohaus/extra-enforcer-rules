File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( '[DEBUG] Ignore: module-info maps to regex ^module-info(\\.class)?$' )

return true;
