package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.HeaderPojo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class DeleteWorker extends ServletEntry {

    public DeleteWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {
        HttpServletResponse resp = buildInputDatas();

        // TODO : occi methods call.



        return resp;
    }
}
