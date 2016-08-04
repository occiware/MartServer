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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.occiware.mart.server.servlet.utils.Utils;

/**
 *
 * @author cgourdin
 */
public abstract class AbstractGetQuery implements IGetQuery {

    @Context
    protected UriInfo uri;

    @Override
    public abstract Response getMixin(String mixinKind);

    @Override
    public Response getEntityCollection(String path, HttpHeaders headers, HttpServletRequest request) {
        System.out.println("getEntityCollection method.");
        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        System.out.println("Context root : " + contextRoot);
        System.out.println("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }

        return response;
    }

    @Override
    public Response getEntity(String path, String entityId, HttpHeaders headers, HttpServletRequest request) {
        System.out.println("getEntity method.");
        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        System.out.println("Context root : " + contextRoot);
        System.out.println("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }
        if (Utils.isUriListContentTypeUsed(headers)) {
            // We must here return a bad request.
            throw new BadRequestException("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/");
        }

        return response;
    }

    /**
     * Query interface client. This give all supported on extension to the
     * client. Concrete Implementation class give the output media type.
     *
     * @param headers
     * @return
     */
    @Override
    public Response getQueryInterface(HttpHeaders headers) {
        System.out.println("getQueryInterface method.");
        Response response;
        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }
        if (Utils.isUriListContentTypeUsed(headers)) {
            // We must here return a bad request.
            throw new BadRequestException("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/");
        }
        return null;
    }

    @Override
    public Response getEntityUriListing(String path, HttpHeaders headers, HttpServletRequest request) {
        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        System.out.println("GetEntityUriListing method.");
        System.out.println("Context root : " + contextRoot);
        System.out.println("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }

        return response;
    }

    protected UriInfo getUri() {
        return uri;
    }

}
