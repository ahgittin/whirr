/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.service.c4tgz.statements;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.whirr.service.c4tgz.C4TgzConstants.MODULES_DIR;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import org.apache.commons.configuration.Configuration;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;

public class RunC4PreFilesPostSequence implements Statement {
  private Iterable<String> roles;
  private Configuration config;

  public RunC4PreFilesPostSequence(Iterable<String> roles, Configuration config) {
    this.roles = checkNotNull(roles, "roles");
    this.config = checkNotNull(config, "config");
  }

  @Override
  public Iterable<String> functionDependencies(OsFamily arg0) {
    return ImmutableSet.of();
  }

  @Override
  public String render(OsFamily arg0) {
    Builder<Statement> statements = ImmutableList.<Statement> builder();

    for (String role: roles) {
    	String moduleName = role.substring(role.lastIndexOf(':')+1);
    	statements.add(exec(
    			"cd " + MODULES_DIR + "/" + moduleName + "/" + "scripts/pre.d && "+
    			"echo pre loop && ( for x in * ; do chmod +x $x ; sudo ./$x ; done ) || exit 9") /*.failOnNonzeroReturnCode()*/ );
    	
    	statements.add(exec(
    			"sudo cp -r " + MODULES_DIR + "/" + moduleName + "/" + "files/* /"));
    	
    	statements.add(exec(
    			"cd " + MODULES_DIR + "/" + moduleName + "/" + "scripts/post.d && "+
    			"echo post loop && ( for x in * ; do chmod +x $x ; sudo ./$x ; done ) || exit 9") /*.failOnNonzeroReturnCode()*/ );
    }

    return new StatementList(statements.build()).render(arg0);
  }

  static final Logger LOG = LoggerFactory.getLogger(RunC4PreFilesPostSequence.class);

}
