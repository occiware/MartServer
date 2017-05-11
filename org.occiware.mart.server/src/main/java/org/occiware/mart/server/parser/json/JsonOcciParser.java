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
package org.occiware.mart.server.parser.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.AbstractRequestParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.parser.json.render.*;
import org.occiware.mart.server.parser.json.render.queryinterface.*;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author Christophe Gourdin
 */
public class JsonOcciParser extends AbstractRequestParser implements IRequestParser {

    public static final String EMPTY_JSON = "{ }";
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonOcciParser.class);

    public JsonOcciParser(String user) {
        super(user);
    }

    //*************************
    // Read input content part
    //*************************

    /**
     * Parse the input query in multiple OCCIRequestData objects (or only one if one thing defined).
     *
     * @param contentObj This is the content json.
     * @throws ParseOCCIException
     */
    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {

        if (!(contentObj instanceof String)) {
            throw new ParseOCCIException("The object parameter must be a String content object");
        }

        String contentJson = (String) contentObj;
        ObjectMapper mapper = new ObjectMapper();
        // Define those attributes have only non null values.
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        OcciMainJson occiMain;
        boolean isCollectionRes;
        boolean isResourceSingle;
        boolean isLinkSingle = false;
        boolean isMixinTagSingle = false;
        boolean isActionInvocation = false;
        String messages = "";

        if (!contentJson.isEmpty()) {
            // Try on occi main json (for multiple resources/links/mixins).
            try {
                occiMain = mapper.readValue(contentJson, OcciMainJson.class);
                parseMainInput(occiMain);
                isCollectionRes = true;
            } catch (IOException ex) {
                messages += ex.getMessage();
                isCollectionRes = false;
            }

            // for one resource, if this is not a collection (resources,links etc..).
            // it goes to try to get a single resource.
            if (!isCollectionRes) {
                try {
                    ResourceJson resJson = mapper.readValue(contentJson, ResourceJson.class);
                    parseResourceJsonInput(resJson);
                    isResourceSingle = true;
                } catch (IOException ex) {
                    messages += " " + ex.getMessage();
                    isResourceSingle = false;
                }

                if (!isResourceSingle) {
                    try {
                        LinkJson linkJson = mapper.readValue(contentJson, LinkJson.class);
                        parseLinkJsonInput(linkJson);
                        isLinkSingle = true;
                    } catch (IOException ex) {
                        messages += " " + ex.getMessage();
                        isLinkSingle = false;
                    }
                }

                // Try to parse single mixin tag.
                if (!isResourceSingle && !isLinkSingle) {
                    try {
                        MixinJson mixinJson = mapper.readValue(contentJson, MixinJson.class);
                        parseMixinJsonTagInput(mixinJson);
                        isMixinTagSingle = true;
                    } catch (IOException ex) {
                        messages += " " + ex.getMessage();
                        isMixinTagSingle = false;
                    }
                }
                // Try to read action json invocation.
                if (!isResourceSingle && !isLinkSingle && !isMixinTagSingle) {
                    try {
                        ActionJson actionJson = mapper.readValue(contentJson, ActionJson.class);
                        parseActionJsonInvocationInput(actionJson);
                        isActionInvocation = true;
                    } catch (IOException ex) {
                        messages += " " + ex.getMessage();
                        isActionInvocation = false;
                    }
                }

                // If all tries are failed throw an exception with otherall exception messages.
                if (!isResourceSingle && !isLinkSingle && !isMixinTagSingle && !isActionInvocation) {
                    LOGGER.error("Unknown json input file, please check your file input. " + messages);
                    throw new ParseOCCIException("Unknown json input file, please check your file. " + messages);
                }
            }

        } else {
            // No content input.
            super.getInputDatas().clear();

        }
    }

    /**
     * Parse collection input files, may defines resources, links, mixins+tag.
     *
     * @param mainJson
     * @throws ParseOCCIException
     */
    public void parseMainInput(final OcciMainJson mainJson) throws ParseOCCIException {
        if (mainJson == null) {
            throw new ParseOCCIException("unknown json format.");
        }
        List<ResourceJson> resources = mainJson.getResources();
        List<LinkJson> links = mainJson.getLinks();
        List<KindJson> kinds = mainJson.getKinds();
        List<MixinJson> mixins = mainJson.getMixins();
        List<ActionJson> actions = mainJson.getActions();
        // if none objects is set, throw a category parse exception.
        boolean hasResources = resources != null && !resources.isEmpty();
        boolean hasLinks = links != null && !links.isEmpty();
        boolean hasMixins = mixins != null && !mixins.isEmpty();
        boolean hasActions = actions != null && !actions.isEmpty();
        boolean hasKinds = kinds != null && !kinds.isEmpty();

        if (!hasResources
                && !hasLinks && !hasMixins && !hasActions && !hasKinds) {
            throw new ParseOCCIException("No content upload parsed. Check your json content input.");
        }
        if (links == null) {
            links = new LinkedList<>();
        }
        if (hasResources) {
            for (ResourceJson resource : resources) {
                parseResourceJsonInput(resource);
            }
        }
        if (hasLinks) {
            for (LinkJson link : links) {
                parseLinkJsonInput(link);
            }
        }

        if (hasMixins) {
            for (MixinJson mixin : mixins) {
                parseMixinJsonTagInput(mixin);
            }
        }

        if (hasKinds) {
            // TODO : Add filter on kinds if the query is a get with upload json file.
        }
        // Multiple Actions invocation.
        if (hasActions) {
            for (ActionJson json : actions) {
                parseActionJsonInvocationInput(json);
            }
        }
    }

    /**
     * Load an input data from resource. If there are links on this resources.
     *
     * @param resource
     * @throws ParseOCCIException
     */
    private void parseResourceJsonInput(ResourceJson resource) throws ParseOCCIException {
        Map<String, Object> attrs;
        OCCIRequestData data = new OCCIRequestData();
        String title = resource.getTitle();
        String summary = resource.getSummary();
        String id = resource.getId();
        String kind = resource.getKind();
        String location = resource.getLocation();
        attrs = resource.getAttributes();
        List<OCCIRequestData> occiRequestDatas = super.getInputDatas();
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        // set title, summary and id.
        if (id != null) {
//            if (!id.startsWith(Constants.URN_UUID_PREFIX)) {
//                attrs.put(Constants.OCCI_CORE_ID, Constants.URN_UUID_PREFIX + id);
//            } else {
//                attrs.put(Constants.OCCI_CORE_ID, id);
//                id = id.replace(Constants.URN_UUID_PREFIX, "");
//            }
            data.setEntityUUID(id);
        }
        if (title != null && !title.isEmpty()) {
            data.setEntityTitle(title);
            // attrs.put(Constants.OCCI_CORE_TITLE, title);
        }
        if (summary != null && !summary.isEmpty()) {
            data.setEntitySummary(summary);
            // attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
        }
        data.setAttrs(attrs);

        if (kind == null) {
            throw new ParseOCCIException("Kind is not defined for resource: " + id);
        }
        data.setKind(kind);

        data.setLocation(location);

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
        occiRequestDatas.add(data);
        // If there are links defined on this resource, we add them to the input OCCIRequestData linked list.
        // We add the links on input OCCIRequestData after the resources to be sure they are all exists if we are in create mode or update mode.
        if (resource.getLinks() != null && !resource.getLinks().isEmpty()) {
            List<LinkJson> links = resource.getLinks();
            for (LinkJson link : links) {
                parseLinkJsonInput(link);
            }
        }
        super.setInputDatas(occiRequestDatas);
    }

    /**
     * Parse a single link input and return the corresponding OCCIRequestData object.
     *
     * @param link json link object.
     * @throws ParseOCCIException
     */
    private void parseLinkJsonInput(LinkJson link) throws ParseOCCIException {
        Map<String, Object> attrs;
        OCCIRequestData data = new OCCIRequestData();
        String title = link.getTitle();
        String summary = link.getSummary();
        String id = link.getId();
        String kind = link.getKind();
        String location = link.getLocation();
        SourceJson source = link.getSource();
        TargetJson target = link.getTarget();
        String sourceLocation;
        String targetLocation;
        attrs = link.getAttributes();
        List<OCCIRequestData> occiRequestDatas = super.getInputDatas();
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        // set title, summary and id.
        if (id != null) {
//            if (!id.startsWith(Constants.URN_UUID_PREFIX)) {
//                attrs.put(Constants.OCCI_CORE_ID, Constants.URN_UUID_PREFIX + id);
//            } else {
//                attrs.put(Constants.OCCI_CORE_ID, id);
//                id = id.replace(Constants.URN_UUID_PREFIX, "");
//            }
            data.setEntityUUID(id);
        }
        if (title != null && !title.isEmpty()) {
            data.setEntityTitle(title);
            // attrs.put(Constants.OCCI_CORE_TITLE, title);
        }
        if (summary != null && !summary.isEmpty()) {
            data.setEntitySummary(summary);
            // attrs.put(Constants.OCCI_CORE_SUMMARY, summary);
        }
        if (source == null) {
            throw new ParseOCCIException("No source set for link: " + id + " , check your json query.");
        }
        if (target == null) {
            throw new ParseOCCIException("No target set for link: " + id + " , check your json query.");
        }
        sourceLocation = source.getLocation();
        targetLocation = target.getLocation();
        if (sourceLocation == null || sourceLocation.isEmpty()) {
            throw new ParseOCCIException("No source location set for link : " + id + " , check your json query.");
        }
        if (targetLocation == null || targetLocation.isEmpty()) {
            throw new ParseOCCIException("No target location set for link : " + id + " , check your json query.");
        }
        attrs.put(Constants.OCCI_CORE_TARGET, targetLocation);
        attrs.put(Constants.OCCI_CORE_SOURCE, sourceLocation);
        data.setAttrs(attrs);

        if (kind == null) {
            throw new ParseOCCIException("Kind is not defined for resource: " + id);
        }
        data.setKind(kind);

        data.setLocation(location);

        List<String> mixinsRes = link.getMixins();
        if (mixinsRes != null && !mixinsRes.isEmpty()) {
            data.setMixins(mixinsRes);
        }

        occiRequestDatas.add(data);
        super.setInputDatas(occiRequestDatas);
    }

    /**
     * Parse mixin tag define in MixinJson object.
     *
     * @param mixinTag
     * @throws ParseOCCIException
     */
    private void parseMixinJsonTagInput(MixinJson mixinTag) throws ParseOCCIException {
        List<OCCIRequestData> occiRequestDatas = super.getInputDatas();
        String title = mixinTag.getTitle();
        String term = mixinTag.getTerm();
        String location = mixinTag.getLocation();
        String scheme = mixinTag.getScheme();
        Map<String, Object> attrs = mixinTag.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            throw new ParseOCCIException("The attributes on mixin tag must be empty.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new ParseOCCIException("The location on mixin tag must be set, like /mytag/my_stuff/");
        }
        if (term == null || term.trim().isEmpty()) {
            throw new ParseOCCIException("A term must be set for a mixin tag.");
        }
        if (scheme == null || scheme.trim().isEmpty()) {
            throw new ParseOCCIException("A scheme must be set for a mixin tag.");
        }
        // Set the input OCCIRequestData object and add it to the global list.
        OCCIRequestData data = new OCCIRequestData();
        data.setAttrs(attrs);
        data.setMixinTag(scheme + term);
        data.setMixinTagTitle(title);
        data.setLocation(location);
        occiRequestDatas.add(data);
        super.setInputDatas(occiRequestDatas);
    }

    /**
     * Parse action invocation.
     *
     * @param action
     */
    private void parseActionJsonInvocationInput(ActionJson action) {
        List<OCCIRequestData> occiRequestDatas = super.getInputDatas();
        OCCIRequestData data = new OCCIRequestData();
        data.setAction(action.getAction());
        data.setAttrs(action.getAttributes());
        occiRequestDatas.add(data);
        super.setInputDatas(occiRequestDatas);
    }


    //********************
    // Render output part
    //********************

    /**
     * Get interface models.
     *
     * @param interfaceData
     * @param user          (the authorized username)
     * @return a string content.
     * @throws ParseOCCIException
     */
    public String getInterface(final QueryInterfaceData interfaceData, final String user) throws ParseOCCIException {

        String resultJson;

        StringBuilder sb;

        List<Kind> kinds = interfaceData.getKinds();
        List<Mixin> mixins = interfaceData.getMixins();

        if (kinds.isEmpty() && mixins.isEmpty()) {
            LOGGER.warn("No kinds and no mixin to render on interface /-/, if you use a filter this may be not found on the current configuration.");
            return EMPTY_JSON;
        }

        sb = new StringBuilder();
        List<Mixin> extUserTagMixins = new LinkedList<>();
        List<ModelInterfaceJson> models = new LinkedList<>();


        // Build the list of mixins user tags.
        for (Mixin mixin : mixins) {
            if (MixinManager.isMixinTags(mixin.getScheme() + mixin.getTerm(), user)) {
                extUserTagMixins.add(mixin);
            }
        }

        try {
            buildKindsModelsJsonInterface(kinds, models, user);
            buildMixinsModelsJsonInterface(mixins, extUserTagMixins, models, user);

            GlobalModelInterfaceJson globalModelInterfaceJson = new GlobalModelInterfaceJson();
            globalModelInterfaceJson.setModel(models);
            sb.append(globalModelInterfaceJson.toStringJson());

        } catch (JsonProcessingException | ConfigurationException ex) {
            LOGGER.error("Exception thrown when rendering json interface : " + ex.getClass().getName() + " : " + ex.getMessage());
            throw new ParseOCCIException("Exception thrown when rendering json interface : " + ex.getClass().getName() + " : " + ex.getMessage());
        }

        resultJson = sb.toString();
        return resultJson;
    }

    /**
     * @param mixins
     * @param extUserTagMixins
     * @param models
     * @throws ConfigurationException
     */
    private void buildMixinsModelsJsonInterface(List<Mixin> mixins, List<Mixin> extUserTagMixins, List<ModelInterfaceJson> models, final String user) throws ConfigurationException {
        if (mixins.isEmpty()) {
            return;
        }
        boolean found;
        List<ActionInterfaceJson> actionsDefinitionJson;
        List<String> actions;

        for (Mixin mixin : mixins) {
            actionsDefinitionJson = new LinkedList<>();
            actions = new LinkedList<>();
            MixinInterfacejson mixinInterfaceJson = new MixinInterfacejson();
            Optional<String> optLocation = ConfigurationManager.getLocation(mixin);
            String loc;
            if (optLocation.isPresent()) {
                loc = optLocation.get();
            } else {
                loc = null;
            }
            mixinInterfaceJson.setLocation(loc);
            mixinInterfaceJson.setScheme(mixin.getScheme());
            mixinInterfaceJson.setTerm(mixin.getTerm());
            mixinInterfaceJson.setTitle(mixin.getTitle());
            mixinInterfaceJson.setAttributes(buildAttributesInterface(mixin.getAttributes()));

            // Array of string actions.
            if (mixin.getActions() != null && !mixin.getActions().isEmpty()) {
                for (Action action : mixin.getActions()) {
                    actions.add(action.getScheme() + action.getTerm());
                    // Build action definition.
                    ActionInterfaceJson actionInterfaceJson = new ActionInterfaceJson();
                    actionInterfaceJson.setTitle(action.getTitle());
                    actionInterfaceJson.setScheme(action.getScheme());
                    actionInterfaceJson.setTerm(action.getTerm());
                    actionInterfaceJson.setAttributes(buildAttributesInterface(action.getAttributes()));
                    actionsDefinitionJson.add(actionInterfaceJson);
                }
            }
            mixinInterfaceJson.setActions(actions);

            ModelInterfaceJson modelJson = null;
            if (extUserTagMixins.contains(mixin)) {
                if (models.isEmpty()) {
                    modelJson = new ModelInterfaceJson();
                    modelJson.setId(mixin.getScheme());
                    models.add(modelJson);
                }
                for (ModelInterfaceJson model : models) {
                    if (model.getId() != null && !model.getId().isEmpty()
                            && model.getId().equals(mixin.getScheme())) {
                        modelJson = model;
                    }
                }
                if (modelJson == null) {
                    modelJson = new ModelInterfaceJson();
                    modelJson.setId(mixin.getScheme());
                    models.add(modelJson);
                }
            } else {
                Optional<Extension> optExt = MixinManager.getExtensionForMixin(mixin.getScheme() + mixin.getTerm(), user);
                Extension ext;
                if (!optExt.isPresent()) {
                    throw new ConfigurationException("Extension not found for mixin : " + mixin.getScheme() + mixin.getTerm());
                } else {
                    ext = optExt.get();
                }

                if (models.isEmpty()) {
                    modelJson = new ModelInterfaceJson();
                    modelJson.setId(ext.getScheme());
                    models.add(modelJson);
                } else {
                    for (ModelInterfaceJson model : models) {
                        if (model.getId() != null && !model.getId().isEmpty()
                                && model.getId().equals(ext.getScheme())) {
                            modelJson = model;
                            break;
                        }
                    }
                    if (modelJson == null) {
                        modelJson = new ModelInterfaceJson();
                        modelJson.setId(ext.getScheme());
                        models.add(modelJson);
                    }

                }
            }

            modelJson.getMixins().add(mixinInterfaceJson);

            for (ActionInterfaceJson action : actionsDefinitionJson) {
                List<ActionInterfaceJson> actionsDef = modelJson.getActions();
                found = false;
                for (ActionInterfaceJson actionDef : actionsDef) {
                    if ((actionDef.getScheme() + actionDef.getTerm()).equals(action.getScheme() + action.getTerm())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    modelJson.getActions().add(action);
                }
            }
        }

    }

    /**
     * @param kinds
     * @param models
     * @param user
     * @throws ConfigurationException
     */
    private void buildKindsModelsJsonInterface(final List<Kind> kinds, List<ModelInterfaceJson> models, final String user) throws ConfigurationException {
        if (kinds.isEmpty()) {
            return;
        }
        List<ActionInterfaceJson> actionsDefinitionJson;
        List<String> actions;
        boolean found;
        for (Kind kind : kinds) {
            actionsDefinitionJson = new LinkedList<>();
            actions = new LinkedList<>();
            KindInterfaceJson kindInterfaceJson = new KindInterfaceJson();
            Optional<String> optKind = ConfigurationManager.getLocation(kind);
            String locKind;
            if (optKind.isPresent()) {
                locKind = optKind.get();
            } else {
                throw new ConfigurationException("Location for kind : " + kind.getScheme() + kind.getTerm() + " is unknown on extension model");
            }
            kindInterfaceJson.setLocation(locKind);
            if (kind.getParent() != null) {
                kindInterfaceJson.setParent(kind.getParent().getScheme() + kind.getParent().getTerm());
            }
            kindInterfaceJson.setScheme(kind.getScheme());
            kindInterfaceJson.setTerm(kind.getTerm());
            kindInterfaceJson.setTitle(kind.getTitle());

            kindInterfaceJson.setAttributes(buildAttributesInterface(kind.getAttributes()));

            // Array of string actions.
            if (kind.getActions() != null && !kind.getActions().isEmpty()) {
                for (Action action : kind.getActions()) {
                    actions.add(action.getScheme() + action.getTerm());
                    // Build action definition.
                    ActionInterfaceJson actionInterfaceJson = new ActionInterfaceJson();
                    actionInterfaceJson.setTitle(action.getTitle());
                    actionInterfaceJson.setScheme(action.getScheme());
                    actionInterfaceJson.setTerm(action.getTerm());
                    actionInterfaceJson.setAttributes(buildAttributesInterface(action.getAttributes()));
                    actionsDefinitionJson.add(actionInterfaceJson);
                }
            }
            kindInterfaceJson.setActions(actions);
            ModelInterfaceJson modelJson = null;
            Optional<Extension> optExt = KindManager.getExtensionForKind(kind.getScheme() + kind.getTerm(), user);
            Extension ext;
            if (optExt.isPresent()) {
                ext = optExt.get();
            } else {
                throw new ConfigurationException("Unknown extension for kind : " + kind.getScheme() + kind.getTerm());
            }
            if (models.isEmpty()) {
                modelJson = new ModelInterfaceJson();
                modelJson.setId(ext.getScheme());
                models.add(modelJson);
            } else {
                for (ModelInterfaceJson model : models) {
                    if (model.getId() != null && !model.getId().isEmpty()
                            && model.getId().equals(ext.getScheme())) {
                        modelJson = model;
                    }
                }
                if (modelJson == null) {
                    modelJson = new ModelInterfaceJson();
                    modelJson.setId(ext.getScheme());
                    models.add(modelJson);
                }
            }
            modelJson.getKinds().add(kindInterfaceJson);
            for (ActionInterfaceJson action : actionsDefinitionJson) {
                List<ActionInterfaceJson> actionsDef = modelJson.getActions();
                found = false;
                for (ActionInterfaceJson actionDef : actionsDef) {
                    if ((actionDef.getScheme() + actionDef.getTerm()).equals(action.getScheme() + action.getTerm())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    modelJson.getActions().add(action);
                }
            }
        }
    }

    /**
     * Build a collection of attributes json rendering interface.
     *
     * @param attributes
     * @return
     */
    private Map<String, AttributeInterfaceJson> buildAttributesInterface(EList<Attribute> attributes) {

        Map<String, AttributeInterfaceJson> attributesToReturn = new HashMap<>();
        String attrName;
        boolean mutable;
        boolean required;
        String type;
        String description;
        Object defaultObj;
        String defaultStr;
        String typeName;
        for (Attribute attribute : attributes) {
            mutable = attribute.isMutable();
            required = attribute.isRequired();
            attrName = attribute.getName();
            description = attribute.getDescription();
            defaultObj = attribute.getDefault();
            defaultStr = attribute.getDefault();

            // pattern value.
            EDataType dataType = attribute.getType();
            if (dataType != null) {
                type = attribute.getType().getInstanceTypeName();

                if (type == null) {
                    type = convertTypeToSchemaType(attribute.getType().getName());
                    typeName = attribute.getType().getName();
                } else {
                    typeName = type;
                    type = convertTypeToSchemaType(type);
                }
            } else {
                type = convertTypeToSchemaType(null);
                typeName = type;
            }
            AttributeInterfaceJson attrJson = new AttributeInterfaceJson();
            attrJson.setType(type);
            attrJson.setMutable(mutable);
            attrJson.setRequired(required);
            attrJson.setDescription(description);
            if (!type.equals("string") && defaultObj != null) {
                if (type.equals("number")) {
                    try {
                        defaultObj = convertStringToNumber(defaultStr, typeName);
                    } catch (NumberFormatException ex) {
                        LOGGER.error("Number conversion error : " + ex.getMessage() + " default value to convert: " + defaultStr + " for type: " + typeName);
                    }
                }
                if (type.equals("boolean")) {
                    defaultObj = Boolean.valueOf(defaultStr);
                }
                attrJson.setDefaultObj(defaultObj);
            } else {
                attrJson.setDefaultObj(defaultObj);
            }
            // Build pattern property.
            if (!type.equals("string")) {
                attrJson.setPatternType(type);
                attrJson.setPatternPattern("");
            }
            attributesToReturn.put(attrName, attrJson);
        }

        return attributesToReturn;
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
     * Usage with render json interface, for default value rendering.
     *
     * @param value
     * @param typeName
     * @return
     */
    private Number convertStringToNumber(final String value, final String typeName) {
        if (typeName == null) {
            return null;
        }
        Number number = null;
        try {
            switch (typeName.toLowerCase()) {
                case "integer":
                case "int":
                    number = Integer.valueOf(value);
                    break;

                case "float":
                case "double":
                    number = Double.valueOf(value);
                    break;
                case "long":
                    number = Long.valueOf(value);
                    break;
                case "bigdecimal":
                    number = new BigDecimal(value);
                    break;
            }
            return number;
        } catch (NumberFormatException ex) {
            LOGGER.error("Cant convert the string: " + value + " to a valid number.");
            throw ex;
        }

    }

    @Override
    public String parseMessage(final String message) throws ParseOCCIException {
        MessageJson msgJson = new MessageJson();
        msgJson.setMessage(message);
        String jsonResult;
        try {
            jsonResult = msgJson.toStringJson();
        } catch (JsonProcessingException ex) {
            throw new ParseOCCIException(ex.getMessage(), ex);
        }
        return jsonResult;
    }

    /**
     * Render one entity as output String.
     *
     * @param entity
     * @return
     */
    @Override
    public String renderOutputEntity(final Entity entity) throws ParseOCCIException {
        String response;
        List<Entity> entities = new LinkedList<>();
        entities.add(entity);
        response = renderOutputEntities(entities);
        return response;
    }

    /**
     * Parse entity or entity collection in output.
     *
     * @param entities
     * @return
     */
    @Override
    public String renderOutputEntities(final List<Entity> entities) throws ParseOCCIException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (entities == null || entities.isEmpty()) {
            // Must never be thrown...
            throw new ParseOCCIException("There is no entities to render.");
        }
        String response;
        OcciMainJson mainJson = new OcciMainJson();
        List<ResourceJson> resources = new LinkedList<>();
        ResourceJson currentResource = null;
        LinkJson currentLink = null;
        List<LinkJson> links = new LinkedList<>();
        for (Entity entity : entities) {
            if (entity instanceof Link) {
                LinkJson linkJson = buildLinkJsonFromEntity(entity);
                currentLink = linkJson;
                links.add(linkJson);

            } else {
                ResourceJson resJson = buildResourceJsonFromEntity(entity);
                currentResource = resJson;
                resources.add(resJson);
            }
        }
        if (!resources.isEmpty()) {
            mainJson.setResources(resources);
        }
        if (!links.isEmpty()) {
            mainJson.setLinks(links);
        }
        try {
            response = mainJson.toStringJson();

            if (entities.size() == 1 && !resources.isEmpty() && resources.size() == 1 && currentResource != null) {
                // One entity to render.
                LOGGER.info("One entity resource to render.");
                response = currentResource.toStringJson();
            }
            if (entities.size() == 1 && resources.isEmpty() && !links.isEmpty() && links.size() == 1 && currentLink != null) {
                LOGGER.info("One entity link to render.");
                response = currentLink.toStringJson();
            }

            // Convert entities to a list of outputData, the purpose here is to let the developer use the parsed result or use a list of output container data.
            super.convertEntitiesToOutputData(entities);

        } catch (JsonProcessingException ex) {
            throw new ParseOCCIException(ex.getMessage(), ex);
        }
        return response;
    }

    /**
     * Used for uri-list content type with application/json rendering.
     *
     * @param locations
     * @return
     * @throws ParseOCCIException
     */
    @Override
    public String renderOutputEntitiesLocations(final List<String> locations) throws ParseOCCIException {
        String response;
        if (!locations.isEmpty()) {
            // Build a json object output.
            LocationsJson locationsJson = new LocationsJson();
            locationsJson.setLocations(locations);
            try {
                response = locationsJson.toStringJson();

                // Output datas will give a list of datas location only.
                super.convertLocationsToOutputDatas(locations);

            } catch (JsonProcessingException ex) {
                throw new ParseOCCIException("Cannot parse the object to application/json representation : " + ex.getMessage(), ex);
            }
        } else {
            throw new ParseOCCIException("No entities locations were found.");
        }
        return response;
    }

    /**
     * Build a resource for entity object model.
     *
     * @param entity
     * @return
     */
    private ResourceJson buildResourceJsonFromEntity(final Entity entity) {
        ResourceJson resJson = new ResourceJson();
        Resource res = (Resource) entity;
        Kind kind = res.getKind();
        List<Mixin> mixins;
        List<String> mixinsStr = new LinkedList<>();
        List<LinkJson> links = new LinkedList<>();
        resJson.setKind(kind.getScheme() + kind.getTerm());
        resJson.setId(Constants.URN_UUID_PREFIX + res.getId());
        resJson.setTitle(res.getTitle());
        resJson.setSummary(res.getSummary());
        resJson.setLocation(EntityManager.getLocation(entity, getUser()));

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
            // We must not include occi.core.title, occi.core.summary and occi.core.id as these attributes are already defined in the json output content (title, id, summary attributes).
            if (!key.equals(Constants.OCCI_CORE_SUMMARY) && !key.equals(Constants.OCCI_CORE_TITLE)
                    && !key.equals(Constants.OCCI_CORE_ID)) {
                Optional<String> optValStr = EntityManager.getAttrValueStr(entity, key);
                Optional<Number> optValNumber = EntityManager.getAttrValueNumber(entity, key);

                if (optValStr.isPresent()) {
                    attributes.put(key, optValStr.get());
                } else if (optValNumber.isPresent()) {
                    attributes.put(key, optValNumber.get());
                } else {
                    if (val != null) {
                        attributes.put(key, val);
                    }
                }

            }
//            if (key.equals(Constants.OCCI_CORE_ID)) {
//                if (val != null && !val.startsWith(Constants.URN_UUID_PREFIX)) {
//                    val = Constants.URN_UUID_PREFIX + val;
//                }
//                attributes.put(key, val);
//            }
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

    /**
     * Build a link entity from Entity object model.
     *
     * @param entity
     * @return
     */
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
        linkJson.setId(Constants.URN_UUID_PREFIX + link.getId());
        linkJson.setTitle(link.getTitle());
        linkJson.setLocation(EntityManager.getLocation(entity, getUser()));
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
                    Optional<EDataType> optEDataType = EntityManager.getEAttributeType(entity, key);
                    EDataType eAttrType;

                    if (optEDataType.isPresent()) {
                        eAttrType = optEDataType.get();
                        if (eAttrType instanceof EEnum || eAttrType.getInstanceClass() == String.class) {
                            // value with quote only for String and EEnum type.
                            attributes.put(key, val);
                        } else {
                            // Not a string nor an enum val.
                            try {
                                Number num = ConfigurationManager.parseNumber(val, eAttrType.getInstanceClassName());
                                attributes.put(key, num);
                            } catch (NumberFormatException ex) {
                                attributes.put(key, val);
                            }
                        }
                    } else {
                        // Cant determine the type.
                        attributes.put(key, val);
                    }
                }
//                if (key.equals(Constants.OCCI_CORE_ID)) {
//                    if (!val.startsWith(Constants.URN_UUID_PREFIX)) {
//                        val = Constants.URN_UUID_PREFIX + val;
//                    }
//                    attributes.put(key, val);
//                }
            }
        }
        Resource resSrc = link.getSource();
        Resource resTarget = link.getTarget();
        SourceJson src = new SourceJson();
        TargetJson target = new TargetJson();
        String relativeLocation = EntityManager.getLocation(resSrc, getUser());
        src.setKind(resSrc.getKind().getScheme() + resSrc.getKind().getTerm());
        if (!relativeLocation.startsWith("/")) {
            relativeLocation = "/" + relativeLocation;
        }
        src.setLocation(relativeLocation);
        relativeLocation = EntityManager.getLocation(resTarget, getUser());
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

}
