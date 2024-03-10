package org.codehaus.mojo.extraenforcer.dependencies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Abstract rule for when the content of the artifacts matters.
 *
 * @author Robert Scholte
 *
 */
abstract class AbstractResolveDependencies extends AbstractEnforcerRule {

    private final MavenSession session;
    private final RepositorySystem repositorySystem;

    private final DependencyGraphBuilder graphBuilder;

    protected AbstractResolveDependencies(
            MavenSession session, RepositorySystem repositorySystem, DependencyGraphBuilder graphBuilder) {
        this.session = session;
        this.repositorySystem = repositorySystem;
        this.graphBuilder = graphBuilder;
    }

    @Override
    public void execute() throws EnforcerRuleException {

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(session.getCurrentProject());

        handleArtifacts(getDependenciesToCheck(buildingRequest));
    }

    protected abstract void handleArtifacts(Set<Artifact> artifacts) throws EnforcerRuleException;

    protected boolean isSearchTransitive() {
        return true;
    }

    private Set<Artifact> getDependenciesToCheck(ProjectBuildingRequest buildingRequest) throws EnforcerRuleException {
        Set<Artifact> dependencies = null;
        try {
            DependencyNode node = graphBuilder.buildDependencyGraph(buildingRequest, null);

            if (isSearchTransitive()) {
                dependencies = getAllDescendants(node);
            } else if (node.getChildren() != null) {
                dependencies = new HashSet<>();
                for (DependencyNode depNode : node.getChildren()) {
                    dependencies.add(depNode.getArtifact());
                }
            }
        } catch (DependencyGraphBuilderException e) {
            throw new EnforcerRuleException(e.getMessage(), e);
        }
        return dependencies;
    }

    private Set<Artifact> getAllDescendants(DependencyNode node) {
        Set<Artifact> children = null;
        if (node.getChildren() != null) {
            children = new HashSet<>();
            for (DependencyNode depNode : node.getChildren()) {
                try {
                    Artifact artifact = depNode.getArtifact();
                    resolveArtifact(artifact);
                    children.add(artifact);

                    Set<Artifact> subNodes = getAllDescendants(depNode);

                    if (subNodes != null) {
                        children.addAll(subNodes);
                    }
                } catch (ArtifactResolutionException e) {
                    getLog().warn(e.getMessage());
                }
            }
        }
        return children;
    }

    private void resolveArtifact(Artifact artifact) throws ArtifactResolutionException {
        ArtifactRequest request = new ArtifactRequest();
        request.setRepositories(session.getCurrentProject().getRemoteProjectRepositories());
        request.setArtifact(RepositoryUtils.toArtifact(artifact));

        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session.getRepositorySession(), request);

        artifact.setFile(artifactResult.getArtifact().getFile());
        artifact.setVersion(artifactResult.getArtifact().getVersion());
        artifact.setResolved(true);
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
