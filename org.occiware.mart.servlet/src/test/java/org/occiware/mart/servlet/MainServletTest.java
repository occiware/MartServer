package org.occiware.mart.servlet;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
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
    public void testServerRequests() {
        try {
            testCreateResourceInputJson();
            // testUpdateResources();
            // testGetResourceLink();
            // testDeleteResources();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }

    @Test
    public void testCreateResourceInputJson() throws Exception {
        HttpMethod httpMethod = HttpMethod.PUT;

        ContentResponse response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource1.json",
                "create a resource with PUT, must be created.");

        // Check if root path with PUT request is authorized, this must not be allowed.
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "/testjson/integration/creation/bad_resource.json",
                "Check if root path with PUT request is authorized, this must not be allowed.");

        // Check Create a bad resource (with a kind definition does not exist on extension / configuration).
        response = executeQuery(httpMethod, "http://localhost:9090/mybadcompute/mybadresource/", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/creation/bad_resource.json",
                "Check Create a bad resource (with a kind definition does not exist on extension / configuration).");


        // Create the second resource with mixins and no uuid defined.
        response = executeQuery(httpMethod, "http://localhost:9090/main/mainnetwork/", HttpServletResponse.SC_OK,
                "/testjson/integration/creation/resource1bis.json",
                "Create the second resource with mixin.");



        // Create a collection of resources.
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource3.json",
                "Create a collection of resources.");

        // Now test a resource single load without "resources" key.
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resourceonly.json",
                "Now test a resource single load without \"resources\" key.");

        // Create a resource with integrated location value.
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource_location.json",
                "Create a resource with integrated location value.");

        // Create a resource without uuid set.
        response = executeQuery(httpMethod, "http://localhost:9090/", HttpServletResponse.SC_CREATED,
                "/testjson/integration/creation/resource_without_uuid.json",
                "Create a resource with integrated location value.");
    }


    public void testGetResourceLink() throws Exception {
        HttpMethod httpMethod = HttpMethod.GET;

        // uuid: f89486b7-0632-482d-a184-a9195733ddd9
        // Test model interface.
        ContentResponse response = executeQuery(httpMethod, "http://localhost:9090/-/?category=network", HttpServletResponse.SC_OK,
                null,
                "GET Request interface /-/.");

        String result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));

        // See file: resource_location.json.
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /testlocation/");
        result = response.getContentAsString();
        assertFalse(result.isEmpty());

        // GET request on resource location /test/
        response = executeQuery(httpMethod, "http://localhost:9090/test/", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /test/");
        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains(Constants.URN_UUID_PREFIX));

        // GET request on resource location : /otherlocation/other2/
        response = executeQuery(httpMethod, "http://localhost:9090/otherlocation/other2/", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /otherlocation/other2/");
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.equals(JsonOcciParser.EMPTY_JSON));

        // GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_OK,
                null,
                "GET Request on resource location : /testlocation/f89486b7-0632-482d-a184-a9195733ddd9");
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Search on a false relative path but with good keys. This must be not found to avoid path confusion.
        executeQuery(httpMethod, "http://localhost:9090/otherresources/f89486b7-0632-482d-a184-a9195733ddd9", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request on resource location : /otherresources/f89486b7-0632-482d-a184-a9195733ddd9");

        // Search a collection on compute kind.
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "GET Request on kind compute collection : /compute/");

        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Test network kind.
        response = executeQuery(httpMethod, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "GET Request on kind network collection : /network/");
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Test network interface link.
        response = executeQuery(httpMethod, "http://localhost:9090/networkinterface/", HttpServletResponse.SC_OK,
                null,
                "GET Request on Networkinterface kind... http://localhost:9090/networkinterface/");

        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Get entities via mixin tag.
        response = executeQuery(httpMethod, "http://localhost:9090/my_mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag (my_mixin2)... http://localhost:9090/my_mixin2/");

        result = response.getContentAsString();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));

        // GET entities Request on mixin tag location ... http://localhost:9090/mixins/my_mixin2/
        response = executeQuery(httpMethod, "http://localhost:9090/mixins/my_mixin2/", HttpServletResponse.SC_OK,
                null,
                "Get entities via mixin tag location: /mixins/my_mixin2 ==> http://localhost:9090/mixins/my_mixin2/");
        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("f88486b7-0632-482d-a184-a9195733ddd0"));

    }


    public void testUpdateResources() throws Exception {
        HttpMethod httpMethod = HttpMethod.POST;
        // Update a resource.
        executeQuery(httpMethod, "http://localhost:9090/mycomputes/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/resource1.json",
                "Update a resource with POST, must be updated.");


        System.out.println("POST Request on resource location : /f88486b7-0632-482d-a184-a9195733ddd0");



        // Check after update GET :
        ContentResponse response = executeQuery(HttpMethod.GET, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0");

        String result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("4.1")); // Check value occi.compute.memory.

        // associate a mixin tag to a resource.
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                "/testjson/integration/update/mixintag_asso.json",
                "Associate a mixin tag to a resource");

        // Get the resource.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0");

        result = response.getContentAsString();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"http://occiware.org/occi/tags#my_mixin2\""));


        // action invocation.
        // We dont use a connector, in the basic implementation the result for an action is "java.lang.UnsupportedOperationException".
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=stop", HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "/testjson/integration/update/action_invocation_test.json",
                "POST Request action stop graceful invocation on location: http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=stop");


        // Action invocation without attributes field. ==> Start
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=start", HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "/testjson/integration/update/action_invocation_test.json",
                "POST Request action start invocation on location: http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=start");

        // action invocation with direct term without the action kind definition.
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=start", HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                null,
                "POST action invocation with direct term without the action kind definition on location: http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=start");

        // bad action invocation the action doesnt exist on entity.
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=test", HttpServletResponse.SC_BAD_REQUEST,
                null,
                "POST Request action test invocation on location: http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=test");

        // 5 : bad action invocation the action scheme+term doesnt exist on entity.
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=titi", HttpServletResponse.SC_BAD_REQUEST,
                "/testjson/integration/update/update_attributes_computes.json",
                "POST Request action test invocation on location: http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0/?action=test");

        // 6 : update attributes on collection /compute/
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/update_attributes_computes.json",
                "update attributes on collection /compute/ ");

        // 7 : Get on /compute/ to check if content has been updated.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/compute/");
        // TODO : Check content updated result.


        // 8 : POST Request on collection mixin tag /mixins/my_mixin2/
        response = executeQuery(httpMethod, "http://localhost:9090/mixins/my_mixin2/", HttpServletResponse.SC_OK,
                "/testjson/integration/update/update_attributes_with_mixintag.json",
                "POST Request on collection mixin tag /mixins/my_mixin2/");

        // 9 : Get to check result.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/mixins/my_mixin2", HttpServletResponse.SC_OK,
                null,
                "POST Request on collection mixin tag /mixins/my_mixin2/");

        // TODO : Check content updated result.

        // Post on collection with a filter set. here : ?category=network.
        response = executeQuery(httpMethod, "http://localhost:9090/?category=network", HttpServletResponse.SC_OK,
                "/testjson/integration/update/update_attributes_with_mixintag.json",
                "POST Request on collection with filter ?category=network");

        // Get collection on kind network.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/network/", HttpServletResponse.SC_OK,
                null,
                "Get collection on kind /network/");
        result = response.getContentAsString();
        assertTrue(result.contains("public"));


        // with curl this give : curl -v -X POST --data-binary @thepathtojsonfile.json "http://localhost:9090/?attribute=occi.core.title&" --data-urlencode "value=other compute 1 title"
        String titleVal = UrlEncoded.encodeString("other compute 1 title", Charset.forName("UTF-8"));
        response = executeQuery(httpMethod, "http://localhost:9090/?attribute=occi.core.title&value=" + titleVal, HttpServletResponse.SC_OK,
                "/testjson/integration/update/update_attributes_with_title_filter.json",
                "POST Request on collection with filter ?category=network");
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
                        "on entity link: urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9");

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/networkinterface/b2fe83ae-a20f-54fc-b436-cec85c94c5e9", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/networkinterface/b2fe83ae-a20f-54fc-b436-cec85c94c5e9");


        String result = response.getContentAsString();
        assertFalse(result.contains("http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"));


        // DELETE Request, dissociate a mixin tag my_mixin2 from a resource compute: urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0
        response = executeQuery(httpMethod, "http://localhost:9090/mycomputes/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/dissociate_mixin_tag.json",
                "dissociate a mixin tag my_mixin2 from a resource compute: urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0");


        // GET request : http://localhost:9090/f88486b7-0632-482d-a184-a9195733ddd0
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/mycomputes/f88486b7-0632-482d-a184-a9195733ddd0");

        result = response.getContentAsString();
        assertFalse(result.contains("http://occiware.org/occi/tags#my_mixin2"));
        System.out.println(result);

        // Delete request : remove a mixin tag definition my_mixin
        response = executeQuery(httpMethod, "http://localhost:9090/-/", HttpServletResponse.SC_OK,
                "/testjson/integration/delete/remove_mixin_tag_definition.json",
                "DELETE Request, remove a mixin tag definition my_mixin.");


        response = executeQuery(HttpMethod.GET, "http://localhost:9090/-/?category=my_mixin", HttpServletResponse.SC_NO_CONTENT,
                null,
                "GET Request http://localhost:9090/-/?category=my_mixin");

        result = response.getContentAsString();
        assertFalse(result.contains("http://occiware.org/occi/tags#my_mixin"));

        // Delete request : remove entity resource
        response = executeQuery(httpMethod, "http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", HttpServletResponse.SC_OK,
                null,
                "DELETE Request on entity resource location : /a1cf3896-500e-48d8-a3f5-a8b3601bcdd8");

        // Get entity previously deleted, must not found.
        response = executeQuery(HttpMethod.GET, "http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8", HttpServletResponse.SC_NOT_FOUND,
                null,
                "GET Request http://localhost:9090/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8");

        // Delete on a collection location (custom)
        response = executeQuery(httpMethod, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "Delete on a collection location (custom), /testlocation/");

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/testlocation/", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/testlocation/");

        result = response.getContentAsString();
        assertNotNull(result);
        assertTrue(result.equals(JsonOcciParser.EMPTY_JSON));


        // DELETE on a collection kind. /compute/
        response = executeQuery(httpMethod, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "DELETE on a collection kind. /compute/");

        response = executeQuery(HttpMethod.GET, "http://localhost:9090/compute/", HttpServletResponse.SC_OK,
                null,
                "GET Request http://localhost:9090/compute/");

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
     * @return a content response object of this executed query.
     * @throws Exception object if any internal error on httpclient service.
     */
    private ContentResponse executeQuery(final HttpMethod httpMethod, String uri, final int statusCodeToCheck, final String filePath, final String messageBefore) throws Exception {
        System.out.println(messageBefore);
        ContentResponse response;

        if (filePath != null) {
            File myFile = getResourceInputFile(filePath);
            response = httpClient.newRequest(uri)
                    .method(httpMethod)
                    .file(myFile.toPath(), "application/json")
                    .accept("application/json")
                    .agent("martclient")
                    .send();
        } else {
            response = httpClient.newRequest(uri)
                    .method(httpMethod)
                    .accept("application/json")
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
