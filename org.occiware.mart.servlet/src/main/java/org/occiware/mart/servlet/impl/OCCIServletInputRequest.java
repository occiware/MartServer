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

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.AbstractOCCIApiInputRequest;
import org.occiware.mart.server.facade.OCCIApiInputRequest;
import org.occiware.mart.server.facade.OCCIApiResponse;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by cgourdin on 11/04/2017.
 */
public class OCCIServletInputRequest extends AbstractOCCIApiInputRequest implements OCCIApiInputRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletInputRequest.class);
    private final String contentType;
    private HeaderPojo headers;
    private HttpServletRequest request;
    private String requestPath;
    private Map<String, String> requestParameters;
    private boolean onEntityLocation = false;


    /**
     * Define if this is a collection location.
     */
    private boolean onCollectionLocation = false;
    private boolean onBoundedLocation = false;
    private boolean onCategoryLocation = false;
    private boolean onMixinTagLocation = false;
    /**
     * Collection of data content on input. True if this is a collection content (like entities) and false if not (one thing like one entity or one mixin tag data).
     */
    private boolean contentCollection = false;
    /**
     * Define if the path is an interface query /-/.
     */
    private boolean interfQuery;

    /**
     * Define if the query is an action query.
     */
    private boolean actionInvocationQuery;


    public OCCIServletInputRequest(OCCIApiResponse response, String contentType, String username, HttpServletRequest req, HeaderPojo headers, Map<String, String> requestParameters, IRequestParser inputParser) {
        super(username, response, inputParser);
        this.headers = headers;
        this.request = req;

        this.requestPath = req.getPathInfo();
        if (!requestPath.startsWith("/")) {
            requestPath = "/" + requestPath;
        }
        if (!requestPath.endsWith("/")) {
            requestPath = requestPath + "/";
        }
        this.requestParameters = requestParameters;
        this.contentType = contentType;

    }

    /**
     * Build the data objects for usage in PUT, GET etc. when call findEntity etc.
     */
    public void parseInput() throws ParseOCCIException {

        String content;
        // For all media type that have content occi build like json, xml, text plain, yml etc..
        if (request == null) {
            throw new ParseOCCIException("No request to parse.");
        }
        // Parse the path
        parsePath();

        // Parse the content body if any.
        switch (contentType) {
            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
            case Constants.MEDIA_TYPE_TEXT_PLAIN_OCCI:

                InputStream in = null;
                LOGGER.info("Parsing input uploaded datas...");
                try {
                    in = request.getInputStream();
                    if (in != null) {
                        // throw new ParseOCCIException("The input has no content delivered.");
                        content = Utils.convertInputStreamToString(in);
                        // for Object occiRequest to be fully completed.
                        getInputParser().parseInputToDatas(content);
                    }
                } catch (IOException ex) {
                    throw new ParseOCCIException("The server cant read the content input --> " + ex.getMessage());
                } finally {
                    Utils.closeQuietly(in);
                }
                break;
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                // For all media type that have header definition only, known for now is text/occi.
                if (headers != null && !headers.getHeaderMap().isEmpty()) {
                    // for object occiRequest to be fully completed, the parameter is Map<String, List<String>> encapsulated on MultivaluedMap.)
                    getInputParser().parseInputToDatas(headers);
                }
                break;
            default:
                throw new ParseOCCIException("Cannot parse for " + contentType + " cause: unknown parser");
        }

        if (getContentDatas().size() > 1) {
            this.contentCollection = true;
        }
    }

    /**
     * Parse the path to a data object. a path may have /category/myresource/
     */
    private void parsePath() {

        // Detect if this is an interface request.
        if (requestPath.equals("/.well-known/org/ogf/occi/-/") || requestPath.endsWith("/-/")) {
            interfQuery = true;
            return;
        }

        // Detect if this is an action invocation request
        if (requestParameters != null && requestParameters.get("action") != null) {
            actionInvocationQuery = true;
            if (isEntityLocation(requestPath)) {
                onEntityLocation = true;
            } else {
                // Action on collection : Category (kind, mixin on extension), mixin defined ?
                onCollectionLocation = true;
                if (isCategoryLocation(requestPath)) {
                    onCategoryLocation = true;
                } else if (isMixinTagLocation(requestPath)) {
                    onMixinTagLocation = true;
                } else {
                    onBoundedLocation = true; // Collection on a fragment path like /myresources/mycomputes/ ==> my entities below this path.
                }
            }
            return;
        }


        // Detect if this path is on an existing entity path.
        if (isEntityLocation(requestPath)) {
            onEntityLocation = true;
            return;
        }

        // Detect if the path is on a category (mixin, kind) like /myconnector/compute/ or /ipnetwork/ or on known path parent (bounded path).
        onCollectionLocation = true;
        if (isCategoryLocation(requestPath)) {
            onCategoryLocation = true;
        } else if (isMixinTagLocation(requestPath)) {
            onMixinTagLocation = true;
        } else {
            onBoundedLocation = true; // Collection on a fragment path like /myresources/mycomputes/ ==> my entities below this path.
        }
    }

    public HeaderPojo getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderPojo headers) {
        this.headers = headers;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }


    public boolean isOnCollectionLocation() {
        return onCollectionLocation;
    }

    public void setOnCollectionLocation(boolean onCollectionLocation) {
        this.onCollectionLocation = onCollectionLocation;
    }

    public boolean isInterfQuery() {
        return interfQuery;
    }

    public void setInterfQuery(boolean interfQuery) {
        this.interfQuery = interfQuery;
    }

    public boolean isActionInvocationQuery() {
        return actionInvocationQuery;
    }

    public void setActionInvocationQuery(boolean actionInvocationQuery) {
        this.actionInvocationQuery = actionInvocationQuery;
    }

    public boolean isOnEntityLocation() {
        return onEntityLocation;
    }

    public boolean isOnBoundedLocation() {
        return onBoundedLocation;
    }

    public boolean isOnCategoryLocation() {
        return onCategoryLocation;
    }

    public List<OCCIRequestData> getContentDatas() {
        return this.getInputParser().getInputDatas();
    }

    public boolean isContentCollection() {
        return contentCollection;
    }

    public boolean isOnMixinTagLocation() {
        return onMixinTagLocation;
    }

    /**
     * Helper method to control if there are entities instance on input datas.
     *
     * @return true if data objects are entities instance false if this is not the case.
     */
    public boolean areDatasEntities() {
        boolean result = true;
        List<OCCIRequestData> datas = getContentDatas();
        if (datas.isEmpty()) {
            return false;
        }
        for (OCCIRequestData data : datas) {
            if (data.getKind() == null) {
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Helper method to control if entities data have the category referenced in their kind / mixin from request path (category path).
     *
     * @return true if ok, false if not.
     */
    public boolean areDatasHaveSameCategoryLocation() {
        boolean result = true;
        List<OCCIRequestData> datas = getContentDatas();
        List<String> mixins;
        String kind;
        if (datas.isEmpty()) {
            return false;
        }
        String requestTerm = requestPath;
        if (requestTerm.startsWith("/")) {
            requestTerm = requestTerm.substring(1);
        }
        if (requestTerm.endsWith("/")) {
            requestTerm = requestTerm.substring(0, requestTerm.length() - 1);
        }
        Optional<String> optCat = super.getCategorySchemeTerm(requestTerm);
        String categoryId;
        if (optCat.isPresent()) {
            categoryId = optCat.get();
        } else {
            return false;
        }

        boolean found;
        for (OCCIRequestData data : datas) {
            // First control the kind.
            kind = data.getKind();
            mixins = data.getMixins();
            found = false;
            // If not the same, control the mixins.
            if (!kind.equals(categoryId)) {
                for (String mixin : mixins) {
                    if (mixin.equals(categoryId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result = false;
                    break;
                }

            }
        }
        return result;
    }

    /**
     * Helper to define if datas content have actions definitions.
     *
     * @return true if content data is action content.
     */
    public boolean isActionOnContentData() {
        boolean result = true;
        List<OCCIRequestData> datas = getContentDatas();
        if (datas.isEmpty()) {
            return false;
        }
        for (OCCIRequestData data : datas) {
            if (data.getAction() == null) {
                result = false;
                break;
            }
        }
        return result;
    }
}
