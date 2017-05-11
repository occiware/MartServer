package org.occiware.mart.servlet;

import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.servlet.impl.DeleteWorker;
import org.occiware.mart.servlet.impl.GetWorker;
import org.occiware.mart.servlet.impl.PostWorker;
import org.occiware.mart.servlet.impl.PutWorker;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
