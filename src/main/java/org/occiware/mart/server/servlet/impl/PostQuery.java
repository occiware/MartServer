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
package org.occiware.mart.server.servlet.impl;

import org.occiware.clouddesigner.occi.*;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractPostQuery;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
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
        // Check header, load parsers, and check occi version.
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            // There was a badrequest, the headers are maybe malformed..
            return response;
        }

        // Check if the query is not on interface query, this path is used only on GET method.
        if (path.equals("-/") || path.equals(".well-known/org/ogf/occi/-/") || path.endsWith("/-/")) {
            try {
                response = outputParser.parseResponse("you cannot use interface query on POST method", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        // Normalize the path without prefix slash and suffix slash.
        path = getPathWithoutPrefixSlash(path);
        LOGGER.info("POST Query on path: " + path);

        if (getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            try {
                response = outputParser.parseResponse("You cannot use " + Constants.MEDIA_TYPE_TEXT_URI_LIST + " on POST method", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }
        // For each data block received on input (only one for text/occi or text/plain, but could be multiple for application/json.
        List<InputData> datas = inputParser.getInputDatas();
        for (InputData data : datas) {
            boolean isActionPost = data.getAction() != null && !data.getAction().isEmpty();
            if (isActionPost && inputParser.getParameter("action") == null) {
                try {
                    response = outputParser.parseResponse("you forgot the parameter ?action=action term", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            Map<String, String> attrs = data.getAttrs();
            boolean isEntityUUIDProvided = Utils.isEntityUUIDProvided(path, attrs);
            String entityUUID = Utils.getUUIDFromPath(path, attrs);


            // Check if uuid is set on path... and replace path without entityuuid.
            if (Utils.isEntityUUIDProvided(path, new HashMap<>())) {
                // UUID is set on path.
                path = path.replace(entityUUID, "");
            }
            String categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);

            // actionId ==> scheme + term.
            String actionId = data.getAction();
            List<Entity> entities;
            Entity entity;


            // Action part.
            if (isActionPost) {

                // Check if this action exist.
                if (ConfigurationManager.getExtensionForAction(ConfigurationManager.DEFAULT_OWNER, actionId) == null) {
                    // Cant find the action on referenced extension.
                    try {
                        response = outputParser.parseResponse("Action : " + actionId + " not found on used extension.", Response.Status.BAD_REQUEST);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }

                // It's an action query on entity or on entity collection.
                if (categoryId != null && !isEntityUUIDProvided) {
                    // This is a collection action.
                    LOGGER.info("Executing action: " + data.getAction() + " on Category: " + categoryId + " collection.");
                    // Load all entities for this category.
                    entities = ConfigurationManager.findAllEntitiesForCategory(ConfigurationManager.DEFAULT_OWNER, categoryId);

                    response = executeActionsOnEntities(actionId, entities);

                } else if (categoryId != null && isEntityUUIDProvided) {
                    // This is an action on an entity category.
                    LOGGER.info("Executing action: " + data.getAction() + " on Entity path: " + path);
                    entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityUUID);

                    // Check if category exist on extensions, this must be a kind or a mixin.
                    Category cat = ConfigurationManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    if (cat == null) {
                        cat = ConfigurationManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    }
                    if (cat == null) {
                        try {
                            response = outputParser.parseResponse("The category : " + categoryId + " doesn't exist on extensions", Response.Status.BAD_REQUEST);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }

                    }

                    if (entity == null) {
                        try {
                            response = outputParser.parseResponse("The entity : " + entityUUID + " doesn't exist on path : " + path, Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }
                    response = executeAction(actionId, entity);

                } else {
                    // This is an action on an entity collection.
                    LOGGER.info("Executing action: " + data.getAction() + " on path: " + path);
                    entities = ConfigurationManager.findAllEntitiesForCategory(ConfigurationManager.DEFAULT_OWNER, actionId);
                    // Remove entities that have not their location equals to path.
                    Iterator<Entity> it = entities.iterator();
                    while (it.hasNext()) {
                        Entity entityTmp = it.next();

                        String location = ConfigurationManager.getEntityRelativePath(entityTmp.getId());

                        if (!location.equals(path)) {
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
                    // launch the action on collection.
                    response = executeActionsOnEntities(actionId, entities);

                }

                return response;
            } // End if action part.

            boolean mixinTagAsso;
            //  - update an entity, 
            //  - update a collection of entities 
            if (isEntityUUIDProvided) {
                if (attrs != null && !attrs.isEmpty()) {
                    // Load the entity.
                    entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, Utils.getUUIDFromPath(path, attrs));
                    if (entity == null) {
                        try {
                            response = outputParser.parseResponse("The entity : " + entityUUID + " doest exit on path : " + path, Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }

                    response = updateEntity(path, entity);
                    return response;
                } else {
                    // No attribute to update.
                    try {
                        response = outputParser.parseResponse("No attributes found", Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }

            } else if (categoryId != null) {
                // Check if this is a mixin tag association or a collection of entities..

                // Check if this is a mixin without attribute.
                Mixin mixin = ConfigurationManager.findUserMixinOnConfiguration(categoryId, ConfigurationManager.DEFAULT_OWNER);
                if (mixin != null) {
                    // This is a mixin tag.
                    mixinTagAsso = true;
                } else {
                    // This must be a collection on category.
                    entities = ConfigurationManager.findAllEntitiesForCategory(ConfigurationManager.DEFAULT_OWNER, categoryId);
                    response = updateEntityCollection(path, entities);
                    return response;
                }
            } else {
                // Defined on a path.
                entities = ConfigurationManager.findAllEntitiesOwner(ConfigurationManager.DEFAULT_OWNER);
                // Remove entities that have not their location equals to path.
                Iterator<Entity> it = entities.iterator();
                while (it.hasNext()) {
                    Entity entityTmp = it.next();
                    String location = ConfigurationManager.getEntityRelativePath(entityTmp.getId());
                    if (!location.equals(path)) {
                        it.remove();
                    }
                }
                response = updateEntityCollection(path, entities);
                return response;

            }

            // - update a mixin association (mixin tag included) 
            if (mixinTagAsso || (data.getMixinTag() != null && ConfigurationManager.isMixinTags(ConfigurationManager.DEFAULT_OWNER, data.getMixinTag()))) {
                String location = data.getMixinTagLocation();
                List<String> xocciLocations = data.getXocciLocation();
                if (location == null) {
                    // Check if X-OCCI-Location is used.
                    if (xocciLocations.isEmpty()) {
                        try {
                            response = outputParser.parseResponse("Request is invalid, mixin tag location is unknown", Response.Status.BAD_REQUEST);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }
                }
                if (!xocciLocations.isEmpty()) {
                    for (String xocciLocation : xocciLocations) {
                        response = Response.fromResponse(updateMixinTagAssociation(categoryId, xocciLocation)).build();
                        if (!response.getStatusInfo().equals(Response.Status.OK)) {
                            return response;
                        }
                    }
                } else {
                    response = updateMixinTagAssociation(categoryId, location);
                }

                return response;
            } else {
                // Request is undefined.
                try {
                    response = outputParser.parseResponse("Request is invalid, please check your query", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

        }

        if (datas.isEmpty()) {
            try {
                response = outputParser.parseResponse("Request is invalid, no datas attributes, nor resources, please check your query", Response.Status.BAD_REQUEST);
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
     * @return
     */
    @Override
    public Response updateEntity(String path, Entity entity) {
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

        InputData data = inputParser.getInputDataForEntityUUID(entity.getId());

        List<String> mixins = data.getMixins();
        if (mixins != null && !mixins.isEmpty()) {
            try {
                ConfigurationManager.addMixinsToEntity(entity, mixins, ConfigurationManager.DEFAULT_OWNER, false);
            } catch (ConfigurationException ex) {
                // Already logged in sub method.
                try {
                    return outputParser.parseResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                } catch (ResponseParseException e) {
                    throw new InternalServerErrorException(e);
                }
            }
        }

        Map<String, String> attrs = data.getAttrs();
        // entity.occiRetrieve();
        // update attributes .
        entity = ConfigurationManager.updateAttributesToEntity(entity, attrs);
        ConfigurationManager.updateVersion(ConfigurationManager.DEFAULT_OWNER, path + entity.getId());
        entity.occiUpdate();

        try {
            response = outputParser.parseResponse(entity);
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }

        return response;
    }

    @Override
    public Response updateEntityCollection(String path, List<Entity> entities) {
        Response response = null;
        Response.ResponseBuilder responseBuilder = null;
        for (Entity entity : entities) {
            responseBuilder = Response.fromResponse(updateEntity(path, entity));
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

        String uuid = Utils.getUUIDFromPath(relativeLocationApply, new HashMap<>());
        if (uuid == null) {
            throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore");
        }
        Entity entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
        if (entity == null) {
            throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore");
        }

        try {
            ConfigurationManager.addMixinsToEntity(entity, mixins, ConfigurationManager.DEFAULT_OWNER, false);
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
     * @return
     */
    @Override
    public Response executeAction(String actionId, Entity entity) {
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

        InputData data = inputParser.getInputDataForEntityUUID(entity.getId());
        if (data == null) {
            // This is an action on collections or on a path.
            List<InputData> datas = inputParser.getInputDatas();
            for (InputData dataTmp : datas) {
                data = dataTmp;
                break;
            }
        }
        String[] actionParameters = null;

        if (data != null && data.getAttrs() != null) {
            actionParameters = Utils.getActionParametersArray(data.getAttrs());
        }

        String entityKind = entity.getKind().getScheme() + entity.getKind().getTerm();
        Extension ext = ConfigurationManager.getExtensionForKind(ConfigurationManager.DEFAULT_OWNER, entityKind);

        Action actionKind = ConfigurationManager.getActionKindFromExtension(ext, actionId);
        if (actionKind == null) {
            LOGGER.error("Action : " + actionId + " doesnt exist on extension : " + ext.getName());
            try {
                response = outputParser.parseResponse("Action : " + actionId + " doesnt exist on extension : " + ext.getName(), Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {
            if (actionParameters == null) {
                OcciHelper.executeAction(entity, actionKind.getTerm());
            } else {
                OcciHelper.executeAction(entity, actionKind.getTerm(), actionParameters);
            }
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            LOGGER.error("Action failed to execute : " + ex.getMessage());
            try {
                response = outputParser.parseResponse("Action failed : " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {
            response = outputParser.parseResponse(entity);
        } catch (ResponseParseException ex) {
            throw new InternalServerErrorException(ex);
        }
        return response;
    }

    @Override
    public Response executeActionsOnEntities(String actionKind, List<Entity> entities) {
        Response response = null;
        Response.ResponseBuilder responseBuilder = null;
        for (Entity entity : entities) {
            responseBuilder = Response.fromResponse(executeAction(actionKind, entity));
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

}
