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
import org.occiware.mart.server.exception.ResourceNotFoundException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This abstract class has generic methods to address OCCI runtime core model managers.
 * Concrete implementation may have more methods to manage datas in input and output.
 * Created by cgourdin on 24/04/2017.
 */
public class AbstractOCCIApiInputRequest implements OCCIApiInputRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOCCIApiInputRequest.class);

    private final String username;
    private OCCIApiResponse occiApiResponse;
    private IRequestParser inputParser;

    public AbstractOCCIApiInputRequest(String username, OCCIApiResponse occiApiResponse, IRequestParser inputParser) {
        if (username == null) {
            username = ConfigurationManager.DEFAULT_OWNER;
        }
        this.username = username;
        this.occiApiResponse = occiApiResponse;
        this.inputParser = inputParser;
        // Initialize configuration manager for this owner (if not initialized previously).
        ConfigurationManager.getConfigurationForOwner(username);
    }


    @Override
    public OCCIApiResponse getModelsInterface(final String categoryFilter, final String extensionFilter) {
        QueryInterfaceData interfData = new QueryInterfaceData();

        interfData.setCategoryFilter(categoryFilter);
        try {
            // TODO : Apply extension filter.
            ConfigurationManager.applyFilterOnInterface(categoryFilter, interfData, username);

            // Render output.
            Object result = occiApiResponse.getOutputParser().getInterface(interfData, username);
            occiApiResponse.setResponseMessage(result);

        } catch (ConfigurationException | ParseOCCIException ex) {
            parseConfigurationExceptionMessageOutput(ex.getMessage());
        }

        return this.occiApiResponse;
    }

    @Override
    public OCCIApiResponse createEntity(final String title, final String summary,
                                        final String kind, final List<String> mixins,
                                        final Map<String, String> attributes, final String location) {
        String message;
        try {
            Entity entity = createEntityOnConfiguration(title, summary, kind, mixins, attributes, location);
            renderEntityOutput(entity);
        } catch (ConfigurationException ex) {
            message = ex.getMessage();
            parseConfigurationExceptionMessageOutput(message);
        }

        return this.occiApiResponse;
    }

    /**
     * Create an new entity (or full update) on configuration model.
     *
     * @param title
     * @param summary
     * @param kind
     * @param mixins
     * @param attributes
     * @param location
     * @return the entity created or full updated.
     * @throws ConfigurationException
     */
    private Entity createEntityOnConfiguration(final String title, final String summary,
                                               final String kind, final List<String> mixins,
                                               final Map<String, String> attributes, final String location) throws ConfigurationException {
        String message;
        if (location == null || location.trim().isEmpty()) {
            message = "No location set for the entity : " + kind;
            throw new ConfigurationException(message);
        }
        if (kind == null || kind.trim().isEmpty()) {
            message = "Cant create entity without kind.";
            throw new ConfigurationException(message);
        }

        boolean isResource = EntityManager.checkIfEntityIsResourceOrLinkFromAttributes(attributes);

        // Define full overwrite of an existing entity.
        boolean overwrite = false;
        Optional<Entity> optEntity = EntityManager.findEntityFromLocation(location, username);
        Entity entity;

        String entityId = null;

        if (optEntity.isPresent()) {
            entity = optEntity.get();
            LOGGER.debug("Entity : " + location + " exist");
            overwrite = true;
            entityId = entity.getId();
            LOGGER.info("Overwriting entity : " + location);
        } else {
            LOGGER.info("Creating entity : " + location + " with kind : " + kind + " with title: " + title);
        }
        if (entityId == null) {
            Optional<String> optUuid = EntityManager.getUUIDFromPath(location, attributes);
            if (optUuid.isPresent()) {
                entityId = optUuid.get();
            }
        }

        // Check if entityId is null or there is a uuid on path, if this is not the case, generate the uuid.
        if (entityId == null || entityId.trim().isEmpty()) {
            // Create a new uuid.
            entityId = ConfigurationManager.createUUID();
        }
        LOGGER.info("Create entity with location: " + location + " with id : " + entityId + " with title : " + title);
        LOGGER.debug("Kind: " + kind);
        for (String mixin : mixins) {
            LOGGER.debug("Mixin : " + mixin);
        }
        LOGGER.debug("Attributes: ");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            LOGGER.debug(entry.getKey() + " ---> " + entry.getValue());
        }

        // check the uuid validity
        if (!EntityManager.isUUIDValid(entityId)) {
            message = "Entity uuid is not valid : " + entityId + ", check the entity identifier, it must be set to a uuid v4 format, like -> f88486b7-0632-482d-a184-a9195733ddd0 ";
            throw new ConfigurationException(message);
        }

        // Check if there is a conflict between declared entityUUID and attribute uuid (core.id) if any.
        if (optEntity.isPresent() && attributes.containsKey(Constants.OCCI_CORE_ID)) {
            // check if this is the same id, if not there is a conflict..
            String coreIdObj = attributes.get(Constants.OCCI_CORE_ID);
            if (coreIdObj == null) {
                // Get the id attribute.
                coreIdObj = attributes.get("id");
            }

            // Check if urn:uuid: is set.
            if (coreIdObj != null) {
                String coreId = coreIdObj;
                if (coreId.contains(Constants.URN_UUID_PREFIX)) {
                    coreId = coreId.replace(Constants.URN_UUID_PREFIX, "");
                }
                if (!coreId.equals(entityId)) {
                    message = "The attribute occi.core.id value is not the same as the uuid specified in location or entity.";
                    throw new ConfigurationException(message);
                }
            }
        }
        if (isResource) {
            EntityManager.addResourceToConfiguration(entityId, title, summary, kind, mixins, attributes, location, username);
        } else {
            String src = attributes.get(Constants.OCCI_CORE_SOURCE);
            String target = attributes.get(Constants.OCCI_CORE_TARGET);

            if (src == null) {
                message = "No source provided for this link : " + entityId;
                throw new ConfigurationException(message);
            }
            if (target == null) {
                message = "No target provided for this link : " + entityId;
                throw new ConfigurationException(message);
            }
            EntityManager.addLinkToConfiguration(entityId, title, kind, mixins, src, target, attributes, location, username);
        }

        // Execute model CRUD method.
        // First refresh entity.
        optEntity = EntityManager.findEntityForUuid(entityId, username);
        if (optEntity.isPresent()) {
            entity = optEntity.get();
            if (overwrite) {
                entity.occiUpdate();
                LOGGER.info("Update entity done returning location : " + location + " with id: " + entityId);
            } else {
                entity.occiCreate();
                LOGGER.info("Create entity done returning location : " + location + " with id: " + entityId);
            }
        } else {
            message = "Entity " + entityId + " was not created on object model, please check your query.";
            throw new ConfigurationException(message);
        }
        return entity;
    }


    /**
     * Create entities from a list of input datas content request.
     *
     * @param datas
     * @return a response object with created entities.
     */
    @Override
    public OCCIApiResponse createEntities(List<OCCIRequestData> datas) {
        String title;
        String summary;
        String location;
        String kind;
        Map<String, String> attributes;
        List<String> mixins;
        String message;
        String entityId;
        Entity entity;
        List<Entity> entities = new LinkedList<>();
        for (OCCIRequestData entityData : datas) {
            entityId = entityData.getEntityUUID();
            title = entityData.getEntityTitle();
            summary = entityData.getEntitySummary();
            kind = entityData.getKind();
            mixins = entityData.getMixins();
            attributes = entityData.getAttrsValStr();
            location = entityData.getLocation();

            if (location == null || location.isEmpty()) {
                message = "No location set for entity : " + title + " , id: " + entityId;
                parseConfigurationExceptionMessageOutput(message);
            }

            try {
                entity = createEntityOnConfiguration(title, summary, kind, mixins, attributes, location);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
            entities.add(entity);
        }
        renderEntitiesOutput(entities);
        return occiApiResponse;
    }

    private void parseConfigurationExceptionMessageOutput(final String message) {
        occiApiResponse.setExceptionMessage(message);
        occiApiResponse.setExceptionThrown(new ConfigurationException(message));
        occiApiResponse.parseResponseMessage(message);
        LOGGER.error(message);
    }

    private void parseNotFoundExceptionMessageOutput(final String message) {
        occiApiResponse.setExceptionMessage(message);
        occiApiResponse.setExceptionThrown(new ResourceNotFoundException(message));
        occiApiResponse.parseResponseMessage(message);
        LOGGER.info(message);
    }

    private void parseModelValidatorExceptionMessageOutput(final String message) {
        occiApiResponse.setExceptionMessage(message);
        occiApiResponse.setExceptionThrown(new ModelValidatorException(message));
        occiApiResponse.parseResponseMessage(message);
        LOGGER.error(message);
    }

    /**
     * Parse output response for an entity.
     *
     * @param entity an entity object to render in output via a parser setted in concrete implementation.
     */
    private void renderEntityOutput(final Entity entity) {
        IRequestParser outputParser = occiApiResponse.getOutputParser();
        if (outputParser != null) {
            try {
                // Launch the parsing output (for json, text/occi and other implementations).
                occiApiResponse.setResponseMessage(outputParser.renderOutputEntity(entity));

            } catch (ParseOCCIException ex) {
                String message = "Exception thrown when rendering entity response, message: " + ex.getMessage();
                LOGGER.error(message);
                occiApiResponse.setExceptionMessage(message);
                occiApiResponse.setExceptionThrown(ex);
                occiApiResponse.parseResponseMessage(message);
            }
        }
    }

    /**
     * Render entities location.
     *
     * @param entities the entities to render.
     */
    private void renderEntitiesLocationOutput(final List<Entity> entities) {
        IRequestParser outputParser = occiApiResponse.getOutputParser();
        if (outputParser != null) {
            try {
                List<String> locations = new LinkedList<>();
                // Launch the parsing output (for json, text/occi and other implementations).
                for (Entity entity : entities) {
                    String location = EntityManager.getLocation(entity, username);
                    locations.add(location);
                }
                occiApiResponse.setResponseMessage(outputParser.renderOutputEntitiesLocations(locations));

            } catch (ParseOCCIException ex) {
                String message = "Exception thrown when rendering entity response, message: " + ex.getMessage();
                LOGGER.error(message);
                occiApiResponse.setExceptionMessage(message);
                occiApiResponse.setExceptionThrown(ex);
                occiApiResponse.parseResponseMessage(message);
            }
        }
    }

    /**
     * Parse output response for an entity.
     *
     * @param entities List of entities to render in output via a parser setted on concrete implementation.
     */
    private void renderEntitiesOutput(final List<Entity> entities) {
        IRequestParser outputParser = occiApiResponse.getOutputParser();
        if (outputParser != null) {
            try {
                // Launch the parsing output (for json, text/occi and other implementations).
                occiApiResponse.setResponseMessage(outputParser.renderOutputEntities(entities));

            } catch (ParseOCCIException ex) {
                String message = "Exception thrown when rendering entity response, message: " + ex.getMessage();
                LOGGER.error(message);
                occiApiResponse.setExceptionMessage(message);
                occiApiResponse.setExceptionThrown(ex);
                occiApiResponse.parseResponseMessage(message);
            }
        }
    }


    @Override
    public OCCIApiResponse updateEntity(final List<String> mixins, final Map<String, String> attributes, final String location) {
        String message;
        Optional<Entity> optEntity;
        if (mixins.isEmpty() && attributes.isEmpty()) {
            message = "No attributes to update or mixins to apply to entities";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        if (occiApiResponse.hasExceptions()) {
            return occiApiResponse;
        }

        // Find entities with location set.
        if (location == null || location.trim().isEmpty()) {
            message = "No location is set for entity to update.";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        // Load the entity with location.

        optEntity = EntityManager.findEntityFromLocation(location, username);

        if (!optEntity.isPresent()) {
            message = "Entity on location: " + location + " doesnt exist.";
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        Entity entity = optEntity.get();
        if (!mixins.isEmpty()) {
            try {
                MixinManager.addMixinsToEntity(entity, mixins, username, false);
            } catch (ConfigurationException ex) {
                message = ex.getMessage();
                parseConfigurationExceptionMessageOutput(message);
                return occiApiResponse;
            }
        }
        // update attributes .
        entity = EntityManager.updateAttributesToEntity(entity, attributes, username);
        // Launch crud update operation on this entity.
        entity.occiUpdate();

        renderEntityOutput(entity);
        return occiApiResponse;
    }

    /**
     * Delete an entity from the user configuration (and execute occiDelete()).
     *
     * @param location the location like /mylocation/myentity.
     * @return
     */
    @Override
    public OCCIApiResponse deleteEntity(final String location) {
        Optional<Entity> optEntity = EntityManager.findEntityFromLocation(location, username);
        Entity entity;
        String message;
        String entityId;
        String title;
        if (!optEntity.isPresent()) {
            message = "Entity not found on location: " + location;
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        entity = optEntity.get();
        entityId = entity.getId();
        title = entity.getTitle();
        entity.occiDelete();
        try {
            LOGGER.info("Remove entity: " + title + " --> " + entityId + " on location: " + location);
            EntityManager.removeOrDissociateFromConfiguration(entityId, username);
            // if all ok, return no response (ok response).
            occiApiResponse.parseResponseMessage("ok");
        } catch (ConfigurationException ex) {
            parseConfigurationExceptionMessageOutput(ex.getMessage());
        }
        return occiApiResponse;
    }

    /**
     * Delete a collection of entities under the specified location.
     * @param location
     * @param filter
     */
    @Override
    public OCCIApiResponse deleteEntities(final String location, CollectionFilter filter) {
        String message;
        int items = Constants.DEFAULT_NUMBER_ITEMS_PER_PAGE;
        int page = Constants.DEFAULT_CURRENT_PAGE;

        if (filter == null) {
            // Build a default filter.
            filter = new CollectionFilter();
            filter.setCurrentPage(page);
            filter.setNumberOfItemsPerPage(items);
            filter.setOperator(0);
        }
        List<Entity> entities;
        // Important to note : filters are defined in concrete implementation (categoryFilter, attributes filter, filter on a value etc.)
        String categoryFilter = filter.getCategoryFilter();
        String filterOnPath = filter.getFilterOnPath();
        // Case of the mixin tag entities request.
        boolean isMixinTagRequest = MixinManager.isMixinTagRequest(location, username);
        if (isMixinTagRequest) {
            LOGGER.info("Mixin tag request... ");
            Optional<Mixin> optMixin = MixinManager.getUserMixinFromLocation(location, username);
            Mixin mixin;
            if (!optMixin.isPresent()) {
                message = "The mixin location : " + location + " is not defined";
                parseConfigurationExceptionMessageOutput(message);
                return occiApiResponse;
            }
            if (categoryFilter == null) {
                mixin = optMixin.get();
                filter.setCategoryFilter(mixin.getTerm());
                filter.setFilterOnPath(null);
            }
        }
        // Determine if this is a collection or an entity location.
        // Collection on categories. // Like : get on myhost/compute/
        if ((categoryFilter == null || categoryFilter.isEmpty()) && (filterOnPath == null || filterOnPath.isEmpty())) {
            boolean isCollectionOnCategoryPath = ConfigurationManager.isCollectionOnCategory(location, username);
            if (isCollectionOnCategoryPath && (categoryFilter == null || categoryFilter.isEmpty())) {
                Optional<String> optCat = ConfigurationManager.getCategoryFilterSchemeTerm(location, username);
                String cat = null;
                if (optCat.isPresent()) {
                    cat = optCat.get();
                }
                filter.setCategoryFilter(cat);
            } else {
                filter.setFilterOnPath(location);
            }
        }

        entities = EntityManager.findAllEntities(filter, username);
        Optional<Entity> optEntityCheck; // To check if a link is present and have no resource deleted previously.
        for (Entity entity : entities) {

            String entityId = entity.getId();
            String title = entity.getTitle();
            optEntityCheck = EntityManager.findEntity(entityId, username);
            if (optEntityCheck.isPresent()) {
                entity.occiDelete();
                try {
                    LOGGER.info("Remove entity: " + title + " --> " + entityId + " on location: " + location);
                    EntityManager.removeOrDissociateFromConfiguration(entityId, username);

                } catch (ConfigurationException ex) {
                    parseConfigurationExceptionMessageOutput(ex.getMessage());
                }
            } else {
                LOGGER.warn("Entity : " + title + " --> " + entityId + " on location: " + location + " is already removed from configuration.");
            }
        }
        if (entities.isEmpty()) {
            // Not found answer.
            parseNotFoundExceptionMessageOutput("Entities not found on location : " + location);
        }

        return occiApiResponse;


    }

    /**
     * Find an entity or a collection of entities, outputparser will render the output to the good type.
     *
     * @param location the location like /mylocation/myentity/ or a category location like /compute/ or bounded path like /mycollections/myentities/
     * @param filter   filter the output entities if this is a collection so may be null if none.
     * @return an occiApiResponse object defined by concrete implementation.
     */
    @Override
    public OCCIApiResponse findEntities(final String location, CollectionFilter filter) {
        String message;
        int items = Constants.DEFAULT_NUMBER_ITEMS_PER_PAGE;
        int page = Constants.DEFAULT_CURRENT_PAGE;

        if (filter == null) {
            // Build a default filter.
            filter = new CollectionFilter();
            filter.setCurrentPage(page);
            filter.setNumberOfItemsPerPage(items);
            filter.setOperator(0);
        }
        List<Entity> entities;
        // Important to note : filters are defined in concrete implementation (categoryFilter, attributes filter, filter on a value etc.)

        String categoryFilter = filter.getCategoryFilter();
        String filterOnPath = filter.getFilterOnPath();
        // Case of the mixin tag entities request.
        boolean isMixinTagRequest = MixinManager.isMixinTagRequest(location, username);
        if (isMixinTagRequest) {
            LOGGER.info("Mixin tag request... ");
            Optional<Mixin> optMixin = MixinManager.getUserMixinFromLocation(location, username);
            Mixin mixin;
            if (!optMixin.isPresent()) {
                message = "The mixin location : " + location + " is not defined";
                parseConfigurationExceptionMessageOutput(message);
                return occiApiResponse;
            }
            if (categoryFilter == null) {
                mixin = optMixin.get();
                filter.setCategoryFilter(mixin.getTerm());
                filter.setFilterOnPath(null);
            }
        }

        // Determine if this is a collection or an entity location.
        // Collection on categories. // Like : get on myhost/compute/
        if ((categoryFilter == null || categoryFilter.isEmpty()) && (filterOnPath == null || filterOnPath.isEmpty())) {
            boolean isCollectionOnCategoryPath = ConfigurationManager.isCollectionOnCategory(location, username);
            if (isCollectionOnCategoryPath && (categoryFilter == null || categoryFilter.isEmpty())) {
                Optional<String> optCat = ConfigurationManager.getCategoryFilterSchemeTerm(location, username);
                String cat = null;
                if (optCat.isPresent()) {
                    cat = optCat.get();
                }
                filter.setCategoryFilter(cat);
            } else {
                filter.setFilterOnPath(location);
            }
        }
        entities = EntityManager.findAllEntities(filter, username);
        for (Entity entity : entities) {
            // Refresh object with crud method : retrieve.
            entity.occiRetrieve();
        }
        if (entities.size() > 1) {
            this.renderEntitiesOutput(entities);
        } else if (entities.size() == 1) {
            this.renderEntityOutput(entities.get(0));
        } else {
            // Not found answer.
            parseNotFoundExceptionMessageOutput("Resource not found on location : " + location);
        }
        return this.occiApiResponse;
    }

    /**
     * Same as findEntities method but render locations link replacing entities data.
     *
     * @param location the collection location like /mylocation/myentity.
     * @param filter   filter the output entities if this is a collection so may be null if none.
     * @return a response object container with response message to publish.
     */
    @Override
    public OCCIApiResponse findEntitiesLocations(final String location, CollectionFilter filter) {
        String message;
        int items = Constants.DEFAULT_NUMBER_ITEMS_PER_PAGE;
        int page = Constants.DEFAULT_CURRENT_PAGE;

        if (filter == null) {
            // Build a default filter.
            filter = new CollectionFilter();
            filter.setCurrentPage(page);
            filter.setNumberOfItemsPerPage(items);
            filter.setOperator(0);
        }
        List<Entity> entities;
        // Important to note : filters are defined in concrete implementation (categoryFilter, attributes filter, filter on a value etc.)

        String categoryFilter = filter.getCategoryFilter();
        // Determine if this is a collection or an entity location.
        // Collection on categories. // Like : get on myhost/compute/
        boolean isCollectionOnCategoryPath = ConfigurationManager.isCollectionOnCategory(location, username);
        if (isCollectionOnCategoryPath && (categoryFilter == null || categoryFilter.isEmpty())) {
            Optional<String> optCat = ConfigurationManager.getCategoryFilterSchemeTerm(location, username);
            String cat = null;
            if (optCat.isPresent()) {
                cat = optCat.get();
            }
            filter.setCategoryFilter(cat);
        } else {
            filter.setFilterOnPath(location);
        }

        // Case of the mixin tag entities request.
        boolean isMixinTagRequest = MixinManager.isMixinTagRequest(location, username);
        if (isMixinTagRequest) {
            LOGGER.info("Mixin tag request... ");
            Optional<Mixin> optMixin = MixinManager.getUserMixinFromLocation(location, username);
            if (!optMixin.isPresent()) {
                message = "The mixin location : " + location + " is not defined";
                parseConfigurationExceptionMessageOutput(message);
                return occiApiResponse;
            }
            if (categoryFilter == null) {
                filter.setCategoryFilter(optMixin.get().getTerm());
                filter.setFilterOnPath(null);
            }
        }
        entities = EntityManager.findAllEntities(filter, username);
        for (Entity entity : entities) {
            // Refresh object.
            entity.occiRetrieve();
        }
        if (!entities.isEmpty()) {
            this.renderEntitiesLocationOutput(entities);
        } else {
            // Not found answer.
            parseNotFoundExceptionMessageOutput("Resource not found on location : " + location);
        }
        return this.occiApiResponse;
    }

    /**
     * Create a new mixin tag and associate it on entities if locations are given in parameter.
     *
     * @param title     a title for this mixin tag.
     * @param mixinTag  mixin scheme+term.
     * @param location
     * @param locations an optional list of entities location, if set this associate the mixin on these locations.  @return a response object given by implemented concrete request class.
     */
    @Override
    public OCCIApiResponse createMixinTag(final String title, final String mixinTag, final String location, final List<String> locations) {
        String message;
        if (mixinTag == null || mixinTag.trim().isEmpty()) {
            message = "No mixin tag id, cannot create mixin tag";
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        if (location == null) {
            message = "No mixin tag location set.";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        try {

            MixinManager.addUserMixinOnConfiguration(mixinTag, title, location, username);

            if (locations != null && !locations.isEmpty()) {

                // Get the mixin scheme+term from path.
                Optional<String> optCat = ConfigurationManager.getCategoryFilterSchemeTerm(location, username);
                if (!optCat.isPresent()) {
                    throw new ConfigurationException("Category is not defined");
                }

                List<String> entities = new ArrayList<>();

                for (String xOcciLocation : locations) {
                    // Build a list of entities from xoccilocations defined.
                    if (EntityManager.isEntityUUIDProvided(xOcciLocation, new HashMap<>())) {
                        // One entity.
                        Optional<String> optUuid = EntityManager.getUUIDFromPath(xOcciLocation, new HashMap<>());
                        if (!optUuid.isPresent()) {
                            message = Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore";
                            parseConfigurationExceptionMessageOutput(message);
                            return occiApiResponse;
                        }
                        Optional<Entity> optEntity = EntityManager.findEntityForUuid(optUuid.get(), ConfigurationManager.DEFAULT_OWNER);
                        if (!optEntity.isPresent()) {
                            message = Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore";
                            parseConfigurationExceptionMessageOutput(message);
                            return occiApiResponse;
                        }
                        entities.add(optEntity.get().getId());
                    } else {
                        // Maybe a collection on inbound or outbound path.
                        List<String> entitiesTmp = EntityManager.getEntityUUIDsFromPath(xOcciLocation, username);
                        if (!entitiesTmp.isEmpty()) {
                            entities.addAll(entitiesTmp);
                        }
                    }
                }
                // Full update mode.
                if (!entities.isEmpty()) {
                    MixinManager.saveMixinForEntities(optCat.get(), entities, true, username);
                }
            }
        } catch (ConfigurationException ex) {
            message = ex.getMessage();
            parseConfigurationExceptionMessageOutput(message);
        }

        // if all ok, return no response (ok response).
        occiApiResponse.parseResponseMessage("ok");
        return this.occiApiResponse;
    }

    /**
     * Replace all association for this mixin tag. Remove all previously entity reference location for this mixin tags if they are not in locations scope paremeter.
     *
     * @param mixinTag  a mixin tag scheme + term.
     * @param locations a list of entity location.
     * @return ok if mixin associations has been replaced successfully.
     */
    @Override
    public OCCIApiResponse replaceMixinTagCollection(final String mixinTag, final List<String> locations) {

        String message;

        // First load the mixin object model.
        Optional<Mixin> optMixin = MixinManager.findUserMixinOnConfiguration(mixinTag, username);
        if (!optMixin.isPresent()) {
            message = "User mixin doesnt exist on your configuration";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        List<Entity> entities = new LinkedList<>();
        if (locations.isEmpty()) {
            // Remove all association.
            LOGGER.info("Remove all assocations for mixin tag : " + mixinTag);
            try {
                EntityManager.removeOrDissociateFromConfiguration(mixinTag, username);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
        }

        // Location must have entity object instance.
        for (String location : locations) {
            Optional<Entity> optEntity = EntityManager.findEntityFromLocation(location, username);
            if (!optEntity.isPresent()) {
                message = "Mixin tag association failed, the entity for location : " + location + " doesnt exist on configuration";
                parseNotFoundExceptionMessageOutput(message);
                return occiApiResponse;
            }
            entities.add(optEntity.get());
        }
        if (!entities.isEmpty()) {
            try {
                MixinManager.saveMixinForEntitiesModel(mixinTag, entities, false, username);
                renderEntitiesOutput(entities);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
        }
        return occiApiResponse;
    }

    @Override
    public OCCIApiResponse associateMixinToEntities(String mixin, final String mixinTagLocation, final List<String> xlocations) {
        Entity entity;
        String message;
        List<Entity> entities = new LinkedList<>();
        if (mixin == null && mixinTagLocation == null) {
            message = "No mixin defined for association with entities";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        if (xlocations == null || xlocations.isEmpty()) {
            message = "No entities to associate the the mixin tag: " + mixin + " on location : " + mixinTagLocation;
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        List<String> mixinTags = new ArrayList<>();
        if (mixin == null) {
            // Search mixin tag with location.
            Optional<Mixin> optMixin = MixinManager.getUserMixinFromLocation(mixinTagLocation, username);
            if (optMixin.isPresent()) {
                Mixin mixinTagModel = optMixin.get();
                mixin = mixinTagModel.getScheme() + mixinTagModel.getTerm();
                mixinTags.add(mixin);
            }
        }


        // Update mixin tags association.
        for (String xocciLocation : xlocations) {
            Optional<Entity> optEntity = EntityManager.findEntityFromLocation(xocciLocation, username);

            if (!optEntity.isPresent()) {
                message = Constants.X_OCCI_LOCATION + " is not set correctly or the entity doesnt exist anymore";
                parseNotFoundExceptionMessageOutput(message);
                return occiApiResponse;
            }
            entity = optEntity.get();
            try {
                MixinManager.addMixinsToEntity(entity, mixinTags, username, false);
                entities.add(entity);
            } catch (ConfigurationException ex) {
                message = ex.getMessage();
                parseConfigurationExceptionMessageOutput(message);
                return occiApiResponse;
            }
        }

        renderEntitiesOutput(entities);
        return occiApiResponse;
    }

    /**
     * Remove definitively the mixin tag and remove all its association.
     *
     * @param mixinTag mixin tag scheme + term.
     * @return a response object.
     */
    @Override
    public OCCIApiResponse deleteMixinTag(final String mixinTag) {
        String message;
        if (mixinTag == null) {
            message = "No mixin tag to delete.";
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        try {
            EntityManager.removeOrDissociateFromConfiguration(mixinTag, username);
        } catch (ConfigurationException ex) {
            // no association with entities, ignore the exception.
        }

        Optional<Mixin> optMixin = MixinManager.findUserMixinOnConfiguration(mixinTag, username);

        if (!optMixin.isPresent()) {
            message = "The mixin tag : " + mixinTag + " is already removed.";
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        try {
            MixinManager.removeUserMixinFromConfiguration(mixinTag, username);
        } catch (ConfigurationException ex) {
            message = "Error while removing a mixin tag from configuration object: " + mixinTag + " --> " + ex.getMessage();
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        occiApiResponse.parseResponseMessage("ok");
        return occiApiResponse;
    }

    /**
     * Remove mixin associations with specified locations. if locations is null or empty, all associations will be removed.
     *
     * @param mixin     the mixin scheme + term.
     * @param locations the location of the entity where to remove the mixin.
     * @return an occiApiResponse object.
     */
    @Override
    public OCCIApiResponse removeMixinAssociations(final String mixin, List<String> locations) {
        String message;
        if (mixin == null) {
            message = "no mixin associations to remove, there is no mixin defined";
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }
        Optional<Mixin> optMixin = MixinManager.findMixinOnExtension(mixin, username);
        if (!optMixin.isPresent()) {
            message = "no mixin associations to remove, there is no mixin defined on model for : " + mixin;
            parseNotFoundExceptionMessageOutput(message);
            return occiApiResponse;
        }

        if (locations == null || locations.isEmpty()) {
            LOGGER.info("Remove all associations with this mixin : " + mixin);
            try {
                EntityManager.removeOrDissociateFromConfiguration(mixin, username);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
        } else {
            Entity entity;
            for (String currentLocation : locations) {
                LOGGER.info("Remove association with mixin : " + mixin + " entity location : " + currentLocation);
                Optional<Entity> optEntity = EntityManager.findEntityFromLocation(currentLocation, username);

                if (optEntity.isPresent()) {
                    try {
                        MixinManager.dissociateMixinFromEntity(mixin, optEntity.get(), username);
                    } catch (ConfigurationException ex) {
                        parseConfigurationExceptionMessageOutput(ex.getMessage());
                        return occiApiResponse;
                    }
                } else {
                    LOGGER.warn("Entity on location: " + currentLocation + " not found for mixin dissociation : " + mixin);
                }
            }
        }
        occiApiResponse.parseResponseMessage("ok");
        return occiApiResponse;
    }

    /**
     * Execute an action operation on entities location.
     *
     * @param action           the action category scheme+term.
     * @param actionAttributes the action attributes in a map of String, String.
     * @param locations        entities locations defined where to execute action.
     * @return
     */
    @Override
    public OCCIApiResponse executeActionOnEntities(final String action, final Map<String, String> actionAttributes, final List<String> locations) {
        String message;
        try {
            EntityManager.checkActionOnModel(action, username);

        } catch (ConfigurationException ex) {
            parseConfigurationExceptionMessageOutput(ex.getMessage());
            return occiApiResponse;
        }
        if (locations == null || locations.isEmpty()) {
            // No entities defined to execute the action.
            message = "Entity locations are not set.";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        Entity entity;
        List<Entity> entities = new LinkedList<>();
        // Execute the action on entity locations.
        for (String location : locations) {
            try {
                LOGGER.info("Triggering action : " + action + " on entity located on : " + location);
                EntityManager.executeActionOnEntityLocation(location, action, actionAttributes, username);
                Optional<Entity> optEntity = EntityManager.findEntityFromLocation(location, username);

                if (optEntity.isPresent()) {
                    entity = optEntity.get();
                    entity.occiRetrieve(); // refresh entity attributes and values, this is welcome if entity state change.
                    entities.add(entity);
                } else {
                    message = "entity not found on location: " + location + " cannot trigger action : " + action + " on this entity";
                    LOGGER.warn(message);
                    parseNotFoundExceptionMessageOutput(message);
                    return occiApiResponse;
                }

            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }

        }
        // All ok, render entities to output.
        renderEntitiesOutput(entities);
        return occiApiResponse;
    }

    /**
     * Trigger an action on a category (kind or mixin) entity collection. This method will load entities from the category defined and execute the action on each entity.
     *
     * @param action       action scheme + term.
     * @param actionAttrs  action attributes
     * @param categoryTerm category term
     */
    @Override
    public OCCIApiResponse executeActionOnCategory(final String action, final Map<String, String> actionAttrs, final String categoryTerm) {
        String message;
        try {
            EntityManager.checkActionOnModel(action, username);
        } catch (ConfigurationException ex) {
            parseConfigurationExceptionMessageOutput(ex.getMessage());
            return occiApiResponse;
        }
        if (categoryTerm == null) {
            message = "Category term is not set for action to trigger : " + action;
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        // Check if category term is on extensions model.
        Optional<String> optCat = getCategorySchemeTerm(categoryTerm);
        String categoryId;

        if (!optCat.isPresent()) {
            message = "Category : " + categoryTerm + " is unknown";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        categoryId = optCat.get();
        // Load all entities on scope of this category.
        List<Entity> entities = EntityManager.findAllEntitiesForKind(categoryId, username);
        if (entities.isEmpty()) {
            // Try on mixins.
            entities = EntityManager.findAllEntitiesForMixin(categoryId, username);
        }

        if (entities.isEmpty()) {
            message = "Category : " + categoryId + " has no referenced entities";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        // Trigger the action.
        LOGGER.info("Triggering action : " + action + " on category : " + categoryId + ", entities number: " + entities.size());
        for (Entity entity : entities) {
            try {
                EntityManager.executeActionOnEntity(entity, action, actionAttrs, username);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
        }
        renderEntitiesOutput(entities);
        return occiApiResponse;
    }

    @Override
    public OCCIApiResponse executeActionOnMixinTag(final String action, final Map<String, String> actionAttrs, final String mixinTag) {
        String message;
        Optional<Mixin> optMixin = MixinManager.findUserMixinOnConfiguration(mixinTag, username);

        if (!optMixin.isPresent()) {
            message = "User defined mixin doesnt exist on configuration model";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }

        // Search the entities that has the mixin tag associated.
        List<Entity> entities = EntityManager.findAllEntitiesForMixin(mixinTag, username);
        if (entities.isEmpty()) {
            message = "Mixin user defined has no referenced entities, no action to trigger";
            parseConfigurationExceptionMessageOutput(message);
            return occiApiResponse;
        }
        LOGGER.info("Triggering action : " + action + " on mixin user defined : " + mixinTag + ", entities number: " + entities.size());
        for (Entity entity : entities) {
            try {
                EntityManager.executeActionOnEntity(entity, action, actionAttrs, username);
            } catch (ConfigurationException ex) {
                parseConfigurationExceptionMessageOutput(ex.getMessage());
                return occiApiResponse;
            }
        }
        renderEntitiesOutput(entities);
        return occiApiResponse;
    }


    // ****************
    // Helper methods
    // ****************

    /**
     * @param location
     * @return
     */
    @Override
    public boolean isCategoryLocation(final String location) {
        boolean onCategoryLocation = false;
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        String locationWork = location;
        if (locationWork.startsWith("/")) {
            locationWork = locationWork.substring(1);
        }
        if (locationWork.endsWith("/")) {
            locationWork = locationWork.substring(0, locationWork.length() - 1);
        }
        return isCategoryTerm(locationWork);
    }

    @Override
    public boolean isCategoryTerm(final String categoryTerm) {
        return this.getCategorySchemeTerm(categoryTerm).isPresent();
    }

    @Override
    public Optional<String> getCategorySchemeTerm(final String categoryTerm) {
        return ConfigurationManager.findCategorySchemeTermFromTerm(categoryTerm, username);
    }

    @Override
    public Optional<String> getMixinTagSchemeTermFromLocation(String location) {
        Optional<Mixin> optMixin = MixinManager.getUserMixinFromLocation(location, username);
        Optional<String> optResult;
        if (optMixin.isPresent()) {
            optResult = Optional.of(optMixin.get().getScheme() + optMixin.get().getTerm());
        } else {
            optResult = Optional.empty();
        }
        return optResult;
    }

    @Override
    public boolean isMixinTagLocation(final String location) {
        return MixinManager.getUserMixinFromLocation(location, username).isPresent();
    }

    @Override
    public boolean isEntityLocation(final String location) {
        return EntityManager.findEntityFromLocation(location, username).isPresent();
    }


    @Override
    public OCCIApiResponse validateInputDataRequest() {
        String message;
        String kind;
        Kind kindModel;
        List<String> mixins;
        Map<String, Object> attrs;
        Map<String, Object> attrsToControlOnActions;
        Map<String, Object> attrsToControlOnMixins;
        String action;
        String attrKey;
        // TODO : Control input value datatype with models.
        Object attrValue;
        Optional<Kind> optKind;
        Optional<Action> optAction;
        Optional<Mixin> optMixin;

        boolean found;
        List<OCCIRequestData> inputDatas = inputParser.getInputDatas();
        for (OCCIRequestData content : inputDatas) {
            // Is the kind exist on this configuration's extensions ?
            kind = content.getKind();
            attrs = content.getAttrs();
            mixins = content.getMixins();
            action = content.getAction();
            attrsToControlOnActions = new LinkedHashMap<>();
            attrsToControlOnMixins = new LinkedHashMap<>();

            if (kind != null) {
                // Check the scheme+term on extensions.
                optKind = KindManager.findKindFromExtension(kind, username);
                if (!optKind.isPresent()) {
                    message = "The kind : " + kind + " doesnt exist on extensions.";
                    parseModelValidatorExceptionMessageOutput(message);
                    return occiApiResponse;
                } else {
                    kindModel = optKind.get();
                }

                // Check the attributes.
                if (!attrs.isEmpty()) {

                    for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                        attrKey = entry.getKey();
                        attrValue = entry.getValue();
                        found = false;
                        if (!(attrKey.equals(Constants.OCCI_CORE_ID)
                                || attrKey.equals(Constants.OCCI_CORE_TITLE)
                                || attrKey.equals(Constants.OCCI_CORE_SUMMARY)
                                || attrKey.equals(Constants.OCCI_CORE_SOURCE)
                                || attrKey.equals(Constants.OCCI_CORE_TARGET))) {
                            for (Attribute attribModel : kindModel.getAttributes()) {
                                if (attribModel.getName().equals(attrKey)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // TODO : Control attribute in parent kind recursively before adding the other attributes..

                                // Add the attribute to control on mixins and action (if one on request).
                                attrsToControlOnActions.put(attrKey, attrValue);
                                attrsToControlOnMixins.put(attrKey, attrValue);
                            }
                        }
                    }
                }

                if (action != null) {
                    found = false;
                    Action actionModelWork = null;
                    Kind currentKind = kindModel;
                    while (currentKind != null && !found) {

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

                    // Control the action attributes if necessary.
                    if (found && !attrsToControlOnActions.isEmpty()) {

                        for (Map.Entry<String, Object> entry : attrsToControlOnActions.entrySet()) {
                            attrKey = entry.getKey();
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
                    if (!found) {
                        // Action is not referenced on this kind.
                        message = "The action : " + action + " doesnt exist for this kind : " + kind + " and all parents";
                        parseModelValidatorExceptionMessageOutput(message);
                        return occiApiResponse;
                    }

                }

                if (mixins.isEmpty()) {
                    // no mixins to control...
                    if (!attrsToControlOnMixins.isEmpty()) {
                        String attributes = "";
                        for (Map.Entry<String, Object> entry : attrsToControlOnMixins.entrySet()) {
                            attributes = attributes + entry.getKey() + ";";
                        }
                        message = "Some attributes were not found on referenced models : " + attributes;
                        parseModelValidatorExceptionMessageOutput(message);
                        return occiApiResponse;
                    }
                } else {
                    // There are mixins contents....
                    Mixin mixinModel;
                    // mixins exists ?
                    // Are there applyable to this kind ?
                    for (String mixin : mixins) {
                        optMixin = MixinManager.findMixinOnExtension(mixin, username);

                        if (!optMixin.isPresent()) {
                            message = "The mixin : " + mixin + " doesnt exist on extensions.";
                            parseModelValidatorExceptionMessageOutput(message);
                            return occiApiResponse;
                        } else {
                            mixinModel = optMixin.get();
                        }
                        // Check the attributes if any.
                        if (!attrsToControlOnMixins.isEmpty()) {
                            found = false;
                            message = null;
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
                                message = "Some attributes were not found on referenced models : " + message;
                                parseModelValidatorExceptionMessageOutput(message);
                                return occiApiResponse;
                            }
                        }
                    }
                }
            } // kind != null

            // if this is an action invocation, is this action is present on extension's or present in parent kind of the entity.
            // Are the actions attributes are correct and defined in extension ?
            if (kind == null && action != null) {
                optAction = ConfigurationManager.findActionOnExtensions(action, username);
                Action actionModelWork = null;
                if (!optAction.isPresent()) {
                    message = "Action : " + action + " doesnt exist on models.";
                    parseModelValidatorExceptionMessageOutput(message);
                    return occiApiResponse;
                } else {
                    actionModelWork = optAction.get();
                }

                // Control the attributes.
                if (!attrs.isEmpty()) {
                    message = null;
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
                        message = "Some action attributes are not defined in action model : " + message;
                        parseModelValidatorExceptionMessageOutput(message);
                        return this.occiApiResponse;
                    }
                }
            }
        }

        return occiApiResponse;
    }

    @Override
    public IRequestParser getInputParser() {
        return inputParser;
    }

    @Override
    public void setInputParser(IRequestParser inputParser) {
        this.inputParser = inputParser;
    }

    @Override
    public String createUUID() {
        return ConfigurationManager.createUUID();
    }


    /**
     * Load all models from disk for all user's configurations.
     *
     * @throws ConfigurationException
     */
    @Override
    public void LoadModelFromDisk() throws ConfigurationException {
        // TODO : Load all configurations model from disk.
    }

    /**
     * Save all models to disk for all user's configurations.
     *
     * @throws ConfigurationException
     */
    @Override
    public void saveModelToDisk() throws ConfigurationException {
        // TODO : Save all configurations model to disk.
    }



}
