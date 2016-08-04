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
 * Global collections of entities, actions and mixins.
 * @author cgourdin
 */
//@Path("/collections")
public class RestCollections implements Serializable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestCollections.class);
    
//    @GET
//    @Path("/collections/entity")
//    public Response getEntityCollection(@Context HttpHeaders headers) {
//        Response response = Utils.checkClientOCCIVersion(headers);
//        if (response != null) {
//            return response;
//        }
//        
//        response = Response.status(404).entity("TO implement !!!").type(MediaType.TEXT_PLAIN).build();
//        
//        return response;
//        // TODO : List of all entities
//        
//    }
    
    
    
    
}
