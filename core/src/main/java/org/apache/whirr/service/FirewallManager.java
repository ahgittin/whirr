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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.whirr.Cluster;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterSpec;
import org.jclouds.aws.util.AWSUtils;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FirewallManager {

  public static class Rule {
    
    public static Rule create() {
      return new Rule();
    }
    
    private String source;
    private Set<Instance> destinations;
    private Predicate<Instance> destinationPredicate;
    private int[] ports;
    
    private Rule() {
    }
    
    /**
     * @param source The allowed source IP for traffic. If not set, this will
     * default to {@link ClusterSpec#getClientCidrs()}, or, if that is not set,
     *  to the client's originating IP.
     */
    public Rule source(String source) {
      this.source = source;
      return this;
    }
    
    /**
     * @param destination The allowed destination instance.
     */
    public Rule destination(Instance destination) {
      this.destinations = Collections.singleton(destination);
      return this;
    }
    
    /**
     * @param destinations The allowed destination instances.
     */
    public Rule destination(Set<Instance> destinations) {
      this.destinations = destinations;
      return this;
    }
    
    /**
     * @param destinationPredicate A predicate which is used to evaluate the
     * allowed destination instances.
     */
    public Rule destination(Predicate<Instance> destinationPredicate) {
      this.destinationPredicate = destinationPredicate;
      return this;
    }
    
    /**
     * @param port The port on the destination which is to be opened. Overrides
     * any previous calls to {@link #port(int)} or {@link #ports(int...)}.
     */
    public Rule port(int port) {
      this.ports = new int[] { port };
      return this;
    }
    
    /**
     * @param ports The ports on the destination which are to be opened.
     * Overrides
     * any previous calls to {@link #port(int)} or {@link #ports(int...)}.
     */
    public Rule ports(int... ports) {
      this.ports = ports;
      return this;
    }
  }

  private static final Logger LOG = LoggerFactory
      .getLogger(FirewallManager.class);

  private ComputeServiceContext computeServiceContext;
  private ClusterSpec clusterSpec;
  private Cluster cluster;

  public FirewallManager(ComputeServiceContext computeServiceContext,
      ClusterSpec clusterSpec, Cluster cluster) {
    this.computeServiceContext = computeServiceContext;
    this.clusterSpec = clusterSpec;
    this.cluster = cluster;
  }

  public void addRules(Rule... rules) throws IOException {
    for (Rule rule : rules) {
      addRule(rule);
    }
  }

  public void addRules(Set<Rule> rules) throws IOException {
    for (Rule rule : rules) {
      addRule(rule);
    }
  }
  
  /**
   * Rules are additive. If no source is set then it will default
   * to {@link ClusterSpec#getClientCidrs()}, or, if that is not set,
   * to the client's originating IP. If no destinations or ports
   * are set then the rule has not effect.
   *
   * @param rule The rule to add to the firewall. 
   * @throws IOException
   */
  public void addRule(Rule rule) throws IOException {
    Set<Instance> instances = Sets.newHashSet();
    if (rule.destinations != null) {
      instances.addAll(rule.destinations);
    }
    if (rule.destinationPredicate != null) {
      instances.addAll(cluster.getInstancesMatching(rule.destinationPredicate));
    }
    List<String> cidrs;
    if (rule.source == null) {
      cidrs = clusterSpec.getClientCidrs();
      if (cidrs == null || cidrs.isEmpty()) {
        cidrs = Lists.newArrayList(getOriginatingIp());
      }
    } else {
      cidrs = Lists.newArrayList(rule.source + "/32");
    }

    Iterable<String> instanceIds =
      Iterables.transform(instances, new Function<Instance, String>() {
        @Override
        public String apply(@Nullable Instance instance) {
          return instance == null ? "<null>" : instance.getId();
        }
      });

    LOG.info("Authorizing firewall ingress to {} on ports {} for {}",
        new Object[] { instanceIds, rule.ports, cidrs });

    authorizeIngress(computeServiceContext, instances,
        clusterSpec, cidrs, rule.ports);
  }

  /**
   * @return the IP address of the client on which this code is running.
   * @throws IOException
   */
  private String getOriginatingIp() throws IOException {
    URL url = new URL("http://checkip.amazonaws.com/");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();
    return IOUtils.toString(connection.getInputStream()).trim() + "/32";
  }

  public static void authorizeIngress(ComputeServiceContext computeServiceContext,
      Set<Instance> instances, ClusterSpec clusterSpec, List<String> cidrs, int... ports) {

    if (computeServiceContext.getProviderSpecificContext().getApi() instanceof
      EC2Client) {
      // This code (or something like it) may be added to jclouds (see
      // http://code.google.com/p/jclouds/issues/detail?id=336).
      // Until then we need this temporary workaround.
      String region = AWSUtils.parseHandle(Iterables.get(instances, 0).getId())[0];
      EC2Client ec2Client = EC2Client.class.cast(
          computeServiceContext.getProviderSpecificContext().getApi());
      String groupName = "jclouds#" + clusterSpec.getClusterName() + "#" + region;
      for (String cidr : cidrs) {
        for (int port : ports) {
          try {
            ec2Client.getSecurityGroupServices()
              .authorizeSecurityGroupIngressInRegion(region, groupName,
                IpProtocol.TCP, port, port, cidr);
          } catch(IllegalStateException e) {
            LOG.warn(e.getMessage());
            /* ignore, it means that this permission was already granted */
          }
        }
      }
    }
  }
}
