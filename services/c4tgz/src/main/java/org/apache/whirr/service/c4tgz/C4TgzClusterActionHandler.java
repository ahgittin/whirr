/**
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

package org.apache.whirr.service.c4tgz;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.whirr.service.c4tgz.C4TgzConstants.C4TGZ;
import static org.apache.whirr.service.c4tgz.predicates.C4TgzPredicates.isFirstC4TgzRoleIn;
import static org.apache.whirr.service.c4tgz.predicates.C4TgzPredicates.isLastC4TgzRoleIn;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.c4tgz.functions.InstallAllModulesStatementFromProperties;
import org.apache.whirr.service.c4tgz.functions.ModulePropertiesFromConfiguration;
import org.apache.whirr.service.c4tgz.functions.RolesManagedByC4Tgz;
import org.apache.whirr.service.c4tgz.functions.StatementToInstallModule;
import org.apache.whirr.service.c4tgz.statements.RunC4PreFilesPostSequence;
import org.jclouds.scriptbuilder.domain.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs Channel 4 TGZ files, and runs them. 
 */
public class C4TgzClusterActionHandler extends C4TgzInstallClusterActionHandler {
  static final Logger LOG = LoggerFactory.getLogger(C4TgzClusterActionHandler.class);

  private final String role;

  public C4TgzClusterActionHandler(String role) {
    this.role = checkNotNull(role, "role");
  }

  @Override
  public String getRole() {
    return role;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    if (isFirstC4TgzRoleIn(event.getInstanceTemplate().getRoles()).apply(getRole())) {
      // install any c4 framework tools when bootstrapping the first c4tgz role
      super.beforeBootstrap(event);
      installAllKnownModules(event);
    }

  }

  private void installAllKnownModules(ClusterActionEvent event) throws IOException {
    Map<String, String> moduleProps = ModulePropertiesFromConfiguration.INSTANCE.apply(event.getClusterSpec()
          .getConfigurationForKeysWithPrefix(C4TGZ));

    StatementToInstallModule statementMaker = new StatementToInstallModule(event);
    Statement installModules = new InstallAllModulesStatementFromProperties(statementMaker).apply(moduleProps);

    event.getStatementBuilder().addStatement(installModules);
    LOG.debug("Whirr-c4tgz finished installing modules for " + event.getInstanceTemplate());
  }

  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);

    if (isLastC4TgzRoleIn(event.getInstanceTemplate().getRoles()).apply(getRole())) {
      Configuration config = event.getClusterSpec().getConfiguration();
      Iterable<String> roles = RolesManagedByC4Tgz.INSTANCE.apply(event.getInstanceTemplate().getRoles());
      
      addStatement(event, new RunC4PreFilesPostSequence(roles, config));
    }

  }
}
