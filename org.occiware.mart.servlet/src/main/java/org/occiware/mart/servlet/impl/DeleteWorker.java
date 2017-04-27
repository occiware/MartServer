package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class DeleteWorker extends ServletEntry {

    public DeleteWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {
        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        // if there is content so check it.
        occiRequest.validateInputDataRequest();

        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isInterfQuery()) {
            return occiResponse.parseMessage("you cannot use interface query on DELETE method", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (occiRequest.isActionInvocationQuery()) {
            return occiResponse.parseMessage("You cannot use an action with DELETE method.", HttpServletResponse.SC_BAD_REQUEST);
        }

        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        // TODO...

        // Against data, we call the removeFromModel method.
        // occiRequest.removeFromModel();
        resp = occiResponse.getHttpResponse();
        return resp;
    }
}
