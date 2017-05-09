package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.AbstractOCCIApiInputRequest;
import org.occiware.mart.server.facade.OCCIApiInputRequest;
import org.occiware.mart.server.facade.OCCIApiResponse;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by cgourdin on 11/04/2017.
 */
public class OCCIServletInputRequest extends AbstractOCCIApiInputRequest implements OCCIApiInputRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletInputRequest.class);

    private HeaderPojo headers;
    private HttpServletRequest request;
    private String requestPath;
    private Map<String, String> requestParameters;
    private final String contentType;



    private boolean onEntityLocation = false;


    /**
     * Define if this is a collection location.
     */
    private boolean onCollectionLocation = false;
    private boolean onBoundedLocation = false;
    private boolean onCategoryLocation = false;
    private boolean onMixinTagLocation = false;
    /**
     * Collection of data content on input. True if this is a collection content (like entities) and false if not (one thing like one entity or one mixin tag data).
     */
    private boolean contentCollection = false;
    /**
     * Define if the path is an interface query /-/.
     */
    private boolean interfQuery;

    /**
     * Define if the query is an action query.
     */
    private boolean actionInvocationQuery;


    public OCCIServletInputRequest(OCCIApiResponse response, String contentType, String username, HttpServletRequest req, HeaderPojo headers, Map<String, String> requestParameters, IRequestParser inputParser) {
        super(username, response, inputParser);
        this.headers = headers;
        this.request = req;

        this.requestPath = req.getPathInfo();
        if (!requestPath.startsWith("/")) {
            requestPath = "/" + requestPath;
        }
        if (!requestPath.endsWith("/")) {
            requestPath = requestPath + "/";
        }
        this.requestParameters = requestParameters;
        this.contentType = contentType;

    }

    /**
     * Build the data objects for usage in PUT, GET etc. when call findEntity etc.
     */
    public void parseInput() throws ParseOCCIException {

        String content;
        // For all media type that have content occi build like json, xml, text plain, yml etc..
        if (request == null) {
            throw new ParseOCCIException("No request to parse.");
        }
        // Parse the path
        parsePath();

        // Parse the content body if any.
        switch (contentType) {
            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                InputStream in = null;
                LOGGER.info("Parsing input uploaded datas...");
                try {
                    in = request.getInputStream();
                    if (in != null) {
                        // throw new ParseOCCIException("The input has no content delivered.");
                        content = Utils.convertInputStreamToString(in);
                        // for Object occiRequest to be fully completed.
                        getInputParser().parseInputToDatas(content);
                    }
                } catch (IOException ex) {
                    throw new ParseOCCIException("The server cant read the content input --> " + ex.getMessage());
                } finally {
                    Utils.closeQuietly(in);
                }
                break;
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                // For all media type that have header definition only, known for now is text/occi.
                if (headers != null && !headers.getHeaderMap().isEmpty()) {
                    // for object occiRequest to be fully completed, the parameter is Map<String, List<String>> encapsulated on MultivaluedMap.)
                    getInputParser().parseInputToDatas(headers);
                }
                break;
            default:
                throw new ParseOCCIException("Cannot parse for " + contentType + " cause: unknown parser");
        }

        if (getContentDatas().size() > 1) {
            this.contentCollection = true;
        }
    }

    /**
     * Parse the path to a data object. a path may have /category/myresource/
     */
    private void parsePath() {

        // Detect if this is an interface request.
        if (requestPath.equals("/.well-known/org/ogf/occi/-/") || requestPath.endsWith("/-/")) {
            interfQuery = true;
            return;
        }

        // Detect if this is an action invocation request
        if (requestParameters != null && requestParameters.get("action") != null) {
            actionInvocationQuery = true;
            if (isEntityLocation(requestPath)) {
                onEntityLocation = true;
            } else {
                // Action on collection : Category (kind, mixin on extension), mixin defined ?
                onCollectionLocation = true;
                if (isCategoryLocation(requestPath)) {
                    onCategoryLocation = true;
                } else if (isMixinTagLocation(requestPath)){
                    onMixinTagLocation = true;
                } else {
                    onBoundedLocation = true; // Collection on a fragment path like /myresources/mycomputes/ ==> my entities below this path.
                }
            }
            return;
        }


        // Detect if this path is on an existing entity path.
        if (isEntityLocation(requestPath)) {
            onEntityLocation = true;
            return;
        }

        // Detect if the path is on a category (mixin, kind) like /myconnector/compute/ or /ipnetwork/ or on known path parent (bounded path).
        onCollectionLocation = true;
        if (isCategoryLocation(requestPath)) {
            onCategoryLocation = true;
        } else if (isMixinTagLocation(requestPath)){
            onMixinTagLocation = true;
        } else {
            onBoundedLocation = true; // Collection on a fragment path like /myresources/mycomputes/ ==> my entities below this path.
        }
    }

    public HeaderPojo getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderPojo headers) {
        this.headers = headers;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }


    public boolean isOnCollectionLocation() {
        return onCollectionLocation;
    }

    public void setOnCollectionLocation(boolean onCollectionLocation) {
        this.onCollectionLocation = onCollectionLocation;
    }

    public boolean isInterfQuery() {
        return interfQuery;
    }

    public void setInterfQuery(boolean interfQuery) {
        this.interfQuery = interfQuery;
    }

    public boolean isActionInvocationQuery() {
        return actionInvocationQuery;
    }

    public void setActionInvocationQuery(boolean actionInvocationQuery) {
        this.actionInvocationQuery = actionInvocationQuery;
    }

    public boolean isOnEntityLocation() {
        return onEntityLocation;
    }

    public boolean isOnBoundedLocation() {
        return onBoundedLocation;
    }

    public boolean isOnCategoryLocation() {
        return onCategoryLocation;
    }

    public List<OCCIRequestData> getContentDatas() {
        return this.getInputParser().getInputDatas();
    }

    public boolean isContentCollection() {
        return contentCollection;
    }

    public boolean isOnMixinTagLocation() {
        return onMixinTagLocation;
    }
}
