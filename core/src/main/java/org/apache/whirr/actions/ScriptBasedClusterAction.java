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

package org.apache.whirr.actions;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterAction;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.InstanceTemplate;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.ClusterActionHandler;
import org.apache.whirr.service.FirewallManager;
import org.apache.whirr.service.jclouds.StatementBuilder;
import org.jclouds.compute.ComputeServiceContext;

/**
 * A {@link ClusterAction} that provides the base functionality for running
 * scripts on instances in the cluster.
 */
public abstract class ScriptBasedClusterAction extends ClusterAction {

  private final Map<String, ClusterActionHandler> handlerMap;
  
  protected ScriptBasedClusterAction(Function<ClusterSpec, ComputeServiceContext> getCompute,
      final Map<String, ClusterActionHandler> handlerMap) {
    super(getCompute);
    this.handlerMap = handlerMap;
  }
  
  protected abstract void doAction(Map<InstanceTemplate, ClusterActionEvent> eventMap)
      throws IOException, InterruptedException;

  public Cluster execute(ClusterSpec clusterSpec, Cluster cluster) throws IOException, InterruptedException {
    
    Map<InstanceTemplate, ClusterActionEvent> eventMap = Maps.newHashMap();
    Map<InstanceTemplate, AtomicReference<String>> eventRoleMap = Maps.newHashMap();
    Cluster newCluster = cluster;
    for (InstanceTemplate instanceTemplate : clusterSpec.getInstanceTemplates()) {
      StatementBuilder statementBuilder = new StatementBuilder();

      AtomicReference<String> roleInEvent = new AtomicReference<String>(null);
      ComputeServiceContext computServiceContext = getCompute().apply(clusterSpec);
      FirewallManager firewallManager = new FirewallManager(computServiceContext,
          clusterSpec, newCluster);
      
      ClusterActionEvent event = new ClusterActionEvent(getAction(),
          clusterSpec, newCluster, roleInEvent, statementBuilder, getCompute(), firewallManager);

      eventMap.put(instanceTemplate, event);
      eventRoleMap.put(instanceTemplate, roleInEvent);
      
      for (String role : instanceTemplate.getRoles()) {
        roleInEvent.set(role);
        getHandlerForRole(role).beforeAction(event);
        roleInEvent.set(null);
      }
      newCluster = event.getCluster(); // cluster may have been updated by handler 
    }
    
    doAction(eventMap);
    
    // cluster may have been updated by action
    newCluster = Iterables.get(eventMap.values(), 0).getCluster();

    for (InstanceTemplate instanceTemplate : clusterSpec.getInstanceTemplates()) {
      AtomicReference<String> roleInEvent = eventRoleMap.get(instanceTemplate);
      for (String role : instanceTemplate.getRoles()) {
        ClusterActionHandler handler = getHandlerForRole(role);
        // TODO alex.heneveld@cloudsoftcorp.com wonders why we do the event lookup and cluster mangling _in_ the roles loop here, but _out_ of the roles loop above
        ClusterActionEvent event = eventMap.get(instanceTemplate);
        event.setCluster(newCluster);
        roleInEvent.set(role);
        handler.afterAction(event);
        roleInEvent.set(null);
        newCluster = event.getCluster(); // cluster may have been updated by handler 
      }
    }
    
    return newCluster;
  }

  protected ClusterActionHandler getHandlerForRole(String role) {
    ClusterActionHandler handler = handlerMap.get(role);
    if (handler == null) {
      int subroleIndex = role.indexOf(':');
      if (subroleIndex>0) {
        // match e.g. puppet: if role is puppet:module
        handler = handlerMap.get(role.substring(0, subroleIndex+1));
      }
    }
    if (handler == null) {
      throw new IllegalArgumentException("No handler for role " + role);
    }
    return handler;
  }
  
}
