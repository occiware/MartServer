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
package org.occiware.mart.server.servlet.facade;

import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.impl.parser.ParserFactory;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.occiware.mart.server.servlet.utils.CollectionFilter;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Christophe Gourdin
 */
public abstract class AbstractEntryPoint implements IEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntryPoint.class);

    @Context
    protected UriInfo uri;
    protected IRequestParser inputParser;
    protected IRequestParser outputParser;
    private String contentType = Constants.MEDIA_TYPE_TEXT_OCCI;
    private String acceptType = Constants.MEDIA_TYPE_TEXT_OCCI;

    @Override
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request) {

        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        LOGGER.info("Context root : " + contextRoot);
        LOGGER.info("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }

        // Load the parsers.
        // check content-type header.
        contentType = Utils.findContentTypeFromHeader(headers);
        acceptType = Utils.findAcceptTypeFromHeader(headers);
        // Default values.
        if (contentType == null || contentType.isEmpty()) {
            contentType = Constants.MEDIA_TYPE_TEXT_OCCI;
        }
        if (acceptType == null || acceptType.isEmpty()) {
            // Default to MEDIA_TYPE_TEXT_OCCI.
            acceptType = Constants.MEDIA_TYPE_TEXT_OCCI;
        }

        inputParser = ParserFactory.build(contentType);
        outputParser = ParserFactory.build(acceptType);
        inputParser.setServerURI(uri.getBaseUri());
        outputParser.setServerURI(uri.getBaseUri());
        try {
            inputParser.parseInputQuery(headers, request);

            List<InputData> datas = inputParser.getInputDatas();
            String kind;
            List<String> mixins;
            List<String> messages = new ArrayList<>();
            for (InputData data : datas) {
                kind = data.getKind();
                mixins = data.getMixins();

                // Check if kind exist on extension.
                if (kind != null
                        && ConfigurationManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, kind) == null) {
                    messages.add (" Kind : " + kind + " doesnt exist on used extensions.");
                }



            }
            if (!messages.isEmpty()) {
                String msgRender = "";
                for (String message : messages) {
                    LOGGER.error(message);
                    msgRender += message + " ; ";
                }
                try {
                    response = outputParser.parseResponse(msgRender, Response.Status.BAD_REQUEST);
                } catch (ResponseParseException e) {
                    throw new BadRequestException(e);
                }

            }


        } catch (AttributeParseException | CategoryParseException ex) {
            String message = ex.getClass().getName() + " --> " + ex.getMessage();
            if (ex.getMessage() == null) {
                message = "Error while parsing input query, exception: " + ex.getClass().getName();
                LOGGER.error(message);
            }
            LOGGER.error(message);

            try {
                response = outputParser.parseResponse(message, Response.Status.BAD_REQUEST);
            } catch (ResponseParseException e) {
                throw new BadRequestException(e);
            }
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
        boolean isCollectionOnCategoryPath = Utils.isCollectionOnCategory(path);

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
            filter.setCategoryFilter(Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER));
        } else {
            filter.setFilterOnPath(path);
        }

        // Case of the mixin tag entities request.
        boolean isMixinTagRequest = Utils.isMixinTagRequest(path, ConfigurationManager.DEFAULT_OWNER);
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
