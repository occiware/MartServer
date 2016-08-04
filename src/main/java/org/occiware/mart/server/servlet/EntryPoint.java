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
package org.occiware.mart.server.servlet;

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Master class, this class manage all entries on this server and delegate entries following the media type hierarchy.
 * Supported media types : application/json, text/occi. other like xml, html, plain/text will come later.
 * @author Christophe Gourdin
 */
// @Path("/")
public class EntryPoint implements Serializable {
   private static final Logger LOGGER = LoggerFactory.getLogger(EntryPoint.class);
   
   
//   @Context
//   private UriInfo uri;
//   
//   // *****************
//   // Request GET part.
//   // *****************
//   
//   /**
//    * Get an entity, return its id and other informations.
//    * @param path
//    * @param id
//    * @param headers
//    * @return 
//    */
//    @Path("{path}/{id}") // {a}/{b}/{id}
//    @GET
//    @Consumes(Constants.MEDIA_TYPE_TEXT_OCCI)
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response getResourceFromPath(@PathParam("path") String path, @PathParam("id") String id, @Context HttpHeaders headers) { //  @PathParam("b") String pathB, @PathParam("id") String id, 
//        
//        String contextRoot = getUri().getBaseUri().toString();
//        String uriPath = getUri().getPath();
//        System.out.println("Context root : " + contextRoot);
//        System.out.println("URI relative path: " + uriPath);
//        
//        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response;
//        }
//        
//        // TODO : Log headers for debug.
//        
////        MultivaluedMap<String, String> headersVal = headers.getRequestHeaders();
////        
////        
////        for (Map.Entry<String, List<String>> entry : headersVal.entrySet()) {
////            String key = entry.getKey();
////            List<String> values = entry.getValue();
////            System.out.println("Header Key: " + key);
////            System.out.println("Header values : ");
////            for (String val : values) {
////                System.out.println("Val: " + val);
////            }
////            
////        }
//        String pathMsg = "Path given : " + Constants.PATH_SEPARATOR + path + "\n "; // + PATH_SEPARATOR + pathB + PATH_SEPARATOR + id;
//        response = Response.ok().
//                entity(pathMsg).
//                header("Server", Constants.OCCI_SERVER_HEADER).build();   
//        return response;
//    }
//    
//    /**
//     * Get a list of entities and mixin from a relative path like : /compute/
//     * @param path
//     * @param headers
//     * @return 
//     */
//    @GET
//    @Path("{path}")
//    @Consumes(Constants.MEDIA_TYPE_TEXT_OCCI)
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response getCollectionCategory(@PathParam("path") String path, @Context HttpHeaders headers) {
//        // TODO : filters and pagination.
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response;
//        }
//        
//        // TODO !!!
//        
//        response = Response.status(404).entity("TO implement !!!").type(MediaType.TEXT_PLAIN).build();
//        return response;
//    }
//    
//    /**
//     * 
//     * @param path
//     * @param headers
//     * @return 
//     */
//    @GET
//    @Path("/-/")
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response getQueryInterfaces(@PathParam("path") String path, @Context HttpHeaders headers) {
//        // TODO : filters and pagination.
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response;
//        }
//        
//        // TODO !!!
//        
//        response = Response.status(404).entity("TO implement !!!").type(MediaType.TEXT_PLAIN).build();
//        return response;
//    }
//    
//    
//    
//    // *****************
//    // Request PUT part.
//    // *****************
//    /**
//     * Create a new entity (Resource or Link).
//     * @param path
//     * @param id
//     * @param headers
//     * @return 
//     */
//    @PUT
//    @Path("{path}/{id}")
//    @Consumes(Constants.MEDIA_TYPE_TEXT_OCCI)
//    @Produces(MediaType.TEXT_PLAIN)
//    public String createEntity(@PathParam("path") String path, @PathParam("id") String id, @Context HttpHeaders headers) {
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response.getEntity().toString();
//        }
//        LOGGER.info("UUID for entity : " + id);
//        String entityId = Constants.PATH_SEPARATOR + path + Constants.PATH_SEPARATOR + id;
//        String kind = Utils.getKindFromHeader(headers);
//        // Get the entity attributes.
//      //   Map<String, String> attributes = Utils.getAttributesFromHeader(headers);
//        // Get the mixins (and mixins attributes).
//        // List<String> mixin = Utils.getMixinFromHeader(headers);
//        // Map<String, String> mixinAttributes = Utils.getMixinAttributesFromHeader(headers);
//        
//        response = Response.status(404).entity("TO implement !!!").type(MediaType.TEXT_PLAIN).build();
//        return response.getEntity().toString();
//        
//    }
//    
//    /**
//     * Create a new entity (Resource or link) with no id provided. uuid is created here.
//     * @param path
//     * @param headers
//     * @return 
//     */
//    @PUT
//    @Path("{path}")
//    @Consumes(Constants.MEDIA_TYPE_TEXT_OCCI)
//    @Produces(MediaType.TEXT_PLAIN)
//    public String createEntity(@PathParam("path") String path, @Context HttpHeaders headers) {
//        
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response.getEntity().toString();
//        }
//        
//        // TODO : Authentication sequence before this call.
//        
//        // Get the id.
//        String uuid = Utils.createUUID();
//        LOGGER.info("UUID for entity : " + uuid);
//        String entityId = Constants.PATH_SEPARATOR + path + Constants.PATH_SEPARATOR + uuid;
//        // Get the kind.
//        String kind = Utils.getKindFromHeader(headers);
//        // Get the entity attributes.
//        // Map<String, String> attributes = Utils.getAttributesFromHeader(headers);
//        
//        // Get the mixins (and mixins attributes).
//        // List<String> mixin = Utils.getMixinFromHeader(headers);
//        // Map<String, String> mixinAttributes = Utils.getMixinAttributesFromHeader(headers);
//        
//        
//        // TODO !!!
//        
//        response = Response.status(404).entity("TO implement !!!").type(MediaType.TEXT_PLAIN).build();
//        return response.getEntity().toString();
//        
//    }
//    
//    
//    
//    
//    
//   
//    private UriInfo getUri() {
//        return uri;
//    }
    
    
}
