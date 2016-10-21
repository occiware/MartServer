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
package org.occiware.mart.server.servlet.impl.parser.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Attribute;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractRequestParser;
import org.occiware.mart.server.servlet.impl.parser.json.utils.ValidatorUtils;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cgourdin
 */
public class JsonOcciParser extends AbstractRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonOcciParser.class);

    private InputStream jsonInput = null;
    // private OutputStream jsonResponseStream = null;
    private JsonNode rootNode = null;

    @Override
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        // If we are here, a json file has been uploaded.
        try {
            jsonInput = request.getInputStream();

            if (jsonInput == null) {
                throw new CategoryParseException("The input has no content delivered.");
            }

            super.parseInputQuery(headers, request);
            // The json file may contains :
            // A resource (with or without links).
            // A link
            // A mixin 
            // An action invocation.
            // A mixin tag.
            // A collection of resources to create (with or without links).

        } catch (IOException ex) {
            LOGGER.error("Error while loading input stream from json file upload : " + ex.getMessage());
            throw new CategoryParseException("Error on reading stream json file.");
        } finally {
            // Close input json stream.
            Utils.closeQuietly(jsonInput);
        }
    }

    @Override
    public void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException {
        LOGGER.info("Parse categories on json file inputstream.");

    }

    @Override
    public void parseOcciAttributes(HttpHeaders headers, HttpServletRequest request) throws AttributeParseException {
        LOGGER.info("Parse occi attributes on json file inputstream.");

    }

    @Override
    public Response parseResponse(Object object, Response.Status status) throws ResponseParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response getInterface(String categoryFilter, String user) {
        super.getInterface(categoryFilter, user);
        Response response;
        List<Kind> kinds = getKindsConf();
        List<Mixin> mixins = getMixinsConf();
        StringBuilder sb;

        try {
            sb = buildJsonInterface(kinds, mixins);
        } catch (IOException ex) {
            sb = new StringBuilder();
        }
        String msg = sb.toString();
        if (msg != null && !msg.isEmpty()) {
            response = Response.ok().entity(msg)
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .type(Constants.MEDIA_TYPE_JSON_OCCI)
                    .header("Accept", getAcceptedTypes())
                    .build();
        } else {
            // May not be called.
            response = Response.noContent().build();
        }

        return response;
    }

    @Override
    public Response parseResponse(Object object) throws ResponseParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Build a full json interface on stringbuilder object, to return in content
     * object response.
     *
     * @param kinds
     * @param mixins
     * @return
     * @throws IOException thrown if json output is not correct.
     */
    private StringBuilder buildJsonInterface(final List<Kind> kinds, final List<Mixin> mixins) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(renderActionsInterface(kinds, mixins));
        if (!kinds.isEmpty() || !mixins.isEmpty()) {
            sb.append(",");
        }

        // Render kinds.
        sb.append(renderKindsInterface(kinds));
        if (!mixins.isEmpty()) {
            sb.append(",");
        }
