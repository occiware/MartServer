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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class PostWorker extends ServletEntry {
    public PostWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }
        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }

        // There is content so check it.
        occiRequest.validateInputDataRequest();
        if (occiResponse.hasExceptions()) {
            // Validation failed.
            return occiResponse.getHttpResponse();
        }

        List<OCCIRequestData> datas = occiRequest.getContentDatas();

        if (datas.isEmpty()) {
            return occiResponse.parseMessage("No content to post.", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isActionInvocationQuery() && occiRequest.isInterfQuery()) {
            return occiResponse.parseMessage("You cannot use action invocation with interface query", HttpServletResponse.SC_BAD_REQUEST);
        }

        OCCIRequestData contentData;

        // Partial update of the entity.
        if (occiRequest.isOnEntityLocation() && !occiRequest.isActionInvocationQuery()) {
            contentData = datas.get(0);
            if (contentData.getLocation() == null) {
                // override with request path.
                contentData.setLocation(occiRequest.getRequestPath());
            }
            // Update entity.
            occiRequest.updateEntity(contentData.getMixins(), contentData.getAttrsValStr(), contentData.getLocation());
            return resp;
        }

        // Mixin tag definition (this may be multiple definitions).
        // Defines if datas has only mixinTags definition.

        if (occiRequest.isInterfQuery() && datas.size() >= 1 && !occiRequest.isActionInvocationQuery()) {
            boolean isMixinTags = true;
            for (OCCIRequestData data : datas) {
                if (data.getMixinTag() == null) {
                    isMixinTags = false;
                    break;
                }
            }

            if (!isMixinTags) {
                return occiResponse.parseMessage("Request is malformed, to define mixin tags, the content body must be only mixin tags contents.", HttpServletResponse.SC_BAD_REQUEST);
            }
            // Define the mixintags.
            for (OCCIRequestData data : datas) {
                occiRequest.createMixinTag(data.getMixinTagTitle(), data.getMixinTag(), data.getLocation(), data.getXocciLocations());
                if (occiResponse.hasExceptions()) {
                    return resp;
                }
            }
            // All ok, mixin tags defined.
            occiResponse.parseResponseMessage("ok");
            return resp;
        }

        // Mixin tag association ==> Add entity to a mixin tag collection defined by user. like on /my_stuff/ --> entities location on xOcciLocations values.
        if (occiRequest.isOnMixinTagLocation() && !occiRequest.isActionInvocationQuery()) {

            // Add mixin tag defined to entities.
            // curl -v -X POST http://localhost:8080/my_stuff/ -H 'X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0'
            List<String> xOcciLocations;
            for (OCCIRequestData data : datas) {
                xOcciLocations = data.getXocciLocations();
                if (data.getMixinTag() != null) {
                    occiRequest.associateMixinToEntities(data.getMixinTag(), data.getLocation(), xOcciLocations);
                } else {
                    occiRequest.associateMixinToEntities(null, occiRequest.getRequestPath(), xOcciLocations);
                }
            }
            return resp;
        }

        // Create entity (or entity create collection) on /category/ (like /compute/ etc.) ==> for kind and mixin (not mixin tags).
        if (occiRequest.isOnCategoryLocation() && !occiRequest.isActionInvocationQuery()) {

            // Check if content are entity instance ==> have a kind defined, if not, return an error message => bad request.
            if (!occiRequest.areDatasEntities()) {
                return occiResponse.parseMessage("Some content datas are not identified as entity, please check your query", HttpServletResponse.SC_BAD_REQUEST);
            }

            // datas are identified as entities, check if that entities have the same kind/mixin (not mixintags) as the term category path.
            if (!occiRequest.areDatasHaveSameCategoryLocation()) {
                return occiResponse.parseMessage("Some entities cannot be created on this location : " + occiRequest.getRequestPath() + " the category term on location is not the same on entity", HttpServletResponse.SC_BAD_REQUEST);
            }

            String entityUUID;
            // Create (or full update) the entities on path : /categorypath/entityuuid/, if a location is set on content for each entities, the location override the requestpath/uuid.

            // First manage entity location.
            for (OCCIRequestData data : datas) {
                entityUUID = data.getEntityUUID();
                if (data.getLocation() == null && entityUUID != null) {
                    // Format the location for example like this : /compute/entityuuid/
                    data.setLocation(occiRequest.getRequestPath() + entityUUID + "/");
                }
                if (data.getLocation() == null && entityUUID == null) {
                    entityUUID = occiRequest.createUUID();
                    data.setEntityUUID(entityUUID);
                    // Format the location for example like this : /compute/newEntityUuid/
                    data.setLocation(occiRequest.getRequestPath() + entityUUID + "/");
                }
            }
            occiRequest.createEntities(datas);
            return resp;
        }

        // Create entities on custom location.
        if (occiRequest.isOnBoundedLocation() && !occiRequest.isActionInvocationQuery()) {

            // Check if content are entity instance ==> have a kind defined, if not, return an error message => bad request.
            if (!occiRequest.areDatasEntities()) {
                return occiResponse.parseMessage("Some content datas are not identified as entity, please check your query, the kind scheme term is mandatory", HttpServletResponse.SC_BAD_REQUEST);
            }
            String entityUUID;
            for (OCCIRequestData data : datas) {
                entityUUID = data.getEntityUUID();
                if (data.getLocation() == null && entityUUID != null) {
                    // Format the location for example like this : /mylocation/entityuuid/
                    data.setLocation(occiRequest.getRequestPath() + entityUUID + "/");
                }
                if (data.getLocation() == null && entityUUID == null) {
                    entityUUID = occiRequest.createUUID();
                    data.setEntityUUID(entityUUID);
                    // Format the location for example like this : /mylocation/newEntityUuid/
                    data.setLocation(occiRequest.getRequestPath() + entityUUID + "/");
                }
            }
            occiRequest.createEntities(datas);
            return resp;
        }

        // Action invocation part
        // ***********************

        if (occiRequest.isActionInvocationQuery()) {
            if (!occiRequest.isActionOnContentData()) {
                return occiResponse.parseMessage("Content must contain action category scheme and term", HttpServletResponse.SC_BAD_REQUEST);
            }
            // Content collection are not authorized with an action invocation query on entity location request path.
            if (occiRequest.isContentCollection()) {
                return occiResponse.parseMessage("Malformed content action collection, the uri must specify one action to execute", HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // Action invocation on entity location path.
        if (occiRequest.isOnEntityLocation() && occiRequest.isActionInvocationQuery()) {
            OCCIRequestData data = datas.get(0);
            List<String> locations = new LinkedList<>();
            locations.add(occiRequest.getRequestPath());
            occiRequest.executeActionOnEntities(data.getAction(), data.getAttrsValStr(), locations);
            return resp;
        }


        // Actions invocation on entity category collection path.
        if (occiRequest.isOnCategoryLocation() && occiRequest.isActionInvocationQuery()) {
            OCCIRequestData data = datas.get(0);
            String requestCategoryTerm = occiRequest.getRequestPath();
            if (requestCategoryTerm.startsWith("/")) {
                requestCategoryTerm = requestCategoryTerm.substring(1);
            }
            if (requestCategoryTerm.endsWith("/")) {
                requestCategoryTerm = requestCategoryTerm.substring(0, requestCategoryTerm.length() - 1);
            }
            occiRequest.executeActionOnCategory(data.getAction(), data.getAttrsValStr(), requestCategoryTerm);
            return resp;
        }

        // Action invocation on mixin tag defined collection.
        if (occiRequest.isOnMixinTagLocation() && occiRequest.isActionInvocationQuery()) {
            OCCIRequestData data = datas.get(0);
            Optional<String> optMixinTag = occiRequest.getMixinTagSchemeTermFromLocation(occiRequest.getRequestPath());
            String mixinTag;
            if (optMixinTag.isPresent()) {
                mixinTag = optMixinTag.get();
            } else {
                occiResponse.parseMessage("Unknown mixin tag location : " + occiRequest.getRequestPath(), HttpServletResponse.SC_NOT_FOUND);
                return resp;
            }
            occiRequest.executeActionOnMixinTag(data.getAction(), data.getAttrsValStr(), mixinTag);
            return resp;
        }


        // Action invocation on a custom path collection.
        if (occiRequest.isOnBoundedLocation() && occiRequest.isActionInvocationQuery()) {
            occiResponse.parseMessage("Triggering actions on custom path collection is not implemented at this time", HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        // If we are here this is an unknown request.
        occiResponse.parseMessage("The request is malformed", HttpServletResponse.SC_BAD_REQUEST);

        return resp;
    }

}
