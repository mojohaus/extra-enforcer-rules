File file = new File( basedir, "build.log" );
assert file.exists();

String text = file.getText("utf-8").replace('\\', '/');

assert text.contains( "Encoding US-ASCII is hard to detect." )
assert text.contains( "Files not encoded in US-ASCII:" )
assert text.contains( "src/main/resources/utf8.txt==>UTF-8".replace('/', File.separator) )
assert text.contains( "src/main/resources/ascii.txt==>ISO-8859-1".replace('/', File.separator) )
assert text.contains( "src/main/resources/iso88591.txt==>ISO-8859-1".replace('/', File.separator) )

return true;
