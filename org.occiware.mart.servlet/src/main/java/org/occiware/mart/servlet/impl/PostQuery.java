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

import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Category;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.occiware.mart.servlet.exception.ResponseParseException;
import org.occiware.mart.servlet.facade.AbstractPostQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * execute actions, update attributes on entities, update mixin tag associations
 *
 * @author Christophe Gourdin
 */
@Path("/")
public class PostQuery extends AbstractPostQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostQuery.class);

    @Path("{path:.*}")
    @POST
    @Override
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        LOGGER.info("--> Call POST method input query for relative path mode --> " + path);
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            return response;
        }
        if (getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            try {
                response = outputParser.parseResponse("You cannot use " + Constants.MEDIA_TYPE_TEXT_URI_LIST + " on POST method", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        List<ContentData> contentDatas = inputParser.getDatas();

        for (ContentData contentData : contentDatas) {

            PathParser pathParser = new PathParser(contentData, path, inputParser.getRequestPameters());

            if (pathParser.isInterfQuery()) {
                try {
                    response = outputParser.parseResponse("you cannot use interface query on POST method", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            String location = pathParser.getLocation();
            if (location == null || location.trim().isEmpty()) {
                location = pathParser.getPath();
            }

            String actionId = contentData.getAction();
            Map<String, String> attrs = contentData.getAttrs();
            List<Entity> entities;
            Entity entity;
            String categoryId = pathParser.getCategoryId();

            if (pathParser.isActionInvocationQuery()) {
                String actionTerm = inputParser.getParameter("action");
                // Check if action exist on extensions and if the parameter ?action=myaction is set.
                if (actionTerm == null) {
                    try {
                        response = outputParser.parseResponse("you forgot the parameter ?action=action term", Response.Status.BAD_REQUEST);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }


                // Is action is scheme + term or only a term parameter.
                if (EntityManager.getExtensionForAction(ConfigurationManager.DEFAULT_OWNER, actionId) == null) {
                    // This is maybe an action term only.
                    actionId = actionTerm;
                }

                // action invocation may be invoke on an entity or a collection.
                if (pathParser.isEntityQuery()) {
                    String entityId = contentData.getEntityUUID();
                    if (entityId == null) {
                        entityId = EntityManager.getUUIDFromPath(location, attrs);
                    }

                    entity = EntityManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityId);

                    if (entity == null || entityId == null) {
                        try {
                            response = outputParser.parseResponse("The entity : " + entityId + " doesn't exist on path : " + location, Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }

                    // Check if location path correspond to entity registered path.
                    String locationTmp;
                    try {
                        locationTmp = EntityManager.getLocation(entityId);
                        locationTmp = locationTmp.replace(entityId, "");
                    } catch (ConfigurationException ex) {
                        try {
                            response = outputParser.parseResponse(ex.getMessage(), Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException e) {
                            throw new InternalServerErrorException();
                        }
                    }
                    String locationCompare = location.replace(entityId, "");
                    // Check if location is a category location and not an entity location.
                    if (!locationCompare.equals(locationTmp)) {

                        Category cat = KindManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                        if (cat == null) {
                            cat = MixinManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                        }
                        if (cat == null) {
                            // Find user mixin tag.
                            cat = MixinManager.getUserMixinFromLocation(locationTmp, ConfigurationManager.DEFAULT_OWNER);
                        }
                        if (cat == null) {
                            try {
                                response = outputParser.parseResponse("The category : " + categoryId + " doesn't exist on your configuration/extension", Response.Status.BAD_REQUEST);
                                return response;
                            } catch (ResponseParseException ex) {
                                throw new InternalServerErrorException(ex);
                            }
                        }
                    }
                    response = executeAction(actionId, entity, contentData);
                    continue;
                }

                // path like /compute/
                if (pathParser.isCollectionOnCategory()) {
                    LOGGER.info("Collection --> Executing action: " + actionId + " on Category: " + categoryId);
                    entities = EntityManager.findAllEntitiesForCategory(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    response = executeActionsOnEntities(actionId, entities, contentData);
                    continue;
                }

                // path like /mycustompath/myentities/
                if (pathParser.isCollectionCustomPath()) {
                    LOGGER.info("Collection --> Executing action: " + actionId + " on inbound path: " + location);
                    entities = EntityManager.findAllEntitiesForCategory(ConfigurationManager.DEFAULT_OWNER, actionId);
                    // Remove entities that have not their location equals to path.
                    Iterator<Entity> it = entities.iterator();
                    while (it.hasNext()) {
                        Entity entityTmp = it.next();
                        String entityLocation;
                        try {
                            entityLocation = EntityManager.getLocation(entityTmp.getId());

                        } catch (ConfigurationException ex) {
                            try {
                                response = outputParser.parseResponse(ex.getMessage(), Response.Status.NOT_FOUND);
                                return response;
                            } catch (ResponseParseException e) {
                                throw new InternalServerErrorException();
                            }
                        }

                        String locationTmp = Utils.getPathWithoutPrefixSuffixSlash(entityLocation);
                        if (!location.equals(locationTmp)) {
                            it.remove();
                        }
                    }
                    if (entities.isEmpty()) {
                        LOGGER.info("No entities found to execute the action.");
                        try {
                            response = outputParser.parseResponse("No entities found to execute the action.", Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }

                    }
                    response = executeActionsOnEntities(actionId, entities, contentData);
                    continue;
                }
            } // end if action query part.


            // Update mixins, tags, attributes etc. on an entity.
            if (pathParser.isEntityQuery()) {
                String entityId = contentData.getEntityUUID();
                if (entityId == null) {
                    entityId = EntityManager.getUUIDFromPath(location, attrs);
                }
                if (entityId == null) {
                    if (contentData.getKind() != null) {
                        // Search if entities below on the given location/path.
                        List<String> entitiesUUIDs = EntityManager.getEntityUUIDsFromPath(location);
                        if (entitiesUUIDs.size() == 1) {
                            entityId = entitiesUUIDs.get(0);
                        }
                    }

                }
                if (entityId == null) {
                    LOGGER.warn("Cant retrieve entity for path : " + location);
                    try {
                        response = outputParser.parseResponse("Cant retrieve entity for path : " + location, Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }


                // Check if location path correspond to entity registered path.
                String locationTmp;
                try {
                    locationTmp = EntityManager.getLocation(entityId);
                    locationTmp = locationTmp.replace(entityId, "");
                    locationTmp = Utils.getPathWithoutPrefixSuffixSlash(locationTmp);
                } catch (ConfigurationException ex) {
                    try {
                        response = outputParser.parseResponse(ex.getMessage(), Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException e) {
                        throw new InternalServerErrorException();
                    }
                }

                String locationCompare = location.replace(entityId, "");
                locationCompare = Utils.getPathWithoutPrefixSuffixSlash(locationCompare);

                // Check if location is a category location and not an entity location.
                if (!locationCompare.equals(locationTmp)) {
                    Category cat = KindManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    if (cat == null) {
                        cat = MixinManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    }
                    if (cat == null) {
                        cat = MixinManager.getUserMixinFromLocation(locationTmp, ConfigurationManager.DEFAULT_OWNER);
                    }
                    if (cat == null) {
                        try {
                            response = outputParser.parseResponse("The category : " + categoryId + " doesn't exist on your configuration/extension", Response.Status.BAD_REQUEST);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }
                }

                if ((attrs != null && !attrs.isEmpty()) || !contentData.getMixins().isEmpty()) {
                    entity = EntityManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityId);
                    if (entity == null) {
                        try {
                            response = outputParser.parseResponse("The entity : " + entityId + " doest exit on path : " + path, Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }

                    response = updateEntity(path, entity, contentData);

                } else {
                    try {
                        response = outputParser.parseResponse("No attributes found and no mixins to associate", Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }
                continue;
            }

            if (pathParser.isCollectionQuery()) {
                response = executeUpdateOnCollection(contentData, location, pathParser);
                continue;
            }

            // Request is undefined.
            try {
                response = outputParser.parseResponse("Request is invalid, please check your query", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }

        }

        if (contentDatas.isEmpty()) {
            try {
                response = outputParser.parseResponse("Request is invalid, no contentDatas set, nor resources, please check your query", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        return response;
    }

    /**
     * Update attribute on entity.
     *
     * @param path
     * @param entity
     * @param contentData   ContentData object, must be never null.
     * @return
     */
    @Override
    public Response updateEntity(String path, Entity entity, ContentData contentData) {
        Response response;

        if (entity == null) {
            try {
                response = outputParser.parseResponse("No entity found for update.", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                // Must never happen if input query is ok.
                throw new InternalServerErrorException(ex);
            }
        }

        List<String> mixins = contentData.getMixins();
        if (mixins != null && !mixins.isEmpty()) {

            try {
                MixinManager.addMixinsToEntity(entity, mixins, ConfigurationManager.DEFAULT_OWNER, false);
            } catch (ConfigurationException ex) {
                // Already logged in sub method.
                try {
                    return outputParser.parseResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                } catch (ResponseParseException e) {
                    throw new InternalServerErrorException(e);
                }
            }
        }

        Map<String, String> attrs = contentData.getAttrs();

        // update attributes .
        entity = EntityManager.updateAttributesToEntity(entity, attrs);
        EntityManager.updateVersion(ConfigurationManager.DEFAULT_OWNER, path + entity.getId());
        entity.occiUpdate();
        // entity.occiRetrieve();
        // TODO : to see if this case is important to retrieve from provider the entity before returning it in response object.
        // TODO : Another solution is to set "ok" to response and force user to get request for entity attributes updated.

        try {
            response = outputParser.parseResponse("ok");
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }

        return response;
    }

    /**
     * @param path
     * @param entities
     * @param contentData
     * @return
     */
    @Override
    public Response updateEntityCollection(String path, List<Entity> entities, ContentData contentData) {
        Response response = null;
        Response.ResponseBuilder responseBuilder = null;
        for (Entity entity : entities) {
            responseBuilder = Response.fromResponse(updateEntity(path, entity, contentData));
        }
        if (responseBuilder != null) {
            response = responseBuilder.build();
        }
        if (entities.isEmpty()) {
            try {
                response = outputParser.parseResponse("No entity collection found for update", Response.Status.NOT_FOUND);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        return response;
    }

    /**
     * Update mixin tag association.
     *
     * @param mixinTag
     * @param relativeLocationApply
     * @return
     */
    @Override
    public Response updateMixinTagAssociation(String mixinTag, String relativeLocationApply) {


        Response response;
        List<String> mixins = new ArrayList<>();
        mixins.add(mixinTag);

        String uuid = EntityManager.getUUIDFromPath(relativeLocationApply, new HashMap<>());
        if (uuid == null) {
            try {
                response = outputParser.parseResponse(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore, exception: " + ex.getMessage());
            }
        }
        Entity entity = EntityManager.findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
        if (entity == null) {
            try {
                response = outputParser.parseResponse(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore, exception: " + ex.getMessage());
            }
        }

        try {
            MixinManager.addMixinsToEntity(entity, mixins, ConfigurationManager.DEFAULT_OWNER, false);
        } catch (ConfigurationException ex) {
            try {
                response = outputParser.parseResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {
            response = outputParser.parseResponse("ok");
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }
        return response;
    }

    /**
     * Execute an action on entity.
     *
     * @param actionId
     * @param entity
     * @param contentData
     * @return
     */
    @Override
    public Response executeAction(String actionId, Entity entity, ContentData contentData) {
        Response response;

        if (entity == null) {
            try {
                response = outputParser.parseResponse("No entity found to execute the action : " + actionId, Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                // Must never happen if input query is ok.
                throw new InternalServerErrorException(ex);
            }
        }

        String[] actionParameters = null;

        if (contentData != null && contentData.getAttrs() != null) {
            actionParameters = Utils.getActionParametersArray(contentData.getAttrs());
        }

        Action actionKind = null;

        try {
            actionKind = EntityManager.getActionFromEntityWithActionId(entity, actionId);
        } catch (ConfigurationException ex) {
            LOGGER.warn(ex.getMessage());
        }

        if (actionKind == null) {

            // Search for this action with the term only on the entity Kind and Mixins.
            try {
                actionKind = EntityManager.getActionFromEntityWithActionTerm(entity, actionId);
            } catch (ConfigurationException ex) {
                LOGGER.error(ex.getMessage());
                try {
                    response = outputParser.parseResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException e) {
                    throw new InternalServerErrorException(e);
                }
            }
        }

        // TODO : Optimize here the try catch element.
        try {
            if (actionParameters == null) {
                OcciHelper.executeAction(entity, actionKind.getTerm());
            } else {
                OcciHelper.executeAction(entity, actionKind.getTerm(), actionParameters);
            }
        } catch (InvocationTargetException ex) {

            String message = "The entity " + entity.getTitle() + "  action : " + actionId + " has throw an exception : " + ex.getCause().getClass().getName();
            if (ex.getMessage() != null) {
                message += " , Message: " + ex.getMessage();
            } else {
                message += ", Message: probably missing connector implementation.";
            }
            LOGGER.error("Action failed to execute : " + message);
            try {
                response = outputParser.parseResponse("Action failed : " + message, Response.Status.INTERNAL_SERVER_ERROR);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        } catch (UnsupportedOperationException ex) {
            String message = "The entity " + entity.getTitle() + "  action : " + actionId + " has throw an exception : " + ex.getClass().getName();
            if (ex.getMessage() != null) {
                message += "\n Message: " + ex.getMessage();
            } else {
                message += "\n Message: probably missing connector implementation.";
            }
            try {
                response = outputParser.parseResponse("Action failed : " + message, Response.Status.INTERNAL_SERVER_ERROR);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {
            response = outputParser.parseResponse("ok");
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }
        return response;
    }

    @Override
    public Response executeActionsOnEntities(String actionKind, List<Entity> entities, ContentData contentData) {
        Response response = null;
        Response.ResponseBuilder responseBuilder = null;
        for (Entity entity : entities) {
            responseBuilder = Response.fromResponse(executeAction(actionKind, entity, contentData));
        }

        if (responseBuilder != null) {
            response = responseBuilder.build();
        }

        if (entities.isEmpty()) {
            try {
                response = outputParser.parseResponse("No entity collection found to execute action: " + actionKind, Response.Status.BAD_REQUEST);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        return response;

    }


    /**
     * Execute update (mixin tag asso + request on entities collection).
     *
     * @param contentData
     * @param location
     * @param pathParser
     * @return
     */
    private Response executeUpdateOnCollection(ContentData contentData, String location, PathParser pathParser) {
        Response response = null;
        String categoryId = pathParser.getCategoryId();
        try {
            // Define if update on mixin tag association.
            if (pathParser.isCollectionOnCategory()) {
                Mixin mixin = MixinManager.findUserMixinOnConfiguration(categoryId, ConfigurationManager.DEFAULT_OWNER);
                if (mixin != null) {
                    LOGGER.info("Mixin tag association query...");
                    List<String> xocciLocations = contentData.getXocciLocations();
                    if (!xocciLocations.isEmpty()) {
                        for (String xocciLocation : xocciLocations) {
                            LOGGER.info("On X-OCCI-Location: " + xocciLocation);
                            response = Response.fromResponse(updateMixinTagAssociation(categoryId, xocciLocation)).build();
                            if (!response.getStatusInfo().equals(Response.Status.OK)) {
                                return response;
                            }
                        }
                    }
                }
            }


            try {
                List<Entity> entities = getEntityCollection(location);

                if (!entities.isEmpty()) {
                    LOGGER.info("Update entities collection on : " + location);
                    response = updateEntityCollection(location, entities, contentData);
                } else {
                    // No entities to update.
                    response = outputParser.parseResponse("No entities to update.", Response.Status.NOT_FOUND);
                }


            } catch (ConfigurationException ex) {
                response = outputParser.parseResponse("error while getting entities collection : " + ex.getMessage());
            }
        } catch (ResponseParseException e) {
            throw new InternalServerErrorException(e);
        }
        return response;

    }

}
