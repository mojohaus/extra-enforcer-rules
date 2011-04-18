package org.codehaus.mojo.enforcer.rule;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Bans duplicate classes on the classpath.
 */
public class BanDuplicateClassesRule implements EnforcerRule {

    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    /**
     * Convert a wildcard into a regex.
     * @param wildcard the wildcard to convert.
     * @return the equivalent regex.
     */
    private static String asRegex(String wildcard) {
        StringBuilder result = new StringBuilder(wildcard.length());
        result.append('^');
        for (int index = 0; index < wildcard.length(); index++) {
            char character = wildcard.charAt(index);
            switch (character) {
                case '*':
                    result.append(".*");
                    break;
                case '?':
                    result.append(".");
                    break;
                case '$':
                case '(':
                case ')':
                case '.':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                    result.append("\\");
                default:
                    result.append(character);
                    break;
            }
        }
        result.append('$');
        return result.toString();
    }


    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        Log log = helper.getLog();
        List<Pattern> ignores = new ArrayList<Pattern>();
        if (ignoreClasses != null) {
            for (String ignore : ignoreClasses) {
                log.info("Adding ignore: " + ignore);
                ignore = ignore.replace('.', '/');
                String pattern = asRegex(ignore);
                log.info("Ignore: " + ignore + " maps to regex " + pattern);
                ignores.add(Pattern.compile(pattern));
            }
        }
        try {
            MavenProject project = (MavenProject) helper.evaluate("${project}");

            ArtifactFactory factory = (ArtifactFactory) helper.getComponent(ArtifactFactory.class);

            Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();

            if (dependencyArtifacts == null) {
                dependencyArtifacts = project.createArtifacts(factory, null, null);
            }

            Map<String, Artifact> classNames = new HashMap<String, Artifact>();
            for (Artifact o : dependencyArtifacts) {
                File file = o.getFile();
                try {
                    JarFile jar = new JarFile(file);
                    try {
                        outer:
                        for (JarEntry entry : Collections.<JarEntry>list(jar.entries())) {
                            String name = entry.getName();
                            if (!name.endsWith(".class")) {
                                continue;
                            }
                            Artifact dup = classNames.get(name);
                            if (dup != null) {
                                for (Pattern p : ignores) {
                                    if (p.matcher(name).matches()) {
                                        log.debug("Ignoring duplicate class " + name);
                                        continue outer;
                                    }
                                }
                                throw new EnforcerRuleException(
                                        "Duplicate class " + name + " found in both " + dup + " and " + o);
                            }
                            classNames.put(name, o);
                        }
                    } finally {
                        try {
                            jar.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } catch (IOException e) {
                    throw new EnforcerRuleException(
                            "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(),
                            e);
                }
            }


        } catch (ComponentLookupException e) {
            throw new EnforcerRuleException("Unable to lookup a component " + e.getLocalizedMessage(), e);
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Unable to lookup an expression " + e.getLocalizedMessage(), e);
        } catch (InvalidDependencyVersionException e) {
            throw new EnforcerRuleException("Unable to resolve dependencies" + e.getLocalizedMessage(), e);
        }
    }

    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid(EnforcerRule enforcerRule) {
        return false;
    }

    public String getCacheId() {
        return "Does not matter as not cacheable";
    }
}
