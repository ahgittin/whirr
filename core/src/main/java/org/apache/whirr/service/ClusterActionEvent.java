/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.service;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.jclouds.StatementBuilder;
import org.apache.whirr.service.jclouds.TemplateBuilderStrategy;
import org.jclouds.compute.ComputeServiceContext;

import com.google.common.base.Function;

/**
 * An event object which is fired when a {@link org.apache.whirr.ClusterAction} occurs. 
 */
public class ClusterActionEvent {
  
  private String action;
  private ClusterSpec clusterSpec;
  private Cluster cluster;
  private AtomicReference<String> role;
  private StatementBuilder statementBuilder;
  private TemplateBuilderStrategy templateBuilderStrategy =
    new TemplateBuilderStrategy();
  private FirewallManager firewallManager;
  private Function<ClusterSpec, ComputeServiceContext> getCompute;

  //TODO alex.heneveld@cloudsoftcorp.com constructor not used?
  public ClusterActionEvent(String action, ClusterSpec clusterSpec,
      Cluster cluster, Function<ClusterSpec, ComputeServiceContext> getCompute,
      FirewallManager firewallManager) {
    this(action, clusterSpec, cluster, null, null, getCompute, firewallManager);
  }
  
  public ClusterActionEvent(String action, ClusterSpec clusterSpec,
      Cluster cluster, AtomicReference<String> role, StatementBuilder statementBuilder,
      Function<ClusterSpec, ComputeServiceContext> getCompute,
      FirewallManager firewallManager) {
    this.action = action;
    this.clusterSpec = clusterSpec;
    this.cluster = cluster;
    this.role = role;
    this.statementBuilder = statementBuilder;
    this.getCompute = getCompute;
    this.firewallManager = firewallManager;
  }
  
  public Cluster getCluster() {
    return cluster;
  }

  public void setCluster(Cluster cluster) {
    this.cluster = cluster;
  }

  public String getAction() {
    return action;
  }

  public ClusterSpec getClusterSpec() {
    return clusterSpec;
  }

  /** returns the role of the ClusterActionHandler that this event is currently passed to, 
   * or null if it is not being looked at by a ClusterActionHandler;
   * used e.g. when passing a full subrole-qualified role (e.g. puppet:module::manifest)
   * to a cluster-action handler that would not otherwise be aware of it (e.g. it declares its role as puppet:)
   */
  public String getRole() {
    return role==null ? null : role.get();
  }
  
  public Function<ClusterSpec, ComputeServiceContext> getCompute() {
    return getCompute;
  }
   
  public StatementBuilder getStatementBuilder() {
    return statementBuilder;
  }

  public TemplateBuilderStrategy getTemplateBuilderStrategy() {
    return templateBuilderStrategy;
  }

  public void setTemplateBuilderStrategy(
      TemplateBuilderStrategy templateBuilderStrategy) {
    this.templateBuilderStrategy = templateBuilderStrategy;
  }
  
  public FirewallManager getFirewallManager() {
    return firewallManager;
  }

}
