String buf = new File( basedir, "build.log" ).text
assert buf =~ /\Qorg.codehaus.mojo.extraenforcer.dependencies.BanDuplicateClasses failed with message\E/ : "Did not fail because of duplicate classes"
