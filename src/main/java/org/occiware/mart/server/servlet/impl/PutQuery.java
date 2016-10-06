/*
 * Copyright 2016 cgourdin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.mart.server.servlet.exception.EntityAddException;
import org.occiware.mart.server.servlet.exception.EntityConflictException;
import org.occiware.mart.server.servlet.facade.AbstractPutQuery;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.textocci.TextOCCIParser;
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
     * Create or replace input point, this method is for creating entity with a relative path like '/foo/bar/uuid'.
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
        
        LOGGER.info("--> Call method input query for relative path mode --> " + path);
        // Check header, load parser, and check occi version.
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            // There was a badrequest, the headers are maybe malformed..
            return response;
        }
        
        if (inputParser.getAction() != null) {
            throw new BadRequestException("cant use action with put request.");
        }
        
        String kind = inputParser.getKind();
        List<String> mixins = inputParser.getMixins();
        if (kind == null && mixins == null) {
            throw new BadRequestException("No category provided !");
        }
        
        String entityId = inputParser.getEntityUUID();
        if (entityId == null) {
            entityId = Utils.getUUIDFromId(path, inputParser.getOcciAttributes());
            // if entityId is null here, no uuid provided for this entity so createEntity method will create a new uuid for future use..
        }
        
        if (kind != null) {
            response = createEntity(path, entityId, kind, mixins, inputParser.getOcciAttributes());
        } else {
            // TODO : response = createMixin(mixins, headers, request);
        }
        
        
        
        return response;
        
    }
    
   /**
     * Create a new resource or link.
     *
     * @param path
     * @param entityId
     * @param headers
     * @param request
     * @return
     */
    @Override
    public Response createEntity(final String path, String entityId, final String kind, final List<String> mixins, final Map<String, String> attributes) {
        
        LOGGER.info("--> Call method createEntity on path: " + path);
        
        Response response;
        
       
        // Later, owner / group will be implemented, for now owner "anonymous" is the default.
        String owner = ConfigurationManager.DEFAULT_OWNER;
        
        Kind kindModel = ConfigurationManager.findKindFromExtension(owner, kind);
        if (kindModel == null) {
            throw new BadRequestException("The kind : " + kind + " doesnt exist on referenced extensions");
        }
        
        String uuid;
        String location;
        // Link or resource ?
        boolean isResource;
        
        // Check if entityId is null or there is a uuid on path, if this is not the case, generate the uuid and add it to location.
        if (entityId == null || entityId.trim().isEmpty()) {
            // Create a new uuid.
            entityId = Utils.createUUID();
        }
        
        // String relativePath = getUri().getPath();
        
        // Determine if this is a link or a resource.
        // Check the attribute map if attr contains occi.core.source or
        // occi.core.target, this is a link !
        isResource = ConfigurationManager.checkIfEntityIsResourceOrLinkFromAttributes(attributes);
        location = getUri().getAbsolutePath().toString();
        if (entityId == null) {
            location += entityId;
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
            if (isResource) {
                ConfigurationManager.addResourceToConfiguration(entityId, kind, mixins, attributes, owner, location);
            } else {
                String src = attributes.get(Constants.OCCI_CORE_SOURCE);
                String target = attributes.get(Constants.OCCI_CORE_TARGET);

                String coreId = Constants.URN_UUID_PREFIX + entityId;
                attributes.put("occi.core.id", Constants.URN_UUID_PREFIX + entityId);
                ConfigurationManager.addLinkToConfiguration(entityId, kind, mixins, src, target, attributes, owner, location);
            }
        } catch (EntityAddException ex) {
            return Response.serverError()
                    .entity("The entity has not been add, it may be produce if you use forbidden attributes. \n Message : " + ex.getMessage())
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .build();
        }
        // Get the entity to be sure that it was inserted on configuration object.
        Entity entity = ConfigurationManager.findEntity(owner, entityId);
        if (entity != null) {
            entity.occiCreate();
            LOGGER.info("Create entity done returning location : " + location);
        } else {
            LOGGER.error("Error, entity was not created on object model, please check your query.");
            throw new BadRequestException("Error, entity was not created on object model, please check your query.");
        }
        
        Utils.printEntity(entity);
        
        
        try {
            // TODO : Check here if we must use outputParser.parseResponse...
            response = Response.created(new URI(location))
                .header("Server", Constants.OCCI_SERVER_HEADER)
                .build();

            return response;
        } catch (URISyntaxException ex) {
            response = Response.created(getUri().getAbsolutePath())
                .header("Server", Constants.OCCI_SERVER_HEADER)
                .build();

            return response;
        }

    }

    @Override
    public Response createMixin(String mixinKind, HttpHeaders headers, HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
