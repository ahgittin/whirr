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

package org.apache.whirr.service.chef.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.failNotEquals;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.whirr.Cluster;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.RolePredicates;
import org.apache.whirr.service.chef.ChefClusterActionHandler;
import org.apache.whirr.service.chef.Recipe;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.Statements;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Integration test for chef.
 */
public class ChefServiceTest {

    private static Predicate<NodeMetadata> allNodes = Predicates.alwaysTrue();

    private static final Logger LOG = LoggerFactory
            .getLogger(ChefServiceTest.class);

    private static ClusterSpec clusterSpec;
    private static ClusterController controller;
    private static Cluster cluster;

    @BeforeClass
    public static void setUp() throws Exception {
        CompositeConfiguration config = new CompositeConfiguration();
        if (System.getProperty("config") != null) {
            config.addConfiguration(new PropertiesConfiguration(System
                    .getProperty("config")));
        }
        config.addConfiguration(new PropertiesConfiguration(
                "whirr-chef-test.properties"));
        clusterSpec = ClusterSpec.withTemporaryKeys(config);
        controller = new ClusterController();
        cluster = controller.launchCluster(clusterSpec);
    }

    @Test
    public void testRecipesWereRanInServiceBootstrap()
            throws RunScriptOnNodesException {

        // a test service was supposed to boot and install ant and maven. let's
        // test it

        Statement testAnt = Statements.exec("ant -version");

        Map<? extends NodeMetadata, ExecResponse> responses = controller
                .runScriptOnNodesMatching(clusterSpec, allNodes, testAnt);

        printResponses(testAnt, responses);

        assertResponsesContain(responses, testAnt, "Apache Ant(TM)");

        Statement testMaven = Statements.exec("mvn --version");

        responses = controller.runScriptOnNodesMatching(clusterSpec, allNodes,
                testMaven);

        printResponses(testMaven, responses);

        assertResponsesContain(responses, testMaven, "Apache Maven");
    }

    @Test
    public void testChefRunRecipeFromURL() throws Exception {

        // As chef will be mostly used indirectly in other services
        // this test tests chef's ability to run a recipe, specifically to
        // install nginx.
        Recipe nginx = new Recipe(
                "nginx",
                null,
                "http://s3.amazonaws.com/opscode-community/cookbook_versions/tarballs/529/original/nginx.tgz");

        printResponses(nginx, controller.runScriptOnNodesMatching(clusterSpec,
                allNodes, nginx,
                controller.defaultRunScriptOptionsForSpec(clusterSpec)
                        .wrapInInitScript(true)));

        // test the web server is up
        Instance chefinstance = cluster.getInstanceMatching(RolePredicates
                .role(ChefClusterActionHandler.CHEF_ROLE));
        String chefHostName = chefinstance.getPublicHostName();

        assertNotNull(chefHostName);

        HttpClient client = new HttpClient();
        GetMethod getIndex = new GetMethod(String.format("http://%s",
                chefHostName));

        int statusCode = client.executeMethod(getIndex);

        assertEquals("Status code should be 200", HttpStatus.SC_OK, statusCode);

        String indexPageHTML = getIndex.getResponseBodyAsString();

        assertTrue("The string 'nginx' should appear on the index page",
                indexPageHTML.contains("nginx"));

    }

    /**
     * Test the execution of recipes (one with attribs and one with non-default
     * recipe) from the provided recipes dowloaded from opscode's git repo.
     * 
     * @throws Exception
     */
    @Test
    public void testChefRunRecipesFromProvidedCookbooks() throws Exception {

        Recipe java = new Recipe("java");
        java.attribs.put("install_flavor", "sun");

        Map<? extends NodeMetadata, ExecResponse> responses = controller
                .runScriptOnNodesMatching(clusterSpec, allNodes, java,
                        controller.defaultRunScriptOptionsForSpec(clusterSpec)
                                .wrapInInitScript(true));

        printResponses(java, responses);

        Statement stmt = Statements.exec("java -version");

        responses = controller.runScriptOnNodesMatching(clusterSpec, allNodes,
                stmt);

        assertResponsesContain(responses, stmt,
                "Java(TM) SE Runtime Environment");

        Recipe postgreSql = new Recipe("postgresql", "server");

        responses = controller.runScriptOnNodesMatching(clusterSpec, allNodes,
                postgreSql);

        printResponses(postgreSql, responses);

        stmt = Statements.exec("psql --version");

        responses = controller.runScriptOnNodesMatching(clusterSpec, allNodes,
                stmt);

        assertResponsesContain(responses, stmt, "PostgreSQL");

    }

    @AfterClass
    public static void tearDown() throws IOException, InterruptedException {
        if (controller != null) {
            controller.destroyCluster(clusterSpec);
        }
    }

    public static void assertResponsesContain(
            Map<? extends NodeMetadata, ExecResponse> responses,
            Statement statement, String text) {
        for (Map.Entry<? extends NodeMetadata, ExecResponse> entry : responses
                .entrySet()) {
            if (!entry.getValue().getOutput().contains(text)) {
                failNotEquals("Node: " + entry.getKey().getId()
                        + " failed to execute the command: " + statement
                        + " as could not find expected text", text,
                        entry.getValue());
            }
        }
    }

    public static void printResponses(Statement statement,
            Map<? extends NodeMetadata, ExecResponse> responses) {
        LOG.debug("Responses for Statement: " + statement);
        for (Map.Entry<? extends NodeMetadata, ExecResponse> entry : responses
                .entrySet()) {
            LOG.debug("Node[" + entry.getKey().getId() + "]: "
                    + entry.getValue());
        }
    }
}
