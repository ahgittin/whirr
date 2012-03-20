/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.service.c4tgz;

import java.io.IOException;

import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.ClusterActionHandlerSupport;

/**
 * Installs prereqs needed for C4 TGZ setup.
 * <p>
 * (This might during exploration require EC2 scripts, but hopefully not!
 * For now we do nothing to "install" the prereqs.)
 */
public class C4TgzInstallClusterActionHandler extends ClusterActionHandlerSupport {

  public static final String C4TGZ_INSTALL_ROLE = "c4tgz-install";

  @Override
  public String getRole() {
   return C4TGZ_INSTALL_ROLE;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
//   
//   // install ruby and ruby-gems in the nodes
//   addStatement(event, call("install_ruby"));
//
//   // install git
//   addStatement(event, call("install_git"));
  }
}
