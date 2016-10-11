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

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;

/**
 * Utility interface for parsing request entry and transform a response to
 * target media type.
 *
 * @author cgourdin
 */
public interface IRequestParser {

    /**
     * The main method to parse all the input query and load this utility
     * object.
     *
     * @param headers
     * @param request
     */
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException;

    /**
     * Fill the class with the kind, mixins scheme + term of resources
     *
     * @param headers
     * @param request
     * @throws CategoryParseException
     */
    public void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException;

    /**
     * Fill the class with all occiAttributes.
     *
     * @param headers
     * @param request
     * @throws AttributeParseException
     */
    public void parseOcciAttributes(final HttpHeaders headers, final HttpServletRequest request) throws AttributeParseException;

    /**
     * Parse the request parameters ex:
     * http://localhost:9090/myquery?attribute=myattributename or
     * http://localh...?category=mymixintag Pagination :
     * http://localhost:9090/myquery?attribute=myattributename&page=2&number=5
     * where page = current page, number : max number of items to display.
     *
     * @param request
     */
    public void parseRequestParameters(final HttpServletRequest request);

    /**
     * Get the kind scheme # + term.
     *
     * @return may return null if no kind is in the query.
     */
    public String getKind();

    /**
     * Get the action sheme + term.
     *
     * @return
     */
    public String getAction();

    /**
     * Get the scheme # + term of all mixins in parser.
     *
     * @return may return empty list if no mixin found.
     */
    public List<String> getMixins();

    /**
     * Get all occi attributes from input query.
     *
     * @return
     */
    public Map<String, String> getOcciAttributes();

    /**
     *
     * @return a universal unique identifier (v4).
     */
    public String getEntityUUID();

    /**
     * Parse the Object to a valid response with accept media type output.
     *
     * @param object, if object is instance of Response, this must be not
     * parsed.
     * @param status, Response.Status code representation, useful if object
     * parameter is a Response object or if there was a String object.
     * @return a response object to return to client.
     * @throws ResponseParseException
     */
    public Response parseResponse(Object object, Response.Status status) throws ResponseParseException;

    public Response parseResponse(Object object) throws ResponseParseException;

    /**
     * From /-/ or .wellknown, return a String on interface object.
     *
     * @param categoryFilter if path contains a category, interface is filtered
     * for this category.
     * @param user
     * @return a response object to return to client.
     */
    public Response getInterface(final String categoryFilter, final String user);

    /**
     * Set the kind scheme # + term.
     *
     * @param kind
     */
    public void setKind(String kind);

    /**
     * Set the action sheme + term.
     *
     * @param action
     */
    public void setAction(String action);

    /**
     * Set the scheme # + term of all mixins.
     *
     * @param mixins
     */
    public void setMixins(List<String> mixins);

    /**
     * Set all occi attributes from input query.
     *
     * @param attributes
     */
    public void setOcciAttributes(Map<String, String> attributes);

    public List<Kind> getKindsConf();

    public List<Mixin> getMixinsConf();

    /**
     * Assign the base uri to parser (for absolute path links).
     *
     * @param uri
     */
    public void setServerURI(URI uri);

    /**
     * Get base server uri.
     *
     * @return
     */
    public URI getServerURI();

    /**
     * Request parameters (set on inputquery method).
     *
     * @return
     */
    public Map<String, String> getRequestPameters();

    public void setRequestParameters(Map<String, String> parameters);

    public String getParameter(final String key);

}
