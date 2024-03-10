File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

assert text.contains('[DEBUG] Ignore: module-info maps to regex ^module-info(\\.class)?$')
assert text.contains('[DEBUG] log4j-api-2.17.2.jar => ')
