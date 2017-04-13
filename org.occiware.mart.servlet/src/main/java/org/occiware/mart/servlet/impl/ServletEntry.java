package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.servlet.utils.ServletResponseHelper;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cgourdin on 13/04/2017.
 * This class is used with main servlet, when a query is done on the servlet, this parse input entries datas (for all queries).
 */
public abstract class ServletEntry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletEntry.class);

    /**
     * Build a new Servlet input entry for workers.
     * @param serverURI http server uri object like http://localhost:8080
     * @param resp Http servlet response.
     * @param headers headers in a map KEY:String, Value: String.
     * @param req http servlet request object.
     * @param path the relative request path. ex: /mycompute/1/
     */
    public ServletEntry(URI serverURI, HttpServletResponse resp, Map<String, String> headers, HttpServletRequest req, String path) {
        this.serverURI = serverURI;
        this.httpResponse = resp;
        this.headers = headers;
        this.httpRequest = req;
        this.path = path;
    }

    private HttpServletRequest httpRequest;
    private Map<String, String> headers;
    private HttpServletResponse httpResponse;
    private String path;

    private OCCIServletInputParser occiRequest;
    private OCCIServletOutputParser occiResponse;

    private String contentType = Constants.MEDIA_TYPE_TEXT_OCCI;
    private String acceptType = Constants.MEDIA_TYPE_TEXT_OCCI;

    /**
     * If parameters are in inputquery, it must declared here. Key: name of the
     * parameter Value: Value of the parameter (if url ==> decode before set the
     * value).
     */
    private Map<String, String> parameters = new HashMap<>();


    /**
     * Uri of the server.
     */
    private URI serverURI;


    /**
     * Get here the parameters of a request, this can be filters, action
     * parameters, pagination as describe here. Parse the request parameters
     * filtering : http://localhost:9090/myquery?attribute=myattributename or
     * http://localh...?category=mymixintag usage with category or attributes.
     * Pagination :
     * http://localhost:9090/myquery?attribute=myattributename&page=2&number=5
     * where page = current page, number : max number of items to display.
     * Request on collections are possible with http://localhost:9090/compute/
     * with text/uri-list accept type, give the full compute resources created
     * uri. Request on collections if no text/uri-list given, return in response
     * the entities defined in detail like a get on each entity (if text/occi
     * this may return a maximum of 3 entities not more due to limitations size
     * of header area in http).
     *
     */
    public void parseRequestParameters() {
        Map<String, String[]> params = httpRequest.getParameterMap();
        String key;
        String[] vals;
        String val;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                key = entry.getKey();
                vals = entry.getValue();
                if (vals != null && vals.length > 0) {
                    val = vals[0];
                    parameters.put(key, val);
                }
            }
        }

    }

    /**
     *
     * @return
     */
    public Map<String, String> getRequestParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        return parameters;
    }


    /**
     * The server uri. (server part of the full url). like http://localhost:8080/
     * @return
     */
    public URI getServerURI() {
        return serverURI;
    }

    public void setServerURI(final URI serverURI) {
        this.serverURI = serverURI;
    }


    public HttpServletResponse buildInputDatas() {

        String contextRoot = serverURI.getPath();
        LOGGER.debug("Context root : " + contextRoot);
        LOGGER.debug("URI relative path: " + path);



        // Load the parsers.
        // check content-type header.
        contentType = headers.get(Constants.HEADER_CONTENT_TYPE);
        acceptType = headers.get(Constants.HEADER_ACCEPT);

        if (acceptType == null) {
            acceptType = contentType;
        }

        // Default values.
        if (contentType == null || contentType.isEmpty()) {
            contentType = Constants.MEDIA_TYPE_JSON;
        }
        if (acceptType == null || acceptType.isEmpty()) {
            // Default to MEDIA_TYPE_JSON.
            acceptType = Constants.MEDIA_TYPE_JSON;
        }

        // TODO : Manage authentication and pass username to MART engine.
        // validateAuth().

        occiResponse = new OCCIServletOutputParser(acceptType, "anonymous", httpResponse);
        occiRequest = new OCCIServletInputParser(occiResponse, contentType, "anonymous", httpRequest, headers);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        boolean result = ServletUtils.checkClientOCCIVersion(headers);
        if (!result) {
            LOGGER.warn("Version is not compliant, max: OCCI v1.2");
            return occiResponse.parseMessage("The requested version is not implemented", HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

        parseRequestParameters();

        // Parse worker datas (to use with MART engine).
        try {
            // Parse input query to data objects.
            occiRequest.parseInput();

        } catch (ParseOCCIException ex) {
            String msg = "Error while parsing input request: " + ex.getMessage();
            LOGGER.error(msg);

            return occiResponse.parseMessage(msg, HttpServletResponse.SC_BAD_REQUEST);
        }

        // TODO : Path parser ==> refactor this object.



        return httpResponse;
    }



}
