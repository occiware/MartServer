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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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

/**
 *
 * @author cgourdin
 */
@Path("/")
public class PutQuery extends AbstractPutQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutQuery.class);

    /**
     * Create or replace input point, this method is for creating entity with a
     * relative path like '/foo/bar/uuid'.
     *
     * @param path
     * @param headers
     * @param request
     * @return
     */
    @Override
    @Path("{path:.*}")
    @PUT
//    @Consumes({Constants.MEDIA_TYPE_TEXT_OCCI, Constants.MEDIA_TYPE_TEXT_URI_LIST})
//    @Produces(Constants.MEDIA_TYPE_TEXT_OCCI)
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {

        LOGGER.info("--> Call CREATE method input query for relative path mode --> " + path);
        // Check header, load parsers, and check occi version.
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            // There was a badrequest, the headers are maybe malformed..
            return response;
        }
        // Check if the query is not on interface query, this path is used only on GET method.
        if (path.equals("-/") || path.equals(".well-known/org/ogf/occi/-/") || path.endsWith("/-/")) {
            try {
                response = outputParser.parseResponse("you cannot use interface query on PUT method", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        // For each data block received on input (only one for text/occi or text/plain, but could be multiple for application/json.
        List<InputData> datas = inputParser.getInputDatas();
        for (InputData data : datas) {
            if (data.getAction() != null) {
                try {
                    response = outputParser.parseResponse("you cannot use an action with PUT method", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            String kind = data.getKind();
            List<String> mixins = data.getMixins();
            if (kind == null && mixins == null) {
                try {
                    response = outputParser.parseResponse("No category provided !", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }
            
            // Check if the query is a create/overwrite mixin tag definition.
            String mixinTag = data.getMixinTag();
            if (mixinTag != null) {
                LOGGER.info("Define or overwrite mixin tag definitions");
                response = defineMixinTag(data);
                continue;
            }
            
            String entityId = data.getEntityUUID();
            if (entityId == null) {
                entityId = Utils.getUUIDFromPath(path, data.getAttrs());
               // if entityId is null here, no uuid provided for this entity so createEntity method will create a new uuid for future use..
            }

            if (kind != null) {
                String relativePath = path;

                if (entityId != null && path.contains(entityId)) {
                    relativePath = path.replace(entityId, "");
                }

                response = createEntity(relativePath, entityId, kind, mixins, data.getAttrs());
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
     * Create a new resource or link.
     *
     * @param path
     * @param entityId
     * @param kind
     * @param mixins
     * @param attributes
     * @return
     */
    @Override
    public Response createEntity(final String path, String entityId, final String kind, final List<String> mixins, final Map<String, String> attributes) {

        LOGGER.info("--> Call method createEntity on path: " + path);

        Response response;

        // Later, owner / group will be implemented, for now owner "anonymous" is the default.
        String owner = ConfigurationManager.DEFAULT_OWNER;

        String location;
        // Link or resource ?
        boolean isResource;
        boolean hasCreatedUUID = false;
        // Check if entityId is null or there is a uuid on path, if this is not the case, generate the uuid and add it to location.
        if (entityId == null || entityId.trim().isEmpty()) {
            // Create a new uuid.
            entityId = Utils.createUUID();
            hasCreatedUUID = true;
        }

        // String relativePath = getUri().getPath();
        // Determine if this is a link or a resource.
        // Check the attribute map if attr contains occi.core.source or
        // occi.core.target, this is a link !
        isResource = ConfigurationManager.checkIfEntityIsResourceOrLinkFromAttributes(attributes);
        location = getUri().getPath();
        if (hasCreatedUUID) {
            location += "/" + entityId;
        }

        // Add location attribute.
//        attrs.put(Constants.X_OCCI_LOCATION, location);
        LOGGER.info("Create entity with location: " + location);
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

            LOGGER.info("Overwriting entity : " + location);
        } else {
            LOGGER.info("Creating entity : " + location);
        }

        try {
            String coreId = Constants.URN_UUID_PREFIX + entityId;
            if (isResource) {
                attributes.put("occi.core.id", coreId);
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
                ConfigurationManager.addLinkToConfiguration(entityId, kind, mixins, src, target, attributes, owner, path);
            }
        } catch (ConfigurationException ex) {
            try {
                response = outputParser.parseResponse("The entity has not been added, it may be produce if you use non referenced attributes. \r\n Message : " + ex.getMessage(), Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }
        // Get the entity to be sure that it was inserted on configuration object.
        Entity entity = ConfigurationManager.findEntity(owner, entityId);
        if (entity != null) {
            entity.occiCreate();
            LOGGER.info("Create entity done returning location : " + location);
        } else {
            LOGGER.error("Error, entity was not created on object model, please check your query.");
            try {
                response = outputParser.parseResponse("Error, entity was not created on object model, please check your query", Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(e);
            }
        }

        try {

            response = Response.created(new URI(location))
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .type(getContentType())
                    .header("Accept", getAcceptType())
                    .build();

        } catch (URISyntaxException ex) {
            response = Response.created(getUri().getAbsolutePath())
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .type(getContentType())
                    .header("Accept", getAcceptType())
                    .build();
        }
        return response;

    }

    @Override
    public Response defineMixinTag(final InputData data) {
        
        Response response;
        String mixinTag = data.getMixinTag();
        LOGGER.info("Define mixin tag : " + mixinTag);
        String mixinLocation = data.getMixinTagLocation();
        String title = data.getMixinTagTitle();
        
        try {
            if (mixinLocation == null || mixinLocation.isEmpty()) {
                throw new ConfigurationException("No location is defined for this mixin.");
            }
            ConfigurationManager.addUserMixinOnConfiguration(mixinTag, title, mixinLocation, ConfigurationManager.DEFAULT_OWNER);
        } catch (ConfigurationException ex) {
            try {
                response = outputParser.parseResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                return response;
            } catch (ResponseParseException e) {
                throw new InternalServerErrorException(ex.getMessage());
            }
        }
        try {
            response = Response.created(new URI(mixinLocation))
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .type(getContentType())
                    .header("Accept", getAcceptType())
                    .build();
        } catch (URISyntaxException ex) {
            response = Response.created(getUri().getAbsolutePath())
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .type(getContentType())
                    .header("Accept", getAcceptType())
                    .build();
        }
        
        return response;
    }

}
