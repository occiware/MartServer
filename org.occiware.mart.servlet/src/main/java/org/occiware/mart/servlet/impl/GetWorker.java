package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * Created by cgourdin on 13/04/2017.
 *
 */
public class GetWorker extends ServletEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetWorker.class);
    public GetWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        if (!occiRequest.getContentDatas().isEmpty()) {
            return occiResponse.parseMessage("Input content are not accepted with GET method", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isActionInvocationQuery()) {
            LOGGER.warn("Querying action invocation on GET method.");
            return occiResponse.parseMessage("You cannot use an action with GET method", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isInterfQuery()) {
            LOGGER.info("Querying the interface on path : " + occiRequest.getRequestPath());
            occiRequest.getModelsInterface(getRequestParameters().get(Constants.CATEGORY_KEY), getRequestParameters().get(Constants.EXTENSION_NAME_KEY));
            return occiResponse.getHttpResponse();
        }

        if (occiRequest.isEntityLocation(getPath()) || occiRequest.isCollectionQuery()) {
            LOGGER.info("Querying entities on location : " + occiRequest.getRequestPath());
            occiRequest.findEntities(getPath(), buildCollectionFilter());
            return occiResponse.getHttpResponse();
        }

        occiResponse.parseMessage("The request is malformed", HttpServletResponse.SC_BAD_REQUEST);

        return resp;
    }



}
