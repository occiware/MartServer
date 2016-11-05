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
import org.occiware.clouddesigner.occi.Configuration;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractDeleteQuery;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
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
            List<InputData> datas = inputParser.getInputDatas();
            
            boolean isMixinTagRequest = Utils.isMixinTagRequest(path, ConfigurationManager.DEFAULT_OWNER);
            if (!isMixinTagRequest) {
                // Check if there is inputdata referencing mixintag.
                for (InputData data : datas) {
                    if (data.getMixinTag() != null || !data.getMixins().isEmpty()) {
                        // Has mixin tag we continue so.
                        isMixinTagRequest = true;
                        break;
                    }
                }
            }

            if (!isMixinTagRequest) {
                try {
                    response = outputParser.parseResponse("You cannot use interface query on DELETE method.", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }
        }

        // For each data block received on input (only one for text/occi or text/plain, but could be multiple for application/json.
        List<InputData> datas = inputParser.getInputDatas();
        for (InputData data : datas) {
            String actionId = data.getAction();
            if (actionId != null) {
                try {
                    response = outputParser.parseResponse("You cannot use an action with DELETE method.", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }
            
            // Check if this is mixins remove association and remove from configuration if not referenced on extension (mixinusertag).
            if (!data.getMixins().isEmpty() || data.getMixinTag() != null) {
                
                if (data.getMixinTag() != null) {
                    return deleteMixin(data.getMixinTag(), ConfigurationManager.DEFAULT_OWNER, true);
                }
                if (!data.getMixins().isEmpty()) {
                    // Remove association if there is mixins or remove it if mixins tag defined without locations.
                    for (String mixinId : data.getMixins()) {
                        response = Response.fromResponse(deleteMixin(mixinId, ConfigurationManager.DEFAULT_OWNER, false)).build();
                    }
                    return response;
                }
                
                
            }
            
            

            // Delete an entity.
            Map<String, String> attrs = data.getAttrs();
            boolean isEntityRequest = Utils.isEntityRequest(path, attrs);
            if (isEntityRequest) {
                response = deleteEntity(path, attrs);
                return response;
            }

            response = deleteEntityCollection(path);

            if (response == null) {
                try {
                    response = outputParser.parseResponse("Unknown DELETE query type.", Response.Status.BAD_REQUEST);
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }
        } // End for each data.

        return response;
    }

    @Override
    public Response deleteMixin(String mixinId, String owner, boolean isMixinTag) {
        Response response = null;
        
        ConfigurationManager.removeOrDissociateFromConfiguration(owner, mixinId);
        boolean hasError = false;
        
        // Check if mixinId is found in mixin tag area.
        Mixin mixin = ConfigurationManager.findUserMixinOnConfiguration(mixinId, owner);
        if (mixin != null) {
            // This is a mixin tag, defined in query without location !.
            isMixinTag = true;
        }
        if (mixin == null) {
            try {
                response = outputParser.parseResponse("ok", Response.Status.NO_CONTENT);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }
        
        // if mixin tag, remove the definition from Configuration object.
        if (isMixinTag) {
            try {
                ConfigurationManager.removeUserMixinFromConfiguration(mixinId, ConfigurationManager.DEFAULT_OWNER);
            } catch (ConfigurationException ex) {
                LOGGER.error("Error while removing a mixin tag from configuration object: " + mixinId + " --> " + ex.getMessage());
                hasError = true;
                try {
                    response = outputParser.parseResponse("error while removing a user mixin tag : " + mixinId + " --> " + ex.getMessage());
                } catch (ResponseParseException e) {
                    throw new InternalServerErrorException(e);
                }
            }
        }
        if (!hasError && response == null) {
            try {
                response = outputParser.parseResponse("ok");
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException();
            }
        } 
        return response;
    }

    @Override
    public Response deleteEntityCollection(String path) {
        Response response = null;
        // Delete a collection of entities.
        List<Entity> entities;
        // Collection on categories. // Like : get on myhost/compute/
        boolean isCollectionOnCategoryPath = Utils.isCollectionOnCategory(path);
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
            boolean isMixinTagRequest = Utils.isMixinTagRequest(path, ConfigurationManager.DEFAULT_OWNER);
            if (isMixinTagRequest) {
                LOGGER.info("Mixin tag delete request... ");
                Mixin mixin = ConfigurationManager.getUserMixinFromLocation(path, ConfigurationManager.DEFAULT_OWNER);
                if (mixin == null) {
                    try {
                        response = outputParser.parseResponse("The mixin location : " + path + " is not defined", Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException("The mixin location : " + path + " is not defined");
                    }
                }
                //entities = ConfigurationManager.findAllEntitiesForMixin(ConfigurationManager.DEFAULT_OWNER, mixin.getScheme()+mixin.getTerm());
                // Add mixin filters.
                filters.clear();
                CollectionFilter filter = new CollectionFilter();
                filter.setCategoryFilter(mixin.getScheme() + mixin.getTerm());
                filter.setOperator(operator);
                filters.add(filter);
            }
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
    public Response deleteEntity(String path, Map<String, String> attrs) {

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
