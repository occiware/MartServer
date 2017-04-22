package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

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

        // TODO : support URI-list combined with other output type like this : uri-list; application/json.
        if (occiRequest.isInterfQuery()) {
            LOGGER.info("Querying the interface on path : " + getPath());
            occiRequest.getInterface(getRequestParameters().get(Constants.CATEGORY_KEY));
            return occiResponse.getHttpResponse();
        }

        if (occiRequest.isEntityLocation(getPath())) {
            LOGGER.info("Querying an entity on path : " + getPath());
            occiRequest.findEntity();
            return occiResponse.getHttpResponse();
        }

        if (occiRequest.isCollectionQuery()) {
            // Query on collections of entities.
            CollectionFilter filter = buildCollectionFilter();
            occiRequest.findEntities(filter);
            return occiResponse.getHttpResponse();
        }

        return resp;
    }



}
