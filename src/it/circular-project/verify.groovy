final File file = new File( basedir, "build.log" );
final String buf = file.getText("utf-8");

assert buf.contains('org.apache.maven.plugins.enforcer.BanProjectCircularDependencies failed with message');

assert buf.contains('org.slf4j:slf4j-api found in transitive dependencies of org.slf4j:childb');

return true;
