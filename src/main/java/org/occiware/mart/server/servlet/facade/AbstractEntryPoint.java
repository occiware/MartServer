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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.impl.parser.ParserFactory;
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
        System.out.println("Context root : " + contextRoot);
        System.out.println("URI relative path: " + uriPath);
        
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
            try {
                inputParser.parseInputQuery(headers, request);
            } catch (AttributeParseException | CategoryParseException ex) {
                LOGGER.error(ex.getMessage());
            }
        }
        
        return response;
    }
    
    protected UriInfo getUri() {
        return uri;
    }
    
}