//         Render mixins.
        sb.append(renderMixinsInterface(mixins));

        sb.append("}");

        // TO check if we add json output and to format the output (pretty print).
        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(sb.toString(), Object.class);
        sb = new StringBuilder().append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));

        return sb;
    }

    private StringBuilder renderActionsInterface(List<Kind> kinds, List<Mixin> mixins) {
        StringBuilder sb = new StringBuilder();

        if (kinds.isEmpty() && mixins.isEmpty()) {
            return sb;
        }
        sb.append("\"actions\": [");
        // Render actions kinds with pattern definition.
        for (Kind kind : kinds) {
            sb.append(renderActionKindInterface(kind));
            if (!kind.getActions().isEmpty()) {
                sb.append(",");
            }
        }
        sb = removeLastComma(sb);

        // Render actions mixins with pattern definition.
        for (Mixin mixin : mixins) {
            sb.append(renderActionMixinInterface(mixin));
            if (!mixin.getActions().isEmpty()) {
                sb.append(",");
            }
        }
        sb = removeLastComma(sb);

        sb.append("]");
        return sb;
    }

    /**
     * Render action part kind for query interface.
     *
     * @param kind
     * @return
     */
    private StringBuilder renderActionKindInterface(final Kind kind) {
        StringBuilder sb = new StringBuilder();
        if (kind == null) {
            return sb;
        }
        List<Action> actions = kind.getActions();
        if (actions.isEmpty()) {
            return sb;
        }

        String term;
        String title;
        String scheme;
        List<Attribute> attributes;
        for (Action action : actions) {
            sb.append("{");
            // We render first the action attributes interface.
            attributes = action.getAttributes();
            if (!attributes.isEmpty()) {
                sb.append(renderAttributesInterface(action.getAttributes())).append(",");
            }
            // We render action scheme, term and title.
            term = action.getTerm();
            scheme = action.getScheme();
            title = action.getTitle();
            sb.append("\"scheme\":").append("\"").append(scheme).append("\",");
            sb.append("\"term\":").append("\"").append(term).append("\",");
            if (title == null) {
                title = "";
            }
            sb.append("\"title\":").append("\"").append(title).append("\"");
            sb.append("}");
            sb.append(",");
        }

        sb = removeLastComma(sb);
        return sb;
    }

    /**
     * Render action part for mixin for query interface.
     *
     * @param mixin
     * @return
     */
    private StringBuilder renderActionMixinInterface(final Mixin mixin) {
        StringBuilder sb = new StringBuilder();
        if (mixin == null) {
            return sb;
        }
        List<Action> actions = mixin.getActions();
        if (actions.isEmpty()) {
            return sb;
        }

        String term;
        String title;
        String scheme;
        List<Attribute> attributes;
        for (Action action : actions) {
            sb.append("{");
            // We render first the action attributes interface.
            attributes = action.getAttributes();
            if (!attributes.isEmpty()) {
                sb.append(renderAttributesInterface(action.getAttributes())).append(",");
            }
            // We render action scheme, term and title.
            term = action.getTerm();
            scheme = action.getScheme();
            title = action.getTitle();
            sb.append("\"scheme\":").append("\"").append(scheme).append("\",");
            sb.append("\"term\":").append("\"").append(term).append("\",");
            sb.append("\"title\":").append("\"").append(title).append("\"");
            sb.append("}");
            sb.append(",");
        }
        sb = removeLastComma(sb);
        return sb;
    }

    private StringBuilder renderKindsInterface(final List<Kind> kinds) {
        StringBuilder sb = new StringBuilder();
        if (kinds == null || kinds.isEmpty()) {
            return sb;
        }
        sb.append("\"kinds\": [");
        // Render kinds with pattern definition.
        String location;
        String term;
        String parentScheme;
        String title;
        String scheme;
        List<Action> actions;
        StringBuilder sbAct = new StringBuilder();
        String actionsStr;
        for (Kind kind : kinds) {

            sb.append("{");

            // Define the actions (array of strings).
            actions = kind.getActions();
            if (!actions.isEmpty()) {
                sbAct.append("\"actions\": [");
            }

            for (Action action : kind.getActions()) {
                sbAct.append("\"").append(action.getScheme()).append(action.getTerm()).append("\"").append(",");
            }
            actionsStr = sbAct.toString();
            if (actionsStr.endsWith(",")) {
                // remove the last comma.
                actionsStr = actionsStr.substring(0, actionsStr.length() - 1);
                sbAct = new StringBuilder(actionsStr);
            }
            if (!actions.isEmpty()) {
                sbAct.append("],");
                sb.append(sbAct);
            }

            // Define kinds attributes.
            if (!kind.getAttributes().isEmpty()) {
                sb.append(renderAttributesInterface(kind.getAttributes()));
                sb.append(",");
            }
            // Define title, location, scheme etc.
            title = kind.getTitle();
            if (title == null) {
                title = "";
            }
            term = kind.getTerm();
            parentScheme = "";
            if (kind.getParent() != null) {
                parentScheme = kind.getParent().getScheme() + kind.getParent().getTerm();
            }
            scheme = kind.getScheme();
            location = ConfigurationManager.getLocation(kind);

            sb.append("\"location\":").append("\"").append(location).append("\",");
            sb.append("\"parent\":").append("\"").append(parentScheme).append("\",");
            sb.append("\"scheme\":").append("\"").append(scheme).append("\",");
            sb.append("\"term\":").append("\"").append(term).append("\",");
            sb.append("\"title\":").append("\"").append(title).append("\"");

            sb.append("}");
            sb.append(",");
        }
        sb = removeLastComma(sb);

        sb.append("]");
        return sb;
    }

    private StringBuilder renderMixinsInterface(final List<Mixin> mixins) {
        StringBuilder sb = new StringBuilder();
        if (mixins == null || mixins.isEmpty()) {
            return sb;
        }

        sb.append("\"mixins\": [");
        // Render kinds with pattern definition.
        String location;
        String term;
        String title;
        String scheme;
        List<Action> actions;
        List<Kind> applies;
        List<Mixin> depends;

        StringBuilder sbAct = new StringBuilder();
        StringBuilder sbDep = new StringBuilder();
        StringBuilder sbApp = new StringBuilder();
        String tmp;
        for (Mixin mixin : mixins) {

            sb.append("{");

            // Define the actions (array of strings).
            actions = mixin.getActions();
            if (!actions.isEmpty()) {
                sbAct.append("\"actions\": [");
            }

            for (Action action : mixin.getActions()) {
                sbAct.append("\"").append(action.getScheme()).append(action.getTerm()).append("\"").append(",");
            }
            tmp = sbAct.toString();
            if (tmp.endsWith(",")) {
                // remove the last comma.
                tmp = tmp.substring(0, tmp.length() - 1);
                sbAct = new StringBuilder(tmp);
            }
            if (!actions.isEmpty()) {
                sbAct.append("],");
                sb.append(sbAct);
            }

            // Define kinds attributes.
            if (!mixin.getAttributes().isEmpty()) {
                sb.append(renderAttributesInterface(mixin.getAttributes()));
                sb.append(",");
            }

            // Define applies.
            applies = mixin.getApplies();

            // Define depends.
            depends = mixin.getDepends();

            sbApp.append("\"applies\": [");
            for (Kind apply : applies) {
                sbApp.append("\"").append(apply.getScheme()).append(apply.getTerm()).append("\"").append(",");
            }
            tmp = sbApp.toString();
            if (tmp.endsWith(",")) {
                // remove the last comma.
                tmp = tmp.substring(0, tmp.length() - 1);
                sbApp = new StringBuilder(tmp);
            }
            if (!applies.isEmpty()) {
                sbApp.append("],");
                sb.append(sbApp);
            }

            sbDep.append("\"depends\": [");
            for (Mixin depend : depends) {
                sbDep.append("\"").append(depend.getScheme()).append(depend.getTerm()).append("\"").append(",");
            }
            tmp = sbDep.toString();
            if (tmp.endsWith(",")) {
                // remove the last comma.
                tmp = tmp.substring(0, tmp.length() - 1);
                sbDep = new StringBuilder(tmp);
            }
            if (!depends.isEmpty()) {
                sbDep.append("],");
                sb.append(sbDep);
            }

            // Define title, location, scheme etc.
            title = mixin.getTitle();
            if (title == null) {
                title = "";
            }
            term = mixin.getTerm();
            scheme = mixin.getScheme();
            location = ConfigurationManager.getLocation(mixin);

            sb.append("\"location\":").append("\"").append(location).append("\",");
            sb.append("\"scheme\":").append("\"").append(scheme).append("\",");
            sb.append("\"term\":").append("\"").append(term).append("\",");
            sb.append("\"title\":").append("\"").append(title).append("\"");

            sb.append("}");
            sb.append(",");
        }
        sb = removeLastComma(sb);

        sb.append("]");
        return sb;
    }

    /**
     * Render attributes for query interface.
     *
     * @param attributes
     * @return
     */
    private StringBuilder renderAttributesInterface(final List<Attribute> attributes) {
        StringBuilder sb = new StringBuilder();
        if (attributes.isEmpty()) {
            return sb;
        }

        sb.append("\"attributes\": {");
        String attrName;
        boolean mutable;
        boolean required;
        String type;
        for (Attribute attribute : attributes) {
            mutable = attribute.isMutable();
            required = attribute.isRequired();

            attrName = attribute.getName();

            // Attribute name.
            sb.append("\"").append(attrName).append("\"").append(": {");
            // mutable or not.
            sb.append("\"").append("mutable").append("\":").append(mutable).append(",");
            // required or not.
            sb.append("\"").append("required").append("\":").append(required).append(",");
            
            // pattern value.
            type = attribute.getType().getInstanceTypeName();
            if (type == null) {
                type = convertTypeToSchemaType(attribute.getType().getName());
            } else {
                type = convertTypeToSchemaType(type);
            }
 
            sb.append("\"").append("pattern").append("\": {")
                    .append("\"$schema\": \"").append(ValidatorUtils.JSON_V4_SCHEMA_IDENTIFIER).append("\",")
                    .append("\"type\": \"").append(type).append("\"").append("},");
            // type value.
            sb.append("\"").append("type").append("\":\"").append(type).append("\"");
            sb.append("}");
            sb.append(",");
        }
        sb = removeLastComma(sb);
        sb.append("}");
        return sb;
    }

    /**
     * Convert a type to a generic type understood by schema validation.
     *
     * @param typeName
     * @return
     */
    private String convertTypeToSchemaType(final String typeName) {
        String type;
        if (typeName == null) {
            return "string"; // default type.
        }
        switch (typeName.toLowerCase()) {
            case "integer":
            case "float":
            case "int":
            case "long":
            case "double":
            case "bigdecimal":
                type = "number";
                break;
            case "list":
            case "set":
            case "collection":
            case "map":
                type = "array";
                break;
            case "boolean":
                type = "boolean";
                break;
            case "string":
            case "":
                type = "string";
                break;
            default:
                type = "string";
        }
        return type;
    }

    /**
     * Remove the last character if defined by comma.
     *
     * @param sb
     * @return
     */
    private StringBuilder removeLastComma(final StringBuilder sb) {
        StringBuilder sbToReturn = sb;
        String tmp = sb.toString();
        if (tmp.endsWith(",")) {
            // remove the last comma.
            tmp = tmp.substring(0, tmp.length() - 1);
            sbToReturn = new StringBuilder(tmp);
        }
        return sbToReturn;
    }

}
