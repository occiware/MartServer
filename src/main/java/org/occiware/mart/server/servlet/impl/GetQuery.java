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

import org.occiware.clouddesigner.occi.Category;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractGetQuery;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe Gourdin
 */
@Path("/")
public class GetQuery extends AbstractGetQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetQuery.class);

    @Path("{path:.*}/")
    @GET
    @Override
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        LOGGER.info("Call GET method in inputQuery() for path: " + path);

        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            return response;
        }

        List<InputData> datas = inputParser.getInputDatas();


        for (InputData data : datas) {

            PathParser pathParser = new PathParser(data, path);

            String location = pathParser.getLocation();
            if (location == null || location.trim().isEmpty()) {
                location = pathParser.getPath();
            }

            String categoryId = Utils.getCategoryFilterSchemeTerm(location, ConfigurationManager.DEFAULT_OWNER);


            // Query interface check, the last case of this check is for query for ex: /compute/-/ where we filter for a category (kind or mixin).
            if (pathParser.isInterfQuery()) {
                // Delegate to query interface method.
                return getQueryInterface(path, headers);
            }

            if (pathParser.isActionInvocationQuery()) {
                try {
                    response = outputParser.parseResponse("You cannot use an action with GET method.", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }

            Entity entity;
            String entityId = data.getEntityUUID();
            Map<String, String> attrs = data.getAttrs();
            if (entityId == null) {
                entityId = Utils.getUUIDFromPath(location, attrs);
            }


            // Get one entity check with uuid provided.
            // path with category/kind : http://localhost:8080/compute/uuid
            // custom location: http://localhost:8080/foo/bar/myvm/uuid
            if (pathParser.isEntityQuery()) {

                if (entityId == null) {
                    entity = ConfigurationManager.getEntityFromPath(location);

                } else {
                    entity = ConfigurationManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityId);
                    String locationTmp = ConfigurationManager.getEntityRelativePath(entityId);
                    locationTmp = locationTmp.replace(entityId, "");
                    locationTmp = Utils.getPathWithoutPrefixSuffixSlash(locationTmp);
                    String locationCompare = location.replace(entityId, "");
                    locationCompare = Utils.getPathWithoutPrefixSuffixSlash(locationCompare);

                    if (entity != null && !locationCompare.equals(locationTmp) && !ConfigurationManager.isCategoryReferencedOnEntity(categoryId, entity)) {
                        try {
                            response = outputParser.parseResponse("resource on " + path + " not found, entity exist but it is on another location : " + locationTmp, Response.Status.NOT_FOUND);
                            return response;
                        } catch (ResponseParseException ex) {
                            throw new InternalServerErrorException(ex);
                        }
                    }
                }

                if (entity != null) {
                    entity.occiRetrieve();

                    if (getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
                        try {
                            String locationTmp = ConfigurationManager.getLocation(entity);
                            List<String> locations = new ArrayList<>();
                            locations.add(locationTmp);
                            response = outputParser.parseResponse(locations);
                            return response;
                        } catch (ResponseParseException ex) {
                            // This must never go here. If that's the case this is a bug in parser.
                            throw new InternalServerErrorException(ex);
                        }
                    } else {
                        try {
                            response = outputParser.parseResponse(entity);
                            return response;
                        } catch (ResponseParseException ex) {
                            // This must never go here. If that's the case this is a bug in parser.
                            throw new InternalServerErrorException(ex);
                        }
                    }
                } else {
                    try {
                        response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }
            }

            if (pathParser.isCollectionQuery()) {
                // Collections part.
                response = getEntities(path);
                return response;
            } else {
                try {
                    response = outputParser.parseResponse("Unknown GET query type.", Response.Status.BAD_REQUEST);
                    return response;
                } catch (ResponseParseException ex) {
                    throw new InternalServerErrorException(ex);
                }
            }
        } // End for each data.

        return response;
    }

    /**
     * Get a query interface.
     *
     * @param path
     * @param headers
     * @return
     */
    @Override
    public Response getQueryInterface(String path, HttpHeaders headers) {
        Response response;

        response = super.getQueryInterface(path, headers);
        if (response != null) {
            return response;
        }
        String categoryFilter;
        categoryFilter = inputParser.getParameter("category");
        // First we check params.
        if (categoryFilter == null) {
            // Check if we need to filter for a category like /compute/-/, we get the term.
            categoryFilter = Utils.getCategoryFilter(path, ConfigurationManager.DEFAULT_OWNER);
        }

        response = outputParser.getInterface(categoryFilter, ConfigurationManager.DEFAULT_OWNER);
        return response;
    }

    @Override
    public Response getEntities(final String path) {
        Response response;
        String acceptType = getAcceptType();
        List<Entity> entities;
        try {
            try {
                entities = getEntityCollection(path);
            } catch (ConfigurationException ex) {
                LOGGER.error(ex.getMessage());
                response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                return response;
            }

            if (acceptType.equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
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
                // Update all the list of entities before setting response.
                for (Entity entityInf : entities) {
                    entityInf.occiRetrieve();
                    entitiesInf.add(entityInf);
                }
                response = outputParser.parseResponse(entitiesInf);
            }

        } catch (ResponseParseException ex) {
            // Must never happen if input query is ok.
            throw new InternalServerErrorException(ex);
        }
        return response;
    }

}
