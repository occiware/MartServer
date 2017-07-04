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
package org.occiware.mart.server.model;

import org.eclipse.cmf.occi.core.*;
import org.eclipse.cmf.occi.core.util.Occi2Ecore;
import org.eclipse.cmf.occi.core.util.OcciHelper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.model.container.EntitiesOwner;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by christophe on 22/04/2017.
 */
public class EntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManager.class);


    private static final String REGEX_CONTROL_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    /**
     * Entities for a user. key: owner, value : EntityOwner container object.
     */
    private static Map<String, EntitiesOwner> entitiesOwnerMap = new ConcurrentHashMap<>();

    /**
     * Used only to create an eTag when object are updated. Key : owner+objectId
     * Value : version number. First version is 1.
     */
    private static Map<String, Integer> versionObjectMap = new ConcurrentHashMap<>();

    /**
     * Find an Entity used by an owner, whatever is its configuration.
     *
     * @param id    may be an uuid, a path/uuid or a location.
     * @param owner the user owner of this entity
     * @return an Entity model object encapsulated in an optional object if found else empty optional.
     */
    public static Optional<Entity> findEntity(final String id, final String owner) {
        Entity entity = null;
        if (id == null || owner == null) {
            return Optional.empty();
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);

        // Search entity with uuid.
        LOGGER.info("Search entity with uuid: " + id);
        if (isUUIDValid(id)) {
            entity = entitiesOwner.getEntityByUuid(id);
        }
        if (entity == null) {
            LOGGER.info("Search entity on location : " + id);
            // search entity with id ==> location.
            entity = entitiesOwner.getEntityByLocation(id);
        }
        return Optional.ofNullable(entity);
    }


    /**
     * Find a resource for owner and entity Id.
     *
     * @param id    (may be an uuid, a path/uuid or a path.)
     * @param owner (the user owner of this entity)
     * @return an OCCI resource else empty optional if not found.
     */
    private static Optional<Resource> findResource(final String id, final String owner) {
        Entity entity;
        if (id == null || owner == null) {
            return Optional.empty();
        }
        Optional<Entity> optEntity = findEntity(id, owner);
        if (optEntity.isPresent()) {
            entity = optEntity.get();
            if (entity instanceof Resource) {
                return Optional.of((Resource) entity);
            }
        }
        return Optional.empty();
    }

    /**
     * Find a link on all chains of resources.
     *
     * @param id    may be an uuid, a path/uuid or a path.
     * @param owner the user owner of this entity.
     * @return a Link OCCI entity model object else empty Optional object if not found.
     */
    private static Optional<Link> findLink(final String id, final String owner) {
        Entity entity;
        if (id == null || owner == null) {
            return null;
        }
        Optional<Entity> optEntity = findEntity(id, owner);
        if (optEntity.isPresent()) {
            entity = optEntity.get();
            if (entity instanceof Link) {
                return Optional.of((Link) entity);
            }
        }

        return Optional.empty();
    }

    /**
     * @param id
     * @param owner
     * @return true if entity exist or false if it doesnt exist.
     */
    public static boolean isEntityExist(final String id, final String owner) {
        return findEntity(id, owner).isPresent();
    }

    /**
     * Find entities for a categoryId (kind or Mixin or actions). actions has no
     * entity list and it's not used here.
     *
     * @param filter a collection filter object.
     * @param owner  owner of this collection.
     * @return a list of entities (key: owner, value : List of entities), never return null, if no entities found, return empty list.
     */
    public static List<Entity> findAllEntities(final CollectionFilter filter, final String owner) {
        List<Entity> entities = new LinkedList<>();

        if (owner == null || owner.isEmpty()) {
            return entities;
        }
        entities.addAll(findAllEntitiesOwner(owner));
        // TODO : Order list by entityId, if entities not empty.
        if (filter != null) {
            entities = filterEntities(filter, entities, owner);
        }
        return entities;
    }

    /**
     * Find all entities referenced for an owner.
     *
     * @param owner
     * @return a global list of entities user by owner of model configuration, if none return empty list of entities.
     */
    public static List<Entity> findAllEntitiesOwner(final String owner) {
        List<Entity> entities = new ArrayList<>();
        if (owner == null) {
            return entities;
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        Map<String, Entity> entitiesMap = entitiesOwner.getEntitiesByUuid();
        for (Map.Entry<String, Entity> entry : entitiesMap.entrySet()) {
            entities.add(entry.getValue());
        }
        return entities;
    }

    /**
     * Find all entities with that kind. (replace getEntities from kind object).
     *
     * @param categoryId Scheme+term category.
     * @param owner      owner of these entities.
     * @return a list of entities or empty list if none.
     */
    public static List<Entity> findAllEntitiesForKind(final String categoryId, final String owner) {
        List<Entity> entities = new ArrayList<>();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return entities;
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        Map<String, Entity> entitiesMap = entitiesOwner.getEntitiesByUuid();
        String kind;
        for (Map.Entry<String, Entity> entry : entitiesMap.entrySet()) {
            Entity entity = entry.getValue();
            kind = entity.getKind().getScheme() + entity.getKind().getTerm();
            if (kind.equals(categoryId)) {
                entities.add(entity);
            }
        }
        return entities;

    }

    /**
     * Find all entities for a mixin.
     *
     * @param categoryId the category scheme + term.
     * @param owner      username
     * @return a list of entities objects.
     */
    public static List<Entity> findAllEntitiesForMixin(final String categoryId, final String owner) {
        List<Entity> entities = new ArrayList<>();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return entities;
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        Map<String, Entity> entitiesMap = entitiesOwner.getEntitiesByUuid();
        String mixin;
        for (Map.Entry<String, Entity> entry : entitiesMap.entrySet()) {
            Entity entity = entry.getValue();
            for (Mixin mix : entity.getMixins()) {
                mixin = mix.getScheme() + mix.getTerm();
                if (mixin.equals(categoryId)) {
                    entities.add(entity);
                    break;
                }
            }
        }
        return entities;
    }

    /**
     * Find a used extension for an action Kind.
     *
     * @param actionId (action : scheme+term)
     * @param owner    (owner of the configuration).
     * @return an optional extension object, may return empty optional object if no extension found with this
     * configuration.
     */
    public static Optional<Extension> getExtensionForAction(final String actionId, final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
        EList<Extension> exts = config.getUse();
        Extension extRet = null;
        // Ext kinds.
        EList<Kind> kinds;
        EList<Action> actionKinds;
        for (Extension ext : exts) {
            kinds = ext.getKinds();
            for (Kind kind : kinds) {
                actionKinds = kind.getActions();
                for (Action action : actionKinds) {
                    if ((action.getScheme() + action.getTerm()).equals(actionId)) {
                        extRet = ext;
                        break;
                    }
                }
                if (extRet != null) {
                    break;
                }

            }
            if (extRet != null) {
                break;
            }
        }

        return Optional.ofNullable(extRet);
    }

    /**
     * Determine if entity is a Resource or a Link from the provided attributes.
     *
     * @param attr = attributes of an entity
     * @return false if this entity is a link, true otherwise.
     */
    public static boolean checkIfEntityIsResourceOrLinkFromAttributes(final Map<String, String> attr) {
        boolean result;
        if (attr == null || attr.isEmpty()) {
            return true;
        }
        result = !(attr.containsKey(Constants.OCCI_CORE_SOURCE) || attr.containsKey(Constants.OCCI_CORE_TARGET));

        return result;
    }

    /**
     * Apply filter where possible. startIndex starts at 1
     *
     * @param filter  Collection filter object.
     * @param sources A list of entities to filter.
     * @param owner   the owner of the entities.
     * @return a filtered list of entities.
     */
    private static List<Entity> filterEntities(final CollectionFilter filter, List<Entity> sources, final String owner) {

        String categoryFilter = filter.getCategoryFilter();
        String subCategoryFilter = filter.getSubCategoryFilter();
        String filterOnPath = filter.getFilterOnEntitiesPath();

        if (categoryFilter != null && !categoryFilter.isEmpty()
                && !ConfigurationManager.checkIfCategorySchemeTerm(categoryFilter, owner)) {
            Optional<String> optCategory = ConfigurationManager.findCategorySchemeTermFromTerm(categoryFilter, owner);
            if (optCategory.isPresent()) {
                categoryFilter = optCategory.get();
            } else {
                LOGGER.warn("Category filter : " + categoryFilter + " has not been found on current configuration.");
            }
        }
        if (subCategoryFilter != null && !subCategoryFilter.isEmpty()
                && !ConfigurationManager.checkIfCategorySchemeTerm(subCategoryFilter, owner)) {
            Optional<String> optCategory = ConfigurationManager.findCategorySchemeTermFromTerm(subCategoryFilter, owner);
            if (optCategory.isPresent()) {
                subCategoryFilter = optCategory.get();
            } else {
                LOGGER.warn("Sub category filter : " + subCategoryFilter + " has not been found on current configuration.");
            }
        }

        // Remove last char if slash on filter path
        if (filterOnPath != null && !filterOnPath.isEmpty()) {
            if (filterOnPath.endsWith("/")) {
                filterOnPath = filterOnPath.substring(0, filterOnPath.length() - 1);
            }
        }

        Iterator<Entity> it = sources.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (checkEntityAttributeFilter(filter, entity)
                    && checkEntityCategoryFilter(categoryFilter, entity)
                    && checkEntityCategoryFilter(subCategoryFilter, entity)
                    && checkEntityFilterOnPath(filterOnPath, entity, owner)) {
                continue;
            }
            it.remove();
        }

        int startIndex = filter.getCurrentPage();
        int number = filter.getNumberOfItemsPerPage();
        if (startIndex > 1 && !sources.isEmpty()) {
            int currentIndex = 0;
            it = sources.iterator();
            while (it.hasNext()) {
                it.next();
                LOGGER.info("currentIndex=" + currentIndex + ", startIndex=" + startIndex);
                if (currentIndex < (startIndex - 1)) {
                    it.remove();
                }
                currentIndex++;
            }
        }

        // Max count, -1 infinite.
        if (number >= 0) {
            int count = 0;
            it = sources.iterator();
            while (it.hasNext()) {
                it.next();
                count++;
                if (count > number) {
                    it.remove();
                }
            }
        }

        return sources;
    }

    /**
     * Check if entity respect filter location path (relative).
     *
     * @param filterOnPath partial location to filter.
     * @param entity       entity to check.
     * @param owner
     * @return true if constraint path is respected (or if filter on path is null or empty) and false elsewhere.
     */
    private static boolean checkEntityFilterOnPath(final String filterOnPath, final Entity entity, final String owner) {
        boolean result = false;

        String filterPath = filterOnPath;
        if (filterPath == null || filterPath.isEmpty()) {
            return true;
        }

        String relativeLocation;
        try {
            relativeLocation = getLocation(entity.getId(), owner);
            if (relativeLocation != null) {
                relativeLocation = relativeLocation.replaceAll("\\s+", "");
                if (!relativeLocation.endsWith("/")) {
                    relativeLocation = relativeLocation + "/";
                }
            }
        } catch (ConfigurationException ex) {
            relativeLocation = null;
        }

        filterPath = filterOnPath.replaceAll("\\s+", "");
        if (!filterPath.endsWith("/")) {
            filterPath = filterPath + "/";
        }

        if (relativeLocation == null || relativeLocation.isEmpty()) {
            result = false;
        } else if (relativeLocation.startsWith(filterPath)) {
            result = true;
        }

        return result;
    }

    /**
     * Check an entity against a category filter.
     *
     * @param categoryFilter
     * @param entity
     * @return true if constraints is respected false elsewhere. if categoryfilter is null return true (all categories are ok).
     */
    private static boolean checkEntityCategoryFilter(final String categoryFilter, final Entity entity) {

        if (categoryFilter == null || categoryFilter.isEmpty()) {
            return true; // all categories ok.
        }
        // Must filter on this category.
        String kindId = entity.getKind().getScheme() + entity.getKind().getTerm();
        String actionId;
        boolean categoryFound = false;
        if (kindId.equals(categoryFilter)) {
            categoryFound = true;
        }
        if (!categoryFound) {
            // Search after if this is an action category.
            for (Action action : entity.getKind().getActions()) {
                actionId = action.getScheme() + action.getTerm();
                if (actionId.equals(categoryFilter)) {
                    categoryFound = true;
                    break;
                }
            }
        }
        if (!categoryFound) {
            // Search on mixins.
            String mixinId;
            for (Mixin mixin : entity.getMixins()) {
                mixinId = mixin.getScheme() + mixin.getTerm();
                if (mixinId.equals(categoryFilter)) {
                    categoryFound = true;
                    break;
                }
            }
        }
        return categoryFound;
    }

    /**
     * Check if an entity respect or not the attribute filter. If attribute filter is null, the entity respect the filter.
     *
     * @param filter
     * @param entity
     * @return true if the constraint is validated, false elsewhere.
     */
    private static boolean checkEntityAttributeFilter(final CollectionFilter filter, final Entity entity) {
        boolean control = false;
        String attributeFilter = filter.getAttributeFilter();
        String attributeValue = filter.getValue();
        List<AttributeState> attrs = entity.getAttributes();

        if (attributeFilter == null || attributeFilter.isEmpty()) {
            return true;
        }
        if (!attributeFilter.equals(Constants.OCCI_CORE_SUMMARY)
                && !attributeFilter.equals(Constants.OCCI_CORE_TITLE)
                && !attributeFilter.equals(Constants.OCCI_CORE_ID)
                && !attributeFilter.equals(Constants.OCCI_CORE_TARGET)
                && !attributeFilter.equals(Constants.OCCI_CORE_SOURCE)) {
            for (AttributeState attr : attrs) {
                if (attributeFilter.equalsIgnoreCase(attr.getName())) {
                    // Check the constraint value.
                    if (attributeValue == null) {
                        // Null: all value is ok for this attribute.
                        control = true;
                        break;
                    }
                    // Check the constraint attribute Value filter.
                    if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && attributeValue.equals(attr.getValue())) {
                        control = true;
                        break;
                    }
                    if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && attr.getValue().contains(attributeValue)) {
                        control = true;
                        break;
                    }
                }
            }
        }

        if (!control) {
            // Check on id.
            if (attributeFilter.equals(Constants.OCCI_CORE_ID) || attributeFilter.equalsIgnoreCase("id")) {
                if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && entity.getId().equalsIgnoreCase(attributeValue)) {
                    control = true;
                }
                if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && entity.getId().contains(attributeValue)) {
                    control = true;
                }
            }

            // Check on title.
            if (attributeFilter.equals(Constants.OCCI_CORE_TITLE) || attributeFilter.equalsIgnoreCase("title")) {
                if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && entity.getTitle().equalsIgnoreCase(attributeValue)) {
                    control = true;
                }
                if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && entity.getTitle().contains(attributeValue)) {
                    control = true;
                }
            }

            // Check on summary.
            if (attributeFilter.equals(Constants.OCCI_CORE_SUMMARY) || attributeFilter.equalsIgnoreCase("summary") && entity instanceof Resource) {
                Resource resource = (Resource) entity;
                if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && resource.getSummary().equalsIgnoreCase(attributeValue)) {
                    control = true;
                }
                if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && resource.getSummary().contains(attributeValue)) {
                    control = true;
                }
            }

            // Check on occi.core.source and link (only location source supported).
            if ((attributeFilter.equals(Constants.OCCI_CORE_SOURCE) || attributeFilter.equalsIgnoreCase("source")) && entity instanceof Link) {
                Link link = (Link) entity;
                if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && link.getSource().getLocation().equalsIgnoreCase(attributeValue)) {
                    control = true;
                }
                if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && link.getSource().getLocation().contains(attributeValue)) {
                    control = true;
                }

            }

            // Check on occi.core.target and link (only location target supported).
            if ((attributeFilter.equals(Constants.OCCI_CORE_TARGET)
                    || attributeFilter.equalsIgnoreCase("target"))
                    && entity instanceof Link) {
                Link link = (Link) entity;
                if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && link.getTarget().getLocation().equalsIgnoreCase(attributeValue)) {
                    control = true;
                }
                if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && link.getTarget().getLocation().contains(attributeValue)) {
                    control = true;
                }
            }
        }

        return control;
    }

    /**
     * Get an attribute state object for key parameter.
     *
     * @param key ex: occi.core.title.
     * @return an AttributeState object, if attribute doesnt exist, empty optional object value
     * is returned.
     */
    private static Optional<AttributeState> getAttributeStateObject(Entity entity, final String key) {
        AttributeState attr = null;
        if (key == null) {
            return null;
        }
        // Load the corresponding attribute state.
        for (AttributeState attrState : entity.getAttributes()) {
            if (attrState.getName().equals(key)) {
                attr = attrState;
                break;
            }
        }

        return Optional.ofNullable(attr);
    }

    /**
     * Find an action object from entity definition kind and associated mixins.
     *
     * @param entity   Entity object model.
     * @param actionId the action scheme + action term.
     * @return an action object must never return null.
     * @throws ConfigurationException If no action is found or no entity defined throw this exception.
     */
    public static Action getActionFromEntityWithActionId(final Entity entity, final String actionId) throws ConfigurationException {
        Action action = null;
        boolean found = false;
        List<Action> actions;
        if (entity == null) {
            throw new ConfigurationException("No entity defined for this action : " + actionId);
        }
        Kind kind = entity.getKind();
        Kind currentKind = kind;
        while (currentKind != null && !found) {
            actions = kind.getActions();
            for (Action actionModel : actions) {
                if ((actionModel.getScheme() + actionModel.getTerm()).equals(actionId)) {
                    // The action is referenced on this kind.
                    found = true;
                    action = actionModel;
                    break;
                }
            }
            if (!found) {
                currentKind = currentKind.getParent();
            }
        }
        if (!found) {
            // Search on mixins.
            List<Mixin> mixins = entity.getMixins();
            for (Mixin mixin : mixins) {
                actions = mixin.getActions();
                for (Action actionMixin : actions) {
                    if ((actionMixin.getScheme() + actionMixin.getTerm()).equals(actionId)) {
                        action = actionMixin;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        if (!found) {
            String message = "Action : " + actionId + " is not referenced on entity : " + entity.getId() + ", this action is not referenced on the kind : " + entity.getKind();
            LOGGER.error(message);
            throw new ConfigurationException(message);
        }
        return action;
    }

    /**
     * Get the location of an entity registered by his uuid, if not found, throw a ConfigurationException.
     *
     * @param uuid  uuid v4 of the entity.
     * @param owner owner of the entity.
     * @return a location for uuid provided.
     * @throws ConfigurationException General configuration exception.
     */
    public static String getLocation(final String uuid, final String owner) throws ConfigurationException {

        if (uuid == null || owner == null) {
            throw new ConfigurationException("No uuid provided to find location for owner: " + owner);
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        String location = entitiesOwner.getEntityLocation(uuid);
        if (location == null) {
            throw new ConfigurationException("No location found for uuid : " + uuid);
        }
        return location;
    }

    /**
     * Get the location of an entity, use of a map to attach path to entity uuid.
     *
     * @param entity
     * @return a location for an entity, if this entity has no location, the location will be "/" (root).
     * Must never return null value.
     */
    public static String getLocation(Entity entity, final String owner) {
        if (entity == null) {
            return "";
        }

        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        String location = entitiesOwner.getEntityLocation(entity);
        if (location == null) {
            location = "/" + entity.getId(); // On root path by default.
            LOGGER.warn("Entity : " + entity.getId() + " for title : " + entity.getTitle() + " has no location !!!, setting location on : " + location);
            // To ensure that the path exist on entities path map.
            entitiesOwner.putEntity(location, entity);
        }
        return location;
    }

    /**
     * Return an ecore type from attribute name and entity object.
     *
     * @param entity
     * @param attrName
     * @return
     */
    public static Optional<EDataType> getEAttributeType(Entity entity, String attrName) {
        EDataType eDataType = null;
        String eAttributeName = Occi2Ecore.convertOcciAttributeName2EcoreAttributeName(attrName);
        final EStructuralFeature eStructuralFeature = entity.eClass().getEStructuralFeature(eAttributeName);
        if (eStructuralFeature != null) {
            if ((eStructuralFeature instanceof EAttribute)) {
                // Obtain the attribute type.
                eDataType = ((EAttribute) eStructuralFeature).getEAttributeType();
            }
        }
        return Optional.ofNullable(eDataType);
    }

    /**
     * @param entity
     * @param attrName
     * @return an object container value from EMF attribute object.
     */
    private static Optional<Object> getEMFValueObject(Entity entity, String attrName) {
        EAttribute eAttr;
        Object result = null;
        String eAttributeName = Occi2Ecore.convertOcciAttributeName2EcoreAttributeName(attrName);
        final EStructuralFeature eStructuralFeature = entity.eClass().getEStructuralFeature(eAttributeName);
        if (eStructuralFeature != null) {
            if ((eStructuralFeature instanceof EAttribute)) {
                eAttr = (EAttribute) eStructuralFeature;
                result = entity.eGet(eAttr);
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Get an attribute value from entity with attribute name given.
     *
     * @param entity   Entity object model
     * @param attrName Attribute name
     * @return an attribute value, String, may return empty optional if no value found.
     */
    public static Optional<String> getAttrValueStr(Entity entity, String attrName) {
        String result = null;
        Optional<EDataType> optEdataType = getEAttributeType(entity, attrName);
        if (optEdataType.isPresent()) {
            EDataType eAttrType = optEdataType.get();

            if (eAttrType.getInstanceClass() == String.class || eAttrType instanceof EEnum) {
                Optional<Object> optEValue = getEMFValueObject(entity, attrName);
                if (optEValue.isPresent()) {
                    Object eValue = optEValue.get();
                    result = eValue.toString();
                }
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * @param entity
     * @param attrName
     * @return
     */
    public static Optional<Number> getAttrValueNumber(Entity entity, String attrName) {
        Number result = null;
        Optional<EDataType> optEAttrType = getEAttributeType(entity, attrName);
        if (optEAttrType.isPresent()) {
            EDataType eAttrType = optEAttrType.get();
            if ((eAttrType.getInstanceClass() == Float.class
                    || eAttrType.getInstanceClass() == Integer.class
                    || eAttrType.getInstanceClass() == BigDecimal.class
                    || eAttrType.getInstanceClass() == Number.class
                    || eAttrType.getInstanceClass() == Double.class
                    || eAttrType.getInstanceClass() == Short.class)) {

                Optional<Object> optEValue = getEMFValueObject(entity, attrName);
                if (optEValue.isPresent()) {
                    Object eValue = optEValue.get();
                    result = (Number) eValue;
                }
            }
            if (result == null) {
                if (eAttrType.getInstanceClassName() != null) {
                    String instanceClassName = eAttrType.getInstanceClassName();
                    if (instanceClassName.equals("float")
                            || instanceClassName.equals("int")
                            || instanceClassName.equals("double")
                            || instanceClassName.equals("short")) {

                        Optional<Object> optEValue = getEMFValueObject(entity, attrName);
                        if (optEValue.isPresent()) {
                            Object eValue = optEValue.get();
                            result = (Number) eValue;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * This method is called when no uuid is provided on request, but you have
     * to ensure that only one entity exist for this path.
     *
     * @param location entity location.
     * @param owner    the entity owner.
     * @return an entity from a location, if entity doesnt exist on path,
     * return empty optional.
     */
    public static Optional<Entity> findEntityFromLocation(final String location, final String owner) {
        Entity entity;
        if (location == null || location.trim().isEmpty()) {
            return Optional.empty();
        }
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        if (entitiesOwner == null) {
            LOGGER.warn("Empty entities owner map !! for owner: " + owner);
            return Optional.empty();
        }
        entity = entitiesOwner.getEntityByLocation(location);
        return Optional.ofNullable(entity);
    }

    /**
     * Add a new resource entity to a configuration and update the
     * configuration's map accordingly.
     *
     * @param uuid       Entity id : "uuid unique identifier"
     * @param title      Title of the entity.
     * @param kind       scheme+term
     * @param mixins     (ex:
     *                   mixins=[http://schemas.ogf.org/occi/infrastructure/network# ipnetwork])
     * @param attributes (ex: attributes={occi.network.vlan=12,
     *                   occi.network.label=private, occi.network.address=10.1.0.0/16,
     *                   occi.network.gateway=10.1.255.254})
     * @param owner      Owner of this entity to create.
     * @throws ConfigurationException
     */
    public static void addResourceToConfiguration(String uuid, String title, String summary, String kind, List<String> mixins,
                                                  Map<String, String> attributes, String location, String owner) throws ConfigurationException {

        if (owner == null || owner.isEmpty()) {
            throw new ConfigurationException("No user defined for the current configuration.");
        }

        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);

        // Assign a new resource to configuration, if configuration has resource
        // existed, inform by logger but overwrite existing one.
        boolean resourceOverwrite;
        Optional<Resource> optResource = findResource(uuid, owner);

        Resource resource;
        if (!optResource.isPresent()) {

            resourceOverwrite = false;

            Kind occiKind;

            // Check if kind already exist in realm (on extension model).
            Optional<Kind> optKind = KindManager.findKindFromExtension(kind, owner);

            if (!optKind.isPresent()) {
                // Kind not found on extension, searching on entities.
                optKind = KindManager.findKindFromEntities(kind, owner);
            }

            if (!optKind.isPresent()) {
                throw new ConfigurationException("Kind not found on used extensions");
            }
            occiKind = optKind.get();
            try {
                // Create an OCCI resource with good resource type (via extension model).
                resource = (Resource) OcciHelper.createEntity(occiKind);

                resource.setId(uuid);

                resource.setTitle(title);

                resource.setSummary(summary);

                // Add a new kind to resource (title, scheme, term).
                // if occiKind is null, this will give a default kind parent.
                resource.setKind(occiKind);

                MixinManager.addMixinsToEntity(resource, mixins, owner, false);

                // Add the attributes...
                updateAttributesToEntity(resource, attributes, owner, true); // create all attributes for the new entity.

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + uuid);
                LOGGER.error("Exception class : " + ex.getClass().getName());
                if (ex instanceof ConfigurationException) {
                    throw ex;
                }
                throw new ConfigurationException("Exception thrown while creating an entity: " + uuid + " Message: " + ex.getMessage(), ex);
            }
        } else {
            LOGGER.info("resource already exist, overwriting...");
            resource = optResource.get();
            resourceOverwrite = true;
            resource.setTitle(title);
            resource.setSummary(summary);
            // Add the mixins if any.
            MixinManager.addMixinsToEntity(resource, mixins, owner, true);

            updateAttributesToEntity(resource, attributes, owner, true);

        }
        resource.setLocation(location);
        // Add resource to configuration.
        if (resourceOverwrite) {
            LOGGER.info("resource updated " + resource.getId() + " on OCCI configuration");
        } else {
            configuration.getResources().add(resource);
            LOGGER.info("Added Resource " + resource.getId() + " to configuration object.");
        }
        updateVersion(owner, uuid);

        // Add or full update the entity to this owner.
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        entitiesOwner.putEntity(location, resource);
    }

    /**
     * Add a new link entity to a configuration and update the configuration's
     * map accordingly.
     *
     * @param id
     * @param title
     * @param kind
     * @param mixins
     * @param src
     * @param target
     * @param attributes
     * @param location
     * @param owner
     * @throws ConfigurationException General configuration exception
     */
    public static void addLinkToConfiguration(String id, String title, String kind, List<String> mixins, String src,
                                              String target, Map<String, String> attributes, String location, String owner) throws ConfigurationException {

        if (owner == null || owner.isEmpty()) {
            throw new ConfigurationException("No user defined for the current configuration.");
        }

        boolean overwrite = false;
        Optional<Resource> optResSrc = findResource(src, owner);
        ;
        Optional<Resource> optResDest = findResource(target, owner);

        if (!optResSrc.isPresent()) {
            throw new ConfigurationException("Cannot find the source of the link: " + id);
        }
        if (!optResDest.isPresent()) {
            throw new ConfigurationException("Cannot find the target of the link: " + id);
        }

        Resource resourceSrc = optResSrc.get();
        Resource resourceDest = optResDest.get();
        Optional<Link> optLink = findLink(id, owner);
        Link link = null;
        if (!optLink.isPresent()) {

            Kind occiKind;
            // Check if kind already exist in realm (on extension model).
            Optional<Kind> optKind = KindManager.findKindFromExtension(kind, owner);

            if (!optKind.isPresent()) {
                // Kind not found on extension, searching on entities.
                optKind = KindManager.findKindFromEntities(kind, owner);
            }

            if (!optKind.isPresent()) {
                throw new ConfigurationException("Kind not found on used extensions");
            }
            occiKind = optKind.get();
            try {
                // Link doesnt exist on configuration, we create it.
                link = (Link) OcciHelper.createEntity(occiKind);
                link.setId(id);
                link.setTitle(title);
                // Add a new kind to resource (title, scheme, term).
                link.setKind(occiKind);

                MixinManager.addMixinsToEntity(link, mixins, owner, false);

                updateAttributesToEntity(link, attributes, owner, true);

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + id);
                if (ex instanceof ConfigurationException) {
                    throw ex;
                }

                throw new ConfigurationException("Exception thrown while creating an entity: " + id + " Message: " + ex.getMessage(), ex);
            }
        } else {
            // Link exist upon our configuration, we update it.
            link = optLink.get();
            overwrite = true;
            link.setTitle(title);
            MixinManager.addMixinsToEntity(link, mixins, owner, true);

            updateAttributesToEntity(link, attributes, owner, true);
        }

        link.setSource(resourceSrc);
        link.setTarget(resourceDest);
        link.setLocation(location);
        // Assign link to resource source.
        resourceSrc.getLinks().add(link);

        updateVersion(owner, id);

        if (overwrite) {
            LOGGER.info("Link " + id + " updated !"); //  Version: " + ConfigurationManager.getVersion().get(owner + id));
        } else {
            LOGGER.info("link " + id + " added to configuration !");
        }
        // Add or full update the entity to this owner.
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        entitiesOwner.putEntity(location, link);
    }

    /**
     * Add all attributes not already present.
     *
     * @param entity
     */
    private static void addAllAttributes(Entity entity) {
        // Compute already present attribute names.
        List<AttributeState> attributeStates = entity.getAttributes();
        HashSet<String> attributeNames = new HashSet<>();
        // Iterate over all attribute state instances.
        for (AttributeState attributeState : attributeStates) {
            attributeNames.add(attributeState.getName());
        }

        // Iterate on kind attributes.
        Collection<Attribute> attribsKind = KindManager.getKindAttributes(entity.getKind());
        for (Attribute attribute : attribsKind) {
            String attributeName = attribute.getName();
            if (!attributeNames.contains(attributeName)) {
                AttributeState attributeState = OCCIFactory.eINSTANCE.createAttributeState();
                attributeState.setName(attributeName);
                String attributeDefault = attribute.getDefault();
                // Default value assignement.
                if (attributeDefault != null) {
                    // if default set then set value.
                    LOGGER.info("Assigning default value to attribute : " + attributeName + " --< value: " + attributeDefault);
                    attributeState.setValue(attributeDefault);
                } else {
                    Optional<EDataType> opteAttrType = null;
                    opteAttrType = getEAttributeType(entity, attributeName);
                    // Search default value in datatype object.
                    if (opteAttrType.isPresent()) {

                        EDataType eAttrType = opteAttrType.get();
                        setDefaultValueForDataType(attributeState, eAttrType);
                    }
                }
                // Add it to attribute states of this entity.
                attributeStates.add(attributeState);
            }

        }

        // Iterate on each mixins attributes and update Entity AND mixin base.
        for (MixinBase mixinBase : entity.getParts()) {

            // Update attribute names reference.
            for (AttributeState attributeState : mixinBase.getAttributes()) {
                if (!attributeNames.contains(attributeState.getName())) {
                    attributeNames.add(attributeState.getName());
                }
            }

            Collection<Attribute> attribsMixin = MixinManager.getAllMixinAttribute(mixinBase);
            for (Attribute attribute : attribsMixin) {
                String attributeName = attribute.getName();

                // If attribute state doesnt exist on mixinBase and entity...
                if (!attributeNames.contains(attributeName)) {
                    AttributeState attributeState = OCCIFactory.eINSTANCE.createAttributeState();
                    attributeState.setName(attributeName);
                    String attributeDefault = attribute.getDefault();
                    // Default value assignement.
                    if (attributeDefault != null) {
                        // if default set then set value.
                        LOGGER.info("MixinBase : " + mixinBase.getMixin().getName() + " , assigning default value to attribute : " + attributeName + " --< value: " + attributeDefault);
                        attributeState.setValue(attributeDefault);
                    } else {
                        Optional<EDataType> opteAttrType = null;
                        opteAttrType = MixinManager.getEAttributeType(mixinBase, attributeName);
                        // Search default value in datatype object.
                        if (opteAttrType.isPresent()) {

                            EDataType eAttrType = opteAttrType.get();
                            setDefaultValueForDataType(attributeState, eAttrType);
                        }
                    }

                    // Add it to attribute states of the mixin base.
                    mixinBase.getAttributes().add(attributeState);

                    // Add it to attribute states (same reference as mixin base) on this entity.
                    attributeStates.add(attributeState);
                }

            }
        }
    }

    /**
     * Assign default value for an AttributeState object using ecore cmf datatypes.
     *
     * @param attributeState
     * @param eAttrType
     */
    private static void setDefaultValueForDataType(AttributeState attributeState, EDataType eAttrType) {
        Object objDefault = null;
        if (eAttrType instanceof EEnum || eAttrType.getInstanceClass() == String.class) {
            // value with quote only for String and EEnum type.
            objDefault = eAttrType.getDefaultValue();

        } else {
            objDefault = eAttrType.getDefaultValue();
            if (objDefault != null) {
                try {
                    Number num = (Number) objDefault;
                    objDefault = num.toString();
                } catch (NumberFormatException ex) {
                }
            } else {
                // Assign 0.
                objDefault = 0;
            }
        }
        if (objDefault != null) {
            String attributeDefault = objDefault.toString();
            LOGGER.warn("Default for the attribute : " + attributeState.getName() + " found on datatype: " + eAttrType.getName() + " --< value: " + attributeDefault);
            attributeState.setValue(attributeDefault);
        }

    }


    /**
     * Update / add attributes to entity.
     *
     * @param entity         entity to update.
     * @param attributes     Attributes to update.
     * @param owner          owner of the entity to update.
     * @param fullUpdateMode true if this method is called by addResourceToConfiguration when creating a new entity or full update it.
     * @return Updated entity object, never null.
     */
    public static Entity updateAttributesToEntity(Entity entity, Map<String, String> attributes, final String owner, final boolean fullUpdateMode) {

        List<AttributeState> entityAttributes = entity.getAttributes();

        if (fullUpdateMode) {
            LOGGER.info("Create / full update and add all attributes on entity : " + entity.getTitle());
        } else {
            LOGGER.info("Partial attributes update.");
        }

        LOGGER.info("Attributes found on entity before updating them : ");
        for (AttributeState attrState : entityAttributes) {
            LOGGER.info("Attribute found : " + attrState.getName() + " --> " + attrState.getValue());
        }

        // Only on full update mode.
        if (fullUpdateMode && (attributes == null || attributes.isEmpty())) {
            entity.getAttributes().clear();
        }
        String attrName;
        String attrValue;

        // Ensure that all attributes are in the entity AttributeState list object.
        addAllAttributes(entity);

        if (attributes == null) {
            LOGGER.info("There is no specified attributes to update");
            return entity;
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attrName = entry.getKey();
            attrValue = entry.getValue();
            if (!attrName.isEmpty()
                    && !attrName.equals(Constants.OCCI_CORE_ID) && !attrName.equals(Constants.OCCI_CORE_TARGET) && !attrName.equals(Constants.OCCI_CORE_SOURCE)) {
                LOGGER.debug("Attribute set value : " + attrValue);

                OcciHelper.setAttribute(entity, attrName, attrValue);
                Optional<AttributeState> optAttrState = getAttributeStateObject(entity, attrName);
                AttributeState attrState;
                if (optAttrState.isPresent()) {
                    attrState = optAttrState.get();
                    String attrStateValue = attrState.getValue();
                    if (attrStateValue != null && !attrStateValue.equals(attrValue)) {
                        // update the attribute value.
                        attrState.setValue(attrValue);

                    } else if (attrValue != null) {
                        attrState.setValue(attrValue);
                    }

                    LOGGER.debug("Attribute : " + attrState.getName() + " --> " + attrState.getValue() + " ==> OK");
                }
            }
        }

        updateVersion(owner, entity.getId());

        return entity;
    }

    /**
     * Increment a version of an object (resource or link << entity)
     *
     * @param owner
     * @param id
     */
    public static void updateVersion(final String owner, final String id) {
        String key = owner + id;
        Integer version = versionObjectMap.get(key);
        if (version == null) {
            version = 0;
        }
        version++;
        versionObjectMap.put(key, version);

    }

    /**
     * Remove an entity (resource or link) from the owner's configuration or
     * delete all entities from given kind id or disassociate entities from
     * given mixin id.
     *
     * @param id    (kind id or mixin id or entity Id!)
     * @param owner owner of the model object to remove.
     * @throws ConfigurationException global model exception if no remove operation from id value is found.
     */
    public static void removeOrDissociateFromConfiguration(final String id, final String owner) throws ConfigurationException {

        Kind kind;
        Resource resource;
        Link link;
        Mixin mixin;

        // searching in resources.
        Optional<Resource> optRes = findResource(id, owner);


        if (optRes.isPresent()) {
            resource = optRes.get();
            removeResource(resource, owner);
            return;
        }

        Optional<Link> optLink = findLink(id, owner);
        if (optLink.isPresent()) {
            link = optLink.get();
            removeLink(link, owner);
            return;
        }

        // check if this is a kind id.
        Optional<Kind> optKind = KindManager.findKindFromEntities(id, owner);

        if (optKind.isPresent()) {
            kind = optKind.get();
            removeEntitiesForKind(kind, owner);
            return;
        }

        Optional<Mixin> optMixin = MixinManager.findMixinOnEntities(id, owner);
        if (optMixin.isPresent()) {
            mixin = optMixin.get();
            MixinManager.dissociateMixinFromEntities(mixin, owner);
            return;
        }
        LOGGER.warn("Removing or dissociate object model operation not found.");
        throw new ConfigurationException("Removing or dissociate object model operation not found");
    }

    /**
     * Remove a resource from owner's configuration.
     *
     * @param resource
     * @param owner
     */
    private static void removeResource(final Resource resource, final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
        Iterator<Link> it = resource.getLinks().iterator();
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        // If this resource have links, remove all the links.
        while (it.hasNext()) {
            Link link = it.next();

            Resource src = link.getSource();

            if (!src.equals(resource)) {
                src.getLinks().remove(link);
                entitiesOwner.removeEntity(link);
            }
            Resource target = link.getTarget();
            if (!target.equals(resource)) {
                target.getLinks().remove(link);
                entitiesOwner.removeEntity(link);
            }
        }
        resource.getLinks().clear();
        config.getResources().remove(resource);
        entitiesOwner.removeEntity(resource);
        LOGGER.info("Resource : " + resource.getLocation() + " removed");

    }

    /**
     * Remove a link from owner's configuration.
     *
     * @param link
     */
    private static void removeLink(final Link link, final String owner) {
        Resource resourceSrc = link.getSource();
        Resource resourceTarget = link.getTarget();
        resourceSrc.getLinks().remove(link);
        resourceTarget.getLinks().remove(link);
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        entitiesOwner.removeEntity(link);
        LOGGER.info("Link : " + link.getLocation() + " removed");
    }

    /**
     * Remove all entities for this kind.
     *
     * @param kind
     * @param owner
     */
    private static void removeEntitiesForKind(final Kind kind, final String owner) {
        if (kind == null) {
            return;
        }
        List<Entity> entities = findAllEntitiesForKind(kind.getScheme() + kind.getTerm(), owner);

        for (Entity entity : entities) {
            if (entity instanceof Resource) {
                removeResource((Resource) entity, owner);
            } else if (entity instanceof Link) {
                removeLink((Link) entity, owner);
            }
        }
        entities.clear();
    }

    /**
     * Remove attributes from entity, only for mixins attributes.
     *
     * @param entity
     * @param attributesToRemove
     */
    public static void removeEntityAttributes(Entity entity, EList<Attribute> attributesToRemove) {

        Iterator<AttributeState> entityAttrs = entity.getAttributes().iterator();
        boolean isKindAttribute;
        while (entityAttrs.hasNext()) {
            AttributeState attrState = entityAttrs.next();
            for (Attribute attribute : attributesToRemove) {
                isKindAttribute = KindManager.isKindAttribute(entity.getKind(), attribute.getName());
                if (attribute.getName().equals(attrState.getName()) && !isKindAttribute) {
                    entityAttrs.remove();
                }
            }
        }

    }

    /**
     * Check if an UUID is provided on a String or attribute occi.core.id.
     *
     * @param id,  an uuid or a path like foo/bar/myuuid
     * @param attr
     * @return true if provided or false if not provided
     */
    public static boolean isEntityUUIDProvided(final String id, final Map<String, Object> attr) {
        String[] uuids = id.split("/");
        boolean match = false;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                match = true;
                break;
            }
        }
        String occiCoreId = (String) attr.get(Constants.OCCI_CORE_ID);
        if (!match && occiCoreId != null && !occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }

        }

        return match;
    }

    /**
     * Search for UUID on a entityId String before attribute occi.core.id.
     *
     * @param path a location like /mylocation/myuuid/
     * @param attr a map of attributes.
     * @return the UUID provided may return optional empty if uuid not found.
     */
    public static Optional<String> getUUIDFromPath(final String path, final Map<String, String> attr) {
        String[] uuids = path.split("/");
        String uuidToReturn = null;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                uuidToReturn = uuid;
                break;
            }
        }
        if (uuidToReturn != null) {
            return Optional.of(uuidToReturn);
        }

        // Check with occi.core.id attribute.
        String occiCoreId = attr.get(Constants.OCCI_CORE_ID);
        if (occiCoreId == null) {
            return Optional.empty();
        }
        occiCoreId = occiCoreId.replace(Constants.URN_UUID_PREFIX, "");
        if (!occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        return Optional.of(uuid);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Check if the uuid is a valid one.
     *
     * @param uuid
     * @return true if the uuid provided is valid false elsewhere.
     */
    public static boolean isUUIDValid(final String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        return uuid.matches(REGEX_CONTROL_UUID);
    }

    /**
     * Get all entities uuid registered on the same path collection.
     *
     * @param path
     * @param owner
     * @return a List of String uuids
     */
    public static List<String> getEntityUUIDsFromPath(final String path, final String owner) {
        List<String> entitiesUUID = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return entitiesUUID;
        }

        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);

        Map<String, Entity> entitiesLocationMap = entitiesOwner.getEntitiesByLocation();

        String uuid;
        String pathTmp;
        for (Map.Entry<String, Entity> entry : entitiesLocationMap.entrySet()) {

            pathTmp = entry.getKey();
            uuid = entry.getValue().getId();

            if (path.equals(pathTmp)) {
                entitiesUUID.add(uuid);
            } else if ((path.startsWith(pathTmp) || pathTmp.startsWith(path)) && !pathTmp.isEmpty()) {
                entitiesUUID.add(uuid);
            }
        }
        return entitiesUUID;
    }

    /**
     * Convert entity attributes to Map object. String name, Object value
     *
     * @param entity must never be null
     * @return attributes map, this must never return null.
     */
    public static Map<String, Object> convertEntityAttributesToMap(final Entity entity) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        List<AttributeState> attrsState = entity.getAttributes();
        for (AttributeState attr : attrsState) {
            String key = attr.getName();
            String val = attr.getValue();
            if (val != null) {
                if (!key.equals(Constants.OCCI_CORE_SUMMARY) && !key.equals(Constants.OCCI_CORE_TITLE)
                        && !key.equals(Constants.OCCI_CORE_ID)
                        && !key.equals(Constants.OCCI_CORE_SOURCE)
                        && !key.equals(Constants.OCCI_CORE_TARGET)) {
                    Optional<EDataType> opteAttrType = getEAttributeType(entity, key);

                    EDataType eAttrType;

                    if (opteAttrType.isPresent()) {
                        eAttrType = opteAttrType.get();
                        if (eAttrType instanceof EEnum || eAttrType.getInstanceClass() == String.class) {
                            // value with quote only for String and EEnum type.
                            attributes.put(key, val);

                        } else {
                            // Not a string nor an enum val.
                            try {
                                Number num = Utils.parseNumber(val, eAttrType.getInstanceClass());
                                attributes.put(key, num);
                            } catch (NumberFormatException ex) {
                                LOGGER.debug("Cannot convert a value : " + val + " to an emf datatype : " + eAttrType.getName());
                                attributes.put(key, val);
                            }
                        }
                    } else {
                        // Cant define the data type.
                        attributes.put(key, val);
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

        return attributes;
    }


    /**
     * Execute an action on an entity.
     *
     * @param entity           entity object model
     * @param actionId         action scheme + term
     * @param actionAttributes action attributes map of String (name), String (value)
     * @param owner            owner of the configuration model
     * @throws ConfigurationException
     */
    public static void executeActionOnEntity(Entity entity, final String actionId, final Map<String, String> actionAttributes, final String owner) throws ConfigurationException {
        if (entity == null) {
            throw new ConfigurationException("No entity defined to execute this action: " + actionId);
        }

        Optional<Extension> optExt = getExtensionForAction(actionId, owner);
        if (!optExt.isPresent()) {
            LOGGER.error("Action " + actionId + " doesnt exist on referenced extensions");
            throw new ConfigurationException("Action " + actionId + " doesnt exist on referenced extensions");
        }

        Action action = getActionFromEntityWithActionId(entity, actionId);
        String actionTerm = action.getTerm();
        String[] actionParameters = Utils.getActionParametersArray(actionAttributes);
        try {
            if (actionParameters == null) {
                OcciHelper.executeAction(entity, actionTerm);
            } else {
                OcciHelper.executeAction(entity, actionTerm, actionParameters);
            }
        } catch (InvocationTargetException ex) {
            String message;
            if (ex.getMessage() != null) {
                message = "Internal error while executing action : " + actionTerm + ", message: " + ex.getMessage();
            } else {
                message = "Internal error while executing action : " + actionTerm;
            }
            throw new ConfigurationException(message, ex);
        }

    }

    /**
     * Execute action on an entity location.
     *
     * @param location         entity location
     * @param actionId         action scheme + term
     * @param actionAttributes map of action attributes
     * @param owner            the owner of the entity where to trigger the action
     */
    public static void executeActionOnEntityLocation(final String location, final String actionId, final Map<String, String> actionAttributes, final String owner) throws ConfigurationException {

        Optional<Entity> optEntity;
        Optional<Extension> optExt;
        Entity entity;


        if (location == null) {
            throw new ConfigurationException("No entity location defined.");
        }
        if (actionId == null) {
            throw new ConfigurationException("No action scheme+term defined.");
        }
        optEntity = findEntityFromLocation(location, owner);

        if (!optEntity.isPresent()) {
            throw new ConfigurationException("Entity on location: " + location + " doesnt exist on configuration");
        }

        optExt = getExtensionForAction(actionId, owner);
        if (!optExt.isPresent()) {
            LOGGER.error("Action " + actionId + " doesnt exist on referenced extensions");
            throw new ConfigurationException("Action " + actionId + " doesnt exist on referenced extensions");
        }
        entity = optEntity.get();
        Action action = getActionFromEntityWithActionId(entity, actionId);

        if (action == null) {
            LOGGER.error("Action cannot be executed on entity : " + entity.getId() + ", this action is not referenced on the kind : " + entity.getKind());
            throw new ConfigurationException("Action cannot be executed on entity : " + entity.getId() + ", this action is not referenced on the kind : " + entity.getKind());
        }
        String actionTerm = action.getTerm();
        String[] actionParameters = Utils.getActionParametersArray(actionAttributes);
        try {
            if (actionParameters == null) {
                OcciHelper.executeAction(entity, actionTerm);
            } else {
                OcciHelper.executeAction(entity, actionTerm, actionParameters);
            }
        } catch (Exception ex) {
            String message;
            if (ex.getMessage() != null) {
                message = "Internal error while executing action : " + actionTerm + ", message: " + ex.getMessage();
            } else {
                message = "Internal error while executing action : " + actionTerm;
            }
            if (ex.getCause() != null) {
                message += ", cause: " + ex.getCause().getClass().getName();
                if (ex.getCause().getMessage() != null) {
                    message += ", " + message;
                }

            }
            throw new ConfigurationException(message, ex);
        }

    }

    /**
     * Check if an action exist on model. Throws configuration exception if not found on model.
     *
     * @param action action scheme+term
     */
    public static void checkActionOnModel(final String action, final String username) throws ConfigurationException {

        String message;
        if (action == null || action.trim().isEmpty()) {
            // No action defined.
            message = "Action is not defined";
            throw new ConfigurationException(message);
        }

        // Find action category on model.
        Optional<Action> optAct = ConfigurationManager.findActionOnExtensions(action, username);

        if (!optAct.isPresent()) {
            message = "Action : " + action + " not found on referenced extensions";
            throw new ConfigurationException(message);
        }
    }


    public static void buildEntitiesOwner(final String owner) {
        entitiesOwnerMap.put(owner, new EntitiesOwner(owner));
    }

    public static Optional<Entity> findEntityForUuid(final String entityId, final String owner) {
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        Entity entity = entitiesOwner.getEntityByUuid(entityId);
        return Optional.ofNullable(entity);
    }

    /**
     * Print on logger an entity.
     * Used only for debugging purpose.
     *
     * @param entity
     */
    public static void printEntity(Entity entity) {

        StringBuilder builder = new StringBuilder("");
        if (entity instanceof Resource) {
            builder.append("Entity is a resource. \n");
        }
        if (entity instanceof Link) {
            builder.append("Entity is a link.\n");
        }
        builder.append("id : ").append(entity.getId()).append(" \n");
        builder.append("kind : ").append(entity.getKind().getScheme()).append(entity.getKind().getTerm()).append(" \n ");
        if (!entity.getMixins().isEmpty()) {
            builder.append("mixins : ").append(entity.getMixins().toString()).append(" \n ");
        } else {
            builder.append("entity has no mixins" + " \n ");
        }
        builder.append("Entity attributes : " + " \n ");
        if (entity.getAttributes().isEmpty()) {
            builder.append("no attributes found." + " \n ");
        }
        for (AttributeState attribute : entity.getAttributes()) {
            builder.append("--> name : ").append(attribute.getName()).append(" \n ");
            builder.append("-- value : ").append(attribute.getValue()).append(" \n ");
        }
        if (entity.getKind().getActions().isEmpty()) {
            builder.append("entity has no action \n ");
        } else {
            builder.append("entity has actions available : \n ");
            for (Action action : entity.getKind().getActions()) {
                builder.append(action.getTitle()).append("--> ").append(action.getScheme()).append(action.getTerm()).append(" \n ");
            }
        }
        LOGGER.debug(builder.toString());

    }


    /**
     * Update all entity in owner's configuration on mapped objects (cross references).
     *
     * @param owner
     */
    public static void updateAllReferencesOnEntitiesOwner(final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);

        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);

        if (entitiesOwner == null) {
            buildEntitiesOwner(owner);
        }

        // List all resources
        for (Resource resource : config.getResources()) {
            updateEntityReferences(resource, owner);

            // Do the same for all the linked entities.
            EList<Link> links = resource.getLinks();
            for (Link link : links) {
                updateEntityReferences(link, owner);
            }
        }

    }

    /**
     * Update all mapped reference for an entity.
     *
     * @param entity
     * @param owner
     */
    private static void updateEntityReferences(final Entity entity, final String owner) {
        String uuid = entity.getId();
        String location = entity.getLocation();
        EntitiesOwner entitiesOwner = entitiesOwnerMap.get(owner);
        // Update references.
        entitiesOwner.putEntity(location, entity);
        updateVersion(owner, uuid);
    }


    public static void clearReferences(final String owner) {
        entitiesOwnerMap.remove(owner);
        versionObjectMap.remove(owner);
    }
}
