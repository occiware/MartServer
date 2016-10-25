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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
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

    @Override
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        // If we are here, a json file has been uploaded.
        // We dont call the super method because the behavior is not the same. It may here have multiple entity to operate.
        // The json file may contains :
        // A resource (with or without links).
        // A link
        // A mixin 
        // An action invocation.
        // A mixin tag.
        // A collection of resources ==> to create or to update (with or without links).
        parseInputQueryToDatas(request);
    }

    /**
     * Parse the input query in multiple InputData objects.
     *
     * @param request
     * @throws CategoryParseException
     * @throws AttributeParseException
     */
    private void parseInputQueryToDatas(HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        InputStream jsonInput = null;
        LOGGER.info("Parsing input uploaded datas...");
        try {
            jsonInput = request.getInputStream();

            if (jsonInput == null) {
                throw new CategoryParseException("The input has no content delivered.");
            }
            // Get root json node.
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonInput);
            Iterator<Map.Entry<String, JsonNode>> nodeIterator = rootNode.fields();
            String rootKey;
            JsonNode node;
            while (nodeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = nodeIterator.next();
                LOGGER.info("Key --> " + entry.getKey() + " --< value: " + entry.getValue());
                // Key --> resources 
                // value: [{"id":"urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0","title":"compute1","summary":"My compute","kind":"http://schemas.ogf.org/occi/infrastructure#compute","attributes":{"occi.compute.speed":2,"occi.compute.memory":4,"occi.compute.cores":2,"occi.compute.architecture":"x64","occi.compute.state":"active"}}]
                rootKey = entry.getKey();
                node = entry.getValue();

                parseInputNode(rootKey, node);

            }

            LOGGER.info("Read nodes finished !");

            throw new CategoryParseException("Not supported yet !");

        } catch (IOException ex) {
            LOGGER.error("Error while loading input stream from json file upload : " + ex.getMessage());
            throw new CategoryParseException("Error on reading stream json file.");
        } finally {
            // Close input json stream.
            Utils.closeQuietly(jsonInput);
        }
    }

    /**
     * This method select the node to parse in.
     *
     * @param nodeKey
     * @param node
     * @throws CategoryParseException
     * @throws AttributeParseException
     */
    private void parseInputNode(final String nodeKey, final JsonNode node) throws CategoryParseException, AttributeParseException {

        switch (nodeKey) {
            case "resources":
                // The json file contains resource(s).
                LOGGER.info("input json has resources, setting to input data...");
                // For each resource in array "resources", it set an InputData object.
                parseInputResources(node);

                break;
            case "links":
                // The json file contains link(s).
                // For each links in array links, it set an InputData object.
                LOGGER.info("input json has links, setting to input data...");
                List<JsonNode> linksNode = new LinkedList<>();
                linksNode.add(node);
                parseInputLinks(linksNode);
                break;

            case "kinds":
                // The json file contains kind(s) to describe.
                // For each kind describe, it set an InputData object.
                LOGGER.info("input json has kinds, setting to input data...");
                // parseInputKinds(node);

                break;

            case "mixins":
                // The json file contains mixin(s).
                LOGGER.info("input json has mixins definitions, setting to input data...");
                // parseInputMixins(node);

                break;
            case "actions":
                // The json file contains action(s).
                LOGGER.info("input json has actions, setting to input data...");
                // parseInputActions(node);
                break;

            default:
                throw new CategoryParseException("Unknown json key: " + nodeKey);

        }
    }

    private void parseInputResources(final JsonNode node) throws CategoryParseException, AttributeParseException {
        if (node == null) {
            throw new CategoryParseException("The resource node has no value.");
        }
        // Example from resource3.json (see testjson directory).
//        INFO  JsonOcciParser - Parsing input uploaded datas...
//INFO  JsonOcciParser - Key --> resources --< value: [{"id":"urn:uuid:b7d55bf4-7057-5113-85c8-141871bf7635","title":"network1","summary":"My main network","kind":"http://schemas.ogf.org/occi/infrastructure#network","mixins":["http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"],"attributes":{"occi.network.vlan":12,"occi.network.label":"private","occi.network.address":"10.1.0.0/16","occi.network.gateway":"10.1.255.254"}},{"id":"urn:uuid:31cf3896-500e-48d8-a3f5-a8b3601bcdd9","title":"compute2","summary":"My other compute","kind":"http://schemas.ogf.org/occi/infrastructure#compute","attributes":{"occi.compute.speed":1,"occi.compute.memory":2,"occi.compute.cores":1,"occi.compute.architecture":"x86","occi.compute.state":"active"},"links":[{"kind":"http://schemas.ogf.org/occi/infrastructure#networkinterface","mixins":["http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"],"attributes":{"occi.infrastructure.networkinterface.interface":"eth0","occi.infrastructure.networkinterface.mac":"00:80:41:ae:fd:7e","occi.infrastructure.networkinterface.address":"192.168.0.100","occi.infrastructure.networkinterface.gateway":"192.168.0.1","occi.infrastructure.networkinterface.allocation":"dynamic"},"actions":["http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up","http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"],"id":"urn:uuid:22fe83ae-a20f-54fc-b436-cec85c94c5e8","target":{"location":"/network/b7d55bf4-7057-5113-85c8-141871bf7635","kind":"http://schemas.ogf.org/occi/infrastructure#network"},"source":{"location":"/compute/31cf3896-500e-48d8-a3f5-a8b3601bcdd9"}}]}]
//INFO  JsonOcciParser - input json has resources, setting to input data...
//INFO  JsonOcciParser - Field name : id --< fieldValue : "urn:uuid:b7d55bf4-7057-5113-85c8-141871bf7635"
//INFO  JsonOcciParser - Field name : title --< fieldValue : "network1"
//INFO  JsonOcciParser - Field name : summary --< fieldValue : "My main network"
//INFO  JsonOcciParser - Field name : kind --< fieldValue : "http://schemas.ogf.org/occi/infrastructure#network"
//INFO  JsonOcciParser - Field name : mixins --< fieldValue : ["http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"]
//INFO  JsonOcciParser - Field name : attributes --< fieldValue : {"occi.network.vlan":12,"occi.network.label":"private","occi.network.address":"10.1.0.0/16","occi.network.gateway":"10.1.255.254"}
//INFO  JsonOcciParser - Field name : id --< fieldValue : "urn:uuid:31cf3896-500e-48d8-a3f5-a8b3601bcdd9"
//INFO  JsonOcciParser - Field name : title --< fieldValue : "compute2"
//INFO  JsonOcciParser - Field name : summary --< fieldValue : "My other compute"
//INFO  JsonOcciParser - Field name : kind --< fieldValue : "http://schemas.ogf.org/occi/infrastructure#compute"
//INFO  JsonOcciParser - Field name : attributes --< fieldValue : {"occi.compute.speed":1,"occi.compute.memory":2,"occi.compute.cores":1,"occi.compute.architecture":"x86","occi.compute.state":"active"}
//INFO  JsonOcciParser - Field name : links --< fieldValue : [{"kind":"http://schemas.ogf.org/occi/infrastructure#networkinterface","mixins":["http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"],"attributes":{"occi.infrastructure.networkinterface.interface":"eth0","occi.infrastructure.networkinterface.mac":"00:80:41:ae:fd:7e","occi.infrastructure.networkinterface.address":"192.168.0.100","occi.infrastructure.networkinterface.gateway":"192.168.0.1","occi.infrastructure.networkinterface.allocation":"dynamic"},"actions":["http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up","http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"],"id":"urn:uuid:22fe83ae-a20f-54fc-b436-cec85c94c5e8","target":{"location":"/network/b7d55bf4-7057-5113-85c8-141871bf7635","kind":"http://schemas.ogf.org/occi/infrastructure#network"},"source":{"location":"/compute/31cf3896-500e-48d8-a3f5-a8b3601bcdd9"}}]
//INFO  JsonOcciParser - Key --> mixins --< value: [{"location":"/mixins/my_mixin/","scheme":"http://example.com/occi/tags#","term":"my_mixin","attributes":{},"title":"1"},{"location":"/mixins/my_mixin2/","scheme":"http://example.com/occi/tags#","term":"my_mixin2","attributes":{},"title":"2"}]
//INFO  JsonOcciParser - Read nodes finished !

        // Read the node contents.
        Iterator<JsonNode> it = node.iterator();
        Iterator<String> itField;
        String fieldName;
        JsonNode fieldValue;
        InputData data;
        List<InputData> datas = getInputDatas();
        String id;
        String summary;
        String title;
        String kind;
        List<String> mixins;
        Map<String, String> attrs;
        List<JsonNode> linksToSet = new LinkedList<>();

        boolean hasLinks = false;

        while (it.hasNext()) {
            JsonNode currentNode = it.next();
            itField = currentNode.fieldNames();
            data = new InputData();

            attrs = new HashMap<>();

            while (itField.hasNext()) {
                fieldName = itField.next();
                fieldValue = currentNode.get(fieldName);

                if (fieldValue == null || fieldValue.isNull()) {
                    continue;
                }
                if (fieldName == null) {
                    continue;
                }
                LOGGER.info("Field name : " + fieldName + " --< fieldValue : " + fieldValue.toString());
                switch (fieldName) {
                    case "id":
                        id = fieldValue.textValue();
                        attrs.put(Constants.OCCI_CORE_ID, id);
                        if (id.startsWith(Constants.URN_UUID_PREFIX)) {
                            id = id.replace(Constants.URN_UUID_PREFIX, "");
                        }
                        data.setEntityUUID(id);

                        break;
                    case "kind":
                        kind = fieldValue.textValue();
                        data.setKind(kind);
                        break;

                    case "title":
                        title = fieldValue.textValue();
                        attrs.put(Constants.OCCI_CORE_TITLE, title);
                        break;
                    case "summary":
                        summary = fieldValue.textValue();
                        attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
                        break;
                    case "attributes":
                        // Parse the attributes.
                        attrs.putAll(parseInputAttributes(fieldValue));
                        break;
                    case "mixins":
                        mixins = new ArrayList<>();
                        if (!fieldValue.isArray()) {
                            throw new CategoryParseException("The mixins defined on resource must be defined in a json array !");
                        }
                        Iterator<JsonNode> itMixin = fieldValue.elements();
                        while (itMixin.hasNext()) {
                            JsonNode mixinNode = itMixin.next();
                            String mixin = mixinNode.asText();
                            if (mixin != null && !mixin.isEmpty()) {
                                mixins.add(mixinNode.asText());
                            }
                        }
                        // Update inputdata object.
                        data.setMixins(mixins);

                        break;
                    case "links":
                        // if links are set on this resources, it must be included in the end of the inputdata linkedlist.
                        hasLinks = true;
                        linksToSet.add(fieldValue);
                        break;
                    default:
                        throw new CategoryParseException("Unknown field : " + fieldName);
                }

            } // End for each field.
            data.setAttrs(attrs);
            datas.add(data);
        } // For each resources node.
        setInputDatas(datas);
        if (hasLinks) {
            // Update inputdatas object list with declared links (added in the end of the list of resources.
            // Node specifies the resources part, if links are setted on the resources part, this method has two way to create links, from links root node or /resources/links path.
            parseInputLinks(linksToSet);

        }
    }

    /**
     * Parse input entry links. This method has two behavior : one from root
     * node, second from resources node.
     *
     * @param node
     * @throws CategoryParseException
     * @throws AttributeParseException
     */
    private void parseInputLinks(final List<JsonNode> nodes) throws CategoryParseException, AttributeParseException {
        Iterator<String> itField;
        String fieldName;
        JsonNode fieldValue;
        InputData data;
        List<InputData> datas = getInputDatas();
        String id;
        String summary;
        String title;
        String kind;
        List<String> mixins;
        Map<String, String> attrs;
        Iterator<JsonNode> it;
        for (JsonNode node : nodes) {

            if (node.isArray()) {
                it = node.elements();
            } else {
                it = node.iterator();
            }
            while (it.hasNext()) {
                JsonNode currentNode = it.next();

                if (currentNode.isArray()) {
                    currentNode.elements();

                }

                itField = currentNode.fieldNames();
                data = new InputData();

                attrs = new HashMap<>();

                while (itField.hasNext()) {
                    fieldName = itField.next();
                    fieldValue = currentNode.get(fieldName);

                    if (fieldValue == null || fieldValue.isNull()) {
                        continue;
                    }
                    if (fieldName == null) {
                        continue;
                    }
                    LOGGER.info("Field name : " + fieldName + " --< fieldValue : " + fieldValue.toString());
                    switch (fieldName) {
                        case "id":
                            id = fieldValue.textValue();
                            attrs.put(Constants.OCCI_CORE_ID, id);
                            if (id.startsWith(Constants.URN_UUID_PREFIX)) {
                                id = id.replace(Constants.URN_UUID_PREFIX, "");
                            }
                            data.setEntityUUID(id);

                            break;
                        case "kind":
                            kind = fieldValue.textValue();
                            data.setKind(kind);
                            break;

                        case "title":
                            title = fieldValue.textValue();
                            attrs.put(Constants.OCCI_CORE_TITLE, title);
                            break;
                        case "summary":
                            summary = fieldValue.textValue();
                            attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
                            break;
                        case "attributes":
                            // Parse the attributes.
                            attrs.putAll(parseInputAttributes(fieldValue));
                            break;
                        case "mixins":
                            mixins = new ArrayList<>();
                            if (!fieldValue.isArray()) {
                                throw new CategoryParseException("The mixins defined on resource must be defined in a json array !");
                            }
                            Iterator<JsonNode> itMixin = fieldValue.elements();
                            while (itMixin.hasNext()) {
                                JsonNode mixinNode = itMixin.next();
                                String mixin = mixinNode.asText();
                                if (mixin != null && !mixin.isEmpty()) {
                                    mixins.add(mixinNode.asText());
                                }
                            }
                            // Update inputdata object.
                            data.setMixins(mixins);

                            break;

                        case "target":
                            // We get here the location (relative path of the resource target link.
                            JsonNode locationNode = fieldValue.get("location");
                            if (locationNode == null || locationNode.isNull() || locationNode.isNumber() || locationNode.isArray()) {
                                throw new AttributeParseException("target location is not set properly, check your json query.");
                            }
                            String location = locationNode.asText();
                            LOGGER.info("Target location : " + location);
                            attrs.put(Constants.OCCI_CORE_TARGET, location);
                            break;
                        case "source":
                            JsonNode locationSrcNode = fieldValue.get("location");
                            if (locationSrcNode == null || locationSrcNode.isNull() || locationSrcNode.isNumber() || locationSrcNode.isArray()) {
                                throw new AttributeParseException("target location is not set properly, check your json query.");
                            }
                            String locationSrc = locationSrcNode.asText();
                            LOGGER.info("Target location : " + locationSrc);
                            attrs.put(Constants.OCCI_CORE_SOURCE, locationSrc);
                            break;

                        case "links":
                            // Links are not set in resources.
                            throw new CategoryParseException("Links cannot be within other links.");
                        case "actions":
                            // this is ignored.
                            break;
                        default:
                            throw new CategoryParseException("Unknown field : " + fieldName);
                    }

                } // End for each field.
                data.setAttrs(attrs);
                datas.add(data);
            } // For each links node.
        } // End for each links root node.
        setInputDatas(datas);

    }

    /**
     * Parse des attributs en entrée (resources, links, mixins etc.).
     *
     * @param attrNode
     * @return
     * @throws AttributeParseException
     */
    private Map<String, String> parseInputAttributes(final JsonNode attrNode) throws AttributeParseException {
        Map<String, String> attrsMap = new HashMap<>();
        if (attrNode == null || attrNode.isNull()) {
            throw new AttributeParseException();
        }
        String fieldName;
        Iterator<String> itField = attrNode.fieldNames();
        JsonNode value;
        String valueStr;
        boolean boolVal;
        while (itField.hasNext()) {
            fieldName = itField.next();
            valueStr = "";
            value = attrNode.get(fieldName);
            if (value.isBoolean()) {
                boolVal = value.asBoolean();
                if (boolVal) {
                    valueStr = "true";
                } else {
                    valueStr = "false";
                }
            }
            if (valueStr.isEmpty()) {
                valueStr = value.asText();
            }

            LOGGER.info("Attribute:--> " + fieldName);
            LOGGER.info("Attribute value :--> " + valueStr);

            attrsMap.put(fieldName, valueStr);
        }
        return attrsMap;
    }

    @Override
    public void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException {
        // This method has no effect here.
        throw new UnsupportedOperationException("Not supported for Json queries.");
    }

    @Override
    public void parseOcciAttributes(HttpHeaders headers, HttpServletRequest request) throws AttributeParseException {
        // This method has no effect here.
        throw new UnsupportedOperationException("Not supported for Json queries.");

    }

    @Override
    public Response parseResponse(Object object, Response.Status status) throws ResponseParseException {
        // TODO : Add response json object.
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
