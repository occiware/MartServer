package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.Data;
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
        occiRequest.validateRequest();

        List<Data> datas = occiRequest.getDatas();
        Data data = null;
        if (!datas.isEmpty()) {
            // Get only the first occurence. The others are ignored.
            data = datas.get(0);
        }
        // TODO : Manage calls on occiRequest for findEntity, getCollections etc..
        if (data == null) {
            return occiResponse.parseMessage("resource " + getPath() + " not found", HttpServletResponse.SC_NOT_FOUND);
        }
        if (data.isActionInvocationQuery()) {
            LOGGER.warn("Querying action invocation on GET method.");
            return occiResponse.parseMessage("You cannot use an action with GET method.", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (data.isInterfQuery()) {
            LOGGER.info("Querying the interface on path : " + getPath());
            occiRequest.getInterface(getRequestParameters().get(Constants.CATEGORY_KEY));
            return occiResponse.getHttpResponse();
        }

        if (data.isEntityQuery()) {
            LOGGER.info("Querying an entity on path : " + getPath());
            occiRequest.findEntity();
            return occiResponse.getHttpResponse();
        }

        if (data.isCollectionQuery()) {
            // Query on collections of entities.
            CollectionFilter filter = buildCollectionFilter();
            occiRequest.findEntities(filter);
            return occiResponse.getHttpResponse();
        }

        return resp;
    }



}
