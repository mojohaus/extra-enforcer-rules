File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert text.contains( "[DEBUG] 	META-INF/versions/9/org/apache/logging/log4j/util/ProcessIdUtil.class => major=53,minor=0" )
assert text.contains( "[DEBUG] 	META-INF/versions/9/org/apache/logging/log4j/util/StackLocator.class => major=53,minor=0" )

return true;
