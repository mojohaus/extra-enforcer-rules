package org.apache.maven.plugins.enforcer;

public class Dependency
{

    private String groupId;
    
    private String artifactId;
    
    private String classifier;
    
    private String type;
    
    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String[] getIgnoreClasses()
    {
        return ignoreClasses;
    }

    public void setIgnoreClasses( String[] ignoreClasses )
    {
        this.ignoreClasses = ignoreClasses;
    }
    
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( groupId ).append( ':' ).append( artifactId ).append( ':' ).append( type );
        if ( classifier != null )
        {
            sb.append( ':' ).append( classifier );
        }
        return sb.toString();
    }
}
