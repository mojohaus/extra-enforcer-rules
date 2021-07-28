File file = new File( basedir, "build.log" );
assert file.exists();

def text = file.getText("utf-8");

try {
    assert text.find(/IOException while reading .*hibernate-annotations-3.4.0.GA.jar/)
} finally {
    File jar = new File( localRepositoryPath, "org/hibernate/hibernate-annotations/3.4.0.GA/hibernate-annotations-3.4.0.GA.jar" );
    if (jar.exists()) {
        jar.delete();
    }
}

return true;
