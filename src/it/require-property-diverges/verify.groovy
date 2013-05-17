final File file = new File( basedir, "build.log" );
final String buf = file.getText( "utf-8" );

assert buf.contains(/org.apache.maven.plugins.enforcer.RequirePropertyDiverges failed with message/);

assert buf.contains('Property \'project.issueManagement\' is required for this build and not defined in hierarchy at all.');

assert buf.contains('Property \'project.url\' evaluates to \'http://company/company-parent-pom/child-fail');

assert buf.contains('Property \'project.groupId\' evaluates to \'company.project1\'. This does match \'company.project1\' from parent MavenProject: company.project1:fail-property-without-regex-not-overridden:1.0-SNAPSHOT');

assert buf.contains('Property \'myFineProperty\' must be overridden:' + System.getProperty('line.separator') + 'All in-house projects need to override this.');

return true;
