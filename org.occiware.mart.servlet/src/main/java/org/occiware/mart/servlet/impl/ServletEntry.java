package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
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
    public ServletEntry(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        this.serverURI = serverURI;
        this.httpResponse = resp;
        this.headers = headers;
        this.httpRequest = req;
        this.path = path;
    }

    private HttpServletRequest httpRequest;
    private HeaderPojo headers;
    private HttpServletResponse httpResponse;
    private String path;

    protected OCCIServletInputParser occiRequest;
    protected OCCIServletOutputParser occiResponse;

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
        contentType = headers.getFirst(Constants.HEADER_CONTENT_TYPE);
        acceptType = headers.getFirst(Constants.HEADER_ACCEPT); // TODO : add support for uri-list combined with other type rendering.

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
        occiRequest = new OCCIServletInputParser(occiResponse, contentType, "anonymous", httpRequest, headers, this.getRequestParameters());

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

        return httpResponse;
    }

    /**
     * Build a collection filter.
     * @return Must never return null.
     */
    public CollectionFilter buildCollectionFilter() {
        String pageTmp = getRequestParameters().get(Constants.CURRENT_PAGE_KEY);
        String itemsNumber = getRequestParameters().get(Constants.NUMBER_ITEMS_PER_PAGE_KEY);
        int items = Constants.DEFAULT_NUMBER_ITEMS_PER_PAGE;
        int page = Constants.DEFAULT_CURRENT_PAGE;
        if (pageTmp != null && !pageTmp.isEmpty()) {
            // Set the value from request only if this is a number.
            try {
                items = Integer.valueOf(itemsNumber);
            } catch (NumberFormatException ex) {
                // Cant parse the number
                LOGGER.error("The parameter \"number\" is not set correctly, please check the parameter, this must be a number.");
                LOGGER.error("Default to " + items);
            }
            try {
                page = Integer.valueOf(pageTmp);
            } catch (NumberFormatException ex) {
                LOGGER.error("The parameter \"page\" is not set correctly, please check the parameter, this must be a number.");
                LOGGER.error("Default to " + page);
            }
        }
        String operatorTmp = getRequestParameters().get(Constants.OPERATOR_KEY);
        if (operatorTmp == null) {
            operatorTmp = "0";
        }
        int operator = 0;
        try {
            operator = Integer.valueOf(operatorTmp);
        } catch (NumberFormatException ex) {
        }
        String categoryFilter = getRequestParameters().get(Constants.CATEGORY_KEY);
        String attributeFilter = getRequestParameters().get(Constants.ATTRIBUTE_KEY);
        String attributeValue = getRequestParameters().get(Constants.VALUE_KEY);
        CollectionFilter filter = new CollectionFilter();
        filter.setOperator(operator);
        filter.setNumberOfItemsPerPage(items);
        filter.setCurrentPage(page);
        filter.setCategoryFilter(categoryFilter);
        filter.setAttributeFilter(attributeFilter);
        filter.setValue(attributeValue);

        // Collection on categories. // Like : get on myhost/compute/
        boolean isCollectionOnCategoryPath = false;

        // Remove starting slash and ending slash from path.
        String pathTmp = path;
        if (pathTmp.startsWith("/")) {
            pathTmp = pathTmp.substring(1);
        }
        if (pathTmp.endsWith("/")) {
            pathTmp = pathTmp.substring(0, pathTmp.length() - 1);
        }
        // Split values and get last word.
        String categoryTermPath;
        String categorySchemeTermFilter = null;
        String[] words = pathTmp.split("/");
        if (words.length >= 1) {
            categoryTermPath = words[words.length - 1];
            if (categoryTermPath != null && !categoryTermPath.trim().isEmpty()) {
                // Check if this is really a category term.
                categorySchemeTermFilter = occiRequest.getCategorySchemeTerm(categoryTermPath);
                isCollectionOnCategoryPath = categorySchemeTermFilter != null;
            }
        }
        if (isCollectionOnCategoryPath && (categoryFilter == null || categoryFilter.isEmpty())) {
            filter.setCategoryFilter(categorySchemeTermFilter);
        } else {
            filter.setFilterOnPath(path);
        }

        // Case of the mixin tag entities request.
        String mixinTagSchemeTerm = occiRequest.getMixinTagSchemeTermFromLocation(path);
        if (mixinTagSchemeTerm != null) {
            if (categoryFilter == null) {
                filter.setCategoryFilter(mixinTagSchemeTerm);
                filter.setFilterOnPath(null);
            }
        }

        return filter;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public void setAcceptType(String acceptType) {
        this.acceptType = acceptType;
    }
}
