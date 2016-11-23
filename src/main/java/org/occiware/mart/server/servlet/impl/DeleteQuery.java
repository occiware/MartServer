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

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractDeleteQuery;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;

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

        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            return response;
        }

        List<InputData> datas = inputParser.getInputDatas();

        for (InputData data : datas) {
            response = null;

            PathParser pathParser = new PathParser(data, path);

            String location = pathParser.getLocation();
            if (location == null || location.trim().isEmpty()) {
                location = pathParser.getPath();
            }

            // String categoryId = Utils.getCategoryFilterSchemeTerm(location, ConfigurationManager.DEFAULT_OWNER);

            boolean isMixinTagRequest = pathParser.isMixinTagDefinitionRequest();
            if (pathParser.isInterfQuery()) {
                try {
                    response = outputParser.parseResponse("You cannot use interface query on DELETE method, only mixin tag remove definition is authorized", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            if (pathParser.isActionInvocationQuery()) {
                try {
                    response = outputParser.parseResponse("You cannot use an action with DELETE method.", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            // Manage mixins dissociation.
            if (!data.getMixins().isEmpty()) {
                for (String mixinId : data.getMixins()) {
                    response = Response.fromResponse(dissociateMixinFromEntities(mixinId, ConfigurationManager.DEFAULT_OWNER, data, location)).build();
                }
                continue;
            }

            // Manage mixins tag remove definition.
            if (isMixinTagRequest) {
                response = deleteMixin(data.getMixinTag(), ConfigurationManager.DEFAULT_OWNER, true);
                continue;
            }

            Map<String, String> attrs = data.getAttrs();

            if (pathParser.isEntityQuery()) {
                response = deleteEntity(location, attrs);
                continue;
            }

            if (pathParser.isCollectionQuery()) {
                response = deleteEntityCollection(location);
                continue;
            }

            try {
                response = outputParser.parseResponse("Unknown DELETE query type.", Response.Status.BAD_REQUEST);
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }

        } // End for each data.

        return response;
    }

    @Override
    public Response dissociateMixinFromEntities(String mixinId, String owner, InputData data, String location) {
        Response response;
        List<String> entityIds = new ArrayList<>();
        List<String> xocciLocations = data.getXocciLocation();

        String entityId = data.getEntityUUID();
        if (entityId == null) {
            entityId = Utils.getUUIDFromPath(location, data.getAttrs());
        }
        if (entityId != null) {
            entityIds.add(entityId);
        }

        for (String xOcciLocation : xocciLocations) {
            String uuid = Utils.getUUIDFromPath(xOcciLocation, new HashMap<>());
            if (uuid != null) {
                entityIds.add(uuid);
            }
        }

        if (entityIds.isEmpty()) {
            try {
                response = outputParser.parseResponse("No entities defined to dissociate with mixin : " + mixinId, Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException("No entities defined to dissociate with mixin : " + mixinId);
            }
        }


        // Build the list of entities to dissociate from this mixin.
        for (String uuid : entityIds) {
            Entity entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
            if (entity == null) {
                try {
                    response = outputParser.parseResponse("the entity " + uuid + " doesnt exist anymore", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException("the entity " + uuid + "doesnt exist anymore, exception: " + ex.getMessage());
                }
            }
            ConfigurationManager.dissociateMixinFromEntity(owner, mixinId, entity);
        }

        response = outputParser.parseEmptyResponse(Response.Status.OK);
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
            response = outputParser.parseEmptyResponse(Response.Status.OK);
        }
        return response;
    }

    @Override
    public Response deleteEntityCollection(String path) {
        Response response;
        // Delete a collection of entities.
        List<Entity> entities;

        try {
            try {
                entities = getEntityCollection(path);
            } catch (ConfigurationException ex) {
                LOGGER.error(ex.getMessage());
                response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                return response;
            }
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

                for (Entity entityInf : entities) {
                    entityInf.occiDelete();
                    ConfigurationManager.removeOrDissociateFromConfiguration(ConfigurationManager.DEFAULT_OWNER, entityInf.getId());
                }
                response = outputParser.parseEmptyResponse(Response.Status.OK);
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
        String title;
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
        title = entity.getTitle();
        entity.occiDelete();
        ConfigurationManager.removeOrDissociateFromConfiguration(ConfigurationManager.DEFAULT_OWNER, entityId);
        LOGGER.info("Remove entity: " + title + " --> " + entityId);
        response = outputParser.parseEmptyResponse(Response.Status.OK);
        return response;

    }

}
