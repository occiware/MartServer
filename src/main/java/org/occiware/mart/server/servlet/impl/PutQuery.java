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
import org.occiware.mart.server.servlet.exception.EntityConflictException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractPutQuery;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe Gourdin
 */
@Path("/")
public class PutQuery extends AbstractPutQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutQuery.class);

    /**
     * Create or replace input point, this method is for creating entity with a
     * relative path like '/foo/bar/uuid'.
     *
     * @param path    object location path.
     * @param headers http headers
     * @param request http request object
     * @return a Response object.
     */
    @Override
    @Path("{path:.*}")
    @PUT
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        LOGGER.info("--> Call PUT method input query for path : " + path);

        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            return response;
        }
        List<InputData> datas = inputParser.getInputDatas();

        for (InputData data : datas) {

            PathParser pathParser = new PathParser(data, path, inputParser.getRequestPameters());

            if (pathParser.isInterfQuery()) {
                try {
                    response = outputParser.parseResponse("you cannot use interface query on PUT method", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            String location = pathParser.getLocation();
            if (location == null || location.trim().isEmpty()) {
                location = pathParser.getPath();
            }

            String kind = data.getKind();
            List<String> mixins = data.getMixins();

            if (kind == null && (mixins == null || mixins.isEmpty()) && !pathParser.isMixinTagDefinitionRequest()) {
                try {
                    response = outputParser.parseResponse("No category provided !", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            if (pathParser.isEntityQuery()) {
                String entityId = data.getEntityUUID();
                if (entityId == null) {
                    entityId = Utils.getUUIDFromPath(location, data.getAttrs());
                    // if entityId is null here, no uuid provided for this entity so createEntity method will create a new uuid for future use..
                }

                if (kind != null) {
                    String relativePath = location;

                    if (entityId != null && location.contains(entityId)) {
                        relativePath = location.replace(entityId, "");
                    }

                    response = createEntity(relativePath, entityId, kind, mixins, data.getAttrs());
                }
            }

            if (pathParser.isMixinTagDefinitionRequest()) {
                LOGGER.info("Define a mixin tag : " + data.getMixinTag());
                defineMixinTag(data, location);
            }

            if (pathParser.isCollectionQuery()) {
                // Set as collection query if no entity found on this path like /mypath/myother/.
                LOGGER.info("PUT resource on path : " + location);
                if (kind != null) {
                    response = createEntity(location, null, kind, mixins, data.getAttrs());
                }
            }


        } // End for each inputdatas.
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
     * Create a new resource or link. Overwrite entity if entity already exist in domain (and call update).
     *
     * @param path
     * @param entityId
     * @param kind
     * @param mixins
     * @param attributes
     * @return a Response object.
     */
    @Override
    public Response createEntity(final String path, String entityId, final String kind, final List<String> mixins, final Map<String, String> attributes) {

        LOGGER.info("--> Call method createEntity on path: " + path);
        boolean overwrite = false;
        Response response;

        // Later, owner / group will be implemented, for now owner "anonymous" is the default.
        String owner = ConfigurationManager.DEFAULT_OWNER;

        String xabsoluteLocation;
        // Link or resource ?
        boolean isResource;
        // Check if entityId is null or there is a uuid on path, if this is not the case, generate the uuid and add it to location.
        if (entityId == null || entityId.trim().isEmpty()) {
            // Create a new uuid.
            entityId = Utils.createUUID();
        }

        isResource = ConfigurationManager.checkIfEntityIsResourceOrLinkFromAttributes(attributes);
        xabsoluteLocation = getUri().getBaseUri().toString();
        if (path.endsWith("/")) {
            xabsoluteLocation += path;
        } else {
            if (!path.isEmpty()) {
                if (xabsoluteLocation.endsWith("/")) {
                    xabsoluteLocation += path;
                } else {
                    xabsoluteLocation += "/" + path;
                }
            }
        }

        if (!Utils.isEntityUUIDProvided(path, new HashMap<>())) {
            if (xabsoluteLocation.endsWith("/")) {
                xabsoluteLocation += entityId;
            } else {
                xabsoluteLocation += "/" + entityId;
            }
        }

        LOGGER.info("Create entity with location: " + xabsoluteLocation);
        LOGGER.info("Kind: " + kind);

        for (String mixin : mixins) {
            LOGGER.info("Mixin : " + mixin);
        }
        LOGGER.info("Attributes: ");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            LOGGER.info(entry.getKey() + " ---> " + entry.getValue());
        }

        if (ConfigurationManager.isEntityExist(owner, entityId)) {
            // Check if occi.core.id reference another id.
            if (attributes.containsKey(Constants.OCCI_CORE_ID)) {
                // check if this is the same id, if not there is a conflict..
                String coreId = attributes.get(Constants.OCCI_CORE_ID);
                // Check if urn:uuid: is set.
                if (coreId != null) {
                    if (coreId.contains(Constants.URN_UUID_PREFIX)) {
                        coreId = coreId.replace(Constants.URN_UUID_PREFIX, "");
                    }
                    if (!coreId.equals(entityId)) {
                        throw new EntityConflictException("The attribute occi.core.id value is not the same as the uuid specified in url path.", Response.Status.CONFLICT);
                    }
                }
            }
            overwrite = true;
            LOGGER.info("Overwriting entity : " + xabsoluteLocation);
        } else {
            LOGGER.info("Creating entity : " + xabsoluteLocation);
        }

        try {
            String coreId = Constants.URN_UUID_PREFIX + entityId;
            if (isResource) {
                attributes.put("occi.core.id", coreId);
                LOGGER.info("Relative path location: " + path);
                ConfigurationManager.addResourceToConfiguration(entityId, kind, mixins, attributes, owner, path);
            } else {
                String src = attributes.get(Constants.OCCI_CORE_SOURCE);
                String target = attributes.get(Constants.OCCI_CORE_TARGET);
                if (src == null) {
                    String message = "No source provided for this link : " + entityId;
                    try {
                        response = outputParser.parseResponse(message, Response.Status.BAD_REQUEST);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }
                if (target == null) {
                    String message = "No target provided for this link : " + entityId;
                    try {
                        response = outputParser.parseResponse(message, Response.Status.BAD_REQUEST);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }

                attributes.put("occi.core.id", coreId);
                LOGGER.info("Relative path location : " + path);
                ConfigurationManager.addLinkToConfiguration(entityId, kind, mixins, src, target, attributes, owner, path);
            }
        } catch (ConfigurationException ex) {
            try {
                response = outputParser.parseResponse("The entity has not been added, it may be produce if you use non referenced attributes. Message : " + ex.getMessage(), Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }
        // Get the entity to be sure that it was inserted on configuration object.
        Entity entity = ConfigurationManager.findEntity(owner, entityId);
        if (entity != null) {
            if (overwrite) {
                entity.occiUpdate();
                LOGGER.info("Update entity done returning location : " + path);
            } else {
                entity.occiCreate();
                LOGGER.info("Create entity done returning location : " + path);
            }

        } else {
            // TODO : update here to enable more explaining message. use try catch...
            LOGGER.error("Error, entity was not created on object model, please check your query.");
            try {
                response = outputParser.parseResponse("Error, entity was not created on object model, please check your query", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {
            if (overwrite) {
                response = outputParser.parseResponse(entity, Response.Status.OK);
            } else {
                response = outputParser.parseResponse(entity, Response.Status.CREATED);
            }
            response.getHeaders().add("Location", new URI(xabsoluteLocation));

        } catch (URISyntaxException | ResponseParseException ex) {
            String message = "Exception thrown when parsing uri location or parsing response : " + ex.getClass().getName() + " --> Message: " + ex.getMessage();
            LOGGER.error(message);
            try {
                response = outputParser.parseResponse("Exception thrown when parsing uri location or parsing response : " + ex.getClass().getName() + " --> Message: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            } catch (ResponseParseException e) {
                response = Response
                        .serverError()
                        .header("Server", Constants.OCCI_SERVER_HEADER)
                        .type(getContentType())
                        .header("Accept", getAcceptType())
                        .build();
            }
        }
        return response;
    }

    /**
     * Define a new mixin tag.
     * @param data
     * @param path
     * @return
     */
    @Override
    public Response defineMixinTag(final InputData data, final String path) {

        Response response;
        String mixinTag = data.getMixinTag();
        LOGGER.info("Define mixin tag : " + mixinTag);
        String mixinLocation = data.getLocation();
        String title = data.getMixinTagTitle();
        List<String> xocciLocations = data.getXocciLocation();
        try {
            if ((mixinLocation == null || mixinLocation.isEmpty()) && (xocciLocations == null || xocciLocations.isEmpty())) {
                throw new ConfigurationException("No location is defined for this mixin.");
            }
            if (mixinLocation != null) {
                ConfigurationManager.addUserMixinOnConfiguration(mixinTag, title, mixinLocation, ConfigurationManager.DEFAULT_OWNER);
            }
            if (xocciLocations != null && !xocciLocations.isEmpty()) {

                // Get the mixin scheme+term from path.
                String categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);
                if (categoryId == null) {
                    throw new ConfigurationException("Category is not defined");
                }
                List<String> entities = new ArrayList<>();
                String base = this.getUri().getBaseUri().toString();
                for (String xOcciLocation : xocciLocations) {
                    // Build a list of entities from xoccilocations defined.
                    if (Utils.isEntityUUIDProvided(xOcciLocation, new HashMap<>())) {
                        // One entity.
                        String uuid = Utils.getUUIDFromPath(xOcciLocation, new HashMap<>());
                        if (uuid == null) {
                            throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore");
                        }
                        Entity entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
                        if (entity == null) {
                            throw new BadRequestException(Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore");
                        }
                        entities.add(entity.getId());
                    } else {
                        // Maybe a collection on inbound or outbound path.
                        String relativePath = xOcciLocation.replace(base, "");
                        List<Entity> entitiesTmp = getEntityCollection(relativePath);
                        if (!entitiesTmp.isEmpty()) {
                            for (Entity entity : entitiesTmp) {
                                entities.add(entity.getId());
                            }
                        }
                    }
                }
                // Full update mode.
                if (!entities.isEmpty()) {
                    ConfigurationManager.saveMixinForEntities(categoryId, entities, true, ConfigurationManager.DEFAULT_OWNER);
                }
            }
        } catch (ConfigurationException ex) {
            try {
                response = outputParser.parseResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(ex.getMessage());
            }
        }
        response = Response.created(getUri().getAbsolutePath())
                .header("Server", Constants.OCCI_SERVER_HEADER)
                .type(getContentType())
                .header("Accept", getAcceptType())
                .build();

        return response;
    }

}
