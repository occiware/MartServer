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
 *
 * @author cgourdin
 */
public abstract class AbstractEntryPoint implements IEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntryPoint.class);

    @Context
    protected UriInfo uri;
    protected IRequestParser inputParser;
    protected IRequestParser outputParser;
    private String contentType = "text/occi";
    private String acceptType = "text/occi";

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
        inputParser = ParserFactory.build(contentType);
        outputParser = ParserFactory.build(acceptType);
        inputParser.setServerURI(uri.getBaseUri());
        outputParser.setServerURI(uri.getBaseUri());
        try {
            inputParser.parseInputQuery(headers, request);

            // Get the datas.
            List<InputData> datas = inputParser.getInputDatas();
            if (!datas.isEmpty()) {
                String error = "";
                boolean hasError = false;
                for (InputData data : datas) {
                    // Check if attributes are in configuration model for kind, mixins and actions.
                    if (!data.getAttrs().isEmpty()) {
                        // if there are attributes so there is a kind / mixins.
                        // Check on configuration and used extension.

                        String kindId = data.getKind();
                        String action = data.getAction();
                        List<String> mixins = data.getMixins();
                        Kind kindModel = null;
                        Action actionModel = null;
                        List<Mixin> mixinsModel = new LinkedList<>();

                        // Search for the kind if an action is set.
                        if (kindId == null && action != null) {
                            // TODO : Export this to configurationManager with a method : getKindFromAction.
                            List<Kind> kinds = ConfigurationManager.getAllConfigurationKind(ConfigurationManager.DEFAULT_OWNER);
                            List<Action> actions;
                            for (Kind kind : kinds) {
                                actions = kind.getActions();
                                for (Action actionTmp : actions) {
                                    if (action.equals(actionTmp.getScheme() + actionTmp.getTerm())) {
                                        actionModel = actionTmp;
                                        kindModel = kind;
                                        kindId = kind.getScheme() + kind.getTerm();
                                        data.setKind(kindId);
                                        break;
                                    }
                                }
                            }
                        }
                        // Actions based on mixins ?
                        if (kindModel == null && mixins == null && action != null) {
                            // TODO : Export this below to configurationManager with a method : getMixinFromAction.
                            List<Mixin> mixinsTmp = ConfigurationManager.getAllConfigurationMixins(ConfigurationManager.DEFAULT_OWNER);
                            List<Action> actions;
                            List<String> mixinsStr = new ArrayList<>();
                            for (Mixin mixin : mixinsTmp) {
                                actions = mixin.getActions();
                                for (Action actionTmp : actions) {
                                    if (action.equals(actionTmp.getScheme() + actionTmp.getTerm())) {
                                        actionModel = actionTmp;
                                        mixinsModel.add(mixin);
                                        mixinsStr.add(mixin.getScheme() + mixin.getTerm());
                                        // mixins = mixinsStr;
                                        data.setMixins(mixinsStr);
                                    }
                                }
                            }
                        }

                        if (kindModel == null) {
                            kindModel = ConfigurationManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, kindId);
                            if (kindModel == null) {
                                error += "The kind : " + data.getKind() + " doesnt exist on referenced extensions";
                                hasError = true;
                            }
                        }

                        if (mixinsModel.isEmpty()) {

                            try {
                                mixinsModel = Utils.loadMixinFromSchemeTerm(data.getMixins());
                            } catch (ConfigurationException ex) {
                                error += " \n ";
                                error += "Mixin model error cause : " + ex.getMessage();
                                hasError = true;
                            }
                        }

//                        if (!hasError && !Utils.checkIfMixinAppliedToKind(mixinsModel, kindModel)) {
//                            error += " \n ";
//                            error += "Some mixins doesnt apply to kind : " + data.getKind();
//                            hasError = true;
//                        }

                        if (!hasError) {

                            if (!Utils.checkIfAttributesExistOnCategory(data.getAttrs(), kindModel, mixinsModel, actionModel)) {
                                error += " \n ";
                                error += "Some attributes doesnt exist on kind and mixins : " + data.getKind();
                                hasError = true;
                            }

                        }

                    }
                }
                if (hasError) {
                    LOGGER.error(error);
                    throw new BadRequestException("Error while parsing input query, some attributes doesnt exist on used extensions. --< " + error);
                }
            }

        } catch (AttributeParseException | CategoryParseException ex) {
            LOGGER.error(ex.getMessage());
            try {
                response = outputParser.parseResponse("Error while parsing input query", Response.Status.BAD_REQUEST);
            } catch (ResponseParseException e) {
                throw new BadRequestException();
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
            LOGGER.info("Mixin tag get request... ");
            Mixin mixin = ConfigurationManager.getUserMixinFromLocation(path, ConfigurationManager.DEFAULT_OWNER);
            if (mixin == null) {
                throw new ConfigurationException("The mixin location : " + path + " is not defined");
            }
            if (categoryFilter != null) {
                filter.setCategoryFilter(mixin.getScheme() + mixin.getTerm());
            }
        }

        entities = ConfigurationManager.findAllEntities(ConfigurationManager.DEFAULT_OWNER, filter);
        return entities;


    }
}
