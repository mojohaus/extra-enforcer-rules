File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");


assert text.contains( '[INFO] Adding ignore: module-info' )
assert text.contains( '[DEBUG] Ignore: module-info maps to regex ^module-info(\\.class)?$' )
assert text.contains( '[INFO] Adding ignore: META-INF/versions/*/module-info' )
assert text.contains( '[DEBUG] Ignore: META-INF/versions/*/module-info maps to regex ^META-INF/versions/.*/module-info(\\.class)?$' )
assert text.contains( '[INFO] BUILD SUCCESS' )

return true;
