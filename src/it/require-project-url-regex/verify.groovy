File file = new File( basedir, "build.log" );
assert file.exists();
String text = file.getText("utf-8");
assert text.contains('org.codehaus.mojo.extraenforcer.model.RequireProjectUrl failed with message');
assert text.contains('The project URL http://example.org does not match the required regex: ^http://localhost/.*');
return true;
