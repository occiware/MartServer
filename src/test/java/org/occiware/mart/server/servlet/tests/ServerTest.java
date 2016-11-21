/**
 * Copyright (c) 2015-2017 Inria
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.mart.server.servlet.tests;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.server.servlet.model.ConfigurationManager;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Global occi server tests.
 *
 * @author christophe
 */
public class ServerTest {

    private static Server server;
    private static HttpClient httpClient;
//     private static URI serverUri;

    @BeforeClass
    public static void startJetty() throws Exception {

        ResourceConfig config = new ResourceConfig();
        config.packages("org.occiware.mart.server.servlet");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        server = new Server(9090);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);
        server.start();

        // manage jetty http client.
        // Instantiate and configure the SslContextFactory (only if we use https protocol).
        SslContextFactory sslContextFactory = new SslContextFactory();

        // Instantiate HttpClient with the SslContextFactory, to support aswell http and https.
        httpClient = new HttpClient(sslContextFactory);

        // Configure HttpClient, for example:
        httpClient.setFollowRedirects(false);

        // Start HttpClient
        httpClient.start();

    }

    @AfterClass
    public static void stopJetty() {
        try {
            httpClient.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testCreateResourceInputJson() throws Exception {
        File resource1 = getResourceInputFile("/testjson/integration/creation/resource1.json");
        ContentResponse response = httpClient.newRequest("localhost", 9090)
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource1.toPath(), "application/json")
                .agent("martclient")
                .send();
        int statusResponse = response.getStatus();

        // Must return "created" status. 
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());

        // Create the second resource with mixins.
        File resource2 = getResourceInputFile("/testjson/integration/creation/resource2.json");
        response = httpClient.newRequest("localhost", 9090)
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource2.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());

        File resource3 = getResourceInputFile("/testjson/integration/creation/resource3.json");
        response = httpClient.newRequest("localhost", 9090)
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource3.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());

        // Now test a resource single load without "resources" key.
        File resource4 = getResourceInputFile("/testjson/integration/creation/resourceonly.json");
        response = httpClient.newRequest("localhost", 9090)
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource4.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());

        File resource5 = getResourceInputFile("/testjson/integration/creation/resource_location.json");
        response = httpClient.newRequest("localhost", 9090)
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource5.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());

    }

    @Test
    public void testGetResourceLink() {

        try {
            testCreateResourceInputJson(); // Launch create tests resources....

            System.out.println("GET Request interface /-/.");
            ContentResponse response = httpClient.newRequest("http://localhost:9090/-/?category=network")  // uuid: f89486b7-0632-482d-a184-a9195733ddd9
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            int statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            String result = response.getContentAsString();
            assertFalse(result.isEmpty());


            System.out.println("GET Request on resource location : /testlocation/" );
            // See file: resource_location.json.
            response = httpClient.newRequest("http://localhost:9090/testlocation/")  // uuid: f89486b7-0632-482d-a184-a9195733ddd9
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            result = response.getContentAsString();
            assertFalse(result.isEmpty());

            // Search on an invalid location path.
            System.out.println("GET Request on resource location : /otherlocation/other2/" );
            // See file: resource_location.json.
            response = httpClient.newRequest("http://localhost:9090/otherlocation/other2/")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.NOT_FOUND.getStatusCode());

            // Search on a relative path location with included key.
            System.out.println("GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9");
            // See file: resource_location.json.
            response = httpClient.newRequest("http://localhost:9090//testlocation/f89486b7-0632-482d-a184-a9195733ddd9")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());

            // Search on a false relative path but with good keys. This must be not found to avoid path confusion.
            System.out.println("GET Request on resource location : /otherresources/f89486b7-0632-482d-a184-a9195733ddd9");
            // See file: resource_location.json.
            response = httpClient.newRequest("http://localhost:9090//otherresources/f89486b7-0632-482d-a184-a9195733ddd9")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            String content = response.getContentAsString();
            System.out.println(content);
            assertTrue(statusResponse == Response.Status.NOT_FOUND.getStatusCode());


            // Search on compute kind.
            System.out.println("GET Request on Compute kind... http://localhost:9090/compute/");
            response = httpClient.newRequest("http://localhost:9090/compute/")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());

            result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            System.out.println(result);

            // Test network kind.
            System.out.println("GET Request on Network kind... http://localhost:9090/network/");
            response = httpClient.newRequest("http://localhost:9090/network/")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            System.out.println(result);

            // Test network interface link.
            System.out.println("GET Request on Networkinterface kind... http://localhost:9090/networkinterface/");
            response = httpClient.newRequest("http://localhost:9090/networkinterface/")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            System.out.println(result);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }


    @Test
    public void testUpdateResources() {

        try {
            testCreateResourceInputJson(); // Launch create tests resources....

            System.out.println("POST Request on resource location : /f88486b7-0632-482d-a184-a9195733ddd0" );

            File resource1 = getResourceInputFile("/testjson/integration/update/resource1.json");
            ContentResponse response = httpClient.newRequest("localhost", 9090)
                    .accept("application/json")
                    .method(HttpMethod.POST)
                    .file(resource1.toPath(), "application/json")
                    .agent("martclient")
                    .send();
            int statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());

            // Check GET :
            System.out.println("GET Request http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0");
            response = httpClient.newRequest("http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();

            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            String result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains("4.1")); // Check value occi.compute.memory.
            System.out.println(result);

            // 2 : associate a mixin tag to a resource.
            File mixintagasso = getResourceInputFile("/testjson/integration/update/mixintag_asso.json");
            response = httpClient.newRequest("http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0")
                    .accept("application/json")
                    .method(HttpMethod.POST)
                    .file(mixintagasso.toPath(), "application/json")
                    .agent("martclient")
                    .send();
            statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());

            System.out.println("GET Request http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0");
            response = httpClient.newRequest("http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            statusResponse = response.getStatus();

            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains("\"http://occiware.org/occi/tags#my_mixin2\""));
            System.out.println(result);



            // 3 : action invocation.
            File action_invocation = getResourceInputFile("/testjson/integration/update/action_invocation_test.json");
            response = httpClient.newRequest("http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0/?action=stop")
                    .accept("application/json")
                    .method(HttpMethod.POST)
                    .file(action_invocation.toPath(), "application/json")
                    .agent("martclient")
                    .send();
            statusResponse = response.getStatus();
            result = response.getContentAsString();

            // We dont use a connector, in the basic implementation the result for an action is "java.lang.UnsupportedOperationException".
            assertTrue(statusResponse == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testDeleteResources() {

        try {
            testCreateResourceInputJson(); // Launch create tests resources....

            System.out.println("DELETE Request on collection resource location : /testlocation/" );
            // TODO : finish integration test with delete method.



        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }


    /**
     * Load file resource test (for json or others files).
     *
     * @param path
     * @return
     */
    private File getResourceInputFile(String path) {
        File inputJsonFile = new File(this.getClass().getResource(path).getFile());
        System.out.println(inputJsonFile.getAbsolutePath());
        return inputJsonFile;
    }

}
