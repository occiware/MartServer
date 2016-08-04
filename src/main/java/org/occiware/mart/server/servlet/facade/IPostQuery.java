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
public interface IPostQuery {
    
    /**
     * Create a new entity.
     * @param path
     * @param entityId
     * @param headers
     * @param request
     * @return 
     */
    public Response updateEntity(String path, String entityId, HttpHeaders headers, HttpServletRequest request);
    
    public Response updateEntityCollection(String path, HttpHeaders headers, HttpServletRequest request);
    
    public Response updateMixin(String mixinKind, HttpHeaders httpHeaders, HttpServletRequest request);
    
}
