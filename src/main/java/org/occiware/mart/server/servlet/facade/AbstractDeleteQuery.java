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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author cgourdin
 */
public abstract class AbstractDeleteQuery implements IDeleteQuery {
   @Context
   protected UriInfo uri;

    @Override
    public abstract Response inputQuery(String path, String entityId, HttpHeaders headers, HttpServletRequest servlet);

    @Override
    public abstract Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request);

    @Override
    public abstract Response deleteMixin(String mixinKind, String entityId, HttpHeaders headers, HttpServletRequest request);

    @Override
    public abstract Response deleteEntityCollection(String path, HttpHeaders headers, HttpServletRequest request);

    @Override
    public abstract Response deleteEntity(String kind, String entityId, HttpHeaders headers, HttpServletRequest request);
    
    protected UriInfo getUri() {
        return uri;
    }
}
