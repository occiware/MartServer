package org.occiware.mart.servlet;

import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.servlet.impl.GetQuery;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

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
        OCCIRequest.initMart();
    }

    /**
     * Get method.
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
        String requestPath = req.getPathInfo();

        // Get input headers in a map.
        Map<String, String> headers = ServletUtils.getRequestHeaders(req);
        URI ServerURI = ServletUtils.getServerURI(req);


        //



        LOGGER.debug("doGet method for path: " + requestPath);

        GetQuery getQuery = new GetQuery();





    }



    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }



}
