package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.Data;
import org.occiware.mart.server.parser.HeaderPojo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

/**
 * Created by cgourdin on 13/04/2017.
 *
 */
public class GetWorker extends ServletEntry {

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





        return resp;
    }



}
