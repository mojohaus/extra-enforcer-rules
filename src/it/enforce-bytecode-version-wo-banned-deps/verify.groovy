File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8");

assert !text.contains("Found Banned Dependency: org.hibernate:hibernate-annotations:jar:3.4.0.GA")
assert !text.contains("Found Banned Dependency: org.hibernate:ejb3-persistence:jar:1.0.2.GA")
assert !text.contains("Found Banned Dependency: org.hibernate:hibernate-commons-annotations:jar:3.1.0.GA")
assert !text.contains("Found Banned Dependency: org.hibernate:hibernate-core:jar:3.3.0.SP1")
assert !text.contains("Found Banned Dependency: javax.transaction:jta:jar:1.1")
assert !text.contains("Found Banned Dependency: org.slf4j:slf4j-api:jar:1.4.2")
assert !text.contains("Found Banned Dependency: dom4j:dom4j:jar:1.6.1")

return true;
