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
package org.occiware.mart.jetty.integration;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.jetty.MartServer;
import org.occiware.mart.server.utils.Constants;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Global occi server tests.
 *
 * @author christophe
 */
public class ServerTest {

    private static HttpClient httpClient;
    private static Thread serverThread;

    @BeforeClass
    public static void startJetty() throws Exception {



        serverThread = new Thread(() -> {
            String[] tests = new String[0];
            try {
                MartServer.main(tests);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        serverThread.start();

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
            MartServer.stopServer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Test the interface.
     */
    @Test
    public void testInterface() throws Exception {
        System.out.println("Testing interface methods on path /-/");
        ContentResponse response;
        String result;

        // First query the interface with GET method.
        // Wait a little to be sure that server thread is started.
        Thread.sleep(4000);
        response = executeQuery(HttpMethod.GET, "http://localhost:8080/-/", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET method on uri /-/", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        // Interface with filter on category network (kind).
        response = executeQuery(HttpMethod.GET, "http://localhost:8080/-/?category=network", HttpServletResponse.SC_OK,
                null,
                "Test query interface with GET Method interface /-/ with network category collection filtering.", Constants.MEDIA_TYPE_JSON, Constants.MEDIA_TYPE_JSON);

        result = response.getContentAsString();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("\"term\" : \"network\","));

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
        	Logger.getLogger(ServerTest.class.getName()).log(Level.SEVERE, null, e);
        }
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
