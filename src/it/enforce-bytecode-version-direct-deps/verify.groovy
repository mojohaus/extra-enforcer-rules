File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

// only direct dependency
assert text.contains('[DEBUG] Analyzing artifact junit:junit:jar')
assert text.contains('[DEBUG] Analyzing artifact org.slf4j:slf4j-simple:jar')

// no transitive dependencies
assert !text.contains('[DEBUG] Analyzing artifact org.hamcrest:hamcrest-core:jar')
assert !text.contains('[DEBUG] Analyzing artifact org.slf4j:slf4j-api:jar')
