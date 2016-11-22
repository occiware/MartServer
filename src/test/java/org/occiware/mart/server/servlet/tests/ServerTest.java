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
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.charset.Charset;

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

    /**
     * Integration test, this test all methods PUT, POST, GET, DELETE.
     * Before start jetty server and after stop server.
     */
    @Test
    public void testServerRequests() {
        try {
            testCreateResourceInputJson();
            testUpdateResources();
            testGetResourceLink();
            testDeleteResources();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
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

        File resource6 = getResourceInputFile("/testjson/integration/creation/resource_without_uuid.json");
        response = httpClient.newRequest("http://localhost:9090/test/")
                .accept("application/json")
                .method(HttpMethod.PUT)
                .file(resource6.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.CREATED.getStatusCode());


    }


    private void testGetResourceLink() throws Exception {

        System.out.println("GET Request interface /-/.");
        ContentResponse response = httpClient.newRequest("http://localhost:9090/-/?category=network")  // uuid: f89486b7-0632-482d-a184-a9195733ddd9
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        int statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        String result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));

        System.out.println("GET Request on resource location : /testlocation/");
        // See file: resource_location.json.
        response = httpClient.newRequest("http://localhost:9090/testlocation/")  // uuid: f89486b7-0632-482d-a184-a9195733ddd9
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        System.out.println(result);

        System.out.println("GET Request on resource location : /test/");
        // See file: resource_location.json.
        response = httpClient.newRequest("http://localhost:9090/test/")  // uuid: unknown
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains(Constants.URN_UUID_PREFIX));
        System.out.println(result);

        // Search on an invalid location path.
        System.out.println("GET Request on resource location : /otherlocation/other2/");
        // See file: resource_location.json.
        response = httpClient.newRequest("http://localhost:9090/otherlocation/other2/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.NOT_FOUND.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        // Search on a relative path location with included key.
        System.out.println("GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9");
        // See file: resource_location.json.
        response = httpClient.newRequest("http://localhost:9090//testlocation/f89486b7-0632-482d-a184-a9195733ddd9")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        // Search on a false relative path but with good keys. This must be not found to avoid path confusion.
        System.out.println("GET Request on resource location : /otherresources/f89486b7-0632-482d-a184-a9195733ddd9");
        // See file: resource_location.json.
        response = httpClient.newRequest("http://localhost:9090/otherresources/f89486b7-0632-482d-a184-a9195733ddd9")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        result = response.getContentAsString();
        System.out.println(result);
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

        System.out.println("GET Request on mixin tag my_mixin2 kind... http://localhost:9090/my_mixin2/");
        response = httpClient.newRequest("http://localhost:9090/my_mixin2/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));
        System.out.println(result);

        System.out.println("GET Request on mixin tag location ... http://localhost:9090/mixins/my_mixin2/");
        response = httpClient.newRequest("http://localhost:9090/mixins/my_mixin2/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));
        System.out.println(result);
    }


    private void testUpdateResources() throws Exception {

        System.out.println("POST Request on resource location : /f88486b7-0632-482d-a184-a9195733ddd0");

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
        System.out.println("POST Request action stop graceful invocation on location: http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0/?action=stop");
        File action_invocation = getResourceInputFile("/testjson/integration/update/action_invocation_test.json");
        response = httpClient.newRequest("http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0/?action=stop")
                .accept("application/json")
                .method(HttpMethod.POST)
                .file(action_invocation.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        // We dont use a connector, in the basic implementation the result for an action is "java.lang.UnsupportedOperationException".
        assertTrue(statusResponse == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        System.out.println("POST Request on collection category /compute/");
        File computesUpdate = getResourceInputFile("/testjson/integration/update/update_attributes_computes.json");
        response = httpClient.newRequest("http://localhost:9090/compute/")
                .accept("application/json")
                .method(HttpMethod.POST)
                .file(computesUpdate.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());

        System.out.println("GET Request http://localhost:9090/compute/");
        response = httpClient.newRequest("http://localhost:9090/compute/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);


        System.out.println("POST Request on collection mixin tag /mixins/my_mixin2/");
        File taggedEntitiesUpdate = getResourceInputFile("/testjson/integration/update/update_attributes_with_mixintag.json");
        response = httpClient.newRequest("http://localhost:9090/mixins/my_mixin2/")
                .accept("application/json")
                .method(HttpMethod.POST)
                .file(taggedEntitiesUpdate.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        System.out.println("GET Request http://localhost:9090/mixins/my_mixin2");
        response = httpClient.newRequest("http://localhost:9090/mixins/my_mixin2")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        System.out.println("GET Request http://localhost:9090/my_mixin2");
        response = httpClient.newRequest("http://localhost:9090/my_mixin2/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        System.out.println("POST Request on collection with filter ?category=network");
        File networkUpdate = getResourceInputFile("/testjson/integration/update/update_network_collection.json");
        response = httpClient.newRequest("http://localhost:9090/?category=network")
                .accept("application/json")
                .method(HttpMethod.POST)
                .file(networkUpdate.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);

        System.out.println("GET Request http://localhost:9090/network/");
        response = httpClient.newRequest("http://localhost:9090/network/")
                .method(HttpMethod.GET)
                .accept("application/json")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        assertTrue(result.contains("public"));
        System.out.println(result);


        // with curl this give : curl -v -X POST --data-binary @thepathtojsonfile.json "http://localhost:9090/?attribute=occi.core.title&" --data-urlencode "value=other compute 1 title"
        System.out.println("POST Request on collection category with filter ?attribute=occi.core.title&value=other compute 1 title");
        File titleFilterUpdate = getResourceInputFile("/testjson/integration/update/update_attributes_with_title_filter.json");

        String titleVal = UrlEncoded.encodeString("other compute 1 title", Charset.forName("UTF-8"));
        response = httpClient.newRequest("http://localhost:9090/?attribute=occi.core.title&value=" + titleVal)
                .accept("application/json")
                .method(HttpMethod.POST)
                .file(titleFilterUpdate.toPath(), "application/json")
                .agent("martclient")
                .send();
        statusResponse = response.getStatus();
        assertTrue(statusResponse == Response.Status.OK.getStatusCode());
        result = response.getContentAsString();
        System.out.println(result);
    }

    private void testDeleteResources() throws Exception {

        System.out.println("DELETE Request, dissociate a mixin http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface on entity link: urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9");



        System.out.println("DELETE Request, dissociate a mixin tag my_mixin2 from a resource compute: urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0");



        System.out.println("DELETE Request, remove a mixin tag definition my_mixin.");



        System.out.println("DELETE Request, remove a mixin tag definition my_mixin2");



        System.out.println("DELETE Request on entity resource location : /urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8/");



        System.out.println("DELETE Request on collection resource (custom) location : /testlocation/");



        System.out.println("DELETE Request on collection resource (kind compute) location : /compute/");





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
