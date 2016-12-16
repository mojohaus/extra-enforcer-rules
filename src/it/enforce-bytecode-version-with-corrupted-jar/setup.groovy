File file = new File( localRepositoryPath, "org/hibernate/hibernate-annotations/3.4.0.GA/hibernate-annotations-3.4.0.GA.jar" );
if (file.exists()) {
    file.delete();
    file.createNewFile();
}

return true
