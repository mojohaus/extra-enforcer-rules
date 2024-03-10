File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

assert text.contains('[DEBUG] Ignore: module-info maps to regex ^module-info(\\.class)?$')
assert !text.contains('[WARNING] Invalid bytecodeVersion for com.fasterxml.jackson.core:jackson-core:jar:2.13.0:runtime')
