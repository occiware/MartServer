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
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class GetWorker extends ServletEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetWorker.class);

    public GetWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        if (!occiRequest.getContentDatas().isEmpty()) {
            return occiResponse.parseMessage("Input content are not accepted with GET method", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isActionInvocationQuery()) {
            LOGGER.warn("Querying action invocation on GET method.");
            return occiResponse.parseMessage("You cannot use an action with GET method", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isInterfQuery()) {
            LOGGER.info("Querying the interface on path : " + occiRequest.getRequestPath());
            occiRequest.getModelsInterface(getRequestParameters().get(Constants.CATEGORY_KEY), getRequestParameters().get(Constants.EXTENSION_NAME_KEY));
            return resp;
        }

        if (occiRequest.isOnEntityLocation() || occiRequest.isOnCollectionLocation()) {

            LOGGER.info("Querying entities on location : " + occiRequest.getRequestPath());
            if (occiRequest.isOnCollectionLocation()) {
                if (!getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
                    occiRequest.findEntities(occiRequest.getRequestPath(), buildCollectionFilter());
                } else {
                    occiRequest.findEntitiesLocations(occiRequest.getRequestPath(), buildCollectionFilter());
                }
            }
            if (occiRequest.isOnEntityLocation()) {
                if (!getAcceptType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
                    occiRequest.findEntity(occiRequest.getRequestPath());
                } else {
                    occiRequest.findEntitiesLocations(occiRequest.getRequestPath(), buildCollectionFilter());
                }
            }

            return resp;
        }
        occiResponse.parseMessage("The request is malformed", HttpServletResponse.SC_BAD_REQUEST);
        return resp;
    }


}
