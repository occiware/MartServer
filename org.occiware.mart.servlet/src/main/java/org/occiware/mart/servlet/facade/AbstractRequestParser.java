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

import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.parser.Data;
import org.occiware.mart.servlet.exception.AttributeParseException;
import org.occiware.mart.servlet.exception.CategoryParseException;
import org.occiware.mart.servlet.exception.ResponseParseException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe Gourdin
 */
public abstract class AbstractRequestParser implements IRequestParser {

    private List<Data> datas = new LinkedList<>();

    // For interface, used in configurations.
    private List<Kind> kindsConf = null;
    private List<Mixin> mixinsConf = null;

    /**
     * Uri of the server.
     */
    private URI serverURI;

    /**
     * If parameters are in inputquery, it must declared here. Key: name of the
     * parameter Value: Value of the parameter (if url ==> decode before set the
     * value).
     */
    private Map<String, String> parameters = new HashMap<>();

    @Override
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        // get the kind and mixins from query.
        parseOcciCategories(headers, request);
        // Get the occi attributes defined in query.
        parseOcciAttributes(headers, request);
        // Parse request parameters (for filtering, for pagination or for action with parameters.
        parseRequestParameters(request);
    }

    @Override
    public abstract void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException;

    @Override
    public abstract void parseOcciAttributes(HttpHeaders headers, HttpServletRequest request) throws AttributeParseException;

    /**
     * Default with response status ok.
     *
     * @param object
     * @return
     * @throws ResponseParseException
     */
    @Override
    public Response parseResponse(Object object) throws ResponseParseException {
        return parseResponse(object, Response.Status.OK);
    }

    @Override
    public abstract Response parseResponse(Object object, Response.Status status) throws ResponseParseException;

    @Override
    public abstract Response parseEmptyResponse(final Response.Status status);

    /**
     * Return input data for an entity uuid if exist, if not exist, return null.
     *
     * @param entityUUID
     * @return
     */
    @Override
    public Data getInputDataForEntityUUID(String entityUUID) {
        List<Data> tmp = getDatas();
        Data dataToReturn = null;
        String entityUUIDCompare;
        for (Data data : tmp) {
            if (data != null) {
                entityUUIDCompare = data.getEntityUUID();

                if (entityUUIDCompare != null && entityUUIDCompare.equals(entityUUID)) {
                    dataToReturn = data;
                }
            }
        }
        return dataToReturn;

    }

    @Override
    public List<Data> getDatas() {
        if (datas == null) {
            datas = new LinkedList<>();
        }
        return datas;
    }

    @Override
    public void setDatas(List<Data> datas) {
        this.datas = datas;
    }

}
