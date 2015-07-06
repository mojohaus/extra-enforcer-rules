import java.util.jar.JarFile
import java.util.zip.ZipFile

def ejb = new File(basedir, 'ejb/target/ejb-1.0.jar')
assert ejb.isFile()
def classPath = new JarFile(ejb).withCloseable { it.manifest.mainAttributes.getValue('Class-Path') }
assert classPath == 'log4j-api-2.17.2.jar'

def ear = new File(basedir, 'ear/target/ear-1.0.ear')
assert ear.isFile()
new ZipFile(ear).withCloseable { zip ->
    assert zip.getEntry('ejb-1.0.jar') != null
    assert zip.getEntry('lib/log4j-api-2.17.2.jar') != null
}

def log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Rule 0: org.codehaus.mojo.extraenforcer.dependencies.EarModuleClasspath passed')
