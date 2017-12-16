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
package org.occiware.mart.servlet.integration;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.server.facade.AppParameters;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.servlet.MainServlet;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by christophe on 19/04/2017.
 */
public class MainServletIT {

    private static Server server = null;
    private static HttpClient httpClient;


    @BeforeClass
    public static void startJetty() throws Exception {

        ServletHandler handler = new ServletHandler();
        int port = 9090;
        Server server = new Server(port);
        server.setHandler(handler);
        handler.addServletWithMapping(MainServlet.class, "/*");

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
            if (server != null) {
                server.stop();
            }
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
            // All the tests are based on occi json.
            // Test operation with /-/ to be compliant with occi spec.
            testInterfaceMethods();

            // Test operations with /myresources/mycompute...
            testsOnEntityLocation();

            // Test operations on collection kind location (/compute/) ==>
            testsOnKindCollection();

            // Test operations on collection mixin tag location (/my_stuff/).
            testsOnMixinTagsAssociation();

            // Test operations on custom location /myresources/*...


        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }

    public void testInterfaceMethods() throws Exception {
        System.out.println("Testing interface methods on path /-/");
        ContentResponse response;
        String result;
        HttpMethod httpMethod;
        AppParameters parameters = AppParameters.getInstance();
        parameters.loadParametersFromConfigFile(null);

        // First query the interface with GET method.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET method on uri /-/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Interface with filter on category network (kind).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/?category=network", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET Method interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));

        // Interface with filter on category ipnetwork (mixin).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/?category=ipnetwork", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET Method interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"ipnetwork\","));

        // System.out.println("Same tests interface methods on /.wellknown../-/ path");
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/.well-known/org/ogf/occi/-/", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET method on uri /.well-known/org/ogf/occi/-/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Interface with filter on category network (kind).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/.well-known/org/ogf/occi/-/?category=network", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET Method interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));

        // Interface with filter on category ipnetwork (mixin).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/.well-known/org/ogf/occi/-/?category=ipnetwork", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET Method interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"ipnetwork\","));

        System.out.println("Add user mixin tests");
        httpMethod = HttpMethod.POST;
        // Add user defined mixins
        // Define a mixin tag
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin1.json",
                "Create a mixin tag named usermixin1 - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // define a second mixin tag
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin2.json",
                "Create a mixin tag named usermixin2 - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // redefine the mixin user tag 1.
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin1.json",
                "Redefine the mixin user tag 1 - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // redefine mixin user tags with a collection of mixins.
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/definemixintags.json",
                "Redefine all the mixin user tags with a collection of mixins  - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create other collection of mixin tags.
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_collection.json",
                "Define another collection of mixin tags - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Retrieve mixin tags definition
        httpMethod = HttpMethod.GET;
        response = executeQuery(httpMethod, "http://localhost:9090/-/?category=usermixin1", HttpServletResponse.SC_OK,
                null,
                "Retrieve mixin tag usermixin1 on interface - GET method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"usermixin1\","));

        response = executeQuery(httpMethod, "http://localhost:9090/-/?category=usermixin2", HttpServletResponse.SC_OK,
                null,
                "Retrieve mixin tag usermixin2 on interface - GET method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"usermixin2\","));


        // Remove a user defined mixin

        response = executeQuery(HttpMethod.DELETE, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/remove_mixin_tag_usermixin2.json",
                "Remove user mixin tag usermixin2 from interface - DELETE method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Check that mixin usermixin2 is totally removed.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/?category=usermixin2", HttpServletResponse.SC_OK,
                null,
                "Retrieve mixin tag usermixin2 on interface - GET method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertFalse(result.contains("\"term\" : \"usermixin2\","));

        // Now testing PUT method with interface, this must fail.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/-/", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/creation/mixintag_usermixin1.json",
                "Test query interface with PUT method. Must fail with bad request.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Test with action post :
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/-/?action=stop", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/update/action_invocation_test.json",
                "Test query interface with action trigger on POST method. Must fail with bad request.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

    }

    public void testsOnEntityLocation() throws Exception {
        System.out.println("Testing entity operations on uri /myresources/mycompute1 and /compute/1");

        ContentResponse response;
        String result;
        HttpMethod httpMethod;

        // Create a resource entity on path /myresources/compute1.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource1.json",
                "create a resource with PUT, must be created on path : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Partial update of the entity on location /myresources/compute1
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_OK,
                "/testjson/integration/update/update_attributes_computes.json",
                "Update compute attribute cores and memory attribute - POST method, must be updated on path : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Action invocation stop on this entity.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/myresources/compute1?action=stop", HttpServletResponse.SC_OK,
                "/testjson/integration/update/action_invocation_test.json",
                "Trigger action stop on entity - POST method, must be triggered on location : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Retrieve the entity.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_OK,
                null,
                "Retrieve entity - GET method, on location : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Remove the entity.
        response = executeQuery(HttpMethod.DELETE, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_OK,
                null,
                "Remove the entity - GET method, on location : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Assure that the entity is removed from configuration.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_NOT_FOUND,
                null,
                "Is the entity is removed - GET method, on location : /myresources/compute1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Check with path /compute/1 as in OCCI spec.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/compute/1", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource1.json",
                "create a resource with PUT, must be created on path : /compute/1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Check if root path with PUT request is authorized, this must not be allowed.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/", HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "/testjson/integration/creation/bad_resource.json",
                "Check if root path with PUT request is authorized, this must not be allowed.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Check Create a bad resource (with a kind definition does not exist on extension / configuration).
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/mybadcompute/mybadresource/", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/creation/bad_resource.json",
                "Check Create a bad resource (with a kind definition does not exist on extension / configuration).", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create the second resource with mixins and no uuid defined.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/mynetworks/mainnetwork/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource1bis.json",
                "Create the second resource with mixin on location : /mynetworks/mainnetwork.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute2", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resourceonly.json",
                "Other test create resource without ending slash on location : /myresources/compute2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // This test location conflict on put method (request path differ from location).
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/", HttpServletResponse.SC_CONFLICT,
                "/testjson/integration/creation/resource_location.json",
                "Create a resource with integrated location value but conflict with request path.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a resource with integrated location value.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/testlocation/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource_location.json",
                "Create a resource with integrated location value.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a resource without uuid set.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute3/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource_without_uuid.json",
                "Create a resource without uuid", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Links part.
        // Link networkinterface from source /myresources/compute2 to target : /mynetworks/mainnetwork/ on /mynetworks/networkint2/
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/mynetworks/networkint2", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/links.json",
                "Create a link without uuid on /mynetworks/networkint2 and source is : /myresources/compute2 and target is : /mynetworks/mainnetwork", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Describe the /myresources/compute2, this must contains the link on location : /mynetworks/networkint2/
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/mynetworks/networkint2", HttpServletResponse.SC_OK,
                null,
                "GET a link on /mynetworks/networkint2 and source is : /myresources/compute2 and target is : /mynetworks/mainnetwork", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        assertTrue(response.getContentAsString().contains("http://schemas.ogf.org/occi/infrastructure#networkinterface"));
        assertTrue(response.getContentAsString().contains("\"location\" : \"/mynetworks/mainnetwork\","));
        assertTrue(response.getContentAsString().contains("\"location\" : \"/myresources/compute2\","));
    }


    public void testsOnKindCollection() throws Exception {
        System.out.println("Testing kind collection on location /compute/ and /network/");

        ContentResponse response;
        String result;
        HttpMethod httpMethod;

        // Create a collection of resources, this must not be authorized in PUT REQUEST, this is authorized in POST request.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/compute/", HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "/testjson/integration/creation/resource3.json",
                "Create a collection of resources with PUT method.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        httpMethod = HttpMethod.POST;
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource4_no_location.json",
                "Create resource postcompute1 with POST using json and collection category path, but no locations set on resource, on location: /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource5_with_location.json",
                "Create resource postcompute2 with POST using location, and id set on id value, on location: /myresources/mypostcomputes/postcompute2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/creation/resource6.json",
                "Create resource network2 with POST, must fail, because network is not a compute, on location: /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        httpMethod = HttpMethod.GET;
        // Get request on /compute/
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "Get resource collection for the compute kind , location : /compute/ ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Get request on /compute/ using text/uri-list.
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "Get resource collection for the compute kind , location : /compute/ using text/uri-list", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        // Search response on headers.
        HttpFields fields = response.getHeaders();
        List<String> xlocations = fields.getValuesList("X-OCCI-Location");
        assertFalse(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }

        // Querying on /network/
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "Get resource collection for the compute kind , location : /network/ ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        // Same query with text/uri-list
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "Get resource collection for the compute kind , location : /network/ using text/uri-list", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        fields = response.getHeaders();
        xlocations = fields.getValuesList("X-OCCI-Location");
        assertFalse(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }

        // Action trigger on collection :  Stop all compute resources kind.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/compute/?action=stop", HttpServletResponse.SC_OK,
                "/testjson/integration/update/action_invocation_test.json",
                "Trigger action stop on entity - POST method, must be triggered on location : /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Add new network resources.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource6.json",
                "Create resource network2 - POST method - on location: /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a collection of networks.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/network_collection.json",
                "Create a collection of networks (network3-network4-network5) - POST method - on location: /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // remove all network.
        response = executeQuery(HttpMethod.DELETE, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "Remove all network - DELETE method, on location : /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Check /network/ collection
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_NOT_FOUND,
                null,
                "Get resource collection for the network kind , location : /network/ ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        fields = response.getHeaders();
        xlocations = fields.getValuesList(Constants.X_OCCI_LOCATION);
        assertTrue(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }

        // Recreate the collection.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/network_collection.json",
                "Create a collection of networks (network3-network4-network5) - POST method - on location: /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Check /network/ collection
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "Get resource collection for the network kind , location : /network/ ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        fields = response.getHeaders();
        xlocations = fields.getValuesList(Constants.X_OCCI_LOCATION);
        assertFalse(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }

    }


    public void testsOnMixinTagsAssociation() throws Exception {

        HttpMethod httpMethod = HttpMethod.POST;
        ContentResponse response;
        // Define a mixin tag

        // Add a collection of resources to a mixin tag using entities locations.
        response = executeQuery(httpMethod, "http://localhost:9090/tags/mixin1/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/mixintag_add_entities.json",
                "Associate a mixin tag with entities", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Get collection mixin :
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin1/", HttpServletResponse.SC_OK,
                null,
                "Get entities collection from location mixin : /tags/mixin1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);
        // Search response on headers.
        HttpFields fields = response.getHeaders();
        List<String> xlocations = fields.getValuesList("X-OCCI-Location");
        assertFalse(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }

        // Re create the mixin usermixin2 for further tests (in post method)...
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin2.json",
                "Recreate a mixin tag named usermixin2 - POST method, this for further testing...", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // Add one other resource to a mixin tag using resource rendering, using entity update method.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/myresources/mypostcomputes/postcompute2", HttpServletResponse.SC_OK,
                "/testjson/integration/update/mixintag_add_resource.json",
                "Associate a mixin tag via update entity method - POST method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // TO check that mixin 2 has the resource added
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities collection from location mixin : /tags/mixin2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        // Search response on headers.
        fields = response.getHeaders();
        xlocations = fields.getValuesList("X-OCCI-Location");
        assertFalse(xlocations.isEmpty());
        for (final String location : xlocations) {
            System.out.println("Location in header X-OCCI-Location: " + location);
        }


        // Replace all associated entities using PUT method.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/tags/mixin1/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/mixintag_replace_all_entities.json",
                "Replace associated entities by new ones", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Mixin associate with entity collection must have been replaced.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin1/", HttpServletResponse.SC_OK,
                null,
                "Get mixin1 collection and check if entity replaced on location /tags/mixin1 - GET method", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST);

        // Search response on headers.
        fields = response.getHeaders();
        xlocations = fields.getValuesList("X-OCCI-Location");
        boolean foundnetwork4 = false;
        for (final String xlocation : xlocations) {
            assertFalse(xlocation.contains("/mainnetwork/network1"));
            if (xlocation.contains("/mainnetwork/network4")) {
                foundnetwork4 = true;
            }
            System.out.println("Location in header X-OCCI-Location: " + xlocation);
        }
        assertTrue(foundnetwork4);

        assertFalse(xlocations.isEmpty());

        // Get entities with filter using mixin tag location.
        // with curl this give : curl -v -X POST --data-binary @thepathtojsonfile.json "http://localhost:9090/?attribute=occi.core.title&" --data-urlencode "value=other compute 1 title"
        String titleVal = UrlEncoded.encodeString("postcompute2", Charset.forName("UTF-8"));
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin1/?attribute=occi.core.title&value=" + titleVal + "&number=-1&page=1", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag location with filter on title attribute value : /tags/mixin1/ - GET method ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Check with category filter where we display only the first one found of the collection (number ==> number of entities to display per page).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin1/?category=network", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag location : /tags/mixin1/ , with filter on kind network - GET method ", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Action on mixin tag location:
        // Action trigger on collection :  start all compute resources kind.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/tags/mixin2/?action=start", HttpServletResponse.SC_OK,
                "/testjson/integration/update/action_invocation_test2.json",
                "Trigger action start on compute entity using tag - POST method, must be triggered on location : /tags/mixin2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Delete entity sub type instances of the mixin2.
        response = executeQuery(HttpMethod.DELETE, "http://localhost:9090/tags/mixin2/", HttpServletResponse.SC_OK,
                null,
                "remove entities using tag collection - DELETE method, on location : /tags/mixin2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Get entity sub type instances for the mixin 2 collection.
        // Must return empty collection =+> mixin tag is a category (mixin category).
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/tags/mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities using tag collection - GET method, on location : /tags/mixin2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

    }


    /**
     * Load file resource test (for json or others files).
     *
     * @param path
     * @return
     */
    private File getResourceInputFile(String path) {
        File inputJsonFile = null;
        try {
            inputJsonFile = new File(new URI(this.getClass().getResource(path).toString()));
            System.out.println(inputJsonFile.getAbsolutePath());
        } catch (URISyntaxException e) {
            Logger.getLogger(MainServletIT.class.getName()).log(Level.SEVERE, null, e);
        }
        return inputJsonFile;
    }

    /**
     * Execute an http query and check if status code response is equals to parameter statusCodeToCheck.
     *
     * @param httpMethod        HttpMethod.GET, .PUT etc.
     * @param uri               uri of the request like http://localhost:9090/mycomputetest/
     * @param statusCodeToCheck http status code to check on response
     * @param messageBefore     message to display before executing this query.
     * @param contentType
     * @param acceptType
     * @return a content response object of this executed query.
     * @throws Exception object if any internal error on httpclient service.
     */
    private ContentResponse executeQuery(final HttpMethod httpMethod, String uri, final int statusCodeToCheck, final String filePath, final String messageBefore, String contentType, String acceptType) throws Exception {
        System.out.println(messageBefore);
        ContentResponse response;

        if (filePath != null) {
            File myFile = getResourceInputFile(filePath);
            response = httpClient.newRequest(uri)
                    .method(httpMethod)
                    .header("Authorization", "Basic dGVzdDoxMjM0") // test default user.
                    .file(myFile.toPath(), contentType)
                    .header("Content-Type", contentType)
                    .accept(acceptType)
                    .agent("martclient")
                    .send();
        } else {
            response = httpClient.newRequest(uri)
                    .method(httpMethod)
                    .header("Authorization", "Basic dGVzdDoxMjM0") // test default user.
                    .accept(acceptType)
                    .header("Content-Type", contentType)
                    .agent("martclient")
                    .send();
        }

        int status = response.getStatus();
        // Display the result.
        String result = response.getContentAsString();
        System.out.println(result);
        // Check status
        assertTrue(status == statusCodeToCheck);

        return response;
    }


}
