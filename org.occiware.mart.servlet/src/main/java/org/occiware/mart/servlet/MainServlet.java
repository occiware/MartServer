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
package org.occiware.mart.servlet;

import org.occiware.mart.server.facade.*;
import org.occiware.mart.server.exception.ApplicationConfigurationException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.servlet.impl.*;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class MainServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainServlet.class);


    @Override
    public void init() throws ServletException {
        super.init();

        // Create a default configuration object (default user) to initialize it.
        LOGGER.info("Init MART main servlet.");

        LOGGER.info("Checking configuration parameters...");
        AppParameters appParameters = AppParameters.getInstance();
        try {
            if (!appParameters.isConfigLoaded()) {
                // try to load configuration default.
                appParameters.loadParametersFromConfigFile(null);
            }
        } catch (ApplicationConfigurationException ex) {
            throw new ServletException("Cannot configure the application : " + ex.getMessage());
        }
    }

    /**
     * Get method.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // super.doGet(req, resp);
        String requestPath = req.getPathInfo();
        // Get input headers in a map.
        HeaderPojo headers = ServletUtils.getRequestHeaders(req);
        URI serverURI = ServletUtils.getServerURI(req);

        LOGGER.debug("doGet method for path: " + requestPath);

        GetWorker worker = new GetWorker(serverURI, resp, headers, req, requestPath);

        resp = worker.executeQuery();
        System.out.println("Response status: " + resp.getStatus());


    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // super.doPost(req, resp);
        String requestPath = req.getPathInfo();
        HeaderPojo headers = ServletUtils.getRequestHeaders(req);
        URI serverURI = ServletUtils.getServerURI(req);

        LOGGER.debug("doPost method for path: " + requestPath);

        PostWorker worker = new PostWorker(serverURI, resp, headers, req, requestPath);

        resp = worker.executeQuery();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // super.doPut(req, resp);
        String requestPath = req.getPathInfo();
        HeaderPojo headers = ServletUtils.getRequestHeaders(req);
        URI serverURI = ServletUtils.getServerURI(req);

        LOGGER.debug("doPut method for location: " + requestPath);

        PutWorker worker = new PutWorker(serverURI, resp, headers, req, requestPath);

        resp = worker.executeQuery();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // super.doDelete(req, resp);
        String requestPath = req.getPathInfo();
        HeaderPojo headers = ServletUtils.getRequestHeaders(req);
        URI serverURI = ServletUtils.getServerURI(req);

        LOGGER.debug("doDelete method for location: " + requestPath);

        DeleteWorker worker = new DeleteWorker(serverURI, resp, headers, req, requestPath);

        resp = worker.executeQuery();

    }



}
