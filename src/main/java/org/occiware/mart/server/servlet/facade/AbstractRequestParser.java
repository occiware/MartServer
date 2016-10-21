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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;

/**
 *
 * @author Christophe Gourdin
 */
public abstract class AbstractRequestParser implements IRequestParser {

    private List<InputData> inputDatas = new LinkedList<>();

    // For interface, used in configurations.
    protected List<Kind> kindsConf = null;
    protected List<Mixin> mixinsConf = null;

    /**
     * Uri of the server.
     */
    protected URI serverURI;

    /**
     * If parameters are in inputquery, it must declared here. Key: name of the
     * parameter Value: Value of the parameter (if url ==> decode before set the
     * value).
     */
    protected Map<String, String> parameters = new HashMap<>();

    @Override
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        // get the kind and mixins from query.
        parseOcciCategories(headers, request);
        // Get the occi attributes defined in query.
        parseOcciAttributes(headers, request);
        // Parse request parameters (for filtering, for pagination or for action with parameters.
        parseRequestParameters(request);
    }

    /**
     * Get here the parameters of a request, this can be filters, action
     * parameters, pagination as describe here. Parse the request parameters
     * filtering : http://localhost:9090/myquery?attribute=myattributename or
     * http://localh...?category=mymixintag usage with category or attributes.
     * Pagination :
     * http://localhost:9090/myquery?attribute=myattributename&page=2&number=5
     * where page = current page, number : max number of items to display.
     * Request on collections are possible with http://localhost:9090/compute/
     * with text/uri-list accept type, give the full compute resources created
     * uri. Request on collections if no text/uri-list given, return in response
     * the entities defined in detail like a get on each entity (if text/occi
     * this may return a maximum of 3 entities not more due to limitations size
     * of header area in http).
     *
     * @param request
     */
    @Override
    public void parseRequestParameters(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        String key;
        String[] vals;
        String val;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                key = entry.getKey();
                vals = entry.getValue();
                if (vals != null && vals.length > 0) {
                    val = vals[0];
                    parameters.put(key, val);
                }
            }
        }

    }

    @Override
    public abstract void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException;

    @Override
    public abstract void parseOcciAttributes(HttpHeaders headers, HttpServletRequest request) throws AttributeParseException;

    @Override
    public abstract Response parseResponse(Object object) throws ResponseParseException;

    @Override
    public abstract Response parseResponse(Object object, Response.Status status) throws ResponseParseException;

    /**
     * Be warned that categoryFilter is the term only.
     *
     * @param categoryFilter
     * @param user
     * @return
     */
    @Override
    public Response getInterface(final String categoryFilter, final String user) {
        // Give all kinds from each extension registered and use by the configuration model of the user.
        kindsConf = ConfigurationManager.getAllConfigurationKind(user);
        // Give all mixins from each extension registered and use by the configuration model of the user.
        mixinsConf = ConfigurationManager.getAllConfigurationMixins(user);

        if (categoryFilter != null) {
            Iterator it = kindsConf.iterator();
            Iterator itMix = mixinsConf.iterator();
            List<Action> actions;
            boolean hasActionFilter = false;
            while (it.hasNext()) {
                Kind kindTmp = (Kind) it.next();

                // Check the action kind, if action found for this kind, we keep it.
                actions = kindTmp.getActions();
                for (Action actionTmp : actions) {
                    if (actionTmp.getTerm().equalsIgnoreCase(categoryFilter)) {
                        hasActionFilter = true;
                        break;
                    }
                }

                if (!kindTmp.getTerm().equalsIgnoreCase(categoryFilter) && !hasActionFilter) {
                    it.remove();
                }
            }
            while (itMix.hasNext()) {
                Mixin mixinTmp = (Mixin) itMix.next();
                if (!mixinTmp.getTerm().equalsIgnoreCase(categoryFilter)) {
                    itMix.remove();
                }
            }
        }

        return null;
    }

    @Override
    public List<Mixin> getMixinsConf() {
        if (mixinsConf == null) {
            mixinsConf = new LinkedList<>();
        }
        return mixinsConf;
    }

    @Override
    public List<Kind> getKindsConf() {
        if (kindsConf == null) {
            kindsConf = new LinkedList<>();
        }
        return kindsConf;

    }

    @Override
    public URI getServerURI() {
        return serverURI;
    }

    @Override
    public void setServerURI(URI uri) {
        this.serverURI = uri;
    }

    @Override
    public void setRequestParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, String> getRequestPameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        return parameters;
    }

    @Override
    public String getParameter(final String key) {
        return getRequestPameters().get(key);
    }

    @Override
    public String getAcceptedTypes() {
        return Constants.MEDIA_TYPE_TEXT_OCCI + ";" + Constants.MEDIA_TYPE_JSON + ";" + Constants.MEDIA_TYPE_JSON_OCCI + ";" + MediaType.TEXT_PLAIN;
    }

    /**
     * Return input data for an entity uuid if exist, if not exist, return null.
     *
     * @param entityUUID
     * @return
     */
    @Override
    public InputData getInputDataForEntityUUID(String entityUUID) {
        List<InputData> tmp = getInputDatas();
        InputData dataToReturn = null;
        String entityUUIDCompare;
        for (InputData data : tmp) {
            if (data != null) {
                entityUUIDCompare = data.getEntityUUID();

                if (entityUUIDCompare != null && entityUUID.equals(entityUUID)) {
                    data = dataToReturn;
                }
            }
        }
        return dataToReturn;

    }

    @Override
    public void setInputDatas(List<InputData> inputData) {
        this.inputDatas = inputData;
    }

    @Override
    public List<InputData> getInputDatas() {
        if (inputDatas == null) {
            inputDatas = new LinkedList<>();
        }
        return inputDatas;
    }

}
