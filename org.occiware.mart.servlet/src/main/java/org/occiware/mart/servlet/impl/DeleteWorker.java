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
import java.util.List;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class DeleteWorker extends ServletEntry {

    public DeleteWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {
        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        // if there is content so check it.
        occiRequest.validateInputDataRequest();
        if (occiResponse.hasExceptions()) {
            // Validation failed.
            return occiResponse.getHttpResponse();
        }
        if (occiRequest.isActionInvocationQuery()) {
            return occiResponse.parseMessage("You cannot use an action trigger with DELETE method.", HttpServletResponse.SC_BAD_REQUEST);
        }

        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (occiRequest.isInterfQuery() && datas.isEmpty()) {
            return occiResponse.parseMessage("you cannot use interface query that way on DELETE method, the interface must be used with remove mixin tag operation", HttpServletResponse.SC_BAD_REQUEST);
        }
        if (occiRequest.isInterfQuery() && datas.size() >= 1) {
            // Check if mixin tags datas.
            if (!isMixinTagsDatas()) {
                return occiResponse.parseMessage("you cannot use interface query that way on DELETE method, the interface must be used with remove mixin tag operation", HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // Remove mixin tag(s) operation.
        if (occiRequest.isInterfQuery() && isMixinTagsDatas()) {
            for (OCCIRequestData data : datas) {
                occiRequest.deleteMixinTag(data.getMixinTag());
                if (occiResponse.hasExceptions()) {
                    return resp;
                }
            }
            return resp;
        }


        resp = occiResponse.getHttpResponse();
        return resp;
    }

    private boolean isMixinTagsDatas() {
        boolean isMixinTags = true;
        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        for (OCCIRequestData data : datas) {
            if (data.getMixinTag() == null) {
                isMixinTags = false;
                break;
            }
        }
        return isMixinTags;
    }
}

