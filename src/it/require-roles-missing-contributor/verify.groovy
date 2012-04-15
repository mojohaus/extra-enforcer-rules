File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");
assert text.contains('org.apache.maven.plugins.enforcer.RequireContributorRoles failed with message');
assert text.contains('undeclared role(s) \'[quality manager]\' for contributors');
return true;
