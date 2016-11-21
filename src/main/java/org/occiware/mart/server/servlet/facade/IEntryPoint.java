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
package org.occiware.mart.server.servlet.facade;

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * This describe method for server first entry, one point method to redirect.
 * All the query interface extends that interface.
 *
 * @author Christophe Gourdin
 */
interface IEntryPoint {

    /**
     * All queries inputs here. for relative path without setting a category on
     * path.
     *
     * @param path
     * @param headers
     * @param request
     * @return
     */
    Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request);

    String getContentType();

    String getAcceptType();

    /**
     * Get a list of entities from a category request or inbound path request.
     * @param path
     * @return
     * @throws ConfigurationException
     */
    List<Entity> getEntityCollection(final String path) throws ConfigurationException;

}
