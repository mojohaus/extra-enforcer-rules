final File file = new File( basedir, "build.log" );
final String buf = file.getText("utf-8");

assert buf.contains(/org.codehaus.mojo.extraenforcer.model.RequirePropertyDiverges failed with message/);

assert buf.contains('Property \'project.url\' evaluates to \'http://company/company-parent-pom/child-fail');

return true;
