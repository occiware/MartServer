/**
 * Copyright 2016 - Christophe Gourdin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.occiware.mart.server.servlet.tests;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.server.servlet.model.ConfigurationManager;

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
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public void testCreateResourceInputJson() throws Exception {
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
        
    }
    
    @Test
    public void getComputeResource() {
        // First create a compute resource.
        
        try {
            testCreateResourceInputJson();
            System.out.println("GET Request on Compute kind... http://localhost:9090/compute/");
            ContentResponse response = httpClient.newRequest("http://localhost:9090/compute/")
                    .method(HttpMethod.GET)
                    .accept("application/json")
                    .send();
            int statusResponse = response.getStatus();
            assertTrue(statusResponse == Response.Status.OK.getStatusCode());
            
            String result = response.getContentAsString();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            System.out.println(result);
            
            // Test network kind.
            System.out.println("GET Request on Network kind... http://localhost:9090/compute/");
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
    
    /**
     * Load file resource test (for json or others files).
     * @param path
     * @return 
     */
    private File getResourceInputFile(String path) {
        File inputJsonFile = new File(this.getClass().getResource(path).getFile());
        System.out.println(inputJsonFile.getAbsolutePath());
        return inputJsonFile;
    }

}
