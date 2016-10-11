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

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.MixinNotFoundOnModelException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.impl.parser.ParserFactory;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cgourdin
 */
public abstract class AbstractEntryPoint implements IEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntryPoint.class);

    @Context
    protected UriInfo uri;
    protected IRequestParser inputParser;

    private String contentType = "text/occi";

    private String acceptType = "text/occi";

    protected IRequestParser outputParser;

    @Override
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request) {

        Response response;
        String contextRoot = getUri().getBaseUri().toString();
        String uriPath = getUri().getPath();
        LOGGER.info("Context root : " + contextRoot);
        LOGGER.info("URI relative path: " + uriPath);

        // Get Client user agent to complain with http_protocol spec, control the occi version if set by client.
        response = Utils.checkClientOCCIVersion(headers);
        if (response != null) {
            return response;
        }

        if (response == null) {
            // Load the parsers.
            // check content-type header.
            contentType = Utils.findContentTypeFromHeader(headers);
            acceptType = Utils.findAcceptTypeFromHeader(headers);
            inputParser = ParserFactory.build(contentType);
            outputParser = ParserFactory.build(acceptType);
            inputParser.setServerURI(uri.getBaseUri());
            outputParser.setServerURI(uri.getBaseUri());
            try {
                inputParser.parseInputQuery(headers, request);

                // Check if attributes are in configuration model for kind and mixins.
                if (!inputParser.getOcciAttributes().isEmpty()) {
                    // if there are attributes so there is a kind / mixins.
                    // Check on configuration and used extension.
                    boolean checkAttrs = false;
                    boolean hasError = false;
                    String error = null;

                    Kind kindModel = ConfigurationManager.findKindFromExtension(ConfigurationManager.DEFAULT_OWNER, inputParser.getKind());
                    if (kindModel == null) {
                        error = "The kind : " + inputParser.getKind() + " doesnt exist on referenced extensions";
                        hasError = true;
                    }
                    List<Mixin> mixinsModel = new LinkedList<>();
                    try {
                        mixinsModel = Utils.loadMixinFromSchemeTerm(inputParser.getMixins());
                    } catch (MixinNotFoundOnModelException ex) {

                        if (error != null) {
                            error += " \n ";
                        }
                        error += "Mixin model error cause : " + ex.getMessage();
                        hasError = true;
                    }

                    if (!hasError && !Utils.checkIfMixinAppliedToKind(mixinsModel, kindModel)) {
                        if (error != null) {
                            error += " \n ";
                        }
                        error += "Some mixins doesnt apply to kind : " + inputParser.getKind();
                        hasError = true;
                    }

                    if (!hasError) {

                        if (!Utils.checkIfAttributesExistOnKindMixin(inputParser.getOcciAttributes(), kindModel, mixinsModel)) {
                            if (error != null) {
                                error += " \n ";
                            }
                            error += "Some attributes doesnt exist on kind and mixins : " + inputParser.getKind();
                            hasError = true;
                        }

                    }

                    if (hasError) {
                        LOGGER.error(error);
                        try {
                            response = outputParser.parseResponse(error, Response.Status.BAD_REQUEST);
                        } catch (ResponseParseException e) {
                            throw new BadRequestException();
                        }
                        throw new BadRequestException("Error while parsing input query, some attributes doesnt exist on used extensions.");
                    }

                }

            } catch (AttributeParseException | CategoryParseException ex) {
                LOGGER.error(ex.getMessage());
                try {
                    response = outputParser.parseResponse("Error while parsing input query", Response.Status.BAD_REQUEST);
                } catch (ResponseParseException e) {
                    throw new BadRequestException();
                }
            }
        }

        return response;
    }

    protected UriInfo getUri() {
        return uri;
    }

    @Override
    public String getAcceptType() {
        return acceptType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

}
