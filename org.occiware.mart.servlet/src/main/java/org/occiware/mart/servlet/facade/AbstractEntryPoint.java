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
package org.occiware.mart.servlet.facade;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.occiware.mart.servlet.impl.OCCIServletInputParser;
import org.occiware.mart.servlet.impl.OCCIServletOutputParser;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe Gourdin
 */
public abstract class AbstractEntryPoint implements IEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntryPoint.class);

    @Context
    protected UriInfo uri;

    protected OCCIRequest occiRequest;
    protected OCCIResponse occiResponse;

    /**
     * Definitive output.
     */
    private Response response;

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
     * @param request
     */
    @Override
    public void parseRequestParameters(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
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

    @Override
    public Map<String, String> getRequestParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        return parameters;
    }


    @Override
    public URI getServerURI() {
        return serverURI;
    }


    @Override
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request) {

        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        LOGGER.debug("Context root : " + contextRoot);
        LOGGER.debug("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = ServletUtils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }

        // Load the parsers.
        // check content-type header.
        contentType = ServletUtils.findContentTypeFromHeader(headers);
        acceptType = ServletUtils.findAcceptTypeFromHeader(headers);
        // Default values.
        if (contentType == null || contentType.isEmpty()) {
            contentType = Constants.MEDIA_TYPE_TEXT_OCCI;
        }
        if (acceptType == null || acceptType.isEmpty()) {
            // Default to MEDIA_TYPE_JSON.
            acceptType = Constants.MEDIA_TYPE_JSON;
        }

        // TODO : Manage authentication therefore.
        occiResponse = new OCCIServletOutputParser(acceptType, "anonymous");
        occiRequest = new OCCIServletInputParser(occiResponse, contentType, "anonymous");

        this.serverURI = uri.getBaseUri());
        parseRequestParameters(request);
        try {
            // Parse input query to data objects.
            occiRequest.parseInput();

        } catch (ParseOCCIException ex) {
            String msg = "Error while parsing input request: " + ex.getMessage();
            LOGGER.error(msg);
            response = occiResponse.parseResponse(msg, Response.Status.BAD_REQUEST);
        }


        return response;
    }


    protected UriInfo getUri() {
        return uri;
    }

    @Override
    public String getAcceptType() {
        return acceptType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }


// TODO : Refactor the following method.


    /**
     * Get collections based on location and Accept = text/uri-list or give entities details for other accept types.
     * Examples of query for filtering and pagination:
     * Filtering (attribute or category) :  http://localhost:9090/myquery?attribute=myattributename or http://localh...?category=mymixintag
     * Pagination : http://localhost:9090/myquery?attribute=myattributename&page=2&number=5 where page = current page, number : max number of items to display.
     * Operator (equal or like) : http://localhost:9090/myquery?attribute=myattributename&page=2&number=5&operator=like&value=MyAttributeValue
     *
     * @param path
     * @return
     * @throws ConfigurationException
     */
    @Override
    public List<Entity> getEntityCollection(final String path) throws ConfigurationException {
        // Get pagination if any (current page number and number max of items, for the last if none defined, used to 20 items per page by default).
        String pageTmp = inputParser.getParameter(Constants.CURRENT_PAGE_KEY);
        String itemsNumber = inputParser.getParameter(Constants.NUMBER_ITEMS_PER_PAGE_KEY);
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
        String operatorTmp = inputParser.getParameter(Constants.OPERATOR_KEY);
        if (operatorTmp == null) {
            operatorTmp = "0";
        }
        int operator = 0;
        try {
            operator = Integer.valueOf(operatorTmp);
        } catch (NumberFormatException ex) {
        }

        List<Entity> entities;
        // Collection on categories. // Like : get on myhost/compute/
        boolean isCollectionOnCategoryPath = ServletUtils.isCollectionOnCategory(path);

        String categoryFilter = inputParser.getParameter("category");
        String attributeFilter = inputParser.getParameter("attribute");
        String attributeValue = inputParser.getParameter("value");


        CollectionFilter filter = new CollectionFilter();
        filter.setOperator(operator);
        filter.setNumberOfItemsPerPage(items);
        filter.setCurrentPage(page);
        filter.setCategoryFilter(categoryFilter);
        filter.setAttributeFilter(attributeFilter);
        filter.setValue(attributeValue);
        if (isCollectionOnCategoryPath && (categoryFilter == null || categoryFilter.isEmpty())) {
            filter.setCategoryFilter(ServletUtils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER));
        } else {
            filter.setFilterOnPath(path);
        }

        // Case of the mixin tag entities request.
        boolean isMixinTagRequest = ServletUtils.isMixinTagRequest(path, ConfigurationManager.DEFAULT_OWNER);
        if (isMixinTagRequest) {
            LOGGER.info("Mixin tag request... ");
            Mixin mixin = ConfigurationManager.getUserMixinFromLocation(path, ConfigurationManager.DEFAULT_OWNER);
            if (mixin == null) {
                throw new ConfigurationException("The mixin location : " + path + " is not defined");
            }
            if (categoryFilter == null) {
                filter.setCategoryFilter(mixin.getTerm());
                filter.setFilterOnPath(null);
            }
        }

        entities = ConfigurationManager.findAllEntities(ConfigurationManager.DEFAULT_OWNER, filter);
        return entities;

    }


}
