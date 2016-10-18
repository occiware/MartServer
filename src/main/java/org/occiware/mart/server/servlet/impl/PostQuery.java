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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Category;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Extension;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractPostQuery;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        if (getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            try {
                response = outputParser.parseResponse("You cannot use " + Constants.MEDIA_TYPE_TEXT_URI_LIST + " on POST method", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        boolean isActionPost = inputParser.getAction() != null && !inputParser.getAction().isEmpty();
        if (isActionPost && inputParser.getParameter("action") == null) {
            try {
                response = outputParser.parseResponse("you forgot the parameter ?action=action term", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        //  - execute an action on entity
        //  - execute an action on entity collection
        String categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);

        Map<String, String> attrs = inputParser.getOcciAttributes();

        boolean isEntityUUIDProvided = Utils.isEntityUUIDProvided(path, attrs);

        // actionId ==> scheme + term.
        String actionId = inputParser.getAction();
        List<Entity> entities;
        Entity entity;

        String entityUUID = Utils.getUUIDFromPath(path, attrs);
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
                LOGGER.info("Executing action: " + inputParser.getAction() + " on Category: " + categoryId + " collection.");
                // Load all entities 
                entities = ConfigurationManager.findAllEntitiesForCategoryId(categoryId).get(ConfigurationManager.DEFAULT_OWNER);

                response = executeActionsOnEntities(actionId, entities);

            } else if (categoryId != null && isEntityUUIDProvided) {
                // This is an action on an entity category.
                LOGGER.info("Executing action: " + inputParser.getAction() + " on Entity path: " + path);
                entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityUUID);

                // Check if category exist on extensions, this must be a kind or a mixin.
                Category cat = ConfigurationManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                if (cat == null) {
                    cat = ConfigurationManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, categoryId);
                }
                if (cat == null) {
                    try {
                        response = outputParser.parseResponse("The category : " + categoryId + " doest exit on extensions", Response.Status.BAD_REQUEST);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }

                }

                if (entity == null) {
                    try {
                        response = outputParser.parseResponse("The entity : " + entityUUID + " doest exit on path : " + path, Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }
                response = executeAction(actionId, entity);

            } else {
                // This is an action on an entity collection.
                LOGGER.info("Executing action: " + inputParser.getAction() + " on path: " + path);
                entities = ConfigurationManager.findAllEntitiesForAction(ConfigurationManager.DEFAULT_OWNER, actionId);
                // Remove entities that have not their location equals to path.
                Iterator<Entity> it = entities.iterator();
                while (it.hasNext()) {
                    Entity entityTmp = it.next();
                    String location = ConfigurationManager.getEntityRelativePath(entityTmp.getId());
                    if (!location.equals(path)) {
                        it.remove();
                    }
                }

                // launch the action on collection.
                response = executeActionsOnEntities(actionId, entities);

            }

            return response;
        } // End if action part.

        boolean mixinTagAsso = false;
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
            Mixin mixin = ConfigurationManager.findUserMixinOnConfigurations(categoryId);
            if (mixin != null) {
                // This is a mixin tag.
                mixinTagAsso = true;
            } else {
                // This must be a collection on category.
                entities = ConfigurationManager.findAllEntitiesForCategoryId(categoryId).get(ConfigurationManager.DEFAULT_OWNER);
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
        if (mixinTagAsso) {
            // Get X-OCCI-Location attribute.
            String location = attrs.get(Constants.X_OCCI_LOCATION);
            response = updateMixinTagAssociation(categoryId, location);
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

    /**
     * Update attribute on entity.
     *
     * @param path
     * @param entity
     * @return
     */
    @Override
    public Response updateEntity(String path, Entity entity) {
        Response response = null;

        Map<String, String> attrs = inputParser.getOcciAttributes();

        if (entity != null) {
            entity.occiRetrieve();
            // update attributes .
            entity = ConfigurationManager.updateAttributesToEntity(entity, attrs);
            ConfigurationManager.updateVersion(ConfigurationManager.DEFAULT_OWNER, path + entity.getId());
            entity.occiUpdate();

            try {
                response = outputParser.parseResponse(entity);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }

        } else {
            try {
                response = outputParser.parseResponse("No entity found for update.", Response.Status.BAD_REQUEST);
            } catch (ResponseParseException ex) {
                // Must never happen if input query is ok.
                throw new InternalServerErrorException(ex);
            }
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
                response = outputParser.parseResponse("No entity collection found for update", Response.Status.BAD_REQUEST);
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
        ConfigurationManager.addMixinsToEntity(entity, mixins, ConfigurationManager.DEFAULT_OWNER, true);

        try {
            response = outputParser.parseResponse("OK");
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
        String[] actionParameters = Utils.getActionParametersArray(inputParser.getOcciAttributes());
        String entityKind = entity.getKind().getScheme() + entity.getKind().getTerm();
        Extension ext = ConfigurationManager.getExtensionForKind(ConfigurationManager.DEFAULT_OWNER, entityKind);

        Action actionKind = ConfigurationManager.getActionKindFromExtension(ext, actionId);
        if (actionKind == null) {
            LOGGER.error("Action : " + actionId + " doesnt exist on extension : " + ext.getName());
            throw new BadRequestException("Action : " + actionId + " doesnt exist on extension : " + ext.getName());
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
                response = outputParser.parseResponse("Action failed : " + ex.getMessage());
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
                response = outputParser.parseResponse("No entity collection found for execute action: " + actionKind, Response.Status.BAD_REQUEST);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        return response;

    }

}
