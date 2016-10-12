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

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.facade.AbstractPostQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * execute actions, update attributes on entities, update mixin tag associations etc.
 * @author Christophe Gourdin
 */
@Path("/")
public class PostQuery extends AbstractPostQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostQuery.class);
    
    @Path("{path:.*}")
    @POST
    @Override
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request) {
        LOGGER.info("--> Call POST method input query for relative path mode --> " + path);
        // Check header, load parser, and check occi version.
        Response response = super.inputQuery(path, headers, request);
        if (response != null) {
            // There was a badrequest, the headers are maybe malformed..
            return response;
        }
        
        // Determine if: 
        //  - update an entity, 
        //  - update a collection of entities 
        //  - Define a new mixin tag
        //  - update a mixin association (mixin tag included) 
        //  - execute an action on entity
        //  - execute an action on entity collection
        
        
        
        return response;
    }

    @Override
    public Response updateEntity(String path, Entity entity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response updateEntityCollection(String path, List<Entity> entities) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response defineMixinTag(String mixinTagKind, String relativeLocationApply) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response updateMixinTagAssociation(String mixinTagKind, String relativeLocationApply) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response.ResponseBuilder executeAction(String actionKind, Entity entity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response executeActionsOnEntities(String actionKind, List<Entity> entity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
