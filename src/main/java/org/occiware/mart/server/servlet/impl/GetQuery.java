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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.mart.server.servlet.facade.AbstractGetQuery;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cgourdin
 */
@Path("/")
public class GetQuery extends AbstractGetQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetQuery.class);

    
    @Path("{path:.*}/")
    @GET
    @Consumes({Constants.MEDIA_TYPE_TEXT_OCCI, Constants.MEDIA_TYPE_TEXT_URI_LIST})
    @Produces(Constants.MEDIA_TYPE_TEXT_OCCI)
    @Override
    public Response inputQuery(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        Response response;
        LOGGER.info("Get method in inputQuery() for path: " + path);
        response = super.inputQuery(path, headers, request);
        if (response != null) {
            // May have a malformed query.
            return response;
        }
        
        // Query interface check, the last case of this check is for query for ex: /compute/-/ where we filter for a category (kind or mixin).
        if (path.equals("-/") || path.equals(".well-known/org/ogf/occi/-/") || path.endsWith("/-/")) {
            // Delegate to query interface method.
            return getQueryInterface(path, headers);
        }
        
        
        // Get entity check.
        
        
        // Get Mixin definition check.
        
        
        
        // Get Collection check like compute/*
        
        
        
        return Response.ok().build();
    }
    
    /**
     *
     * @param path
     * @param entityId
     * @param headers
     * @param request, use only for json and file upload features (use
     * request.getReader() to retrieve json String).
     * @return
     */
    @Override
//    @Path("{path}/{id}") // {a}/{b}/{id}
//    @GET
//    @Consumes({Constants.MEDIA_TYPE_TEXT_OCCI, Constants.MEDIA_TYPE_TEXT_URI_LIST})
//    @Produces(Constants.MEDIA_TYPE_TEXT_OCCI)
    public Response getEntity(@PathParam("path") String path, @PathParam("id") String entityId, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        
//        if (path.equals("collections")) {
//            return getEntityCollection(entityId, headers, request);
//        }
        
        // Manage occi server version and other things before processing the query.
        Response response = super.getEntity(path, entityId, headers, request);
        // If something goes wrong, the response here is not null.
        if (response != null) {
            return response;
        }

        String pathMsg = "Path given : " + Constants.PATH_SEPARATOR + path + "\n "; // + PATH_SEPARATOR + pathB + PATH_SEPARATOR + id;
        response = Response.ok().
                entity(pathMsg).
                header("Server", Constants.OCCI_SERVER_HEADER).build();
        return response;
    }

    @Override
    @Path("collections/{kind}") // {a}/{b}/{id}
    @GET
    @Consumes({Constants.MEDIA_TYPE_TEXT_OCCI, Constants.MEDIA_TYPE_TEXT_URI_LIST})
    @Produces(Constants.MEDIA_TYPE_TEXT_OCCI)
    public Response getEntityCollection(@PathParam("kind") String kind, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        Response response;

        String pathMsg = "Collections kind given : " + Constants.PATH_SEPARATOR + "collections/" + kind + "\n ";
        response = Response.ok().
                entity(pathMsg).
                header("Server", Constants.OCCI_SERVER_HEADER).build();
        return response;
    }

    @Override
    @Path("{path}")
    @GET
    @Produces(Constants.MEDIA_TYPE_TEXT_URI_LIST)
    public Response getEntityUriListing(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest request) {
        
        Response response = null;
        
        // Manage /-/ relative path for listing the full query interface.
        if (super.getUri().getPath().equals("-/")) {
            return getQueryInterface(path, headers);
        }

        String msg = "ok";
        response = Response.ok().
                entity(msg).
                header("Server", Constants.OCCI_SERVER_HEADER).build();
        return response;
    }

    @Override
    public Response getMixin(String mixinKind) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // With well known method.
    @Override
    // @Path("/.well-known/org/ogf/occi/-/")
    public Response getQueryInterface(String path, HttpHeaders headers) {
        Response response;

        response = super.getQueryInterface(path, headers);
        if (response != null) {
            return response;
        }
        // Check if we need to filter for a category like /compute/-/
        String categoryFilter = Utils.getCategoryFilter(ConfigurationManager.DEFAULT_OWNER, path);
        
        response = inputParser.getInterface(categoryFilter, ConfigurationManager.DEFAULT_OWNER);
        return response;
        // String msg = "ok";

        // String pathMsg = "Path given : " + Constants.PATH_SEPARATOR + "-/" + "\n "; // + PATH_SEPARATOR + pathB + PATH_SEPARATOR + id;
//        response = Response.ok().
//                entity(msg).
//                header("Server", Constants.OCCI_SERVER_HEADER).build();
                
//        return response;
    }
    
}
