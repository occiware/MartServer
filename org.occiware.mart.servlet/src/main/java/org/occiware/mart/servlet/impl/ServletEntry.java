/**
 * Copyright (c) 2015-2017 Inria
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.parser.DefaultParser;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.ParserFactory;
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
import java.util.Optional;

/**
 * Created by cgourdin on 13/04/2017.
 * This class is used with main servlet, when a query is done on the servlet, this parse input entries datas (for all queries).
 */
public abstract class ServletEntry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletEntry.class);
    protected OCCIServletInputRequest occiRequest;
    protected OCCIServletOutputResponse occiResponse;
    private HttpServletRequest httpRequest;
    private HeaderPojo headers;
    private HttpServletResponse httpResponse;
    private String path;
    private String contentType = Constants.MEDIA_TYPE_JSON;
    private String acceptType = Constants.MEDIA_TYPE_JSON;
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
     * Build a new Servlet input entry for workers.
     *
     * @param serverURI http server uri object like http://localhost:8080
     * @param resp      Http servlet response.
     * @param headers   headers in a map KEY:String, Value: String.
     * @param req       http servlet request object.
     * @param path      the relative request path. ex: /mycompute/1/
     */
    public ServletEntry(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        this.serverURI = serverURI;
        this.httpResponse = resp;
        this.headers = headers;
        this.httpRequest = req;
        this.path = path;
    }

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
     *
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
        if (acceptType == null || acceptType.isEmpty() || acceptType.equals("*/*")) {
            // Default to MEDIA_TYPE_JSON.
            acceptType = Constants.MEDIA_TYPE_JSON;
        }

        // TODO : Manage authentication and pass username to MART engine.
        String username = "anonymous";
        // validateAuth().

        // Build inputparser and output parser.
        IRequestParser inputParser = ParserFactory.build(contentType, username);
        IRequestParser outputParser = ParserFactory.build(acceptType, username);

        // Create occiRequest objects.
        occiResponse = new OCCIServletOutputResponse(acceptType, username, httpResponse, outputParser);
        occiRequest = new OCCIServletInputRequest(occiResponse, contentType, username, httpRequest, headers, this.getRequestParameters(), inputParser);

        if (inputParser instanceof DefaultParser) {
            LOGGER.warn("No parser for content type : " + contentType);
            return occiResponse.parseMessage("content type : " + contentType + " not implemented.", HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
        if (outputParser instanceof DefaultParser) {
            LOGGER.warn("No parser for accept type : " + acceptType);
            return occiResponse.parseMessage("accept type : " + contentType + " not implemented.", HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        boolean result = ServletUtils.checkClientOCCIVersion(headers);
        if (!result) {
            LOGGER.warn("Version is not compliant, max: OCCI v1.2");
            return occiResponse.parseMessage("The requested version is not implemented", HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

        parseRequestParameters();

        // Parse worker datas content.
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
     *
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


        String requestPath = occiRequest.getRequestPath();

        // Collection on partial or complete entity location.
        boolean onEntitiesLocation = occiRequest.isOnBoundedLocation();
        boolean onEntityLocation = occiRequest.isOnEntityLocation();
        if (onEntitiesLocation || onEntityLocation) {
            filter.setFilterOnEntitiesPath(path);
            return filter;
        }

        // Collection on mixin tag location.
        boolean onMixinTagLocation = occiRequest.isOnMixinTagLocation();
        if (onMixinTagLocation) {
            // Case of the mixin tag entities request.
            Optional<String> optMixinTag = occiRequest.getMixinTagSchemeTermFromLocation(path);
            if (optMixinTag.isPresent()) {
                // Check if we need a subfilter.
                if (categoryFilter != null) {
                    filter.setSubCategoryFilter(filter.getCategoryFilter());
                    filter.setCategoryFilter(optMixinTag.get());
                    filter.setFilterOnEntitiesPath(null);
                } else {
                    filter.setCategoryFilter(optMixinTag.get());
                    filter.setSubCategoryFilter(null);
                    filter.setFilterOnEntitiesPath(null);
                }
                return filter;
            } else {
                LOGGER.warn("Unknown mixin tag location : " + requestPath);
            }
        }

        // Determine if the collection is on a category path (kind or mixins term location).
        boolean onExtensionCategoryLocation = occiRequest.isOnCategoryLocation();
        if (onExtensionCategoryLocation) {
            String categorySchemeTermFilter = null;
            Optional<String> optCat = occiRequest.getCategorySchemeTermFromLocation(requestPath);

            if (optCat.isPresent()) {
                // If a category filter is already there via request parameters we use a subfilter.
                if (categoryFilter != null) {
                    filter.setSubCategoryFilter(filter.getCategoryFilter());
                    filter.setCategoryFilter(optCat.get());
                    filter.setFilterOnEntitiesPath(null);
                } else {
                    filter.setCategoryFilter(optCat.get());
                    filter.setSubCategoryFilter(null);
                    filter.setFilterOnEntitiesPath(null);
                }
                return filter;
            } else {
                LOGGER.warn("Unknown Category kind/mixin location : " + requestPath);
            }
        }
        // Default to entities location.
        filter.setFilterOnEntitiesPath(requestPath);

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
