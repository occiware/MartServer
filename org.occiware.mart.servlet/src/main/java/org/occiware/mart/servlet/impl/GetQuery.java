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

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.occiware.mart.servlet.exception.ResponseParseException;
import org.occiware.mart.servlet.facade.AbstractGetQuery;
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

        List<ContentData> contentDatas = inputParser.getDatas();
        ContentData contentData = null;
        if (!contentDatas.isEmpty()) {
            // Get only the first occurence. The others are ignored.
            contentData = contentDatas.get(0);
        }
        if (contentData == null) {
            try {
                response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                return response;
            } catch (ResponseParseException ex) {
                throw new InternalServerErrorException(ex);
            }
        }

        PathParser pathParser = new PathParser(contentData, path, inputParser.getRequestPameters());

        String location = pathParser.getLocation();
        if (location == null || location.trim().isEmpty()) {
            location = pathParser.getPath();
        }

        String categoryId = pathParser.getCategoryId();


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
        String entityId = contentData.getEntityUUID();
        Map<String, String> attrs = contentData.getAttrs();
        if (entityId == null) {
            entityId = EntityManager.getUUIDFromPath(location, attrs);
        }


        // Get one entity check with uuid provided.
        // path with category/kind : http://localhost:8080/compute/uuid
        // custom location: http://localhost:8080/foo/bar/myvm/uuid
        if (pathParser.isEntityQuery()) {

            if (entityId == null) {
                entity = EntityManager.findEntityFromLocation(location);

            } else {
                entity = EntityManager.findEntity(ConfigurationManager.DEFAULT_OWNER, entityId);
                if (entity == null) {
                    try {
                        response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                        return response;
                    } catch (ResponseParseException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }

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
                locationTmp = Utils.getPathWithoutPrefixSuffixSlash(locationTmp);
                String locationCompare = location.replace(entityId, "");
                locationCompare = Utils.getPathWithoutPrefixSuffixSlash(locationCompare);

                if (!locationCompare.equals(locationTmp) && !EntityManager.isCategoryReferencedOnEntity(categoryId, entity)) {
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
                        String locationTmp = EntityManager.getLocation(entity);
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
        if (categoryFilter == null || categoryFilter.trim().isEmpty()) {
            // Check if we need to filter for a category like /compute/-/, we get the term.
            categoryFilter = ConfigurationManager.getCategoryFilter(path, ConfigurationManager.DEFAULT_OWNER);
            if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
                LOGGER.warn("use a category filter: " + categoryFilter);
            }
        } else {
            LOGGER.warn("use a category filter: " + categoryFilter);
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
                    location = EntityManager.getLocation(entityTmp);
                    locations.add(location);
                }
                if (locations.isEmpty()) {

                    if (acceptType.equals(Constants.MEDIA_TYPE_JSON) || acceptType.equals(Constants.MEDIA_TYPE_JSON_OCCI)) {
                        response = outputParser.parseResponse("{ }");
                    } else {
                        response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                    }
                    return response;
                }
                response = outputParser.parseResponse(locations);
            } else {
                if (entities.isEmpty()) {
                    if (acceptType.equals(Constants.MEDIA_TYPE_JSON) || acceptType.equals(Constants.MEDIA_TYPE_JSON_OCCI)) {
                        response = outputParser.parseResponse(JsonOcciParser.EMPTY_JSON);
                    } else {
                        response = outputParser.parseResponse("resource " + path + " not found", Response.Status.NOT_FOUND);
                    }
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
