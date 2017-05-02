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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.occiware.clouddesigner.occi.*;
import org.occiware.clouddesigner.occi.util.Occi2Ecore;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.server.exception.ConfigurationException;
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
     * key: uuid, value: entity relative path.
     */
    private static Map<String, String> entitiesLocation = new ConcurrentHashMap<>();


    /**
     * Used only to create an eTag when object are updated. Key : owner+objectId
     * Value : version number. First version is 1.
     */
    private static Map<String, Integer> versionObjectMap = new ConcurrentHashMap<>();

    /**
     * Find a resource for owner and entity Id.
     *
     * @param owner
     * @param id    (may be an uuid, a path/uuid or a path.)
     * @return an OCCI resource.
     */
    private static Resource findResource(final String owner, final String id) {
        Resource resFound = null;
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);

        boolean isEntityUUID = isEntityUUIDProvided(id, new HashMap<>());
        String entityUUID;
        if (isEntityUUID) {
            entityUUID = getUUIDFromPath(id, new HashMap<>());

            for (Resource resource : configuration.getResources()) {
                if (resource.getId().equals(entityUUID)) {
                    resFound = resource;
                    break;
                }
            }
        }

        if (resFound == null && !isEntityUUID) {
            List<String> uuids = getEntityUUIDsFromPath(id);
            if (uuids.size() == 1) {
                String uuidTmp = uuids.get(0);
                for (Resource resource : configuration.getResources()) {
                    if (resource.getId().equals(uuidTmp)) {
                        resFound = resource;
                        break;
                    }
                }
            }
        }

        return resFound;
    }

    /**
     * Find a link on all chains of resources.
     *
     * @param owner
     * @param id
     * @return
     */
    private static Link findLink(final String owner, final String id) {
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
        String entityUUID;
        boolean isEntityUUID = isEntityUUIDProvided(id, new HashMap<>());

        Link link = null;
        EList<Link> links;
        for (Resource resource : configuration.getResources()) {
            entityUUID = getUUIDFromPath(id, new HashMap<>());
            links = resource.getLinks();
            if (!links.isEmpty()) {
                for (Link lnk : links) {
                    if (lnk.getId().equals(entityUUID)) {
                        link = lnk;
                        break;

                    }
                }
                if (link != null) {
                    break;
                }
            }

        }

        if (link == null && !isEntityUUID) {
            // The id hasn't an uuid. Search on map if a single entity is on the path.
            List<String> uuids = getEntityUUIDsFromPath(id);
            if (uuids.size() == 1) {
                String uuidTmp = uuids.get(0);

                for (Resource resource : configuration.getResources()) {
                    links = resource.getLinks();
                    if (!links.isEmpty()) {
                        for (Link lnk : links) {
                            if (lnk.getId().equals(uuidTmp)) {
                                link = lnk;
                                break;

                            }
                        }
                        if (link != null) {
                            break;
                        }
                    }
                }
            }
        }

        return link;
    }

    /**
     * Search an entity (link or resource) on the current configuration.
     *
     * @param id    (entityId is unique for all owners)
     * @param owner
     * @return an OCCI Entity, could be null, if entity has is not found.
     * @throws ConfigurationException
     */
    public static Entity findEntity(final String id, String owner) {
        Entity entity = null;
        if (owner == null) {
            owner = ConfigurationManager.DEFAULT_OWNER;
        }
        Resource resource = findResource(owner, id);
        Link link;
        if (resource == null) {
            link = findLink(owner, id);
            if (link != null) {
                entity = link;
            }
        } else {
            entity = resource;
        }

        return entity;
    }

    /**
     * @param owner
     * @param id
     * @return true if entity exist or false if it doesnt exist.
     */
    public static boolean isEntityExist(final String owner, final String id) {
        if (isEntityUUIDProvided(id, new HashMap<>())) {
            return findEntity(id, owner) != null;
        } else {
            String path;
            boolean found = false;
            for (Map.Entry<String, String> entry : entitiesLocation.entrySet()) {
                path = entry.getValue();
                if (path.equals(id)) {
                    found = true;
                    break;
                }

            }
            return found;
        }
    }

    /**
     * Find entities for a categoryId (kind or Mixin or actions). actions has no
     * entity list and it's not used here.
     *
     * @param filter
     * @param owner
     * @return a list of entities (key: owner, value : List of entities).
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
     * @return
     */
    public static List<Entity> findAllEntitiesOwner(final String owner) {
        List<Entity> entities = new ArrayList<>();
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
        EList<Resource> resources = configuration.getResources();
        EList<Link> links;
        for (Resource resource : resources) {
            entities.add(resource);
            links = resource.getLinks();
            if (!links.isEmpty()) {
                for (Link link : links) {
                    if (!entities.contains(link)) {
                        entities.add(link);
                    }
                }
            }
        }

        return entities;
    }

    /**
     * Find all entities with that kind. (replace getEntities from kind object).
     *
     * @param owner
     * @param categoryId
     * @return
     */
    private static List<Entity> findAllEntitiesForKind(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        for (Resource res : ConfigurationManager.getConfigurationForOwner(owner).getResources()) {
            if ((res.getKind().getScheme() + res.getKind().getTerm()).equals(categoryId)) {
                entities.add(res);
            }
            for (Link link : res.getLinks()) {
                if ((link.getKind().getScheme() + link.getKind().getTerm()).equals(categoryId)) {
                    entities.add(link);
                }
            }

        }
        return entities;

    }

    /**
     * Find all entities for a mixin.
     *
     * @param owner  username
     * @param categoryId the category scheme + term.
     * @return a list of entities objects.
     */
    public static List<Entity> findAllEntitiesForMixin(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        for (Resource res : ConfigurationManager.getConfigurationForOwner(owner).getResources()) {
            for (Mixin mix : res.getMixins()) {
                if ((mix.getScheme() + mix.getTerm()).equals(categoryId)) {
                    entities.add(res);
                }
            }
            for (Link link : res.getLinks()) {
                for (Mixin mix : link.getMixins()) {
                    if ((mix.getScheme() + mix.getTerm()).equals(categoryId)) {
                        entities.add(link);
                    }
                }

            }

        }
        return entities;
    }

    /**
     * Find all entities for a category.
     *
     * @param owner
     * @param categoryId (id of kind, mixin or action, composed by scheme+term.
     * @return a collection list of entity.
     */
    public static List<Entity> findAllEntitiesForCategory(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        String kind;
        for (Resource res : ConfigurationManager.getConfigurationForOwner(owner).getResources()) {
            kind = res.getKind().getScheme() + res.getKind().getTerm();

            if (kind.equals(categoryId)) {
                entities.add(res);
                continue;
            }

            for (Action act : res.getKind().getActions()) {
                if ((act.getScheme() + act.getTerm()).equals(categoryId)) {
                    entities.add(res);
                }

            }
            for (Mixin mixin : res.getMixins()) {
                for (Action act : mixin.getActions()) {

                    if ((act.getScheme() + act.getTerm()).equals(categoryId)) {
                        entities.add(res);
                    }
                }
            }

            for (Link link : res.getLinks()) {
                for (Action act : link.getKind().getActions()) {
                    if ((act.getScheme() + act.getTerm()).equals(categoryId)) {
                        entities.add(link);
                    }
                }
                for (Mixin mixin : link.getMixins()) {
                    for (Action act : mixin.getActions()) {

                        if ((act.getScheme() + act.getTerm()).equals(categoryId)) {
                            entities.add(link);
                        }
                    }
                }
            }

        }
        return entities;
    }

    /**
     * Get all the attributes of an Entity instance.
     *
     * @param entity the given Entity instance.
     * @return all the attributes of the given instance.
     */
    private static Collection<Attribute> getAllAttributes(final Entity entity) {
        List<Attribute> attributes = new ArrayList<>();
        Kind entityKind = entity.getKind();
        if (entityKind != null) {
            ConfigurationManager.addAllAttributes(attributes, entityKind);
        }
        for (Mixin mixin : entity.getMixins()) {
            ConfigurationManager.addAllAttributes(attributes, mixin);
        }
        return attributes;
    }

    /**
     * Find a used extension for an action Kind.
     *
     * @param owner     (owner of the configuration).
     * @param action_id (kind : scheme+term)
     * @return extension found, may return null if no extension found with this
     * configuration.
     */
    public static Extension getExtensionForAction(String owner, String action_id) {
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
                    if ((action.getScheme() + action.getTerm()).equals(action_id)) {
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

        return extRet;
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
     * @param filter
     * @param sources
     * @return a filtered list of entities.
     */
    private static List<Entity> filterEntities(final CollectionFilter filter, List<Entity> sources, final String user) {

        String categoryFilter = filter.getCategoryFilter();
        if (categoryFilter != null && !categoryFilter.isEmpty() && !ConfigurationManager.checkIfCategorySchemeTerm(categoryFilter, user)) {
            categoryFilter = ConfigurationManager.findCategorySchemeTermFromTerm(categoryFilter, user);
        }

        String filterOnPath = filter.getFilterOnPath();
        if (filterOnPath != null && !filterOnPath.isEmpty()) {
            if (filterOnPath.endsWith("/")) {
                filterOnPath = filterOnPath.substring(0, filterOnPath.length() - 1);
            }
        }

        Iterator<Entity> it = sources.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();

            if (checkEntityAttributeFilter(filter, entity) && checkEntityCategoryFilter(categoryFilter, entity) && checkEntityFilterOnPath(filterOnPath, entity)) {
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
     * @param filterOnPath
     * @param entity
     * @return true if constraint path is respected (or if filter on path is null or empty) and false elsewhere.
     */
    private static boolean checkEntityFilterOnPath(final String filterOnPath, final Entity entity) {
        boolean result = false;

        String filterPath = filterOnPath;
        if (filterPath == null || filterPath.isEmpty()) {
            return true;
        }

        String relativeLocation;
        try {
            relativeLocation = getLocation(entity.getId());
            relativeLocation = relativeLocation.replaceAll("\\s+", "");
            if (!relativeLocation.endsWith("/")) {
                relativeLocation = relativeLocation + "/";
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
        return control;
    }

    /**
     * Get an attribute state object for key parameter.
     *
     * @param key ex: occi.core.title.
     * @return an AttributeState object, if attribute doesnt exist, null value
     * is returned.
     */
    private static AttributeState getAttributeStateObject(Entity entity, final String key) {
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

        return attr;
    }

    /**
     * @param category
     * @param entity
     * @return
     */
    public static boolean isCategoryReferencedOnEntity(final String category, final Entity entity) {
        if (entity == null || category == null) {
            return false; // must not arrive.
        }

        Kind kind = entity.getKind();
        List<Mixin> mixins = entity.getMixins();
        String kindId = kind.getScheme() + kind.getTerm();

        if (kindId.equals(category)) {
            return true;
        }
        for (Mixin mixin : mixins) {
            String mixinId = mixin.getScheme() + mixin.getTerm();
            if (mixinId.equals(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find an action object from entity definition kind and associated mixins.
     *
     * @param entity     Entity object model.
     * @param actionTerm the action term like start.
     * @return an action object must never return null.
     * @throws ConfigurationException If no action is found or no entity defined throw this exception.
     */
    public static Action getActionFromEntityWithActionTerm(final Entity entity, String actionTerm) throws ConfigurationException {
        Action action = null;
        boolean found = false;
        if (entity == null) {
            throw new ConfigurationException("No entity defined for this action : " + actionTerm);
        }
        Kind kind = entity.getKind();
        List<Action> actions = kind.getActions();

        // Search the action on kind first.
        for (Action actionKind : actions) {
            if (actionKind.getTerm().equals(actionTerm)) {
                action = actionKind;
                found = true;
                break;
            }
        }

        if (!found) {
            // Search on mixins.
            List<Mixin> mixins = entity.getMixins();
            for (Mixin mixin : mixins) {
                actions = mixin.getActions();
                for (Action actionMixin : actions) {
                    if (actionMixin.getTerm().equals(actionTerm)) {
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
            throw new ConfigurationException("Action " + actionTerm + " not found on entity : " + entity.getId());
        }

        return action;
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
        if (entity == null) {
            throw new ConfigurationException("No entity defined for this action : " + actionId);
        }
        Kind kind = entity.getKind();
        List<Action> actions = kind.getActions();

        // Search the action on kind first.
        for (Action actionKind : actions) {
            if ((actionKind.getScheme() + actionKind.getTerm()).equals(actionId)) {
                action = actionKind;
                found = true;
                break;
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
            throw new ConfigurationException("Action " + actionId + " not found on entity : " + entity.getId());
        }

        return action;
    }

    /**
     * Get the location of an entity registered by his uuid, if not found, throw a ConfigurationException.
     *
     * @param uuid uuid v4 of the entity.
     * @return a location for uuid provided.
     * @throws ConfigurationException General configuration exception.
     */
    public static String getLocation(String uuid) throws ConfigurationException {
        if (uuid == null) {
            throw new ConfigurationException("No uuid provided to find location.");
        }
        String result = getEntitiesLocation().get(uuid);
        if (result == null) {
            throw new ConfigurationException("No location found for uuid : " + uuid);
        }
        return result;
    }

    /**
     * Get the location of an entity, use of a map to attach path to entity uuid.
     *
     * @param entity
     * @return a location for an entity, if this entity has no location, the location will be "/" (root).
     * Must never return null value.
     */
    public static String getLocation(Entity entity) {
        if (entity == null || entitiesLocation == null) {
            if (entity == null) {
                return "";
            }
            entitiesLocation = new ConcurrentHashMap<>();
        }
        // TODO : Check if in future we have location defined in connectors.
        String location = entitiesLocation.get(entity.getId());

        if (location == null) {
            location = "/"; // On root path by default.
            // To ensure that the path exist on entities path map.
            entitiesLocation.put(entity.getId(), location);
        }

        // we have maybe no leading slash.
        if (!location.equals("/") && !location.endsWith("/")) {
            location += "/";
        }

        // location += entity.getId(); // add the uuid to get a full location.

        return location;
    }

    /**
     * Return an ecore type from attribute name and entity object.
     *
     * @param entity
     * @param attrName
     * @return
     */
    public static EDataType getEAttributeType(Entity entity, String attrName) {
        EDataType eDataType = null;
        String eAttributeName = Occi2Ecore.convertOcciAttributeName2EcoreAttributeName(attrName);
        final EStructuralFeature eStructuralFeature = entity.eClass().getEStructuralFeature(eAttributeName);
        if (eStructuralFeature != null) {
            if ((eStructuralFeature instanceof EAttribute)) {
                // Obtain the attribute type.
                eDataType = ((EAttribute) eStructuralFeature).getEAttributeType();
            }
        }
        return eDataType;
    }

    /**
     * @param entity
     * @param attrName
     * @return an object container value from EMF attribute object.
     */
    private static Object getEMFValueObject(Entity entity, String attrName) {
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
        return result;
    }

    /**
     * @param entity
     * @param attrName
     * @return
     */
    public static String getAttrValueStr(Entity entity, String attrName) {
        String result = null;
        EDataType eAttrType = getEAttributeType(entity, attrName);

        if (eAttrType != null && eAttrType.getInstanceClass() == String.class || eAttrType instanceof EEnum) {
            Object eValue = getEMFValueObject(entity, attrName);
            if (eValue != null) {
                result = eValue.toString();
            }
        }
        return result;
    }

    /**
     * @param entity
     * @param attrName
     * @return
     */
    public static Number getAttrValueNumber(Entity entity, String attrName) {
        Number result = null;
        EDataType eAttrType = getEAttributeType(entity, attrName);
        if (eAttrType != null &&
                (eAttrType.getInstanceClass() == Float.class
                        || eAttrType.getInstanceClass() == Integer.class
                        || eAttrType.getInstanceClass() == BigDecimal.class
                        || eAttrType.getInstanceClass() == Number.class
                        || eAttrType.getInstanceClass() == Double.class
                        || eAttrType.getInstanceClass() == Short.class)) {

            Object eValue = getEMFValueObject(entity, attrName);
            if (eValue != null) {
                result = (Number) eValue;
            }
        }
        if (result == null && eAttrType != null) {
            if (eAttrType.getInstanceClassName() != null) {
                String instanceClassName = eAttrType.getInstanceClassName();
                if (instanceClassName.equals("float")
                        || instanceClassName.equals("int")
                        || instanceClassName.equals("double")
                        || instanceClassName.equals("short")) {
                    Object eValue = getEMFValueObject(entity, attrName);
                    if (eValue != null) {
                        result = (Number) eValue;
                    }
                }
            }
        }

        return result;
    }

    /**
     * This method is called when no uuid is provided on request, but you have
     * to ensure that only one entity exist for this path.
     *
     * @param path
     * @return an entity from a relative path, if entity doesnt exist on path,
     * return null.
     */
    public static Entity findEntityFromLocation(final String path, final String username) {
        Entity entity = null;
        String uuid = null;
        String pathTmp;
        if (path != null) {
            for (Map.Entry<String, String> entry : entitiesLocation.entrySet()) {
                uuid = entry.getKey();
                pathTmp = entry.getValue();
                if (path.equals(pathTmp)) {
                    // entity found.
                    break;
                }
            }
            if (uuid != null) {
                entity = findEntity(uuid, username);
            }

        }

        return entity;
    }

    /**
     * Add a new resource entity to a configuration and update the
     * configuration's map accordingly.
     *
     * @param id           (entity id : "uuid unique identifier")
     * @param title
     * @param kind         (scheme#term)
     * @param mixins       (ex:
     *                     mixins=[http://schemas.ogf.org/occi/infrastructure/network# ipnetwork])
     * @param attributes   (ex: attributes={occi.network.vlan=12,
     *                     occi.network.label=private, occi.network.address=10.1.0.0/16,
     *                     occi.network.gateway=10.1.255.254})
     * @param owner
     * @param relativePath (ex: compute/myuuid
     * @throws ConfigurationException
     */
    public static void addResourceToConfiguration(String id, String title, String summary, String kind, List<String> mixins,
                                                  Map<String, String> attributes, String owner, String relativePath) throws ConfigurationException {

        if (owner == null || owner.isEmpty()) {
            // Assume if owner is not used to a default user uuid "anonymous".
            owner = ConfigurationManager.DEFAULT_OWNER;
        }

        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);

        // Assign a new resource to configuration, if configuration has resource
        // existed, inform by logger but overwrite existing one.
        boolean resourceOverwrite;
        Resource resource = findResource(owner, id);
        if (resource == null) {
            resourceOverwrite = false;

            Kind occiKind;

            // Check if kind already exist in realm (on extension model).
            occiKind = KindManager.findKindFromExtension(kind, owner);

            if (occiKind == null) {
                // Kind not found on extension, searching on entities.
                occiKind = KindManager.findKindFromEntities(kind, owner);
            }
            try {
                // Create an OCCI resource with good resource type (via extension model).
                resource = (Resource) OcciHelper.createEntity(occiKind);

                resource.setId(id);

                resource.setTitle(title);

                resource.setSummary(summary);

                // Add a new kind to resource (title, scheme, term).
                // if occiKind is null, this will give a default kind parent.
                resource.setKind(occiKind);

                MixinManager.addMixinsToEntity(resource, mixins, owner, false);

                // Add the attributes...
                updateAttributesToEntity(resource, attributes, owner);

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + id);
                LOGGER.error("Exception class : " + ex.getClass().getName());
                if (ex instanceof ConfigurationException) {
                    throw ex;
                }
                throw new ConfigurationException("Exception thrown while creating an entity: " + id + " Message: " + ex.getMessage(), ex);
            }
        } else {
            LOGGER.info("resource already exist, overwriting...");
            resourceOverwrite = true;
            resource.setTitle(title);
            resource.setSummary(summary);
            // Add the mixins if any.
            MixinManager.addMixinsToEntity(resource, mixins, owner, true);

            updateAttributesToEntity(resource, attributes, owner);

        }

        // Add resource to configuration.
        if (resourceOverwrite) {
            LOGGER.info("resource updated " + resource.getId() + " on OCCI configuration");
        } else {
            configuration.getResources().add(resource);
            LOGGER.info("Added Resource " + resource.getId() + " to configuration object.");
        }
        updateVersion(owner, id);
        // Add the entity to relative path map.
        entitiesLocation.put(id, relativePath);

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
     * @param owner
     * @param relativePath
     * @throws ConfigurationException General configuration exception
     */
    public static void addLinkToConfiguration(String id, String title, String kind, List<String> mixins, String src,
                                              String target, Map<String, String> attributes, String owner, String relativePath) throws ConfigurationException {

        if (owner == null || owner.isEmpty()) {
            // Assume if owner is not used to a default user uuid "anonymous".
            owner = ConfigurationManager.DEFAULT_OWNER;
        }

        boolean overwrite = false;
        Resource resourceSrc = findResource(owner, src);
        Resource resourceDest = findResource(owner, target);

        if (resourceSrc == null) {
            throw new ConfigurationException("Cannot find the source of the link: " + id);
        }
        if (resourceDest == null) {
            throw new ConfigurationException("Cannot find the target of the link: " + id);
        }

        Link link = findLink(owner, id);
        if (link == null) {

            Kind occiKind;
            // Check if kind already exist in realm (on extension model).
            occiKind = KindManager.findKindFromExtension(kind, owner);

            if (occiKind == null) {
                // Kind not found on extension, searching on entities.
                occiKind = KindManager.findKindFromEntities(kind, owner);
            }
            try {
                // Link doesnt exist on configuration, we create it.
                link = (Link) OcciHelper.createEntity(occiKind);
                link.setId(id);
                link.setTitle(title);
                // Add a new kind to resource (title, scheme, term).
                link.setKind(occiKind);

                MixinManager.addMixinsToEntity(link, mixins, owner, false);

                updateAttributesToEntity(link, attributes, owner);

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + id);
                if (ex instanceof ConfigurationException) {
                    throw ex;
                }

                throw new ConfigurationException("Exception thrown while creating an entity: " + id + " Message: " + ex.getMessage(), ex);
            }
        } else {
            // Link exist upon our configuration, we update it.

            overwrite = true;
            link.setTitle(title);
            MixinManager.addMixinsToEntity(link, mixins, owner, true);

            updateAttributesToEntity(link, attributes, owner);
        }


        link.setSource(resourceSrc);
        link.setTarget(resourceDest);

        // Assign link to resource source.
        resourceSrc.getLinks().add(link);

        updateVersion(owner, id);

        if (overwrite) {
            LOGGER.info("Link " + id + " updated !"); //  Version: " + ConfigurationManager.getVersion().get(owner + id));
        } else {
            LOGGER.info("link " + id + " added to configuration !");
        }
        entitiesLocation.put(id, relativePath);

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
        Collection<Attribute> attribs = getAllAttributes(entity);
        // Iterate over all attributes.
        for (Attribute attribute : attribs) {
            String attributeName = attribute.getName();
            if (!attributeNames.contains(attributeName)) {
                // If not already present create it.
                AttributeState attributeState = OCCIFactory.eINSTANCE.createAttributeState();
                attributeState.setName(attributeName);
                String attributeDefault = attribute.getDefault();
                if (attributeDefault != null) {
                    // if default set then set value.
                    attributeState.setValue(attributeDefault);
                }
                // Add it to attribute states of this entity.
                attributeStates.add(attributeState);
            }
        }
    }

    /**
     * Update / add attributes to entity.
     *
     * @param entity
     * @param attributes
     * @return Updated entity object.
     */
    public static Entity updateAttributesToEntity(Entity entity, Map<String, String> attributes, final String username) {
        if (attributes == null || attributes.isEmpty()) {
            // TODO : Check if concrete object attributes are deleted, or update MART with a remove attributes method.
            entity.getAttributes().clear();
            return entity;
        }
        String attrName;
        String attrValue;

        // Ensure that all attributes are in the entity AttributeState list object.
        addAllAttributes(entity);

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attrName = entry.getKey();
            attrValue = entry.getValue();
            if (!attrName.isEmpty()
                    && !attrName.equals(Constants.OCCI_CORE_ID) && !attrName.equals(Constants.OCCI_CORE_TARGET) && !attrName.equals(Constants.OCCI_CORE_SOURCE)) {
                LOGGER.debug("Attribute set value : " + attrValue);

                OcciHelper.setAttribute(entity, attrName, attrValue);

                AttributeState attrState = getAttributeStateObject(entity, attrName);
                if (attrState != null) {
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

        updateVersion(username, entity.getId());

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
            version = 1;
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
     * @param owner
     */
    public static void removeOrDissociateFromConfiguration(final String id, final String owner) {
        boolean found = false;
        boolean resourceToDelete = false;
        boolean kindEntitiesToDelete = false;
        boolean linkToDelete = false;
        boolean mixinToDissociate = false;

        Kind kind = null;
        Resource resource;
        Link link = null;
        Mixin mixin = null;

        // searching in resources.
        resource = findResource(owner, id);
        if (resource != null) {
            found = true;
            resourceToDelete = true;
        }
        if (!found) {
            link = findLink(owner, id);
            if (link != null) {
                found = true;
                linkToDelete = true;
            }
        }
        if (!found) {
            // check if this is a kind id.
            kind = KindManager.findKindFromEntities(id, owner);
            if (kind != null) {
                kindEntitiesToDelete = true;
                found = true;
            }
        }
        if (!found) {
            mixin = MixinManager.findMixinOnEntities(owner, id);
            if (mixin != null) {
                mixinToDissociate = true;
            }
        }

        if (resourceToDelete) {
            removeResource(owner, resource);
        }
        if (linkToDelete) {
            removeLink(link);
        }
        if (kindEntitiesToDelete) {
            removeEntitiesForKind(owner, kind);
        }
        if (mixinToDissociate) {
            MixinManager.dissociateMixinFromEntities(owner, mixin);
        }
    }

    /**
     * Remove a resource from owner's configuration.
     *
     * @param owner
     * @param resource
     */
    private static void removeResource(final String owner, final Resource resource) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);

        Iterator<Link> it = resource.getLinks().iterator();
        while (it.hasNext()) {
            Link link = it.next();
            Resource src = link.getSource();
            if (!src.equals(resource)) {
                src.getLinks().remove(link);
                entitiesLocation.remove(link.getId());
            }
            Resource target = link.getTarget();
            if (!target.equals(resource)) {
                target.getLinks().remove(link);
                entitiesLocation.remove(link.getId());
            }
        }

        resource.getLinks().clear();
        config.getResources().remove(resource);
        entitiesLocation.remove(resource.getId());
    }

    /**
     * Remove a link from owner's configuration.
     *
     * @param link
     */
    private static void removeLink(final Link link) {
        Resource resourceSrc = link.getSource();
        Resource resourceTarget = link.getTarget();
        resourceSrc.getLinks().remove(link);
        resourceTarget.getLinks().remove(link);
        entitiesLocation.remove(link.getId());

    }

    /**
     * Remove all entities for this kind.
     *
     * @param owner
     * @param kind
     */
    private static void removeEntitiesForKind(final String owner, final Kind kind) {
        if (kind == null) {
            return;
        }
        List<Entity> entities = findAllEntitiesForKind(owner, kind.getScheme() + kind.getTerm());

        for (Entity entity : entities) {
            if (entity instanceof Resource) {
                removeResource(owner, (Resource) entity);
            } else if (entity instanceof Link) {
                removeLink((Link) entity);
            }
        }
        entities.clear();
    }

    /**
     * Remove attributes from entity.
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
     * @param path
     * @param attr
     * @return the UUID provided may return null if uuid not found.
     */
    public static String getUUIDFromPath(final String path, final Map<String, String> attr) {
        String[] uuids = path.split("/");
        String uuidToReturn = null;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                uuidToReturn = uuid;
                break;
            }
        }
        if (uuidToReturn != null) {
            return uuidToReturn;
        }

        // Check with occi.core.id attribute.
        String occiCoreId = attr.get(Constants.OCCI_CORE_ID);
        if (occiCoreId == null) {
            return null;
        }
        occiCoreId = occiCoreId.replace(Constants.URN_UUID_PREFIX, "");
        if (!occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        return uuid;
                    }
                }
            }
        }

        return null;
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
     * Print on logger an entity.
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
        LOGGER.info(builder.toString());

    }

    /**
     * Get all entities registered on the same path.
     *
     * @param path
     * @return a List of String uuids
     */
    public static List<String> getEntityUUIDsFromPath(final String path) {
        List<String> entitiesUUID = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return entitiesUUID;
        }
        String pathCompare = path;
        if (!path.equals("/") && path.endsWith("/")) {
            // Delete ending slash.
            pathCompare = path.substring(0, path.length() - 1);
        }
        // Remove leading "/".
        if (path.startsWith("/")) {
            pathCompare = path.substring(1);
        }

        Map<String, String> entitiesPath = getEntitiesLocation();
        String uuid;
        String pathTmp;
        for (Map.Entry<String, String> entry : entitiesPath.entrySet()) {
            uuid = entry.getKey();
            pathTmp = entry.getValue();

            if (!pathTmp.equals("/") && pathTmp.endsWith("/")) {
                pathTmp = pathTmp.substring(0, pathTmp.length() - 1);
            }
            // Remove leading "/".
            if (pathTmp.startsWith("/")) {
                pathTmp = pathTmp.substring(1);
            }

            if (pathCompare.equals(pathTmp)) {
                entitiesUUID.add(uuid);
            } else if ((pathCompare.startsWith(pathTmp) || pathTmp.startsWith(pathCompare)) && !pathTmp.isEmpty()) {
                entitiesUUID.add(uuid);
            }
        }
        return entitiesUUID;
    }

    /**
     * Convert entity attributes to Map object. String name, Object value
     *
     * @param entity
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

                    EDataType eAttrType = getEAttributeType(entity, key);

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
                            Number num = ConfigurationManager.parseNumber(val, eAttrType.getInstanceClassName());
                            attributes.put(key, num);
                        } catch (NumberFormatException ex) {
                            // TODO : Check here if boolean object works.
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

        return attributes;
    }

    /**
     * @return
     */
    public static Map<String, String> getEntitiesLocation() {
        if (entitiesLocation == null) {
            entitiesLocation = new ConcurrentHashMap<>();
        }
        return entitiesLocation;
    }

    /**
     * Execute an action on an entity.
     * @param entityUUID
     * @param actionId
     * @param actionAttributes
     * @param owner
     * @throws ConfigurationException
     */
    public static void executeActionOnEntity(final String entityUUID, final String actionId, final Map<String, String> actionAttributes, final String owner) throws ConfigurationException {
        if (entityUUID == null) {
            throw new ConfigurationException("No entity defined to execute this action: " + actionId);
        }
        Entity entity = findEntity(entityUUID, owner);
        if (entity == null) {
            throw new ConfigurationException("Entity : " + entityUUID + " doesnt exist on configuration");
        }

        Extension ext = getExtensionForAction(owner, actionId);
        if (ext == null) {
            LOGGER.error("Action " + actionId + " doesnt exist on referenced extensions");
            throw new ConfigurationException("Action " + actionId + " doesnt exist on referenced extensions");
        }

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
     * @param location
     * @param actionId
     * @param actionAttributes
     * @param owner
     */
    public static void executeActionOnlocation(final String location, final String actionId, final Map<String, String> actionAttributes, final String owner) throws ConfigurationException {
        if (location == null) {
            throw new ConfigurationException("No entity location defined.");
        }
        if (actionId == null) {
            throw new ConfigurationException("No action scheme+term defined.");
        }

        Entity entity = findEntityFromLocation(location, owner);

        if (entity == null) {
            throw new ConfigurationException("Entity on location: " + location + " doesnt exist on configuration");
        }

        Extension ext = getExtensionForAction(owner, actionId);
        if (ext == null) {
            LOGGER.error("Action " + actionId + " doesnt exist on referenced extensions");
            throw new ConfigurationException("Action " + actionId + " doesnt exist on referenced extensions");
        }

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
            throw new ConfigurationException(message, ex);
        }

    }


}
