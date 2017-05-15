package org.occiware.mart.servlet;

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
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.utils.Constants;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by christophe on 19/04/2017.
 */
public class MainServletTest {

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
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute1", HttpServletResponse.SC_OK,
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
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/compute/1", HttpServletResponse.SC_OK,
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
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/mynetworks/mainnetwork/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource1bis.json",
                "Create the second resource with mixin on location : /mynetworks/mainnetwork.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute2", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resourceonly.json",
                "Other test create resource without ending slash on location : /myresources/compute2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // This test location conflict on put method (request path differ from location).
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/", HttpServletResponse.SC_CONFLICT,
                "/testjson/integration/creation/resource_location.json",
                "Create a resource with integrated location value but conflict with request path.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a resource with integrated location value.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/testlocation/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource_location.json",
                "Create a resource with integrated location value.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a resource without uuid set.
        response = executeQuery(HttpMethod.PUT, "http://localhost:9090/myresources/compute3/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource_without_uuid.json",
                "Create a resource without uuid", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
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
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource4_no_location.json",
                "Create resource postcompute1 with POST using json and collection category path, but no locations set on resource, on location: /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
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
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource6.json",
                "Create resource network2 - POST method - on location: /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Create a collection of networks.
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
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
        response = executeQuery(HttpMethod.POST, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
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


        // with curl this give : curl -v -X POST --data-binary @thepathtojsonfile.json "http://localhost:9090/?attribute=occi.core.title&" --data-urlencode "value=other compute 1 title"
        // String titleVal = UrlEncoded.encodeString("other compute 1 title", Charset.forName("UTF-8"));
        // response = executeQuery(httpMethod, "http://localhost:9090/?attribute=occi.core.title&value=" + titleVal, HttpServletResponse.SC_OK,
        //         "/testjson/integration/update/update_attributes_with_title_filter.json",
        //         "POST Request on collection with filter ?category=network", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


    }


    public void postMixinTagsAssociation() throws Exception {
        // TODO : PUT other entities collection on mixins tag path and check if the new ones replace old entities association.
        HttpMethod httpMethod = HttpMethod.POST;
        ContentResponse response;
        // Define a mixin tag
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin1.json",
                "Create a mixin tag named usermixin1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // define a second mixin tag
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin2.json",
                "Create a mixin tag named usermixin2", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // redefine the mixin user tag 1.
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_usermixin1.json",
                "Redefine the mixin user tag 1", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // redefine mixin user tags with a collection of mixins.
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/definemixintags.json",
                "Redefine all the mixin user tags with a collection of mixins", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Define a new collection of mixin tags
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/mixintag_collection.json",
                "Define a new collection of mixin tags", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        // Add a collection of resources to a mixin tag.
        executeQuery(httpMethod, "http://localhost:9090/tags/mixin1/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/resource1.json",
                "Associate a mixin tag to entities from its mixin tag location path.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
    }



    public void testGetResourceLink() throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;

        // Test model interface with no filters.
        ContentResponse response = executeQuery(httpMethod, "http://localhost:9090/.well-known/org/ogf/occi/-/", HttpServletResponse.SC_OK,
                null,
                "GET Request interface /-/ with no filters.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Test model interface.
        response = executeQuery(httpMethod, "http://localhost:9090/-/?category=network", HttpServletResponse.SC_OK,
                null,
                "GET Request interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        String result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));


        // Test on collection compute category with location only.
        response = executeQuery(httpMethod, "http://localhost:9090/compute", HttpServletResponse.SC_OK,
                null,
                "GET Request collection.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // See file: resource_location.json.
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /testlocation/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertFalse(result.isEmpty());

        // Same test with myresources collection.
        response = executeQuery(httpMethod, "http://localhost:9090/myresources/", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /myresources/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // GET request on resource location /test/
        response = executeQuery(httpMethod, "http://localhost:9090/test/", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request on resource location : /test/, this must be resource not found ==> 404", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
//        result = response.getContentAsString();
//        assertFalse(result.isEmpty());
//        assertTrue(result.contains(Constants.URN_UUID_PREFIX));

        // GET request on resource location : /otherlocation/other2/
        response = executeQuery(httpMethod, "http://localhost:9090/otherlocation/other2/", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request on resource location : /otherlocation/other2/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
//        result = response.getContentAsString();
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//        assertTrue(result.equals(JsonOcciParser.EMPTY_JSON));

        // GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(Constants.URN_UUID_PREFIX));
        assertTrue(result.contains("f89486b7-0632-482d-a184-a9195733ddd9"));

        // Search on a false relative path but with good keys. This must be not found to avoid path confusion.
        executeQuery(httpMethod, "http://localhost:9090/otherresources/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request on resource location : /otherresources/f89486b7-0632-482d-a184-a9195733ddd9", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Search a collection on compute kind.
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "GET Request on kind compute collection : /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Test network kind.
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "GET Request on kind network collection : /network/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Test network interface link.
        response = executeQuery(httpMethod, "http://localhost:9090/networkinterface/", HttpServletResponse.SC_OK,
                null,
                "GET Request on Networkinterface kind... http://localhost:9090/networkinterface/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Get entities via mixin tag.
        response = executeQuery(httpMethod, "http://localhost:9090/my_mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag (my_mixin2)... http://localhost:9090/my_mixin2/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));

        // GET entities Request on mixin tag location ... http://localhost:9090/mixins/my_mixin2/
        response = executeQuery(httpMethod, "http://localhost:9090/mixins/my_mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag location: /mixins/my_mixin2 ==> http://localhost:9090/mixins/my_mixin2/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));

    }




    /**
     *
     * @throws Exception
     */
    private void testDeleteResources() throws Exception {
        HttpMethod httpMethod = HttpMethod.DELETE;
        // DELETE Request, dissociate a mixin http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface
        // on entity link: urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9

        ContentResponse response;
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/dissociate_mixin.json",
                "DELETE Request, dissociate a mixin http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface " +
                        "on entity link: urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/networkinterface/b2fe83ae-a20f-54fc-b436-cec85c94c5e9", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/networkinterface/b2fe83ae-a20f-54fc-b436-cec85c94c5e9", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        String result = response.getContentAsString();
        assertFalse(result.contains("http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"));


        // DELETE Request, dissociate a mixin tag my_mixin2 from a resource compute: urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/dissociate_mixin_tag.json",
                "dissociate a mixin tag my_mixin2 from a resource compute: urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        // GET request : http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.contains("http://occiware.org/occi/tags#my_mixin2"));
        System.out.println(result);

        // Delete request : remove a mixin tag definition my_mixin
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/remove_mixin_tag_definition.json",
                "DELETE Request, remove a mixin tag definition my_mixin.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);


        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/?category=my_mixin", HttpServletResponse.SC_NO_CONTENT,
                null,
                "GET Request http://localhost:9090/-/?category=my_mixin", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.contains("http://occiware.org/occi/tags#my_mixin"));

        // Delete request : remove entity resource
        response = executeQuery(httpMethod, "http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", HttpServletResponse.SC_OK,
                null,
                "DELETE Request on entity resource location : /a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Get entity previously deleted, must not found.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Delete on a collection location (custom)
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "Delete on a collection location (custom), /testlocation/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/testlocation/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertNotNull(result);
        assertTrue(result.equals(JsonOcciParser.EMPTY_JSON));


        // DELETE on a collection kind. /compute/
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "DELETE on a collection kind. /compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/compute/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertNotNull(result);
        assertTrue(result.equals(JsonOcciParser.EMPTY_JSON));
        System.out.println(result);

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

    /**
     * Execute an http query and check if status code response is equals to parameter statusCodeToCheck.
     * @param httpMethod HttpMethod.GET, .PUT etc.
     * @param uri uri of the request like http://localhost:9090/mycomputetest/
     * @param statusCodeToCheck http status code to check on response
     * @param messageBefore message to display before executing this query.
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
                    .file(myFile.toPath(), contentType)
                    .accept(acceptType)
                    .agent("martclient")
                    .send();
        } else {
            response = httpClient.newRequest(uri)
                    .method(httpMethod)
                    .accept(acceptType)
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
