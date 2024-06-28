package org.codehaus.mojo.extraenforcer.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

/**
 * Abstract rule for when the content of the artifacts matters.
 *
 * @author Robert Scholte
 */
abstract class AbstractResolveDependencies extends AbstractEnforcerRule {

    /**
     * Optional list of dependency scopes to ignore. {@code test} and {@code provided} make sense here.
     */
    private List<String> ignoredScopes = Collections.emptyList();

    /**
     * Only verify dependencies with one of these scopes
     */
    private List<String> scopes = Collections.emptyList();

    /**
     * Ignore all dependencies which have {@code &lt;optional&gt;true&lt;/optional&gt;}.
     *
     * @since 1.2
     */
    private boolean ignoreOptionals = false;

    /**
     * Specify if transitive dependencies should be searched (default) or only look at direct dependencies.
     */
    private boolean searchTransitive = true;

    private final MavenSession session;

    private final RepositorySystem repositorySystem;

    protected AbstractResolveDependencies(MavenSession session, RepositorySystem repositorySystem) {
        this.session = session;
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        handleArtifacts(getDependenciesToCheck());
    }

    protected abstract void handleArtifacts(Set<Artifact> artifacts) throws EnforcerRuleException;

    private Set<Artifact> getDependenciesToCheck() throws EnforcerRuleException {
        try {
            ArtifactTypeRegistry artifactTypeRegistry =
                    session.getRepositorySession().getArtifactTypeRegistry();

            final MavenProject currentProject = session.getCurrentProject();

            final List<DependencyFilter> filters = new ArrayList<>(3);
            Optional.ofNullable(createOptionalFilter()).ifPresent(filters::add);
            Optional.ofNullable(createScopeDependencyFilter()).ifPresent(filters::add);
            filters.add((node, parents) -> searchTransitive || parents.size() <= 1);
            DependencyFilter dependencyFilter = new AndDependencyFilter(filters);

            List<Dependency> dependencies = currentProject.getDependencies().stream()
                    .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                    .collect(Collectors.toList());

            List<Dependency> managedDependencies = Optional.ofNullable(currentProject.getDependencyManagement())
                    .map(DependencyManagement::getDependencies)
                    .map(list -> list.stream()
                            .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                            .collect(Collectors.toList()))
                    .orElse(null);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setManagedDependencies(managedDependencies);
            collectRequest.setRepositories(currentProject.getRemoteProjectRepositories());
            collectRequest.setDependencies(dependencies);
            collectRequest.setRootArtifact(RepositoryUtils.toArtifact(currentProject.getArtifact()));

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter);

            final DependencyResult dependencyResult =
                    this.repositorySystem.resolveDependencies(session.getRepositorySession(), dependencyRequest);

            return dependencyResult.getArtifactResults().stream()
                    .map(ArtifactResult::getArtifact)
                    .map(RepositoryUtils::toArtifact)
                    .collect(Collectors.toSet());
        } catch (DependencyResolutionException e) {
            throw new EnforcerRuleError(e.getMessage(), e);
        }
    }

    private DependencyFilter createOptionalFilter() {
        if (!ignoreOptionals) {
            return null;
        }

        return (node, parents) -> {
            if (node.getDependency() != null && node.getDependency().isOptional()) {
                getLog().debug(() -> "Skipping " + node + " due to skip optional");
                return false;
            }
            return true;
        };
    }

    private DependencyFilter createScopeDependencyFilter() {
        if (scopes.isEmpty() && ignoredScopes.isEmpty()) {
            return null;
        }

        ScopeDependencyFilter scopeDependencyFilter = new ScopeDependencyFilter(scopes, ignoredScopes);
        return (node, parents) -> {
            if (!scopeDependencyFilter.accept(node, parents)) {
                getLog().debug(() -> "Skipping " + node + " due to scope");
                return false;
            }
            return true;
        };
    }

    /**
     * Convert a wildcard into a regex.
     *
     * @param wildcard the wildcard to convert.
     * @return the equivalent regex.
     */
    protected static String asRegex(String wildcard) {
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
        result.append("(\\.class)?");
        result.append('$');
        return result.toString();
    }

    /**
     *
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected class IgnorableDependency {
        // TODO should be private, fix and remove SuppressWarnings
        public Pattern groupId;

        public Pattern artifactId;

        public Pattern classifier;

        public Pattern type;

        public List<Pattern> ignores = new ArrayList<>();

        public void applyIgnoreClasses(String[] ignores, boolean indent) {
            String prefix = indent ? "  " : "";
            for (String ignore : ignores) {
                String pattern = asRegex(ignore.replace('.', '/'));
                getLog().debug(() -> prefix + "Ignore: " + ignore + " maps to regex " + pattern);
                this.ignores.add(Pattern.compile(pattern));
            }
        }

        public boolean matchesArtifact(Artifact dup) {
            return (artifactId == null
                            || artifactId.matcher(dup.getArtifactId()).matches())
                    && (groupId == null || groupId.matcher(dup.getGroupId()).matches())
                    && (classifier == null
                            || classifier.matcher(dup.getClassifier()).matches())
                    && (type == null || type.matcher(dup.getType()).matches());
        }

        public boolean matches(String className) {
            for (Pattern p : ignores) {
                if (p.matcher(className).matches()) {
                    return true;
                }
            }
            return false;
        }
    }
}
