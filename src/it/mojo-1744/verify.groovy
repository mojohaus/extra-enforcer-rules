/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.codehaus.mojo.extraenforcer.it.BanDuplicateClassesLogParser;

File log = new File( basedir, 'build.log' )
assert log.exists()
def duplicates =  new BanDuplicateClassesLogParser( log ).parse();

assert duplicates == [
  (["org.jvnet.hudson.plugins.m2release:nexus:jar:0.0.1:compile",
    "org.slf4j:jcl-over-slf4j:jar:1.5.11:compile"] as Set)
  : (["org/apache/commons/logging/impl/SLF4JLocationAwareLog.class"] as Set),
  (["org.jvnet.hudson.plugins.m2release:nexus:jar:0.0.1:compile",
    "org.slf4j:jcl-over-slf4j:jar:1.5.11:compile",
    "commons-logging:commons-logging:jar:1.1.1:compile"] as Set)
  : (["org/apache/commons/logging/impl/NoOpLog.class"] as Set),
  (["commons-logging:commons-logging:jar:1.1.1:compile",
    "org.jvnet.hudson.plugins.m2release:nexus:jar:0.0.1:compile"] as Set)
  : (["org/apache/commons/logging/impl/SimpleLog.class",
      "org/apache/commons/logging/impl/SimpleLog\$1.class",
      "org/apache/commons/logging/Log.class"] as Set)
]