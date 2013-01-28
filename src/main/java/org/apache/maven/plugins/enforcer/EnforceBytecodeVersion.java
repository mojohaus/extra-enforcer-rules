package org.apache.maven.plugins.enforcer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.AbstractBanDependencies;

/**
 * Enforcer rule that will check the bytecode version of each class of each dependency.
 * 
 * FIXME : Special jar like Hibernate, that embeds .class files with many different compilers, but
 * are only loaded under right condition, is gonna difficult to handle here. Maybe a solution would
 * be to introduce some kind of exclusion.
 * 
 * @see http://en.wikipedia.org/wiki/Java_class_file#General_layout
 */
public class EnforceBytecodeVersion extends AbstractBanDependencies
{
	// mandatory
	int maxJavaMajorVersionNumber = -1;

	// High value since this param is optional
	int maxJavaMinorVersionNumber = 100;

	private EnforcerRuleHelper helper;

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException
	{
		if (maxJavaMajorVersionNumber == -1)
		{
			throw new EnforcerRuleException(
				"maxJavaMajorVersionNumber must be set in the plugin configuration");
		}
		this.helper = helper;
		super.execute(helper);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Set<Artifact> checkDependencies(Set/* <Artifact> */dependencies, Log log)
		throws EnforcerRuleException
	{
		Set<Artifact> problematic = new LinkedHashSet<Artifact>();
		for (Iterator<Artifact> it = dependencies.iterator(); it.hasNext();)
		{
			Artifact artifact = it.next();
			getLog().debug("Analyzing artifact " + artifact);
			if (isBadArtifact(artifact))
			{
				getLog().info(
					"Artifact " + artifact + " contains .class compiled with incorrect version");
				problematic.add(artifact);
			}
		}
		return problematic;
	}

	private boolean isBadArtifact(Artifact a) throws EnforcerRuleException
	{
		File f = a.getFile();
		if (!f.getName().endsWith(".jar"))
		{
			return false;
		}
		try
		{
			JarFile jarFile = new JarFile(f);
			getLog().debug(f.getName() + " => " + f.getPath());
			byte[] magicAndClassFileVersion = new byte[8];
			for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();)
			{
				JarEntry entry = e.nextElement();
				if (!entry.isDirectory() && entry.getName().endsWith(".class"))
				{
					StringBuilder builder = new StringBuilder();
					builder.append("\t").append(entry.getName()).append(" => ");
					InputStream is = jarFile.getInputStream(entry);
					int read = is.read(magicAndClassFileVersion);
					is.close();
					assert read != 8;

					int minor = (magicAndClassFileVersion[4] << 8) + magicAndClassFileVersion[5];
					int major = (magicAndClassFileVersion[6] << 8) + magicAndClassFileVersion[7];
					builder.append("major=").append(major).append(",minor=").append(minor);
					getLog().debug(builder.toString());

					if ((major > maxJavaMajorVersionNumber)
						|| (major == maxJavaMajorVersionNumber && minor > maxJavaMinorVersionNumber))
					{
						return true;
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new EnforcerRuleException("Error while reading jar file", e);
		}
		return false;
	}

	private Log getLog()
	{
		return helper.getLog();
	}

	public void setMaxJavaMajorVersionNumber(int maxJavaMajorVersionNumber)
	{
		this.maxJavaMajorVersionNumber = maxJavaMajorVersionNumber;
	}

	public void setMaxJavaMinorVersionNumber(int maxJavaMinorVersionNumber)
	{
		this.maxJavaMinorVersionNumber = maxJavaMinorVersionNumber;
	}
}
