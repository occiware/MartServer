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
package org.occiware.mart.server.facade;

import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.exception.ModelValidatorException;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.parser.DummyParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.parser.text.TextOcciParser;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;

/**
 * Created by cgourdin on 10/04/2017.
 */
public abstract class AbstractOCCIRequest implements OCCIRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOCCIRequest.class);

    protected String contentType;
    private List<ContentData> contentDatas = new LinkedList<>();
    private OCCIResponse occiResponse;

    private String location = null;

    private String username = ConfigurationManager.DEFAULT_OWNER;

    /**
     * Input parser.
     */
    private IRequestParser inputParser;


    /**
     * @param response
     * @param contentType
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType) {
        this.occiResponse = response;
        this.contentType = contentType;
        this.inputParser = buildParser();

    }

    /**
     * @param response
     * @param contentType
     * @param username
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType, final String username) {
        this.occiResponse = response;
        this.contentType = contentType;
        if (username == null || username.isEmpty()) {
            this.username = ConfigurationManager.DEFAULT_OWNER;
        } else {
            this.username = username;
        }
        this.inputParser = buildParser();

    }

    /**
     * @param response
     * @param contentType
     * @param location
     * @param username
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType, final String location, final String username) {
        this.occiResponse = response;
        this.occiResponse.setUsername(username);
        this.contentType = contentType;
        this.location = location;
        if (!location.startsWith("/")) {
            this.location = "/" + location;
        }
        if (!location.endsWith("/")) {
            this.location = location + "/";
        }

        if (username == null || username.isEmpty()) {
            this.username = ConfigurationManager.DEFAULT_OWNER;
        } else {
            this.username = username;
        }
        this.inputParser = buildParser();
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setOCCIResponse(OCCIResponse occiResponse) {
        this.occiResponse = occiResponse;
        this.occiResponse.setUsername(username);
    }

    @Override
    public List<ContentData> getContentDatas() {
        if (contentDatas == null) {
            contentDatas = new LinkedList<>();
        }
        return contentDatas;
    }

    @Override
    public void setContentDatas(List<ContentData> contentDatas) {
        if (contentDatas == null) {
            this.contentDatas = new LinkedList<>();
        } else {
            this.contentDatas = contentDatas;
        }
    }

    @Override
    public OCCIResponse findEntity() {

        for (ContentData contentData : contentDatas) {

            Entity entity;
            try {
                if (contentData.getEntityUUID() == null || contentData.getEntityUUID().trim().isEmpty()) {
                    // Try to find entity by its location
                    entity = EntityManager.findEntityFromLocation(location, username);
                } else {
                    // Try to find entity by its id.
                    entity = EntityManager.findEntity(username, location);
                }
                if (entity == null) {
                    throw new ConfigurationException("Entity on location: " + contentData.getLocation() + " doesnt exist.");
                }
                // Load entity attributes, mixins and kind to output.
                entity.occiRetrieve(); // Refresh values.
                renderEntityOutput(entity);

            } catch (ConfigurationException ex) {
                occiResponse.setExceptionMessage(ex.getMessage());
                occiResponse.setExceptionThrown(ex);
            }
            // We take the first one.
            break;
        }
        return this.occiResponse;
    }

    @Override
    public OCCIResponse findEntities(CollectionFilter filter) {
        try {
            List<Entity> entities = EntityManager.findAllEntities(username, filter);

            for (Entity entity : entities) {
                // Refresh object.
                entity.occiRetrieve();

                // Build contentDatas output.
                ContentData outputContentData = new ContentData();
                Map<String, Object> attrs = EntityManager.convertEntityAttributesToMap(entity);
                List<String> mixins = MixinManager.convertEntityMixinsToList(entity);
                String kind = entity.getKind().getScheme() + entity.getKind().getTerm();
                outputContentData.setKind(kind);
                outputContentData.setMixins(mixins);
                outputContentData.setAttrs(attrs);
                occiResponse.getContentDatas().add(outputContentData);
            }

            IRequestParser outputParser = occiResponse.getOutputParser();
            if (entities.isEmpty() && outputParser != null) {
                outputParser.renderOutputEntities(entities);
            }
        } catch (ParseOCCIException ex) {
            occiResponse.setExceptionMessage(ex.getMessage());
            occiResponse.setExceptionThrown(ex);
        }
        return this.occiResponse;
    }

    @Override
    public boolean isCategoryTerm(final String categoryTerm) {
        return this.getCategorySchemeTerm(categoryTerm) != null;
    }

    @Override
    public String getCategorySchemeTerm(final String categoryTerm) {
        return ConfigurationManager.findCategorySchemeTermFromTerm(categoryTerm, username);
    }

    @Override
    public String getMixinTagSchemeTermFromLocation(String location) {
        String result = null;
        Mixin mixin = MixinManager.getUserMixinFromLocation(location, username);
        if (mixin != null) {
            result = mixin.getScheme() + mixin.getTerm();
        }
        return result;
    }

    @Override
    public OCCIResponse getInterface(final String categoryFilter) {
        QueryInterfaceData interfData = new QueryInterfaceData();

        interfData.setCategoryFilter(categoryFilter);
        try {
            ConfigurationManager.applyFilterOnInterface(categoryFilter, interfData, username);
            occiResponse.setQueryInterfaceData(interfData);
            // Render output.
            occiResponse.getOutputParser().getInterface(username);

        } catch (ConfigurationException | ParseOCCIException ex) {
            occiResponse.setExceptionMessage(ex.getMessage());
            occiResponse.setExceptionThrown(ex);
        }

        return this.occiResponse;
    }

    @Override
    public OCCIResponse createEntity() {
        if (contentDatas.isEmpty()) {
            occiResponse.setExceptionMessage("Cant create entity without entity data.");
            occiResponse.setExceptionThrown(new ConfigurationException("Cant create entity"));
            return occiResponse;
        }
        ContentData data = contentDatas.get(0);
        // Create entity with data content.
        Entity entity = createEntityOnConfig(data);
        if (entity == null || occiResponse.hasExceptions()) {
            return occiResponse;
        }
        renderEntityOutput(entity);

        return this.occiResponse;
    }

    @Override
    public OCCIResponse defineMixinTags() {
        String message;
        if (contentDatas.isEmpty()) {
            message = "No datas defined for defining with mixin tag.";
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(new ConfigurationException(message));
            return occiResponse;
        }

        for (ContentData data : contentDatas) {
            defineMixinTag(data);
        }

        return this.occiResponse;
    }

    /**
     * Define or apply mixin tags...
     * @param data
     */
    private void defineMixinTag(final ContentData data) {
        String message;
        if (data == null) {
            message = "No datas defined for creation with mixin tag.";
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(new ConfigurationException(message));
            return;
        }

        String mixinTag = data.getMixinTag();
        if (mixinTag == null || mixinTag.trim().isEmpty()) {
            message = "No mixin tag id, defined for creation with mixin tag.";
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(new ConfigurationException(message));
            return;
        }
        LOGGER.info("Define mixin tag : " + mixinTag);

        String mixinLocation = data.getLocation();
        String title = data.getMixinTagTitle();
        List<String> xocciLocations = data.getXocciLocations();
        try {
            if ((mixinLocation == null || mixinLocation.isEmpty()) && (xocciLocations == null || xocciLocations.isEmpty())) {
                message = "No location is defined for this mixin tag: " + mixinTag;
                occiResponse.setExceptionMessage(message);
                occiResponse.setExceptionThrown(new ConfigurationException(message));
                return;
            }
            if (mixinLocation != null) {
                MixinManager.addUserMixinOnConfiguration(mixinTag, title, mixinLocation, username);
            }
            if (xocciLocations != null && !xocciLocations.isEmpty()) {

                // Get the mixin scheme+term from path.
                String categoryId = ConfigurationManager.getCategoryFilterSchemeTerm(location, ConfigurationManager.DEFAULT_OWNER);
                if (categoryId == null) {
                    throw new ConfigurationException("Category is not defined");
                }

                List<String> entities = new ArrayList<>();

                for (String xOcciLocation : xocciLocations) {
                    // Build a list of entities from xoccilocations defined.
                    if (EntityManager.isEntityUUIDProvided(xOcciLocation, new HashMap<>())) {
                        // One entity.
                        String uuid = EntityManager.getUUIDFromPath(xOcciLocation, new HashMap<>());
                        if (uuid == null) {
                            message = Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore";
                            occiResponse.setExceptionMessage(message);
                            occiResponse.setExceptionThrown(new ConfigurationException(message));
                            return;
                        }
                        Entity entity = EntityManager.findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
                        if (entity == null) {
                            message = Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore";
                            occiResponse.setExceptionMessage(message);
                            occiResponse.setExceptionThrown(new ConfigurationException(message));
                            return;
                        }
                        entities.add(entity.getId());
                    } else {
                        // Maybe a collection on inbound or outbound path.
                        List<String> entitiesTmp = EntityManager.getEntityUUIDsFromPath(xOcciLocation);
                        if (!entitiesTmp.isEmpty()) {
                            for (String entity : entitiesTmp) {
                                entities.add(entity);
                            }
                        }
                    }
                }
                // Full update mode.
                if (!entities.isEmpty()) {
                    MixinManager.saveMixinForEntities(categoryId, entities, true, ConfigurationManager.DEFAULT_OWNER);
                }
            }
        } catch (ConfigurationException ex) {
            message = ex.getMessage();
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(ex);
        }

    }

    @Override
    public OCCIResponse createEntities() {

        List<Entity> createdEntities = new LinkedList<>();


        for (ContentData data : contentDatas) {
            Entity entity;
            if (data.getMixinTag() != null) {
                // There are mixin tag definition in the collection.
                entity = null;
                // Define mixin tag.
                this.defineMixinTag(data);

            } else {
                entity = createEntityOnConfig(data);
            }
            if (occiResponse.hasExceptions()) {
                return occiResponse;
            }
            if (entity != null) {
                createdEntities.add(entity);
            }
        }

        if (!createdEntities.isEmpty()) {
            try {
                // TODO : If uri-list combined with other renderings then we render output locations, not the entities.
                // occiResponse.getOutputParser().renderOutputEntitiesLocations(locations);
                occiResponse.getOutputParser().renderOutputEntities(createdEntities);

            } catch (ParseOCCIException ex) {
                occiResponse.setExceptionMessage(ex.getMessage());
                occiResponse.setExceptionThrown(ex);
            }
        }



        return occiResponse;
    }

    private Entity createEntityOnConfig(final ContentData data) {
        String location = data.getLocation();
        if (location == null || location.trim().isEmpty()) {
            LOGGER.warn("No location attribute set, rely on global location path");
            location = this.location;
        }

        boolean isResource = EntityManager.checkIfEntityIsResourceOrLinkFromAttributes(data.getAttrs());
        // Define full overwrite of an existing entity.
        boolean overwrite = false;

        String entityId = data.getEntityUUID();
        // Check if entityId is null or there is a uuid on path, if this is not the case, generate the uuid and add it to location.
        if (entityId == null || entityId.trim().isEmpty()) {
            // Create a new uuid.
            entityId = ConfigurationManager.createUUID();
        }
        String kind = data.getKind();
        if (kind == null || kind.trim().isEmpty()) {
            occiResponse.setExceptionMessage("Cant create entity without kind.");
            occiResponse.setExceptionThrown(new ConfigurationException("Cant create entity without kind"));
            return null;
        }

        LOGGER.info("Create entity with location: " + location);
        LOGGER.info("Kind: " + kind);
        List<String> mixins = data.getMixins();
        Map<String, Object> attributes = data.getAttrs();
        for (String mixin : mixins) {
            LOGGER.info("Mixin : " + mixin);
        }
        LOGGER.info("Attributes: ");
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            LOGGER.info(entry.getKey() + " ---> " + entry.getValue());
        }

        // check the uuid validity
        if (!EntityManager.isUUIDValid(entityId)) {
            String message = "Entity uuid is not valid : " + entityId + ", check the entity identifier, it must be set to a uuid v4 format, like -> f88486b7-0632-482d-a184-a9195733ddd0 ";
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(new ConfigurationException(message));
            return null;
        }

        // Check if there is a conflict between declared entityUUID and attribute uuid (core.id) if any.
        // After define if full overwrite or initial creation.
        if (EntityManager.isEntityExist(username, entityId)) {
            // Check if occi.core.id reference another id.
            if (attributes.containsKey(Constants.OCCI_CORE_ID)) {
                // check if this is the same id, if not there is a conflict..
                Object coreIdObj = attributes.get(Constants.OCCI_CORE_ID);
                if (coreIdObj == null) {
                    // Get the id attribute.
                    coreIdObj = attributes.get("id");
                }

                // Check if urn:uuid: is set.
                if (coreIdObj != null) {
                    String coreId = (String) coreIdObj;
                    if (coreId.contains(Constants.URN_UUID_PREFIX)) {
                        coreId = coreId.replace(Constants.URN_UUID_PREFIX, "");
                    }
                    if (!coreId.equals(entityId)) {
                        String message = "The attribute occi.core.id value is not the same as the uuid specified in location or entity.";
                        occiResponse.setExceptionMessage(message);
                        occiResponse.setExceptionThrown(new ConfigurationException(message));
                        return null;
                    }
                }
            }
            overwrite = true;
            LOGGER.info("Overwriting entity : " + location);
        } else {
            LOGGER.info("Creating entity : " + location);
        }
        Map<String, String> attributesStr = data.getAttrsValStr();
        try {
            String coreId = Constants.URN_UUID_PREFIX + entityId;
            attributesStr.put("occi.core.id", coreId);
            attributes.put("occi.core.id", coreId);
            if (isResource) {
                EntityManager.addResourceToConfiguration(entityId, kind, mixins, attributesStr, username, location);
            } else {
                Object src = attributes.get(Constants.OCCI_CORE_SOURCE);
                String srcStr;
                Object target = attributes.get(Constants.OCCI_CORE_TARGET);
                String targetStr;
                if (src == null) {
                    String message = "No source provided for this link : " + entityId;
                    LOGGER.error(message);
                    occiResponse.setExceptionMessage(message);
                    occiResponse.setExceptionThrown(new ConfigurationException(message));
                    return null;
                }
                if (target == null) {
                    String message = "No target provided for this link : " + entityId;
                    LOGGER.error(message);
                    occiResponse.setExceptionMessage(message);
                    occiResponse.setExceptionThrown(new ConfigurationException(message));
                    return null;
                }
                srcStr = (String) src;
                targetStr = (String) target;
                EntityManager.addLinkToConfiguration(entityId, kind, mixins, srcStr, targetStr, attributesStr, username, location);
            }
        } catch (ConfigurationException ex) {
            String message = "The entity has not been added, it may be produce if you use non referenced attributes. Message: " + ex.getMessage();
            LOGGER.error(message);
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(ex);
            return null;
        }


        // Execute model CRUD method.
        Entity entity = EntityManager.findEntity(username, entityId);
        if (entity != null) {
            if (overwrite) {
                entity.occiUpdate();
                LOGGER.info("Update entity done returning location : " + location);
            } else {
                entity.occiCreate();
                LOGGER.info("Create entity done returning location : " + location);
            }

        } else {
            String message = "Entity " + entityId + " was not created on object model, please check your query.";
            LOGGER.error(message);
            occiResponse.setExceptionMessage(message);
            occiResponse.setExceptionThrown(new ConfigurationException(message));
            return null;
        }
        return entity;
    }

    @Override
    public OCCIResponse applyMixins() {
        return null;
    }

    @Override
    public OCCIResponse executeAction() {
        return null;
    }

    @Override
    public Future<OCCIResponse> executeAsyncAction() {
        return null;
    }

    @Override
    public OCCIResponse updateEntities() {
        return null;
    }

    @Override
    public OCCIResponse deleteEntity() {
        return null;
    }

    @Override
    public OCCIResponse deleteEntities() {
        return null;
    }

    @Override
    public OCCIResponse removeMixinAssociation() {
        return null;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }


    @Override
    public abstract void parseInput() throws ParseOCCIException;

    @Override
    public OCCIResponse validateDataContentRequest() {
        String kind;
        Kind kindModel;
        List<String> mixins;
        String mixinTag;
        Map<String, Object> attrs;
        Map<String, Object> attrsToControlOnActions;
        Map<String, Object> attrsToControlOnMixins;
        String action;
        String attrKey;
        // TODO : Control input value datatype with models.
        Object attrValue;
        boolean found;
        for (ContentData content : contentDatas) {
            // Is the kind exist on this configuration's extensions ?
            kind = content.getKind();
            attrs = content.getAttrs();
            mixins = content.getMixins();
            mixinTag = content.getMixinTag();
            action = content.getAction();
            attrsToControlOnActions = new LinkedHashMap<>();
            attrsToControlOnMixins = new LinkedHashMap<>();
            found = false;

            if (kind != null) {
                // Check the scheme+term on extensions.
                kindModel = KindManager.findKindFromExtension(username, kind);
                if (kindModel == null) {
                    this.occiResponse.setExceptionMessage("The kind : " + kind + " doesnt exist on extensions.");
                    this.occiResponse.setExceptionThrown(new ModelValidatorException("The kind : " + kind + " doesnt exist on extensions."));
                    return occiResponse;
                }

                // Check the attributes.
                if (!attrs.isEmpty()) {

                    for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                        attrKey = entry.getKey();
                        attrValue = entry.getValue();
                        found = false;
                        for (Attribute attribModel : kindModel.getAttributes()) {
                            if (attribModel.getName().equals(attrKey)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            // Add the attribute to control on mixins and action (if one on request).
                            attrsToControlOnActions.put(attrKey, attrValue);
                            attrsToControlOnMixins.put(attrKey, attrValue);
                        }
                    }
                }
                found = false;
                if (action != null) {
                    Action actionModelWork = null;
                    Kind currentKind = kindModel;
                    while(currentKind != null && !found) {

                        for (Action actionModel : currentKind.getActions()) {
                            if ((actionModel.getScheme() + actionModel.getTerm()).equals(action)) {
                                // The action is referenced on this kind.
                                found = true;
                                actionModelWork = actionModel;
                                break;
                            }
                        }
                        if (!found) {
                            currentKind = currentKind.getParent();
                        }
                    }

                    if (!found) {
                        // Action is not referenced on this kind.
                        this.occiResponse.setExceptionMessage("The action : " + action + " doesnt exist for this kind : " + kind + " and all parents");
                        this.occiResponse.setExceptionThrown(new ModelValidatorException("The kind : " + kind + " doesnt exist on extensions."));
                        return occiResponse;
                    }

                    found = false;
                    // Control the action attributes if necessary.
                    if (!attrsToControlOnActions.isEmpty()) {

                        for (Map.Entry<String, Object> entry : attrsToControlOnActions.entrySet()) {
                            attrKey = entry.getKey();
                            attrValue = entry.getValue();
                            found = false;
                            for (Attribute attr : actionModelWork.getAttributes()) {
                                if (attr.getName().equals(attrKey)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                attrsToControlOnMixins.remove(attrKey);
                            }

                        }
                    }


                }
                found = false;
                if (mixins.isEmpty()) {
                    // no mixins to control...
                    if (!attrsToControlOnMixins.isEmpty()) {
                        String attributes = "";
                        for (Map.Entry<String, Object> entry : attrsToControlOnMixins.entrySet()) {
                            attributes = attributes + entry.getKey() + ";";
                        }
                        this.occiResponse.setExceptionMessage("Some attributes were not found on referenced models : " + attributes);
                        this.occiResponse.setExceptionThrown(new ModelValidatorException("Some attributes were not found on referenced models : " + attributes));
                    }
                } else {
                    // There are mixins contents....
                    Mixin mixinModel;
                    // mixins exists ?
                    // Are there applyable to this kind ?
                    for (String mixin : mixins) {
                        mixinModel = MixinManager.findMixinOnExtension(username, mixin);
                        if (mixinModel == null) {
                            this.occiResponse.setExceptionMessage("The mixin : " + mixin + " doesnt exist on extensions.");
                            this.occiResponse.setExceptionThrown(new ModelValidatorException("The model : " + mixin + " doesnt exist on extensions."));
                            return occiResponse;
                        }
                        // Check the attributes if any.
                        if (!attrsToControlOnMixins.isEmpty()) {
                            found = false;
                            String message = null;
                            for (Map.Entry<String, Object> entry : attrsToControlOnMixins.entrySet()) {
                                attrKey = entry.getKey();
                                for (Attribute attr : mixinModel.getAttributes()) {
                                    if (attrKey.equals(attr.getName())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    message = "" + attrKey + ";";
                                }

                            }
                            if (message != null) {
                                this.occiResponse.setExceptionMessage("Some attributes were not found on referenced models : " + message);
                                this.occiResponse.setExceptionThrown(new ModelValidatorException("Some attributes were not found on referenced models : " + message));
                                return occiResponse;
                            }
                        }
                    }
                }
            } // kind != null

            // if this is an action invocation, is this action is present on extension's or present in parent kind of the entity.
            // Are the actions attributes are correct and defined in extension ?
            if (kind == null && action != null) {
                Action actionModelWork = ConfigurationManager.findActionOnExtensions(action, username);
                if (actionModelWork == null) {
                    this.occiResponse.setExceptionMessage("Action : " + action + " doesnt exist on models.");
                    this.occiResponse.setExceptionThrown(new ModelValidatorException("Action : " + action + " doesnt exist on models."));
                    return occiResponse;
                }

                // Control the attributes.
                if (!attrs.isEmpty()) {
                    String message = null;
                    for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                        found = false;
                        attrKey = entry.getKey();

                        for (Attribute attr : actionModelWork.getAttributes()) {
                            if (attr.getName().equals(attrKey)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            message += "" + attrKey + ";";
                        }
                    }
                    if (message != null) {
                        this.occiResponse.setExceptionMessage("Some action attributes are not defined in action model : " + message);
                        this.occiResponse.setExceptionThrown(new ModelValidatorException("Some action attributes are not defined in action model : " + message));
                        return this.occiResponse;
                    }
                }
            }
        }

        return this.occiResponse;
    }


    /**
     * Build a parser relative to contentType input.
     * @return
     */
    private IRequestParser buildParser() {
        if (contentType == null) {
            // Default content type if none on headers.
            return new JsonOcciParser(this);
        }
        switch (contentType) {
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                LOGGER.info("Parser request: TextOcciParser");
                return new TextOcciParser(this);

            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
                LOGGER.info("Parser request: JsonOcciParser");
                return new JsonOcciParser(this);
            // You can add here all other parsers you need without updating class like GetQuery, PostQuery etc.
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                // TODO : plain text parser.
                return new DummyParser(this);

            case Constants.MEDIA_TYPE_TEXT_URI_LIST:
                // TODO : test uri list parser and combined parsers (json + uri list for example).
                return new DummyParser(this);

            default:
                // No parser.
                LOGGER.warn("The parser for " + contentType + " doesnt exist !");
                return new DummyParser(this);
            // throw new ParseOCCIException("The parser for " + contentType + " doesnt exist !");
        }

    }

    @Override
    public IRequestParser getInputParser() {
        return this.inputParser;
    }

    @Override
    public void setInputParser(IRequestParser inputParser) {
        this.inputParser = inputParser;
    }


    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isCategoryLocation(final String location) {
        boolean onCategoryLocation = false;
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        String[] fragments = location.split("/");
        if (fragments.length > 0) {
            // Check only the first and the second fragment.
            if (isCategoryTerm(fragments[0]) || (fragments.length > 1 && isCategoryTerm(fragments[1]))) {
                onCategoryLocation = true;
            }
        }
        return onCategoryLocation;

    }

    @Override
    public boolean isMixinTagLocation(final String location) {
        boolean onMixinTagLocation = false;
        if (MixinManager.getUserMixinFromLocation(location, username) != null) {
            onMixinTagLocation = true;
        }
        return onMixinTagLocation;
    }

    @Override
    public boolean isEntityLocation(final String location) {
        boolean onEntityLocation = false;
        if (EntityManager.findEntityFromLocation(location, username) != null) {
            onEntityLocation = true;
        }
        return onEntityLocation;
    }

    /**
     * Parse output response for an entity.
     * @param entity
     */
    private void renderEntityOutput(final Entity entity) {
        // Render output entity.
        ContentData outputContentData = new ContentData();
        Map<String, Object> entityAttrs = EntityManager.convertEntityAttributesToMap(entity);
        List<String> entityMixins = MixinManager.convertEntityMixinsToList(entity);
        String entityKind = entity.getKind().getScheme() + entity.getKind().getTerm();
        outputContentData.setKind(entityKind);
        outputContentData.setMixins(entityMixins);
        outputContentData.setAttrs(entityAttrs);
        occiResponse.getContentDatas().add(outputContentData);

        IRequestParser outputParser = occiResponse.getOutputParser();
        if (outputParser != null) {
            try {
                // Launch the parsing output (for json, text/occi and other implementations).
                outputParser.renderOutputEntity(entity);

            } catch (ParseOCCIException ex) {
                String message = "Exception thrown when rendering entity response, message: " + ex.getMessage();
                LOGGER.error(message);
                occiResponse.setExceptionMessage(message);
                occiResponse.setExceptionThrown(ex);
            }
        }
    }

}
