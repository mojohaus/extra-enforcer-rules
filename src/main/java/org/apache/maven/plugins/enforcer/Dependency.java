package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * <p>Dependency class.</p>
 *
 * @version $Id: $
 */
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

    /**
     * <p>Getter for the field <code>groupId</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * <p>Setter for the field <code>groupId</code>.</p>
     *
     * @param groupId a {@link java.lang.String} object.
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * <p>Getter for the field <code>artifactId</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * <p>Setter for the field <code>artifactId</code>.</p>
     *
     * @param artifactId a {@link java.lang.String} object.
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * <p>Getter for the field <code>classifier</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * <p>Setter for the field <code>classifier</code>.</p>
     *
     * @param classifier a {@link java.lang.String} object.
     */
    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getType()
    {
        return type;
    }

    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param type a {@link java.lang.String} object.
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * <p>Getter for the field <code>ignoreClasses</code>.</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getIgnoreClasses()
    {
        return ignoreClasses;
    }

    /**
     * <p>Setter for the field <code>ignoreClasses</code>.</p>
     *
     * @param ignoreClasses an array of {@link java.lang.String} objects.
     */
    public void setIgnoreClasses( String[] ignoreClasses )
    {
        this.ignoreClasses = ignoreClasses;
    }
    
    /** {@inheritDoc} */
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
