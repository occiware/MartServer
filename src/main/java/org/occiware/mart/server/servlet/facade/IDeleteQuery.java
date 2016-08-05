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
package org.occiware.mart.server.servlet.facade;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * 
 * @author Christophe Gourdin
 */
public interface IDeleteQuery extends IEntryPoint {
    
    /**
     * Remove an entity.
     * @param kind (ex: compute)
     * @param entityId (entity uuid)
     * @param headers
     * @param request
     * @return 
     */
    public Response deleteEntity(String kind, String entityId, HttpHeaders headers, HttpServletRequest request);
    
    /**
     * Remove an entire collection of entities with path = kind (ex: compute).
     * @param path
     * @param headers
     * @param request
     * @return 
     */
    public Response deleteEntityCollection(String path, HttpHeaders headers, HttpServletRequest request);
    
    /**
     * if on other methods parameter kind is a mixin, remove a mixin by calling this method.
     * @param mixinKind
     * @param entityId (entity uuid if any), if this is a mixin tag no entityId is provided and this will have null value.
     * @param request
     * @return a Response object.
     */
    public Response deleteMixin(String mixinKind, String entityId, HttpHeaders headers, HttpServletRequest request);
    
    
}
