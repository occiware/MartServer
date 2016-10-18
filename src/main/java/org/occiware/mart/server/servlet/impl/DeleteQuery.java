/**
 * Copyright (c) 2015-2017 Inria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.mart.server.servlet.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractDeleteQuery;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.CollectionFilter;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delete entity (or entities), delete mixin tag, remove mixin association.
 *
 * @author cgourdin
 */
@Path("/")
public class DeleteQuery extends AbstractDeleteQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteQuery.class);

    @Path("{path:.*}")
    @DELETE
    @Override
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        LOGGER.info("--> Call DELETE method input query for relative path mode --> " + path);
        // Check header, load parsers, and check occi version.
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            // There was a badrequest, the headers are maybe malformed..
            return response;
        }

        // Check if the query is not on interface query, this path is used only on GET method.
        if (path.equals("-/") || path.equals(".well-known/org/ogf/occi/-/") || path.endsWith("/-/")) {
            try {
                response = outputParser.parseResponse("You cannot use interface query on DELETE method.", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }
        String actionId = inputParser.getAction();
        if (actionId != null) {
            try {
                response = outputParser.parseResponse("You cannot use an action with DELETE method.", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        // Delete an entity.
        Map<String, String> attrs = inputParser.getOcciAttributes();
        boolean isEntityRequest = Utils.isEntityRequest(path, attrs);
        if (isEntityRequest) {
            response = deleteEntity(path);
            return response;
        }

        // Remove mixin association.
        boolean isMixinRemoveRequest = Utils.isMixinTagRequest(path, attrs, inputParser.getMixins(), inputParser.getMixinTagLocation());
        if (isMixinRemoveRequest) {
            LOGGER.info("Mixin remove request.");

//            for (String mixin : inputParser.getMixins()) {
//                LOGGER.info("Remove mixin : " + mixin);
//                ConfigurationManager.removeOrDissociate(mixin);
//            }
//            try {
//                LOGGER.info("Mixin removed: " + inputParser.getMixins());
//                response = outputParser.parseResponse("Mixin removed from path : " + path);
//            } catch (ResponseParseException ex) {
//                throw new InternalServerErrorException(ex);
//            }
            try {
                response = outputParser.parseResponse("Not implemented, this will coming soon", Response.Status.NOT_IMPLEMENTED);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        response = deleteEntityCollection(path);

        if (response == null) {
            try {
                response = outputParser.parseResponse("Unknown DELETE query type.", Response.Status.BAD_REQUEST);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        return response;
    }

    @Override
    public Response deleteMixin(String mixinKind, String entityId, HttpHeaders headers, HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response deleteEntityCollection(String path) {
        Response response = null;
        Map<String, String> attrs = inputParser.getOcciAttributes();
        // Delete a collection of entities.
        List<Entity> entities;
        // Collection on categories. // Like : get on myhost/compute/
        boolean isCollectionOnCategoryPath = Utils.isCollectionOnCategory(path, attrs);
        // Collections part.
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
        // Get the filter parameters and build a CollectionFilter object for each filter parameters defined.
        List<CollectionFilter> filters = new LinkedList<>();
        // Category filter check.
        String paramTmp = inputParser.getParameter("category");
        if (paramTmp != null && !paramTmp.isEmpty()) {
            CollectionFilter filter = new CollectionFilter();
            filter.setCategoryFilter(paramTmp);
            filter.setOperator(operator);
            filters.add(filter);
        }
        // Attribute filter check.
        paramTmp = inputParser.getParameter("attribute");
        if (paramTmp != null && !paramTmp.isEmpty()) {
            CollectionFilter filter = new CollectionFilter();
            filter.setAttributeFilter(paramTmp);
            filter.setOperator(operator);
            filters.add(filter);
        }

        if (isCollectionOnCategoryPath) {
            // Check category uri.
            String categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);
            CollectionFilter filter = new CollectionFilter();
            filter.setOperator(operator);
            filter.setCategoryFilter(categoryId);
            filters.add(filter);
        } else {
            // Unknown collection type.
            CollectionFilter filter = new CollectionFilter();
            filter.setOperator(operator);
            filter.setFilterOnPath(path);
            filters.add(filter);
        }

        try {
            entities = ConfigurationManager.findAllEntities(ConfigurationManager.DEFAULT_OWNER, page, items, filters);

            if (getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
                List<String> locations = new LinkedList<>();
                String location;
                for (Entity entityTmp : entities) {
                    location = ConfigurationManager.getLocation(entityTmp);
                    locations.add(location);
                }
                if (locations.isEmpty()) {
                    response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                    return response;
                }
                response = outputParser.parseResponse(locations);
            } else {
                if (entities.isEmpty()) {
                    response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                    return response;
                }
                List<Entity> entitiesInf = new LinkedList<>();

                for (Entity entityInf : entities) {
                    entityInf.occiDelete();
                    entitiesInf.add(entityInf);
                    ConfigurationManager.removeOrDissociateFromConfiguration(ConfigurationManager.DEFAULT_OWNER, entityInf.getId());
                }
                response = outputParser.parseResponse(entitiesInf);
            }

        } catch (ResponseParseException ex) {
            // Must never happen if input query is ok.
            throw new InternalServerErrorException(ex);
        }

        return response;
    }

    @Override
    public Response deleteEntity(String path) {
        Map<String, String> attrs = inputParser.getOcciAttributes();
        boolean isEntityUUIDProvided = Utils.isEntityUUIDProvided(path, attrs);
        Entity entity;
        String entityId;
        Response response;
        if (!isEntityUUIDProvided) {
            entity = ConfigurationManager.getEntityFromPath(path);

        } else {
            entityId = Utils.getUUIDFromPath(path, attrs);
            entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityId);
        }

        if (entity == null) {
            try {
                response = outputParser.parseResponse("Entity not found on path : " + path, Response.Status.NOT_FOUND);
                return response;
            } catch (ResponseParseException ex) {
                // Must never happen if input query is ok.
                throw new InternalServerErrorException(ex);
            }
        }

        entityId = entity.getId();
        entity.occiDelete();
        ConfigurationManager.removeOrDissociateFromConfiguration(ConfigurationManager.DEFAULT_OWNER, entityId);
        try {
            LOGGER.info("Remove entity: " + entity.getTitle() + " --> " + entityId);
            response = outputParser.parseResponse("Entity removed from path : " + path);
            return response;
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

}
