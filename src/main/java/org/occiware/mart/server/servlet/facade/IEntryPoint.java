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
package org.occiware.mart.server.servlet.facade;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * This describe method for server first entry, one point method to redirect. All the query interface extends that interface.
 * @author Christophe Gourdin
 */
public interface IEntryPoint {
    /**
     * All queries inputs here. for relative path without setting a category on path.
     * @param path
     * @param headers
     * @param request
     * @return 
     */
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request);
    
    /**
     * All queries inputs here. for category or single input section like "/category/entityuuid" or "/foo/myEntityname".
     * @param path
     * @param entityId
     * @param headers
     * @param servlet
     * @return 
     */
    public Response inputQuery(String path, String entityId, HttpHeaders headers, HttpServletRequest servlet);
    
    
}
