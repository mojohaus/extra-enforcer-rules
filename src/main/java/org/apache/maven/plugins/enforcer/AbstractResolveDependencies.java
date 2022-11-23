package org.apache.maven.plugins.enforcer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
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
public abstract class AbstractResolveDependencies extends AbstractMojoHausEnforcerRule
{

    private DependencyGraphBuilder graphBuilder;

    private MavenSession session;
    private RepositorySystem repositorySystem;

    private EnforcerRuleHelper helper;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        this.helper = helper;

        // Get components
        try
        {
            repositorySystem = helper.getComponent( RepositorySystem.class );
            graphBuilder = helper.getComponent( DependencyGraphBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
                throw new EnforcerRuleException( "Unable to lookup DependencyTreeBuilder: ", e );
        }

        // Resolve expressions
        try
        {
            session = (MavenSession) helper.evaluate( "${session}" );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }

        ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setProject( session.getCurrentProject() );

        handleArtifacts( getDependenciesToCheck( buildingRequest ) );
    }

    protected abstract void handleArtifacts( Set<Artifact> artifacts ) throws EnforcerRuleException;
    
    protected boolean isSearchTransitive()
    {
        return true;
    }
        
    private Set<Artifact> getDependenciesToCheck( ProjectBuildingRequest buildingRequest ) throws EnforcerRuleException
    {
        Set<Artifact> dependencies = null;
        try
        {
            DependencyNode node = graphBuilder.buildDependencyGraph( buildingRequest ,null );
            
            if( isSearchTransitive() )
            {
                dependencies  = getAllDescendants( node );
            }
            else if ( node.getChildren() != null )
            {
                dependencies = new HashSet<>();
                for( DependencyNode depNode : node.getChildren() )
                {
                    dependencies.add( depNode.getArtifact() );
                }
            }
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new EnforcerRuleException( e.getMessage(), e );
        }
        return dependencies;
    }

    private Set<Artifact> getAllDescendants( DependencyNode node )
    {
        Set<Artifact> children = null;
        if( node.getChildren() != null )
        {
            children = new HashSet<>();
            for( DependencyNode depNode : node.getChildren() )
            {
                try
                {
                    Artifact artifact = depNode.getArtifact();
                    resolveArtifact( artifact );
                    children.add( artifact );

                    Set<Artifact> subNodes = getAllDescendants( depNode );

                    if( subNodes != null )
                    {
                        children.addAll( subNodes );
                    }
                }
                catch ( ArtifactResolutionException e )
                {
                    getLog().warn( e.getMessage() );
                }
            }
        }
        return children;
    }

    private void resolveArtifact( Artifact artifact ) throws ArtifactResolutionException
    {
        ArtifactRequest request = new ArtifactRequest();
        request.setRepositories( session.getCurrentProject().getRemoteProjectRepositories() );
        request.setArtifact( RepositoryUtils.toArtifact( artifact ) );

        ArtifactResult artifactResult = repositorySystem.resolveArtifact( session.getRepositorySession(), request );

        artifact.setFile( artifactResult.getArtifact().getFile() );
        artifact.setVersion( artifactResult.getArtifact().getVersion() );
        artifact.setResolved( true );
    }

    protected Log getLog()
    {
        return helper.getLog();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isResultValid( EnforcerRule enforcerRule )
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheId()
    {
        return "Does not matter as not cacheable";
    }


    /**
     * Convert a wildcard into a regex.
     *
     * @param wildcard the wildcard to convert.
     * @return the equivalent regex.
     */
    protected static String asRegex(String wildcard)
    {
        StringBuilder result = new StringBuilder( wildcard.length() );
        result.append( '^' );
        for ( int index = 0; index < wildcard.length(); index++ )
        {
            char character = wildcard.charAt( index );
            switch ( character )
            {
                case '*':
                    result.append( ".*" );
                    break;
                case '?':
                    result.append( "." );
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
                    result.append( "\\" );
                default:
                    result.append( character );
                    break;
            }
        }
        result.append( "(\\.class)?" );
        result.append( '$' );
        return result.toString();
    }

    /**
     *
     */
    protected class IgnorableDependency
    {
        public Pattern groupId;
        public Pattern artifactId;
        public Pattern classifier;
        public Pattern type;
        public List<Pattern> ignores = new ArrayList<>();

        public IgnorableDependency applyIgnoreClasses( String[] ignores, boolean indent )
        {
            String prefix = indent ? "  " : "";
            for ( String ignore : ignores )
            {
                getLog().info( prefix + "Adding ignore: " + ignore );
                ignore = ignore.replace( '.', '/' );
                String pattern = asRegex( ignore );
                getLog().debug( prefix + "Ignore: " + ignore + " maps to regex " + pattern );
                this.ignores.add( Pattern.compile( pattern ) );
            }
            return this;
        }

        public boolean matchesArtifact( Artifact dup )
        {
            return ( artifactId == null || artifactId.matcher( dup.getArtifactId() ).matches() )
                && ( groupId == null || groupId.matcher( dup.getGroupId() ).matches() )
                && ( classifier == null || classifier.matcher( dup.getClassifier() ).matches() )
                && ( type == null || type.matcher( dup.getType() ).matches() );
        }

        public boolean matches(String className)
        {
            for ( Pattern p : ignores )
            {
                if ( p.matcher( className ).matches() )
                {
                    return true;
                }
            }
            return false;
        }
    }
}
