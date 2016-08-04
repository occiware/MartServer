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
package org.occiware.mart.server.servlet.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.occiware.mart.server.servlet.facade.AbstractGetQuery;
import org.occiware.mart.server.servlet.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cgourdin
 */
// @Path("/")
public class GetQuery  { // extends AbstractGetQuery
    private static final Logger LOGGER = LoggerFactory.getLogger(GetQuery.class);

//    @Path("{path}/{id}") // {a}/{b}/{id}
//    @GET
//    @Consumes({MediaType.APPLICATION_JSON, Constants.MEDIA_TYPE_TEXT_URI_LIST})
//    @Produces(MediaType.APPLICATION_JSON)
//    @Override
//    public Response getEntity(@PathParam("path") String path,@PathParam("id") String entityId, @Context HttpHeaders headers, @Context HttpServletRequest request) {
//        // Manage occi server version and other things before processing the query.
//        Response response = super.getEntity(path, entityId, headers, request);
//        // If something goes wrong, the response here is not null.
//        if (response != null) {
//            return response;
//        }
//        try {
//            InputStream in = request.getInputStream();
//            if (in == null) {
//                response = Response.noContent().
//                entity("No input content").
//                header("Server", Constants.OCCI_SERVER_HEADER).build();
//                return response;
//            }
//            ObjectMapper  mapper = new ObjectMapper();
//            
//            
//            // Map<String, Object> jsonMap = mapper.readValue(in, Map.class);
//            
//            
//            JsonNode rootNode = mapper.readTree(in);
//            LOGGER.info("Root node : " + rootNode.asText());
//            Iterator<Map.Entry<String, JsonNode>> nodeIterator = rootNode.fields();
//            while (nodeIterator.hasNext()) {
//                Map.Entry<String, JsonNode> entry = nodeIterator.next();
//                LOGGER.info("Key --> " + entry.getKey() + " value: " + entry.getValue());
//                
//            }
//            
//            
////            
////            for (JsonNode node : rootNode) {
////                
////                LOGGER.info("Node: " + node.asText());
////            }
//            
//            // String resultRoot = rootNode.asText();
//            LOGGER.info("Read nodes finished !");
//            
//        } catch (IOException ex) {
//            
//        }
//        
//        
//        
//        
//       // String jsonEntryStr;
//        
//        String pathMsg = "{Path given: " + Constants.PATH_SEPARATOR + path + "\n }"; // + PATH_SEPARATOR + pathB + PATH_SEPARATOR + id;
//        
//        response = Response.ok().
//                entity(pathMsg).
//                header("Server", Constants.OCCI_SERVER_HEADER).build();
//        return response;
//        
//        
//    }
//
//    @Override
//    public Response getEntityCollection(String path, HttpHeaders headers, HttpServletRequest request) {
//        
//        
//        
//        return super.getEntityCollection(path, headers, request); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Response getMixin(String mixinKind) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Response getQueryInterface(HttpHeaders headers) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public Response getEntityUriListing(String path, HttpHeaders headers, HttpServletRequest request) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//    
    
    
}
