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
package org.occiware.mart.server.servlet.textocci;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.ConfigurationManager;
import org.occiware.mart.server.servlet.exception.EntityAddException;
import org.occiware.mart.server.servlet.exception.EntityConflictException;
import org.occiware.mart.server.servlet.facade.AbstractPutQuery;
import org.occiware.mart.server.servlet.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christophe Gourdin
 */
@Path("/")
public class PutQuery extends AbstractPutQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutQuery.class);

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
    @Path("{path}/{id}")
    @GET
    @Consumes({Constants.MEDIA_TYPE_TEXT_OCCI, Constants.MEDIA_TYPE_TEXT_URI_LIST})
    @Produces(Constants.MEDIA_TYPE_TEXT_OCCI)
    public Response createEntity(@PathParam("path") String path, @PathParam("entityId") String entityId, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        Response response;

        // Link or resource ?
        boolean isResource;

        // Kind.
        String kind = TextOCCIParser.getKindFromQuery(headers, request);

        String owner = ConfigurationManager.DEFAULT_OWNER;
        String relativePath = getUri().getPath();

        Map<String, String> attributes = TextOCCIParser.convertAttributesInQueryToMap(headers, request);

        List<String> mixins = TextOCCIParser.convertMixinsInQueryToList(headers, request);

        // Determine if this is a link or a resource.
        // Check the attribute map if attr contains occi.core.source or
        // occi.core.target, this is a link !
        isResource = ConfigurationManager.checkIfEntityIsResourceOrLinkFromAttributes(attributes);

        LOGGER.info("Create entity with location set, input with location=" + getUri().getAbsolutePath() + ", kind=" + path + ", mixins="
                + mixins + ", attributes=" + attributes + " , owner : " + owner);
        
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

            LOGGER.info("Overwriting entity : " + entityId);
        } else {
            LOGGER.info("Creating entity : " + entityId);
        }
        List<String> links = new LinkedList<>();
        try {
            if (isResource) {
                ConfigurationManager.addResourceToConfiguration(entityId, kind, mixins, attributes, owner, relativePath);
            } else {
                String src = attributes.get(Constants.OCCI_CORE_SOURCE);
                String target = attributes.get(Constants.OCCI_CORE_TARGET);
                if (src != null) {
                    links.add(src);
                }
                if (target != null) {
                    links.add(target);
                }
                String coreId = Constants.URN_UUID_PREFIX + entityId;
                attributes.put("occi.core.id", Constants.URN_UUID_PREFIX + entityId);
                ConfigurationManager.addLinkToConfiguration(entityId, kind, mixins, src, target, attributes, owner, relativePath);
            }
        } catch (EntityAddException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        // Get the entity to be sure that it was inserted on configuration object.
        Entity entity = ConfigurationManager.findEntity(owner, entityId);
        if (entity != null) {
            entity.occiCreate();
            LOGGER.info("Create entity done returning relative path : " + entity.getId());
        } else {
            LOGGER.error("Error, entity was not created on object model, please check your query.");
            throw new BadRequestException("Error, entity was not created on object model, please check your query.");
        }

        response = Response.created(super.getUri().getAbsolutePath())
                .build();

        return response;

    }

    @Override
    public Response createMixin(String mixinKind, HttpHeaders headers, HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
