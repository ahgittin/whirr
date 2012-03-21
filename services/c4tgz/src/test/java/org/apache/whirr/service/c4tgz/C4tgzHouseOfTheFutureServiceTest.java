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
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.Cluster;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.TestConstants;
import org.jclouds.net.IPSocket;
import org.jclouds.predicates.InetSocketAddressConnect;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.util.Strings2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Install a web site on the remote machine with whirr and C4 tgz pattern!
 */
public class C4tgzHouseOfTheFutureServiceTest {
	static final Logger LOG = LoggerFactory.getLogger(C4tgzHouseOfTheFutureServiceTest.class);
	
	private ClusterSpec clusterSpec;
	private ClusterController controller;
	private Cluster cluster;
	private RetryablePredicate<IPSocket> socketTester;

	@Before
	public void setUp() throws Exception {
		CompositeConfiguration config = new CompositeConfiguration();
		if (System.getProperty("config") != null) {
			config.addConfiguration(new PropertiesConfiguration(System.getProperty("config")));
		}
		config.addConfiguration(new PropertiesConfiguration("hotf-recipe.properties"));

		clusterSpec = ClusterSpec.withTemporaryKeys(config);
		controller = new ClusterController();
		cluster = controller.launchCluster(clusterSpec);
		socketTester = new RetryablePredicate<IPSocket>(new InetSocketAddressConnect(), 60, 1, TimeUnit.SECONDS);

	}

	@Test(timeout = TestConstants.ITEST_TIMEOUT)
	public void testSiteAvailable() throws Exception {

		// check that the http server started
		for (Instance instance : cluster.getInstances()) {
			// first, check the socket
			IPSocket socket = new IPSocket(instance.getPublicAddress().getHostAddress(), 80);
			assert socketTester.apply(socket) : instance;

			// then, try a GET
			URI httpUrl = URI.create("http://" + instance.getPublicAddress().getHostAddress());
			String output = Strings2.toStringAndClose(httpUrl.toURL().openStream());
			LOG.info("OUTPUT FROM WEB SITE:\n"+output);
		}

	}

	@After
	public void tearDown() throws IOException, InterruptedException {
		if (controller != null) {
			LOG.info("SHUTTING DOWN IF YOU PRESS ENTER");
			// FIXME No-check-in
			System.in.read();
			controller.destroyCluster(clusterSpec);
		}
	}

}
