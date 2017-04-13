package org.occiware.mart.servlet.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class PostWorker extends ServletEntry {
    public PostWorker(URI serverURI, HttpServletResponse resp, Map<String, String> headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }
}
