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

import org.apache.maven.artifact.Artifact;

/**
 * Utility methods for working with Java jar files.
 */
public class JarUtils
{
    /**
     * @param artifact the artifact to check (could be a jar file, directory, etc.)
     * @return true if the artifact is a jar file, false if it's something else (like a directory)
     */
    public static boolean isJarFile( Artifact artifact )
    {
        return artifact.getFile().isFile() && "jar".equals( artifact.getType() );
    }

}
