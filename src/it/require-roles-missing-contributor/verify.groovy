File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");
assert text.contains('org.codehaus.mojo.extraenforcer.model.RequireContributorRoles failed with message');
assert text.contains('Found no contributor representing role(s) \'[quality manager]\'');
return true;
