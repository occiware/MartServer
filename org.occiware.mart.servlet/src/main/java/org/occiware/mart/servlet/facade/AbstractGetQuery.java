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
package org.occiware.mart.servlet.facade;

import org.occiware.mart.servlet.utils.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * @author cgourdin
 */
public abstract class AbstractGetQuery extends AbstractEntryPoint implements IGetQuery {

    @Override
    public abstract Response getEntities(String path);

    @Override
    public Response getEntity(String path, String entityId, HttpHeaders headers, HttpServletRequest request) {
        if (ServletUtils.isUriListContentTypeUsed(headers)) {
            // We must here return a bad request.
            throw new BadRequestException("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/");
        }

        return null;
    }

    /**
     * Query interface client. This give all supported on extension to the
     * client. Concrete Implementation class give the output media type.
     *
     * @param path
     * @param headers
     * @return
     */
    @Override
    public Response getQueryInterface(String path, HttpHeaders headers) {
        System.out.println("getQueryInterface method.");

        if (ServletUtils.isUriListContentTypeUsed(headers)) {
            // We must here return a bad request.
            throw new BadRequestException("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/");
        }
        return null;
    }


}
