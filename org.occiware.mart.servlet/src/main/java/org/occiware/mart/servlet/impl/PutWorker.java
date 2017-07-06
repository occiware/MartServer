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
package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class PutWorker extends ServletEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PutWorker.class);

    public PutWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    /**
     * Put request authorized only the creation of mixin tag and entity (only one).
     *
     * @return
     */
    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            // Authentication failed.
            return occiResponse.getHttpResponse();
        }

        String requestPath = occiRequest.getRequestPath();

        // Root request are not allowed by PUT method ==> 405 http error.
        if (requestPath.trim().isEmpty() || requestPath.equals("/")) {
            return occiResponse.parseMessage("This uri : " + occiRequest.getRequestPath() + " is not supported by HTTP PUT method.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        if (occiRequest.isInterfQuery()) {
            return occiResponse.parseMessage("you cannot use interface query on PUT method", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (occiRequest.isOnCategoryLocation()) {
            return occiResponse.parseMessage("The category collection : " + requestPath + " is not supported by HTTP PUT method.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }


        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        if (datas.isEmpty()) {
            return occiResponse.parseMessage("No content to put.", HttpServletResponse.SC_BAD_REQUEST);
        }

        // There is content so check it.
        occiRequest.validateInputDataRequest();
        if (occiResponse.hasExceptions()) {
            // Validation failed.
            return occiResponse.getHttpResponse();
        }

        if (occiRequest.isActionInvocationQuery()) {
            LOGGER.warn("Querying action invocation on PUT method.");
            return occiResponse.parseMessage("You cannot use an action with PUT method.", HttpServletResponse.SC_BAD_REQUEST);
        }

        String location;
        OCCIRequestData data = datas.get(0);

        if (data.getMixinTag() != null) {
            LOGGER.warn("Querying mixin tag definition with PUT method.");
            return occiResponse.parseMessage("you cannot use mixin tag definition with PUT method, please use POST method for mixin tag definition.", HttpServletResponse.SC_BAD_REQUEST);
        }

        // Manage entity data.
        if (occiRequest.isOnEntityLocation() || occiRequest.isOnBoundedLocation()) {
            // Check if PUT has more than one content data and datas doesnt contain mixin tags.
            if (occiRequest.isContentCollection()) {
                return occiResponse.parseMessage("Content has more than one entity to put, this is not authorized on HTTP PUT request, please use POST request for entity collection creation.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            location = data.getLocation();
            // if no location set on request content, the location is the request path (relative).
            if (location == null) {
                // Location has not been set on content, this is set on request path like /mycompute/myentity/.
                data.setLocation(requestPath);
                location = requestPath;
            }
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            // If the data location is different from request path, throw a location conflict exception.
            if (!requestPath.equals(location)) {
                // return an error.
                return occiResponse.parseMessage("Location attribute must be the same as request path.", HttpServletResponse.SC_CONFLICT);
            }
            // This is an entity creation query.
            occiRequest.createEntity(data.getEntityTitle(), data.getEntitySummary(), data.getKind(), data.getMixins(), data.getAttrsValStr(), data.getLocation());
            if (!occiResponse.hasExceptions()) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }
            String locationRender = data.getLocation().substring(0, data.getLocation().length() - 1); // This is an entity remove ending slash.
            resp.addHeader("Location", getServerURI().toString() + locationRender);
            return resp;
        }

        // Reference collection entities on mixin tag (replace all mixin tag referenced collection of entities).
        if (occiRequest.isOnMixinTagLocation()) {
            Optional<String> optMixinTag = occiRequest.getMixinTagSchemeTermFromLocation(requestPath);
            String mixinTag;
            if (optMixinTag.isPresent()) {
                mixinTag = optMixinTag.get();
            } else {
                occiResponse.parseMessage("Unknown mixin tag location : " + requestPath, HttpServletResponse.SC_NOT_FOUND);
                return resp;
            }
            List<String> xOcciLocations = data.getXocciLocations();
            // X occi location may be empty, this will remove all entity instance for this mixin tag.
            occiRequest.replaceMixinTagCollection(mixinTag, xOcciLocations);
            return resp;
        }

        // If we are here this is an unknown request.
        occiResponse.parseMessage("The request is malformed", HttpServletResponse.SC_BAD_REQUEST);

        return resp;
    }
}
