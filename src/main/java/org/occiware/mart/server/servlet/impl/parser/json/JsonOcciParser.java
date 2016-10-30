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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Attribute;
import org.occiware.clouddesigner.occi.AttributeState;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Link;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.clouddesigner.occi.Resource;
import org.occiware.mart.MART;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.facade.AbstractRequestParser;
import org.occiware.mart.server.servlet.impl.parser.json.render.ActionJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.KindJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.LinkJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.MixinJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.OcciMainJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.ResourceJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.SourceJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.TargetJson;
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
        OcciMainJson occiMain;
        try {
            jsonInput = request.getInputStream();

            if (jsonInput == null) {
                throw new CategoryParseException("The input has no content delivered.");
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            // Parse to OcciMainJson object based on collections objects.
            occiMain = mapper.readValue(jsonInput, OcciMainJson.class);
            parseMainInput(occiMain);
        } catch (IOException ex) {
            LOGGER.error("Error while loading input stream from json file upload : " + ex.getMessage());
            throw new CategoryParseException("Error on reading stream json file.");
        } finally {
            // Close input json stream.
            Utils.closeQuietly(jsonInput);
        }
    }

    public void parseMainInput(final OcciMainJson mainJson) throws CategoryParseException, AttributeParseException {
        if (mainJson == null) {
            throw new CategoryParseException("unknown json format.");
        }

        List<ResourceJson> resources = mainJson.getResources();
        List<LinkJson> links = mainJson.getLinks();
        List<KindJson> kinds = mainJson.getKinds();
        List<MixinJson> mixins = mainJson.getMixins();
        List<ActionJson> actions = mainJson.getActions();
        // For only one action invocation.
        String action = mainJson.getAction();
        List<InputData> datas = getInputDatas();
        // if none objects is set, throw a category parse exception.
        boolean hasResources = resources != null && !resources.isEmpty();
        boolean hasLinks = links != null && !links.isEmpty();
        boolean hasMixins = mixins != null && !mixins.isEmpty();
        boolean hasActions = actions != null && !actions.isEmpty();
        boolean hasActionInvocation = action != null;
        boolean hasKinds = kinds != null && !kinds.isEmpty();

        if (!hasResources
                && !hasLinks && !hasMixins && !hasActions && !hasActionInvocation && !hasKinds) {
            throw new CategoryParseException("No content upload parsed. Check your json file input.");
        }

        if (hasResources) {
            for (ResourceJson resource : resources) {
                Map<String, Object> attrs;
                InputData data = new InputData();
                String title = resource.getTitle();
                String summary = resource.getSummary();
                String id = resource.getId();
                String kind = resource.getKind();
                attrs = resource.getAttributes();
                if (attrs == null) {
                    attrs = new HashMap<>();
                }
                // set title, summary and id.
                if (id != null) {
                    if (!id.startsWith(Constants.URN_UUID_PREFIX)) {
                        attrs.put(Constants.OCCI_CORE_ID, Constants.URN_UUID_PREFIX + id);
                    } else {
                        attrs.put(Constants.OCCI_CORE_ID, id);
                        id = id.replace(Constants.URN_UUID_PREFIX, "");
                    }
                    data.setEntityUUID(id);
                }
                if (title != null && !title.isEmpty()) {
                    attrs.put(Constants.OCCI_CORE_TITLE, title);
                }
                if (summary != null && !summary.isEmpty()) {
                    attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
                }
                data.setAttrObjects(attrs);

                if (kind == null) {
                    throw new CategoryParseException("Kind is not defined for resource: " + id);
                }
                data.setKind(kind);

                // We add the links on input data after the resources to be sure they are all exists if we are in create mode or update mode.
                if (resource.getLinks() != null && !resource.getLinks().isEmpty()) {
                    if (links == null) {
                        links = new LinkedList<>();
                    }
                    links.addAll(resource.getLinks());
                    hasLinks = true;
                }

                List<String> mixinsRes = resource.getMixins();
                if (mixinsRes != null && !mixinsRes.isEmpty()) {
                    data.setMixins(mixinsRes);
                }
                List<String> actionRes = resource.getActions();
                if (actionRes != null && !actionRes.isEmpty()) {
                    data.setAction(actionRes.get(0));
                    // We set the first action.
                    // TODO : Define if actions is assigned to a resource if we must execute them...
                }
                datas.add(data);
            }

        }
        if (hasLinks) {
            for (LinkJson link : links) {
                Map<String, Object> attrs;
                InputData data = new InputData();
                String title = link.getTitle();
                String summary = link.getSummary();
                String id = link.getId();
                String kind = link.getKind();
                SourceJson source = link.getSource();
                TargetJson target = link.getTarget();
                String sourceLocation;
                String targetLocation;
                attrs = link.getAttributes();
                if (attrs == null) {
                    attrs = new HashMap<>();
                }
                // set title, summary and id.
                if (id != null) {
                    if (!id.startsWith(Constants.URN_UUID_PREFIX)) {
                        attrs.put(Constants.OCCI_CORE_ID, Constants.URN_UUID_PREFIX + id);
                    } else {
                        attrs.put(Constants.OCCI_CORE_ID, id);
                        id = id.replace(Constants.URN_UUID_PREFIX, "");
                    }
                    data.setEntityUUID(id);
                }
                if (title != null && !title.isEmpty()) {
                    attrs.put(Constants.OCCI_CORE_TITLE, title);
                }
                if (summary != null && !summary.isEmpty()) {
                    attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
                }
                if (source == null) {
                    throw new AttributeParseException("No source set for link: " + id + " , check your json query.");
                }
                if (target == null) {
                    throw new AttributeParseException("No target set for link: " + id + " , check your json query.");
                }
                sourceLocation = source.getLocation();
                targetLocation = target.getLocation();
                if (sourceLocation == null || sourceLocation.isEmpty()) {
                    throw new AttributeParseException("No source location set for link : " + id + " , check your json query.");
                }
                if (targetLocation == null || targetLocation.isEmpty()) {
                    throw new AttributeParseException("No target location set for link : " + id + " , check your json query.");
                }
                attrs.put(Constants.OCCI_CORE_TARGET, targetLocation);
                attrs.put(Constants.OCCI_CORE_SOURCE, sourceLocation);
                data.setAttrObjects(attrs);

                if (kind == null) {
                    throw new CategoryParseException("Kind is not defined for resource: " + id);
                }
                data.setKind(kind);

                List<String> mixinsRes = link.getMixins();
                if (mixinsRes != null && !mixinsRes.isEmpty()) {
                    data.setMixins(mixinsRes);
                }
//                List<String> actionRes = link.getActions();
//                if (actionRes != null && !actionRes.isEmpty()) {
//                    data.setAction(actionRes.get(0)); 
//                    // We set the first action.
//                    // TODO : Define if actions is assigned to a resource if we must execute them...
//                }
                datas.add(data);
            }
        }

        if (hasMixins) {
            for (MixinJson mixin : mixins) {
                InputData data = new InputData();
                String location = mixin.getLocation();
                String term = mixin.getTerm();
                String title = mixin.getTitle();
                String scheme = mixin.getScheme();
                Map<String, Object> attrs = mixin.getAttributes();
                if (term == null || scheme == null || scheme.isEmpty() || term.isEmpty()) {
                    throw new AttributeParseException("The mixin must have scheme and term, check your json file.");
                }
                if (location == null || location.isEmpty()) {
                    throw new AttributeParseException("The mixin must have a location, check your json file.");
                }
                data.setMixinTag(scheme + term);
                data.setMixinTagLocation(location);
                data.setMixinTagTitle(title);
                data.setAttrObjects(attrs);
                datas.add(data);
            }
        }

        if (hasKinds) {
            // TODO : Add filter on kinds if the query is a get with upload json file.

        }
        // Multiple Actions invocation.
        if (hasActions) {
            for (ActionJson json : actions) {
                InputData data = new InputData();
                data.setAction(json.getAction());
                data.setAttrObjects(json.getAttributes());
                datas.add(data);
            }
        }
        // If we had an action invocation only.
        if (hasActionInvocation) {
            InputData data = new InputData();
            data.setAction(action);
            data.setAttrObjects(mainJson.getAttributes());
            datas.add(data);
        }
        this.setInputDatas(datas);
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
        Response response = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String msg;
        try {
            // Case 1 : Object is a Response object.
            if (object instanceof Response) {

                if (status != null && status.equals(Response.Status.OK)) {
                    msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("OK");
                    response = Response.fromResponse((Response) object)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("Accept", getAcceptedTypes())
                            .type(Constants.MEDIA_TYPE_JSON)
                            .entity(msg)
                            .status(status)
                            .build();
                } else {
                    msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString((Response) ((Response) object).getEntity());

                    response = Response.fromResponse((Response) object)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("Accept", getAcceptedTypes())
                            .type(Constants.MEDIA_TYPE_JSON)
                            .entity(msg)
                            .status(status)
                            .build();
                }
            }
            // Case 2 : Object is a String.
            if (object instanceof String) {

                if (status != null && status.equals(Response.Status.OK)) {
                    msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("OK");
                    response = Response.status(status)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("content", (String) object)
                            .header("Accept", getAcceptedTypes())
                            .entity(msg)
                            .type(Constants.MEDIA_TYPE_JSON)
                            .build();
                } else {
                    msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString((String) object);
                    response = Response.status(status)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("content", (String) object)
                            .header("Accept", getAcceptedTypes())
                            .entity(msg)
                            .type(Constants.MEDIA_TYPE_JSON)
                            .build();
                }
            }

            if (object instanceof Entity) {
                // Build an object response from entity occiware object model.
                Entity entity = (Entity) object;
                List<Entity> entities = new LinkedList<>();
                entities.add(entity);
                response = renderEntityResponse(entities, status);
            }

            if (object instanceof List) {
                LOGGER.info("Collection to render.");
                List<Object> objects = (List<Object>) object;
                List<String> locations = new LinkedList<>();
                List<Entity> entities = new LinkedList<>();
                String tmp;
                Entity entityTmp;
                // To determine if location or if entities to render..
                for (Object objectTmp : objects) {
                    if (objectTmp instanceof String) {
                        // List of locations.
                        tmp = (String) objectTmp;
                        locations.add(tmp);

                    } else if (objectTmp instanceof Entity) {
                        // List of entities to render.
                        entityTmp = (Entity) objectTmp;
                        entities.add(entityTmp);

                    } else {
                        throw new ResponseParseException("unknown datatype collection.");
                    }
                }

                if (!locations.isEmpty()) {
                    
                    msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(locations);
                    Response.ResponseBuilder responseBuilder = Response.status(status)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("Accept", getAcceptedTypes())
                            .entity(msg)
                            .type(Constants.MEDIA_TYPE_JSON);
//                    for (String location : locations) {
//                        String absLocation = getServerURI().toString() + location;
//                        responseBuilder.header(Constants.X_OCCI_LOCATION, absLocation);
//                    }
                    response = responseBuilder.build();
                }
                if (!entities.isEmpty()) {
                    response = renderEntityResponse(entities, status);
                }

            }
            
            if (response == null) {
                throw new ResponseParseException("Cannot parse the object to application/json representation.");
            }

            return response;

        } catch (JsonProcessingException ex) {
            throw new ResponseParseException(ex.getMessage());
        }
    }

    private Response renderEntityResponse(List<Entity> entities, Response.Status status) throws JsonProcessingException {

        Response response;
        OcciMainJson mainJson = new OcciMainJson();
        List<ResourceJson> resources = new LinkedList<>();
        List<LinkJson> links = new LinkedList<>();
        for (Entity entity : entities) {
            // Rendering json of this entity.
            String relativeLocation = ConfigurationManager.getLocation(entity);
            String absoluteEntityLocation = getServerURI().toString() + relativeLocation;

            // An entity may be a link or a resource.
            if (entity instanceof Link) {
                // This is a link.
                LinkJson linkJson = buildLinkJsonFromEntity(entity);
                links.add(linkJson);
                
            } else {
                // This is a resource.
                ResourceJson resJson = buildResourceJsonFromEntity(entity);
                resources.add(resJson);
            }
            

        } // End for each entities.

        if (!resources.isEmpty()) {
            mainJson.setResources(resources);
        }
        if (!links.isEmpty()) {
            mainJson.setLinks(links);
        }
        String msg = mainJson.toStringJson();
        // Build response object...
        response = Response.status(status)
                            .header("Server", Constants.OCCI_SERVER_HEADER)
                            .header("Accept", getAcceptedTypes())
                            .entity(msg)
                            .type(Constants.MEDIA_TYPE_JSON)
                            .build();
        
        return response;
    }

    private ResourceJson buildResourceJsonFromEntity(final Entity entity) {
        ResourceJson resJson = new ResourceJson();
        Resource res = (Resource) entity;
        Kind kind = res.getKind();
        List<Mixin> mixins;
        List<String> mixinsStr = new LinkedList<>();
        List<LinkJson> links = new LinkedList<>();
        resJson.setKind(kind.getScheme() + kind.getTerm());
        resJson.setId(res.getId());
        resJson.setTitle(res.getTitle());
        resJson.setSummary(res.getSummary());
        resJson.setLocation(ConfigurationManager.getLocation(entity));
        
        List<String> actionsStr = new LinkedList<>();
        String actionStr;
        for (Action action : kind.getActions()) {
            actionStr = action.getScheme() + action.getTerm();
            actionsStr.add(actionStr);
        }
        resJson.setActions(actionsStr);
        Map<String, Object> attributes = new LinkedHashMap<>();
        // Attributes.
        List<AttributeState> attrsState = res.getAttributes();
        for (AttributeState attr : attrsState) {
            String key = attr.getName();
            String val = attr.getValue();
            if (val != null) {
                if (!key.equals(Constants.OCCI_CORE_SUMMARY) && !key.equals(Constants.OCCI_CORE_TITLE) 
                        && !key.equals(Constants.OCCI_CORE_ID)) {
                    
                    EDataType eAttrType = ConfigurationManager.getEAttributeType(entity, key);

                    if (eAttrType != null 
                            && (eAttrType instanceof EEnum || eAttrType.getInstanceClass() == String.class)) {
                        // value with quote only for String and EEnum type.
                        attributes.put(key, val);
                    } else if (eAttrType == null) {
                        // Cant determine the type.
                        attributes.put(key, val);
                    } else {
                        // Not a string nor an enum val.
                        try {
                            Number num = Utils.parseNumber(val, eAttrType.getInstanceClassName());
                            attributes.put(key, num);
                        } catch (NumberFormatException ex) {
                            attributes.put(key, val);
                        }
                    }
                    
                }
                if (key.equals(Constants.OCCI_CORE_ID)) {
                    if (!val.startsWith(Constants.URN_UUID_PREFIX)) {
                        val = Constants.URN_UUID_PREFIX + val; 
                    }
                    attributes.put(key, val);
                }
            }
        }
        resJson.setAttributes(attributes);
        
        mixins = res.getMixins();
        for (Mixin mixin : mixins) {
            String mixinStr = mixin.getScheme() + mixin.getTerm();
            mixinsStr.add(mixinStr);
        }
        resJson.setMixins(mixinsStr);
        // resources has links ?
        for (Link link : res.getLinks()) {
            LinkJson linkJson = buildLinkJsonFromEntity(link);
            links.add(linkJson);
        }
        if (!links.isEmpty()) {
            resJson.setLinks(links);
        }
        
        return resJson;
    }

    private LinkJson buildLinkJsonFromEntity(final Entity entity) {
        LinkJson linkJson = new LinkJson();
        Link link = (Link) entity;
        Kind kind;
        List<Mixin> mixins;
        List<String> mixinsStr = new LinkedList<>();
        List<Action> actions;
        List<String> actionsStr = new LinkedList<>();
        String actionStr;
        Map<String, Object> attributes = new LinkedHashMap<>();
        kind = link.getKind();
        linkJson.setKind(kind.getScheme() + kind.getTerm());
        linkJson.setId(link.getId());
        linkJson.setTitle(link.getTitle());
        linkJson.setLocation(ConfigurationManager.getLocation(entity));
        actions = kind.getActions();
        for (Action action : actions) {
            actionStr = action.getScheme() + action.getTerm();
            actionsStr.add(actionStr);
        }
        linkJson.setActions(actionsStr);

        // Attributes.
        List<AttributeState> attrsState = link.getAttributes();
        for (AttributeState attr : attrsState) {
            String key = attr.getName();
            String val = attr.getValue();
            if (val != null) {
                if (!key.equals(Constants.OCCI_CORE_SUMMARY) && !key.equals(Constants.OCCI_CORE_TITLE) 
                        && !key.equals(Constants.OCCI_CORE_ID) 
                        && !key.equals(Constants.OCCI_CORE_SOURCE) 
                        && !key.equals(Constants.OCCI_CORE_TARGET)) {
                   
                    EDataType eAttrType = ConfigurationManager.getEAttributeType(entity, key);

                    if (eAttrType != null 
                            && (eAttrType instanceof EEnum || eAttrType.getInstanceClass() == String.class)) {
                        // value with quote only for String and EEnum type.
                        attributes.put(key, val);
                    } else if (eAttrType == null) {
                        // Cant determine the type.
                        attributes.put(key, val);
                    } else {
                        // Not a string nor an enum val.
                        try {
                            Number num = Utils.parseNumber(val, eAttrType.getInstanceClassName());
                            attributes.put(key, num);
                        } catch (NumberFormatException ex) {
                            attributes.put(key, val);
                        }
                    }
                }
                if (key.equals(Constants.OCCI_CORE_ID)) {
                    if (!val.startsWith(Constants.URN_UUID_PREFIX)) {
                        val = Constants.URN_UUID_PREFIX + val; 
                    }
                    attributes.put(key, val);
                }
            }
        }
        Resource resSrc = link.getSource();
        Resource resTarget = link.getTarget();
        SourceJson src = new SourceJson();
        TargetJson target = new TargetJson();
        String relativeLocation = ConfigurationManager.getLocation(resSrc);
        src.setKind(resSrc.getKind().getScheme() + resSrc.getKind().getTerm());
        if (!relativeLocation.startsWith("/")) {
            relativeLocation = "/" + relativeLocation;
        }
        src.setLocation(relativeLocation);
        relativeLocation = ConfigurationManager.getLocation(resTarget);
        target.setKind(resTarget.getKind().getScheme() + resTarget.getKind().getTerm());
        if (!relativeLocation.startsWith("/")) {
            relativeLocation = "/" + relativeLocation;
        }
        target.setLocation(relativeLocation);
        linkJson.setSource(src);
        linkJson.setTarget(target);

        linkJson.setAttributes(attributes);

        mixins = link.getMixins();
        for (Mixin mixin : mixins) {
            String mixinStr = mixin.getScheme() + mixin.getTerm();
            mixinsStr.add(mixinStr);
        }
        linkJson.setMixins(mixinsStr);

        return linkJson;
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

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
