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
package org.occiware.mart.server.servlet.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Attribute;
import org.occiware.clouddesigner.occi.AttributeState;
import org.occiware.clouddesigner.occi.Category;
import org.occiware.clouddesigner.occi.Configuration;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Extension;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Link;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.clouddesigner.occi.OCCIFactory;
import org.occiware.clouddesigner.occi.OCCIRegistry;
import org.occiware.clouddesigner.occi.Resource;
import org.occiware.clouddesigner.occi.util.Occi2Ecore;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.MART;
import org.occiware.mart.server.servlet.exception.EntityAddException;
import org.occiware.mart.server.servlet.utils.CollectionFilter;
import org.occiware.mart.server.servlet.utils.Constants;
import org.occiware.mart.server.servlet.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage configurations (OCCI Model).
 *
 * @author Christophe Gourdin
 *
 */
public class ConfigurationManager {

    static {

        // Registering extension found in classpath.
        MART.initMART();

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

    private static String occiSchema = "";

    /**
     * Used for now when no owner defined
     */
    public static final String DEFAULT_OWNER = "anonymous";

    /**
     * This map reference all occi configurations by users. The first ref string
     * is the user uuid. To be updated for multiusers and multiconfigs.
     */
    protected static Map<String, Configuration> configurations = new HashMap<>();

    /**
     * References location for a user mixin. this is used by find method to find
     * the collection of user mixin. Key : Mixin sheme + term, must be unique
     * Value : Location with form of : http://localhost:8080/mymixincollection/
     */
    protected static Map<String, String> userMixinLocationMap = new HashMap<>();

    /**
     * key: uuid, value: entity relative path.
     */
    protected static Map<String, String> entitiesRelativePath = new HashMap<>();

    /**
     * Obtain the factory to create OCCI objects.
     */
    protected static OCCIFactory occiFactory = OCCIFactory.eINSTANCE;

    /**
     * Used only to create an eTag when object are updated. Key : owner+objectId
     * Value : version number. First version is 1.
     */
    private static Map<String, Integer> versionObjectMap = new HashMap<>();

    /**
     * Get a configuration from the configuration's map.
     *
     * @param owner
     * @return a configuration object for an owner.
     */
    public static Configuration getConfigurationForOwner(final String owner) {
        if (configurations.get(owner) == null) {
            createConfiguration(owner);
        }

        return configurations.get(owner);
    }

    /**
     * Create a new configuration (empty ==> without any resources and link and
     * extension) for the user.
     *
     * @param owner
     * @return a new configuration for the user.
     */
    public static Configuration createConfiguration(final String owner) {

        // Create an empty OCCI configuration.
        Configuration configuration = occiFactory.createConfiguration();

        // Update reference configuration map.
        configurations.put(owner, configuration);

        LOGGER.info("Configuration for user " + owner + " created");

        return configuration;
    }

    /**
     * Remove a configuration from the configuration's map.
     *
     * @param owner
     */
    public static void removeConfiguration(final String owner) {
        configurations.remove(owner);
    }

    /**
     * Add a new resource entity to a configuration and update the
     * configuration's map accordingly.
     *
     * @param id (entity id : "term/title")
     * @param kind (scheme#term)
     * @param mixins (ex:
     * mixins=[http://schemas.ogf.org/occi/infrastructure/network# ipnetwork])
     * @param attributes (ex: attributes={occi.network.vlan=12,
     * occi.network.label=private, occi.network.address=10.1.0.0/16,
     * occi.network.gateway=10.1.255.254})
     * @param owner
     * @param relativePath (ex: compute/myuuid
     * @throws org.occiware.mart.server.servlet.exception.EntityAddException
     */
    public static void addResourceToConfiguration(String id, String kind, List<String> mixins,
            Map<String, String> attributes, String owner, String relativePath) throws EntityAddException {

        if (owner == null || owner.isEmpty()) {
            // Assume if owner is not used to a default user uuid "anonymous".
            owner = DEFAULT_OWNER;
        }

        Configuration configuration = getConfigurationForOwner(owner);

        // Assign a new resource to configuration, if configuration has resource
        // existed, inform by logger but overwrite existing one.
        boolean resourceOverwrite;
        Resource resource = findResource(owner, id);
        if (resource == null) {
            resourceOverwrite = false;

            Kind occiKind;

            // Check if kind already exist in realm (on extension model).
            occiKind = findKindFromExtension(owner, kind);

            if (occiKind == null) {
                // Kind not found on extension, searching on entities.
                occiKind = findKindFromEntities(owner, kind);
            }
            try {
                // Create an OCCI resource with good resource type (via extension model).
                resource = (Resource) OcciHelper.createEntity(occiKind);

                resource.setId(id);

                // Add a new kind to resource (title, scheme, term).
                // if occiKind is null, this will give a default kind parent.
                resource.setKind(occiKind);

                addMixinsToEntity(resource, mixins, owner, false);

                // Add the attributes...
                updateAttributesToEntity(resource, attributes);

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + id);

                throw new EntityAddException("Exception thrown while creating an entity: " + id + " Message: " + ex.getMessage(), ex);
            }
        } else {
            LOGGER.info("resource already exist, overwriting...");
            resourceOverwrite = true;

            // Add the mixins if any.
            addMixinsToEntity(resource, mixins, owner, true);

            updateAttributesToEntity(resource, attributes);

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
        entitiesRelativePath.put(id, relativePath);

    }

    /**
     * Add a new link entity to a configuration and update the configuration's
     * map accordingly.
     *
     * @param id
     * @param kind
     * @param mixins
     * @param src
     * @param target
     * @param attributes
     * @param owner
     * @param relativePath
     * @throws org.occiware.mart.server.servlet.exception.EntityAddException
     */
    public static void addLinkToConfiguration(String id, String kind, java.util.List<String> mixins, String src,
            String target, Map<String, String> attributes, String owner, String relativePath) throws EntityAddException {

        if (owner == null || owner.isEmpty()) {
            // Assume if owner is not used to a default user uuid "anonymous".
            owner = DEFAULT_OWNER;
        }

        boolean overwrite = false;
        Resource resourceSrc = findResource(owner, src);
        Resource resourceDest = findResource(owner, target);

        if (resourceSrc == null) {
            throw new EntityAddException("Cannot find the source of the link: " + id);
        }
        if (resourceDest == null) {
            throw new EntityAddException("Cannot find the target of the link: " + id);
        }

        Link link = findLink(owner, id);
        if (link == null) {

            Kind occiKind;
            // Check if kind already exist in realm (on extension model).
            occiKind = findKindFromExtension(owner, kind);

            if (occiKind == null) {
                // Kind not found on extension, searching on entities.
                occiKind = findKindFromEntities(owner, kind);
            }
            try {
                // Link doesnt exist on configuration, we create it.
                link = (Link) OcciHelper.createEntity(occiKind);
                link.setId(id);

                // Add a new kind to resource (title, scheme, term).
                link.setKind(occiKind);

                addMixinsToEntity(link, mixins, owner, false);

                updateAttributesToEntity(link, attributes);

            } catch (Throwable ex) {
                LOGGER.error("Exception thrown while creating an entity. " + id);
                throw new EntityAddException("Exception thrown while creating an entity: " + id + " Message: " + ex.getMessage(), ex);
            }
        } else {
            // Link exist upon our configuration, we update it.

            overwrite = true;

            addMixinsToEntity(link, mixins, owner, true);

            updateAttributesToEntity(link, attributes);
        }

        link.setSource(resourceSrc);
        link.setTarget(resourceDest);

        // Assign link to resource source.
        resourceSrc.getLinks().add(link);

        updateVersion(owner, id);

        if (overwrite) {
            LOGGER.info("Link " + id + " updated ! Version: " + versionObjectMap.get(owner + id));
        } else {
            LOGGER.info("link " + id + " added to configuration !");
        }
        entitiesRelativePath.put(id, relativePath);

    }

    /**
     * Get all the attributes of an Entity instance.
     *
     * @param entity the given Entity instance.
     * @return all the attributes of the given instance.
     */
    public static Collection<Attribute> getAllAttributes(final Entity entity) {
        List<Attribute> attributes = new ArrayList<>();
        Kind entityKind = entity.getKind();
        if (entityKind != null) {
            addAllAttributes(attributes, entityKind);
        }
        for (Mixin mixin : entity.getMixins()) {
            addAllAttributes(attributes, mixin);
        }
        return attributes;
    }

    /**
     * Add all the attributes of a given Kind instance and all its parent kinds.
     *
     * @param attributes the collection where attributes will be added.
     * @param kind the given Kind instance.
     */
    public static void addAllAttributes(final Collection<Attribute> attributes, final Kind kind) {
        Kind kindParent = kind.getParent();
        if (kindParent != null) {
            addAllAttributes(attributes, kindParent);
        }
        attributes.addAll(kind.getAttributes());
    }

    /**
     * Add all the attributes of a given Mixin instance and all its depend
     * mixins.
     *
     * @param attributes the collection where attributes will be added.
     * @param mixin the given Mixin instance.
     */
    public static void addAllAttributes(final Collection<Attribute> attributes, final Mixin mixin) {
        boolean found;
        for (Mixin md : mixin.getDepends()) {
            addAllAttributes(attributes, md);
        }
        // Compare with attributes on entity.
        List<Attribute> mixinAttrs = mixin.getAttributes();
        if (!attributes.isEmpty()) {

            for (Attribute attrMixin : mixinAttrs) {
                found = false;
                // Check if we can add it. avoid doublon. cf CRTP : mixin medium :> resource_tpl. for example.
                for (Attribute attrKind : attributes) {
                    if (attrKind.getName().equals(attrMixin.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Add the mixin attribute.
                    attributes.add(attrMixin);
                }
            }
        } else {
            attributes.addAll(mixin.getAttributes());
        }

    }

    /**
     * Update / add attributes to entity.
     *
     * @param entity
     * @param attributes
     * @return Updated entity object.
     */
    public static Entity updateAttributesToEntity(Entity entity, Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            // TODO : Check if concrete object attributes are deleted, or update MART with a remove attributes method.
            entity.getAttributes().clear();
            return entity;
        }
        String attrName;
        String attrValue;

        // Ensure that all attributes are in the entity AttributeState list object.
        addAllAttributes(entity);

        // Collection<Attribute> occiAttrs = getAllAttributes(entity);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attrName = entry.getKey();
            attrValue = entry.getValue();
            if (!attrName.isEmpty()
                    && !attrName.equals("occi.core.id") && !attrName.equals("occi.core.target") && !attrName.equals("occi.core.source")) {
                LOGGER.info("Attribute set value : " + attrValue);

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

                    LOGGER.info("Attribute : " + attrState.getName() + " --> " + attrState.getValue() + " ==> OK");
                }
            }
        }

        // Check attributes.
        LOGGER.info("Entity :--> ");
        Utils.printEntity(entity);

        return entity;
    }

    /**
     * Add all attributes not already present.
     *
     * @param entity
     */
    public static void addAllAttributes(Entity entity) {
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
     * Set an attribute of an OCCI entity, patched from OcciHelper.
     *
     * @param entity the given entity.
     * @param attributeName the attribute name.
     * @param attributeValue the attribute value.
     * @throws java.lang.IllegalArgumentException Thrown when the attribute name
     * is unknown or the attribute value is invalid.
     */
    private static void setAttributeFromOcciHelper(Entity entity, String attributeName, String attributeValue) {
        // Check that attribute name exists from this entity.
        OcciHelper.getAttribute(entity, attributeName);

        // Search the Ecore structural feature associated to the OCCI attribute.
        String eAttributeName = Occi2Ecore.convertOcciAttributeName2EcoreAttributeName(attributeName);

        final EStructuralFeature eStructuralFeature = entity.eClass().getEStructuralFeature(eAttributeName);

        if (eStructuralFeature == null) {
            // Create the attribute state and update it, if none, create it.
            AttributeState attrState = getAttributeStateObject(entity, attributeName);
            if (attrState == null) {
                // Create the attribute.
                attrState = createAttributeState(attributeName, attributeValue);
                entity.getAttributes().add(attrState);
            }

            return;
            // throw new IllegalArgumentException("Ecore structural feature '" + eAttributeName + "' not found!");
        }
        if (!(eStructuralFeature instanceof EAttribute)) {
            throw new IllegalArgumentException("Ecore structural feature '" + eAttributeName + "' is not an Ecore attribute!");
        }

        // Obtain the attribute type.
        EDataType eAttributeType = ((EAttribute) eStructuralFeature).getEAttributeType();

        // Convert the attribute value according to the attribute type.
        Object eAttributeValue = eAttributeType.getEPackage().getEFactoryInstance().createFromString(eAttributeType, attributeValue);

        // Set the Ecore attribute.
        entity.eSet(eStructuralFeature, eAttributeValue);
    }

    /**
     * Remove an entity (resource or link) from the configuration on overall
     * owners.
     *
     * @param id if full url: category id (bounded collection) if path relative
     * url part: unbounded collection or entity
     */
    public static void removeOrDissociate(final String id) {
        // get the owners
        Set<String> owners = configurations.keySet();
        for (String owner : owners) {
            removeOrDissociateFromConfiguration(owner, id);
        }
    }

    /**
     * Remove an entity (resource or link) from the owner's configuration or
     * delete all entities from given kind id or disassociate entities from
     * given mixin id.
     *
     * @param owner
     * @param id (kind id or mixin id or entity Id!)
     */
    public static void removeOrDissociateFromConfiguration(final String owner, final String id) {
        // Find if this is an entity id or a mixin id or a kind id.
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
            kind = findKindFromEntities(owner, id);
            if (kind != null) {
                kindEntitiesToDelete = true;
                found = true;
            }
        }
        if (!found) {
            mixin = findMixinOnEntities(owner, id);
            if (mixin != null) {
                mixinToDissociate = true;
            }
        }

        if (resourceToDelete) {
            removeResource(owner, resource);
        }
        if (linkToDelete) {
            removeLink(owner, link);
        }
        if (kindEntitiesToDelete) {
            removeEntitiesForKind(owner, kind);
        }
        if (mixinToDissociate) {
            dissociateMixinFromEntities(owner, mixin);
        }

    }

    /**
     * Remove a resource from owner's configuration.
     *
     * @param owner
     * @param resource
     */
    public static void removeResource(final String owner, final Resource resource) {
        Configuration config = getConfigurationForOwner(owner);
        EList<Link> resLink = resource.getLinks();
        if (resLink != null) {

            for (Link link : resLink) {
                removeLink(owner, link);
            }
        }
        resource.getLinks().clear(); // Remove all links on that resource.
        config.getResources().remove(resource);
        entitiesRelativePath.remove(resource.getId());
    }

    /**
     * Remove a link from owner's configuration.
     *
     * @param owner
     * @param link
     */
    public static void removeLink(final String owner, final Link link) {
        Resource resourceSrc = link.getSource();
        Resource resourceTarget = link.getTarget();
        resourceSrc.getLinks().remove(link);
        resourceTarget.getLinks().remove(link);
        entitiesRelativePath.remove(link.getId());

    }

    /**
     * Remove all entities for this kind.
     *
     * @param owner
     * @param kind
     */
    public static void removeEntitiesForKind(final String owner, final Kind kind) {
        // Configuration config = configurations.get(owner);
        if (kind == null) {
            return;
        }
        List<Entity> entities = findAllEntitiesForKind(owner, kind.getScheme() + kind.getTerm());

        for (Entity entity : entities) {
            if (entity instanceof Resource) {
                removeResource(owner, (Resource) entity);
            } else if (entity instanceof Link) {
                removeLink(owner, (Link) entity);
            }
        }
        entities.clear();
    }

    /**
     * Dissociate entities from this mixin.
     *
     * @param owner
     * @param mixin
     */
    public static void dissociateMixinFromEntities(final String owner, final Mixin mixin) {
        if (mixin == null) {
            return;
        }
        List<Entity> entities = findAllEntitiesForMixin(owner, mixin.getScheme() + mixin.getTerm());
        for (Entity entity : entities) {
            entity.getMixins().remove(mixin);
            updateVersion(owner, entity.getId());
        }
        entities.clear();

    }

    /**
     * Dissociate a mixin from an entity.
     *
     * @param owner
     * @param mixinId
     * @param entity
     * @return
     */
    public static boolean dissociateMixinFromEntity(final String owner, final String mixinId, Entity entity) {
        boolean result = false;
        if (mixinId == null) {
            return result;
        }
        // Load the mixin object.
        List<Mixin> mixins = entity.getMixins();
        if (mixins.isEmpty()) {
            result = true;
            return result;
        }
        Mixin myMixin = null;
        for (Mixin mixin : mixins) {
            if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                myMixin = mixin;
                break;
            }
        }
        // Remove the mixin.
        if (myMixin != null) {
            entity.getMixins().remove(myMixin);
            updateVersion(owner, entity.getId());
            result = true;
        }
        return result;
    }

    /**
     * Find a resource for owner and entity Id.
     *
     * @param owner
     * @param id (may be an uuid, a path/uuid or a path.)
     * @return an OCCI resource.
     */
    public static Resource findResource(final String owner, final String id) {
        Resource resFound = null;
        Configuration configuration = getConfigurationForOwner(owner);

        boolean isEntityUUID = Utils.isEntityUUIDProvided(id, new HashMap<>());
        String entityUUID;
        if (isEntityUUID) {
            entityUUID = Utils.getUUIDFromPath(id, new HashMap<>());

            for (Resource resource : configuration.getResources()) {
                if (resource.getId().equals(entityUUID)) {
                    resFound = resource;
                    // Resource found.
                    break;
                }
            }
        }
        // Case: resource not found but a path is given (without uuid).
        if (resFound == null && !isEntityUUID) {
            // The id hasn't an uuid. Search on map if a single entity is on the path.
            List<String> uuids = Utils.getEntityUUIDsFromPath(id);
            if (uuids.size() == 1) {
                for (String uuidTmp : uuids) {
                    // Load the resource..
                    for (Resource resource : configuration.getResources()) {
                        if (resource.getId().equals(uuidTmp)) {
                            resFound = resource;
                            // Resource found.
                            break;
                        }

                    }
                    break;

                }
            }
            // if multiple entities, return null.

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
    public static Link findLink(final String owner, final String id) {
        Configuration configuration = getConfigurationForOwner(owner);
        String entityUUID;
        boolean isEntityUUID = Utils.isEntityUUIDProvided(id, new HashMap<>());
        
        
        Link link = null;
        EList<Link> links;
        for (Resource resource : configuration.getResources()) {
            entityUUID = Utils.getUUIDFromPath(id, new HashMap<>());
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

        // Path only case. but compare with real uuid, if uuids number > 1 return null.
        if (link == null && !isEntityUUID) {
            // The id hasn't an uuid. Search on map if a single entity is on the path.
            List<String> uuids = Utils.getEntityUUIDsFromPath(id);
            if (uuids.size() == 1) {
                for (String uuidTmp : uuids) {
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
                    break;
                }
            }
            // if multiple entities, return null.

        }

        return link;
    }

    /**
     * Search an entity (link or resource) on the current configuration.
     *
     * @param owner
     * @param id (entityId is unique for all owners)
     * @return an OCCI Entity, could be null, if entity has is not found.
     */
    public static Entity findEntity(String owner, final String id) {
        Entity entity = null;

        if (owner == null) {
            entity = findEntityAndGetOwner(owner, id);
            return entity;
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
     *
     * @param owner
     * @param id
     * @return true if entity exist or false if it doesnt exist.
     */
    public static boolean isEntityExist(final String owner, final String id) {
        if (Utils.isEntityUUIDProvided(id, new HashMap<>())) {
            return findEntity(owner, id) != null;
        } else {
            // UUID not provided, search on relative path and check if one entity is found.
            String path;
            boolean found = false;
            for (Map.Entry<String, String> entry : entitiesRelativePath.entrySet()) {
                path = entry.getValue();
                if (path.equals(id)) {
                    // There is an entity on this relative path.
                    found = true;
                    break;
                }

            }
            return found;
        }

    }

    /**
     * Find entity on all owner,
     *
     * @param owner (value on upper)
     * @param entityId (unique on all the map)
     * @return
     */
    public static Entity findEntityAndGetOwner(String owner, final String entityId) {
        Entity entity;
        Map<String, Entity> ents = findEntitiesOnAllOwner(entityId);
        Set<String> owners = ents.keySet();
        if (owners.size() != 1) {
            return null;
        }

        for (String own : owners) {
            owner = own;
        }
        entity = ents.get(owner);

        return entity;
    }

    /**
     * Search the first entity found on all owner's configurations.
     *
     * @param ownerFound
     * @param id
     * @return
     */
    public static Entity findEntityOnAllOwner(String ownerFound, final String id) {
        if (configurations.isEmpty()) {
            return null;
        }

        Set<String> owners = configurations.keySet();
        Entity entity = null;
        for (String owner : owners) {
            entity = findEntity(owner, id);
            ownerFound = owner;
            if (entity != null) {
                break;
            }
        }
        if (ownerFound == null) {
            ownerFound = DEFAULT_OWNER;
        }
        return entity;
    }

    /**
     * Search for a kind.
     *
     * @param owner
     * @param id
     * @return
     */
    public static Kind findKindFromEntities(final String owner, final String id) {
        Configuration configuration = getConfigurationForOwner(owner);
        Kind kind = null;
        EList<Link> links;

        EList<Resource> resources = configuration.getResources();
        for (Resource resource : resources) {
            if ((resource.getKind().getScheme() + resource.getKind().getTerm()).equals(id)) {
                kind = resource.getKind();
            } else {
                // On check les links de la resource.
                links = resource.getLinks();
                for (Link link : links) {
                    if ((link.getKind().getScheme() + link.getKind().getTerm()).equals(id)) {
                        kind = link.getKind();
                        break;
                    }
                }
            }
            if (kind != null) {
                break;
            }

        }

        return kind;

    }

    /**
     * Search for a kind from referenced extension model.
     *
     * @param owner
     * @param kindId
     * @return
     */
    public static Kind findKindFromExtension(final String owner, final String kindId) {
        Configuration config = getConfigurationForOwner(owner);
        Kind kindToReturn = null;
        EList<Kind> kinds;
        List<String> extUsed = new ArrayList<>();
        EList<Extension> exts = config.getUse();
        for (Extension ext : exts) {
            extUsed.add(ext.getScheme());
            kinds = ext.getKinds();
            for (Kind kind : kinds) {
                if (((kind.getScheme() + kind.getTerm()).equals(kindId))) {
                    kindToReturn = kind;
                    break;
                }
            }

            if (kindToReturn != null) {
                break;
            }

        }

        if (kindToReturn == null) {

            // Search kind in unreferenced extensions, if found reference the
            // new extension to this configuration.
            Collection<String> extReg = OCCIRegistry.getInstance().getRegisteredExtensions();

            extReg.removeAll(extUsed);
            Extension ext;
            for (String extScheme : extReg) {
                // ext = OcciHelper.loadExtension(OCCIRegistry.getInstance().getExtensionURI(extScheme));
                ext = OcciHelper.loadExtension(extScheme);
                kinds = ext.getKinds();
                for (Kind kind : kinds) {
                    if (((kind.getScheme() + kind.getTerm()).equals(kindId))) {
                        kindToReturn = kind;
                        config.getUse().add(ext);
                        LOGGER.info("New extension: " + ext.getName() + " --< added to configuration owner: " + owner);
                        break;
                    }
                }
                if (kindToReturn != null) {
                    break;
                }
            }
        }

        return kindToReturn;

    }

    /**
     * Find entities for a categoryId (kind or Mixin or actions). actions has no
     * entity list and it's not used here.
     *
     * @param categoryId
     * @return an hmap (key: owner, value : List of entities).
     */
    public static Map<String, List<Entity>> findAllEntitiesForCategoryId(final String categoryId) {
        Map<String, List<Entity>> entitiesMap = new HashMap<>();
        if (configurations.isEmpty()) {
            return entitiesMap;
        }
        Set<String> owners = configurations.keySet();
        List<Entity> entities = new ArrayList<>();

        for (String owner : owners) {
            entities.clear();

            entities.addAll(findAllEntitiesForKind(owner, categoryId));
            entities.addAll(findAllEntitiesForMixin(owner, categoryId));
            entities.addAll(findAllEntitiesForAction(owner, categoryId));

            if (!entities.isEmpty()) {
                entitiesMap.put(owner, entities);
            } else {
                entities = new ArrayList<>();
                entitiesMap.put(owner, entities);
            }

        }
        return entitiesMap;
    }

    /**
     * Find entities for a categoryId (kind or Mixin or actions). actions has no
     * entity list and it's not used here.
     *
     * @param owner
     * @param startIndex
     * @param number (number max of entities to return).
     * @param filters (List of entity attribute to filter).
     * @return a list of entities (key: owner, value : List of entities).
     */
    public static List<Entity> findAllEntities(final String owner, final int startIndex, final int number, final List<CollectionFilter> filters) {
        List<Entity> entities = new LinkedList<>();

        if (configurations.isEmpty() || owner == null || owner.isEmpty()) {
            return entities;
        }

        String categoryId = null;
        for (CollectionFilter filter : filters) {
            if (filter.getCategoryFilter() != null) {
                categoryId = filter.getCategoryFilter();
            }

        }

        // Load all entities and after filter them.
        entities.addAll(findAllEntitiesOwner(owner));

//        entities.addAll(findAllEntitiesForKind(owner, categoryId));
//        entities.addAll(findAllEntitiesForMixin(owner, categoryId));
//        entities.addAll(findAllEntitiesForAction(owner, categoryId));
        // TODO : Order list by entityId, if entities not empty.
        entities = filterEntities(startIndex, number, filters, entities, owner);

        return entities;
    }

    /**
     * Get all used collection types (like : collections/compute)
     *
     * @return
     */
    public static List<String> getUsedCollectionTypes() {
        List<String> collectionTypes = new ArrayList<>();
        Map<String, List<Entity>> entitiesMap = getAllEntities();
        String collection = "collections/";
        List<Entity> entities;
        String result;
        for (Map.Entry<String, List<Entity>> entry : entitiesMap.entrySet()) {
            entities = entry.getValue();
            for (Entity entity : entities) {
                String kindTerm = entity.getKind().getTerm();
                result = collection + kindTerm;
                if (!collectionTypes.contains(result)) {
                    collectionTypes.add(collection + kindTerm);
                }
            }
        }

        return collectionTypes;
    }

    /**
     * Get all used kinds (scheme + term) like
     * http://schemas.ogf.org/occi/infrastructure#network.
     *
     * @return
     */
    public static List<String> getAllUsedKind() {
        List<String> usedKinds = new ArrayList<>();
        Map<String, List<Entity>> entitiesMap = getAllEntities();
        List<Entity> entities;
        String result;

        for (Map.Entry<String, List<Entity>> entry : entitiesMap.entrySet()) {
            entities = entry.getValue();
            for (Entity entity : entities) {
                String kindTerm = entity.getKind().getTerm();
                String scheme = entity.getKind().getScheme();

                result = scheme + kindTerm;
                if (!usedKinds.contains(result)) {
                    usedKinds.add(result);
                }
            }

        }
        String mixinId;
        // Get all location of a user mixins.
        for (Map.Entry<String, String> entry : userMixinLocationMap.entrySet()) {
            mixinId = entry.getKey(); // Scheme + term.
            usedKinds.add(mixinId);
        }

        return usedKinds;
    }

    /**
     * Find all user mixins kind that have this location.
     *
     * @param owner
     * @param location (http://localhost:8080/mymixincollection/
     * @return a list of user mixins, empty if none found.
     */
    public static List<String> findAllUserMixinKindByLocation(final String owner, final String location) {

        List<String> mixinKinds;
        // Recherche sur tous les users mixin kinds.
        Configuration config;
        EList<Mixin> mixins;
        String mixinId;
        String locationTmp;
        mixinKinds = new ArrayList<>();
        config = getConfigurationForOwner(owner);
        mixins = config.getMixins();
        for (Mixin mixin : mixins) {
            mixinId = mixin.getScheme() + mixin.getTerm();
            locationTmp = userMixinLocationMap.get(mixinId);
            if (locationTmp != null && locationTmp.contains(location)) {
                // Location found for this mixin.
                mixinKinds.add(mixinId);
            }
        }
        return mixinKinds;
    }

    /**
     * Get all the entities for all owner.
     *
     * @return an hmap (key: owner, value : List of entities.
     */
    public static Map<String, List<Entity>> getAllEntities() {
        Map<String, List<Entity>> entitiesMap = new HashMap<>();
        Set<String> owners = configurations.keySet();
        if (configurations.isEmpty()) {
            return entitiesMap;
        }
        List<Entity> entities = new ArrayList<>();
        for (String owner : owners) {
            entities.clear();
            entities.addAll(findAllEntitiesOwner(owner));

            if (!entities.isEmpty()) {
                entitiesMap.put(owner, entities);
            } else {
                entities = new ArrayList<>();
                entitiesMap.put(owner, entities);
            }
        }
        return entitiesMap;
    }

    /**
     * Find all entities referenced for an owner.
     *
     * @param owner
     * @return
     */
    public static List<Entity> findAllEntitiesOwner(final String owner) {
        List<Entity> entities = new ArrayList<>();
        Configuration configuration = getConfigurationForOwner(owner);
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
     * Search for an action with entityId and a full category scheme.
     *
     * @param relativePath (like "compute/vm1")
     * @param actionId (like
     * "http://schemas.ogf.org/occi/infrastructure/compute/action#start")
     * @return an entity map (key=owner, value=Entity) with this relative path
     * and has this actionId, may return empty map if action not found on entity
     * object, or if entity not found.
     */
    public static Map<String, Entity> findEntityAction(final String relativePath, final String actionId) {

        Map<String, Entity> entities = findEntitiesOnAllOwner(relativePath);

        Map<String, Entity> entitiesFound = new HashMap<>();
        if (entities.isEmpty()) {
            return entities;
        }

        Map<String, List<Entity>> entitiesAction = findAllEntitiesForCategoryId(actionId);

        List<Entity> entitiesToCompare;
        // Search relative path entity.
        String owner;
        for (Map.Entry<String, List<Entity>> entry : entitiesAction.entrySet()) {
            owner = entry.getKey();
            entitiesToCompare = entry.getValue();
            for (Entity ent : entitiesToCompare) {
                if (entities.get(owner) != null && entities.get(owner).equals(ent)) {
                    // Entity is found for this action.
                    entitiesFound.put(owner, ent);
                }
            }
        }
        return entitiesFound;

    }

    /**
     * Find a list of entity with his relative path.
     *
     * @param entityId
     * @return entities
     */
    public static Map<String, Entity> findEntitiesOnAllOwner(final String entityId) {
        Entity entity;
        Map<String, Entity> entitiesMap = new HashMap<>();
        for (String owner : configurations.keySet()) {
            entity = findEntity(owner, entityId);
            if (entity != null) {
                entitiesMap.put(owner, entity);
            }
        }
        return entitiesMap;
    }

    /**
     * Find all entities with that kind. (replace getEntities from kind object).
     *
     * @param owner
     * @param categoryId
     * @return
     */
    public static List<Entity> findAllEntitiesForKind(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        for (Resource res : getConfigurationForOwner(owner).getResources()) {
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
     * Find all entities for a mixin (replace getEntities() method from Mixin
     * object).
     *
     * @param owner
     * @param categoryId
     * @return
     */
    public static List<Entity> findAllEntitiesForMixin(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        for (Resource res : getConfigurationForOwner(owner).getResources()) {
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
     * Find all entities for an action (replace getEntities() method from Mixin
     * object).
     *
     * @param owner
     * @param categoryId (id of kind, mixin or action, composed by scheme+term.
     * @return
     */
    public static List<Entity> findAllEntitiesForAction(final String owner, final String categoryId) {
        List<Entity> entities = new ArrayList<>();
        for (Resource res : getConfigurationForOwner(owner).getResources()) {

            for (Action act : res.getKind().getActions()) {
                if ((act.getScheme() + act.getTerm()).equals(categoryId)) {
                    entities.add(res);
                }

            }
            // Search in mixins.
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
     * Return all entity based on a partial Id (like request).
     *
     * @param owner
     * @param partialId
     * @return
     */
    public static List<Entity> findAllEntitiesLikePartialId(final String owner, final String partialId) {
        List<Entity> entities = new ArrayList<>();

        Configuration configuration = getConfigurationForOwner(owner);

        if (partialId == null) {
            return entities;
        }

        List<Resource> resources = configuration.getResources();
        for (Resource resource : resources) {
            if (resource.getId().contains(partialId)) {
                entities.add(resource);
            }
            if (!resource.getLinks().isEmpty()) {
                for (Link link : resource.getLinks()) {
                    if (link.getId().contains(partialId)) {
                        entities.add(link);
                    }
                }
            }
        }

        return entities;
    }

    /**
     * Destroy all configurations for all owners.
     */
    public static void resetAll() {
        configurations.clear();
        versionObjectMap.clear();
    }

    /**
     * Add mixins to an existing entity (resources or links). Ex of mixin string
     * format : http://schemas.ogf.org/occi/infrastructure/network#ipnetwork
     *
     * @param entity (OCCI Entity).
     * @param mixins (List of mixins).
     * @param owner
     * @param updateMode (if updateMode is true, reset existing and replace with
     * new ones)
     * @return
     */
    public static boolean addMixinsToEntity(Entity entity, final List<String> mixins, final String owner, final boolean updateMode) {
        boolean result = false;
        if (updateMode) {
            entity.getMixins().clear();
        }
        if (mixins != null && !mixins.isEmpty()) {

            for (String mixinStr : mixins) {
                // Check if this mixin exist in realm extensions.
                Mixin mixin = findMixinOnExtension(owner, mixinStr);

                if (mixin == null) {
                    LOGGER.info("Mixin not found on extensions, searching on referenced entities: --> Term : " + mixinStr);
                    // Search the mixin on entities.
                    mixin = findMixinOnEntities(owner, mixinStr);

                    if (mixin == null) {
                        LOGGER.info("Create the mixin : --> Term : " + mixinStr);
                        mixin = createMixin(mixinStr);
                    }

                } else {
                    LOGGER.info("Mixin found on used extensions : --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
                }

                LOGGER.info("Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
                LOGGER.info("Mixin attributes : ");

                Collection<Attribute> attrs = mixin.getAttributes();
                if (attrs != null && !attrs.isEmpty()) {
                    LOGGER.info("Attributes found for mixin : " + "Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
                    for (Attribute attr : attrs) {
                        LOGGER.info("Attribute : " + attr.getName() + " --> " + attr.getDescription());
                    }
                } else {
                    LOGGER.warn("No attributes found for mixin : " + "Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
                }

                entity.getMixins().add(mixin);
                result = true;
            }
        }
        return result;
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
     * Search mixin on owner's configuration.
     *
     * @param owner
     * @param mixinId
     * @return a mixin found or null if not found
     */
    public static Mixin findMixinOnEntities(final String owner, final String mixinId) {
        Configuration configuration = getConfigurationForOwner(owner);
        Mixin mixinToReturn = null;
        boolean mixinOk;

        for (Resource res : configuration.getResources()) {
            mixinOk = false;
            for (Mixin mixin : res.getMixins()) {
                if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                    mixinToReturn = mixin;
                    mixinOk = true;
                    break;
                }
            }

            if (mixinOk) {
                break;
            } else {
                // Recherche dans les links.
                for (Link link : res.getLinks()) {
                    for (Mixin mixin : link.getMixins()) {
                        if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                            mixinToReturn = mixin;
                            mixinOk = true;
                            break;
                        }
                    }
                    if (mixinOk) {
                        break;
                    }
                }
            }
            if (mixinOk) {
                break;
            }

        }

        return mixinToReturn;
    }

    /**
     * Find a mixin on loaded extension on configuration.
     *
     * @param owner
     * @param mixinId
     * @return
     */
    public static Mixin findMixinOnExtension(final String owner, final String mixinId) {
        Configuration config = getConfigurationForOwner(owner);
        Mixin mixinToReturn = null;
        for (Extension ext : config.getUse()) {
            for (Mixin mixin : ext.getMixins()) {
                if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                    mixinToReturn = mixin;
                    break;
                }

            }
            if (mixinToReturn != null) {
                break;
            }
        }

        return mixinToReturn;
    }

    /**
     * Associate a list of entities with a mixin, replacing existing list if
     * any. if mixin doest exist, this will create it.
     *
     * @param mixinId
     * @param entityIds
     * @param updateMode
     */
    public static void saveMixinForEntities(final String mixinId, final List<String> entityIds,
            final boolean updateMode) {
        // TODO : Pass owner on coreImpl saveMixin and UpdateMixin method.
        for (String owner : configurations.keySet()) {
            saveMixinForEntities(owner, mixinId, entityIds, updateMode);
        }

    }

    /**
     * Associate a list of entities with a mixin, replacing existing list if
     * any. if mixin doest exist, this will create it.
     *
     * @param owner
     * @param mixinId
     * @param entityIds
     * @param updateMode
     */
    public static void saveMixinForEntities(final String owner, final String mixinId, final List<String> entityIds,
            final boolean updateMode) {

        // searching for the mixin to register.
        Mixin mixin = findMixinOnExtension(owner, mixinId);

        if (mixin == null) {
            mixin = findMixinOnEntities(owner, mixinId);

            if (mixin == null) {
                // TODO : Check this : User mixin tag ?
                mixin = createMixin(mixinId);
            }
        }
        LOGGER.info("Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
        List<Entity> entities = new ArrayList<>();
        for (String entityId : entityIds) {
            Entity entity = findEntity(owner, entityId);

            if (entity != null && !entity.getMixins().contains(mixin)) {
                entity.getMixins().add(mixin);
                updateVersion(owner, entityId);
            }
            if (entity != null) {
                entities.add(entity);
            }
        }

        if (!updateMode) {
            boolean found;
            // Remove entities those are not in the list.
            Iterator<Entity> it = entities.iterator();
            while (it.hasNext()) {
                found = false;
                Entity entityMixin = it.next();
                for (String entityId : entityIds) {
                    if (entityMixin.getId().equals(entityId)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // Remove reference mixin of the entity.
                    entityMixin.getMixins().remove(mixin);

                    // Remove the entity from mixin.
                    // it.remove();
                }

            }
        }

    }

    /**
     * Add a user mixin to configuration's Object (user tag).
     *
     * @param id
     * @param location
     * @param owner
     */
    public static void addUserMixinOnConfiguration(final String id, final String location, final String owner) {
        if (owner == null || id == null || location == null) {
            return;
        }

        Configuration configuration = getConfigurationForOwner(owner);
        Mixin mixin = createMixin(id);

        // We add the mixin location to the userMixin map.
        userMixinLocationMap.put(id, location);

        configuration.getMixins().add(mixin);

    }

    /**
     * Search for a user mixin tag on all configurations.
     *
     * @param mixinId (scheme + term)
     * @return null if not found on configurations.
     */
    public static Mixin findUserMixinOnConfigurations(final String mixinId) {
        Mixin mixinToReturn = null;
        Set<String> owners = configurations.keySet();
        Configuration config;
        EList<Mixin> mixins;
        for (String owner : owners) {
            config = getConfigurationForOwner(owner);
            mixins = config.getMixins();
            for (Mixin mixin : mixins) {
                if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                    mixinToReturn = mixin;
                    break;
                }
            }

            if (mixinToReturn != null) {
                break;
            }
        }

        return mixinToReturn;
    }

    /**
     * Delete a user mixin from configuration's Object (user tag).
     *
     * @param mixinId
     */
    public static void removeUserMixinFromConfiguration(final String mixinId) {
        if (mixinId == null) {
            return;
        }

        // Search for userMixin.
        Mixin mixin = findUserMixinOnConfigurations(mixinId);

        if (mixin == null) {
            // TODO : Throw an exception mixinNotFound.
            LOGGER.info("mixin not found on configurations.");
            return;
        }

        // We remove the mixin location from the userMixin map.
        userMixinLocationMap.remove(mixinId);

        // Delete from configuration.
        Set<String> owners = configurations.keySet();
        Configuration config;
        for (String owner : owners) {
            config = getConfigurationForOwner(owner);
            config.getMixins().remove(mixin);
        }

    }

    /**
     * Create a new mixin without any association.
     *
     * @param mixinId
     * @return
     */
    public static Mixin createMixin(final String mixinId) {
        Mixin mixin = occiFactory.createMixin();
        if (mixinId != null) {
            // Create mixin.
            String scheme;
            String term;
            // String title;
            // mixin = occiFactory.createMixin();
            term = mixinId.split("#")[1];
            scheme = mixinId.split("#")[0] + "#";
            // TODO : How to find the title in this string ?
            // mixin.setTitle(title);
            mixin.setTerm(term);
            mixin.setScheme(scheme);
        }

        return mixin;
    }

    /**
     * Find a used extension for an action Kind.
     *
     * @param owner (owner of the configuration).
     * @param action_id (kind : scheme+term)
     * @return extension found, may return null if no extension found with this
     * configuration.
     */
    public static Extension getExtensionForAction(String owner, String action_id) {
        Configuration config = getConfigurationForOwner(owner);
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
     *
     * @param ext
     * @param actionId (action scheme+term)
     * @return Action, may return null if not found on extension.
     */
    public static Action getActionKindFromExtension(final Extension ext, final String actionId) {
        EList<Kind> kinds = ext.getKinds();
        EList<Action> actionKinds;
        Action actionKind = null;
        for (Kind kind : kinds) {
            actionKinds = kind.getActions();
            for (Action action : actionKinds) {
                if ((action.getScheme() + action.getTerm()).equals(actionId)) {
                    actionKind = action;
                    break;
                }
            }
            if (actionKind != null) {
                break;
            }
        }

        return actionKind;
    }

    /**
     * Get used extension with this kind.
     *
     * @param owner owner of the configuration
     * @param kind (represent a Kind Scheme+term)
     * @return
     */
    public static Extension getExtensionForKind(String owner, String kind) {
        Extension extRet = null;
        Configuration configuration = getConfigurationForOwner(owner);
        EList<Extension> exts = configuration.getUse();
        EList<Kind> kinds;
        for (Extension ext : exts) {
            kinds = ext.getKinds();
            for (Kind kindObj : kinds) {
                if ((kindObj.getScheme() + kindObj.getTerm()).equals(kind)) {
                    extRet = ext;
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
     * Find extension used with this entity.
     *
     * @param entity
     * @return an extension or null if not found
     */
    public static Extension getExtensionFromEntity(Entity entity) {
        Extension extRet = null;
        // Search owner of the entity.
        Set<String> owners = configurations.keySet();
        Configuration configuration;
        EList<Resource> resources;
        EList<Extension> exts;
        for (String owner : owners) {
            configuration = getConfigurationForOwner(owner);

            resources = configuration.getResources();
            for (Resource resource : resources) {
                if (resource.getId().equals(entity.getId())) {
                    // We found the configuration and owner of the entity.
                    // Now we get the extension from entity kind.
                    Kind entityKind = entity.getKind();
                    exts = configuration.getUse();
                    for (Extension ext : exts) {
                        if (ext.getKinds().contains(entityKind)) {
                            extRet = ext;
                            break;
                        }
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
     * Get all declared owner.
     *
     * @return
     */
    public static Set<String> getAllOwner() {
        return configurations.keySet();
    }

    /**
     * Find all user mixins kind that have this location.
     *
     * @param location (http://localhost:8080/mymixincollection/
     * @return a map by owner and list of user mixins, map is empty if none
     * found.
     */
    public static Map<String, List<String>> findAllUserMixinKindByLocation(final String location) {

        List<String> mixinKinds;
        Map<String, List<String>> mixinKindsByOwner = new HashMap<>();
        // Recherche sur tous les users mixin kinds.
        Set<String> owners = configurations.keySet();
        Configuration config;
        EList<Mixin> mixins;
        String mixinId;
        String locationTmp;
        for (String owner : owners) {
            mixinKinds = new ArrayList<>();
            config = getConfigurationForOwner(owner);
            mixins = config.getMixins();
            for (Mixin mixin : mixins) {
                mixinId = mixin.getScheme() + mixin.getTerm();
                locationTmp = userMixinLocationMap.get(mixinId);
                if (locationTmp != null && locationTmp.contains(location)) {
                    // Location found for this mixin.
                    mixinKinds.add(mixinId);
                }
            }
            if (!mixinKinds.isEmpty()) {
                mixinKindsByOwner.put(owner, mixinKinds);
            }
        }

        return mixinKindsByOwner;
    }

    /**
     * Remove referenced configuration for an owner.
     *
     * @param owner
     */
    public static void resetForOwner(final String owner) {
        removeConfiguration(owner);

    }

    /**
     * Update attributes for all entities that have this path.
     *
     * @param entityId , relative path of the entity
     * @param attributes , attributes to update
     */
    public static void updateAttributesForEntity(final String entityId, Map<String, String> attributes) {
        String ownerFound = null;
        Entity entity = findEntityOnAllOwner(ownerFound, entityId);

        if (entity != null) {
            // update the attributes.
            updateAttributesToEntity(entity, attributes);
            LOGGER.info("owner : " + ownerFound + " --< entity id : " + entityId);
            updateVersion(ownerFound, entityId);
            // printEntity(entity);

        } else {
            // TODO : Report an exception, impossible to update entity, it
            // doesnt exist.
            LOGGER.error("The entity " + entityId + " doesnt exist, can''t update ! ");
        }

        // return entity;
    }

    /**
     * Determine if entity is a Resource or a Link from the provided attributes.
     *
     * @param attr = attributes of an entity
     * @return false if this entity is a link, true otherwise.
     */
    public static boolean checkIfEntityIsResourceOrLinkFromAttributes(Map<String, String> attr) {
        boolean result = true;
        if (attr == null || attr.isEmpty()) {
            return result;
        }
        if (attr.containsKey(Constants.OCCI_CORE_SOURCE) || attr.containsKey(Constants.OCCI_CORE_TARGET)) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Create a Map<String,String> from EList<AttributeState>.
     *
     * @param attributes
     * @return a map<String, String>
     */
    public static Map<String, String> getEntityAttributesMap(EList<AttributeState> attributes) {
        Map<String, String> mapResult = new HashMap<>();
        for (AttributeState attr : attributes) {
            mapResult.put(attr.getName(), attr.getValue());
        }

        return mapResult;
    }

    /**
     * Apply filter where possible. startIndex starts at 1
     *
     * @param startIndex
     * @param number
     * @param filters
     * @param sources
     * @return a filtered list of entities.
     */
    private static List<Entity> filterEntities(final int startIndex, final int number, final List<CollectionFilter> filters, List<Entity> sources, final String user) {
        List<Entity> entities = sources;
        // Filter the lists if filter are set.
        if (!filters.isEmpty() && !entities.isEmpty()) {
            String categoryFilter = null;
            // Check category filter.
            for (CollectionFilter filter : filters) {
                if (filter.getCategoryFilter() != null && !filter.getCategoryFilter().isEmpty()) {
                    // We dont know here if category filter has only the term or has scheme+term, so we must check if there is only the term.
                    categoryFilter = filter.getCategoryFilter();
                    break;
                }
            }
            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                // Check if this is a term or a scheme + term
                if (!Utils.checkIfCategorySchemeTerm(categoryFilter, user)) {
                    // this category is a term.
                    categoryFilter = ConfigurationManager.findCategorySchemeTermFromTerm(categoryFilter, ConfigurationManager.DEFAULT_OWNER);
                }
            }

            String filterOnPath = null;
            boolean hasFilterOnPath = false;
            for (CollectionFilter filter : filters) {
                filterOnPath = filter.getFilterOnPath();
                if (filterOnPath != null && !filterOnPath.isEmpty()) {
                    hasFilterOnPath = true;
                    if (filterOnPath.endsWith("/")) {
                        filterOnPath = filterOnPath.substring(0, filterOnPath.length() - 1);
                    }
                    break;
                }
            }

            Iterator<Entity> it = entities.iterator();
            boolean control = false;
            String constraintValue;
            while (it.hasNext()) {
                Entity entity = it.next();
                // Check if attribute and attribute value is in filter, if not remove this entity from the list to return.
                List<AttributeState> attrs = entity.getAttributes();
                for (AttributeState attr : attrs) {
                    for (CollectionFilter filter : filters) {
                        if (filter.getAttributeFilter().equalsIgnoreCase(attr.getName())) {
                            // Check the constraint value.
                            if (filter.getValue() == null) {
                                // Null: all value is ok for this attribute.
                                control = true;
                                break;
                            } else {
                                constraintValue = filter.getValue();
                            }
                            // Check the constraint attribute Value filter.
                            if (filter.getOperator() == CollectionFilter.OPERATOR_EQUAL && constraintValue.equals(attr.getValue())) {
                                control = true;
                                break;
                            }
                            if (filter.getOperator() == CollectionFilter.OPERATOR_LIKE && attr.getValue().contains(constraintValue)) {
                                control = true;
                                break;
                            }
                        } // end if attribute found from filter on entity.

                    } // for each filters.
                    if (control) {
                        // attribute found and filter respected.
                        break;
                    }
                } // end for each entity attributes.

                if (categoryFilter != null && !categoryFilter.isEmpty()) {
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
                    if (!categoryFound) {
                        control = false;
                    } else {
                        control = true;
                    }

                }

                if (hasFilterOnPath && filterOnPath != null) {
                    String relativeLocation = getEntityRelativePath(entity.getId());
                    
                    if (relativeLocation == null || relativeLocation.isEmpty()) {
                        control = false;
                        
                    } else if (relativeLocation.startsWith(filterOnPath)) {
                        control = true;
                    } else {
                        control = false;
                    }
                } // endif has a filter on relative path.

                if (!control) {
                    it.remove(); // remove this entity from collection, the entity doesnt respect constraints attrib and value.
                }
            }

        } // has filters and has entities.

        // Position the start index if > 0.
        // JP: start index starts with 1
        if (startIndex > 1 && !entities.isEmpty()) {
            int currentIndex = 0;
            Iterator<Entity> it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = it.next();
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
            Iterator<Entity> it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = it.next();
                count++;
                if (count <= number) {

                } else {
                    it.remove();
                }
            }
        }

        return entities;
    }

    /**
     * Get the relative path from entity registered, null else.
     *
     * @param uuid
     * @return
     */
    public static String getEntityRelativePath(String uuid) {
        return entitiesRelativePath.get(uuid);
    }

    // TODO : rebuild etag number method...
//    /**
//     * Generate eTag number from version map.
//     *
//     * @param owner
//     * @param id
//     * @return
//     */
//    public static UInt32 getEtagNumber(final String owner, final String id) {
//        Integer version = versionObjectMap.get(owner + id);
//        if (version == null) {
//            version = 1;
//        }
//        // Generate eTag.
//        return Utils.createEtagNumber(id, owner, version);
//    }
    /**
     * Create an attribute without add this to the entity object.
     *
     * @param name
     * @param value
     * @return AttributeState object.
     */
    private static AttributeState createAttributeState(final String name, final String value) {
        AttributeState attr = OCCIFactory.eINSTANCE.createAttributeState();
        attr.setName(name);
        attr.setValue(value);
        return attr;
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
            return attr;
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

    public static void useAllExtensionForConfigurationInClasspath(String owner) {
        Configuration config = getConfigurationForOwner(owner);
        Extension ext;
        List<Extension> extensions = new LinkedList<>();
        Collection<String> extReg = OCCIRegistry.getInstance().getRegisteredExtensions();
        LOGGER.info("Collection: " + extReg);
        boolean coreAdded = false;
        for (String extScheme : extReg) {
            // Load the extension and register, include the core as well...
            LOGGER.info("Loading model extension : " + extScheme);
            ext = OcciHelper.loadExtension(extScheme);
            if (ext.getName().equals("core")) {
                extensions.add(0, ext); // Add on first infrastructure extension.
                coreAdded = true;
            } else if (ext.getName().equals("infrastructure")) {
                if (coreAdded && extensions.size() > 1) {
                    extensions.add(1, ext);
                } else {
                    extensions.add(0, ext);
                }
            } else {
                extensions.add(ext);
            }
        }

        for (Extension extension : extensions) {
            LOGGER.info("Extension : " + extension.getName() + " added to user configuration.");
            config.getUse().add(extension);
        }
    }

    public static String getLocation(Category category) {
        return '/' + category.getTerm() + '/';
    }

    public static String getLocation(Entity entity) {
        if (entity == null || entitiesRelativePath == null) {
            return null;
        }
        String location = entitiesRelativePath.get(entity.getId());

        // we have maybe no leading slash.
        if (!location.equals("/") && !location.endsWith("/")) {
            location += "/";
        }

        location += entity.getId(); // add the uuid to get a full location.
        // return getLocation(entity.getKind()) + entity.getId();
        return location;
    }

    /**
     * Get all kinds for the configuration used extensions.
     *
     * @param user
     * @return
     */
    public static List<Kind> getAllConfigurationKind(String user) {
        Configuration config = getConfigurationForOwner(user);
        List<Extension> exts = config.getUse();
        List<Kind> allKinds = new LinkedList<>();
        for (Extension ext : exts) {
            allKinds.addAll(ext.getKinds());
        }
        return allKinds;
    }

    /**
     * Get all mixins for the configuration used extensions.
     *
     * @param user
     * @return
     */
    public static List<Mixin> getAllConfigurationMixins(String user) {
        Configuration config = getConfigurationForOwner(user);
        List<Extension> exts = config.getUse();
        List<Mixin> allMixins = new LinkedList<>();
        for (Extension ext : exts) {
            allMixins.addAll(ext.getMixins());
        }
        return allMixins;
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
     * Find category id from category term value for a user configuration.
     *
     * @param categoryTerm
     * @param user
     * @return a String, scheme + term or null if not found on configuration.
     */
    public static String findCategorySchemeTermFromTerm(String categoryTerm, String user) {
        List<Kind> kinds = getAllConfigurationKind(user);
        List<Mixin> mixins = getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;
        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (categoryTerm.equalsIgnoreCase(term)) {
                    return id;
                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (categoryTerm.equalsIgnoreCase(term)) {
                return id;
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (categoryTerm.equalsIgnoreCase(term)) {
                return id;
            }
        }

        return null;

    }

    public static Map<String, String> getEntitiesRelativePath() {
        if (entitiesRelativePath == null) {
            entitiesRelativePath = new HashMap<>();
        }
        return entitiesRelativePath;
    }

    /**
     * This method is called when no uuid is provided on request, but you have
     * to ensure that only one entity exist for this path.
     *
     * @param path
     * @return an entity from a relative path, if entity doesnt exist on path,
     * return null.
     */
    public static Entity getEntityFromPath(final String path) {
        Entity entity = null;
        String uuid = null;
        String pathTmp = null;
        if (path == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : entitiesRelativePath.entrySet()) {
            uuid = entry.getKey();
            pathTmp = entry.getValue();
            if (path.equals(pathTmp)) {
                // entity found.
                break;
            }
        }
        if (uuid != null) {
            entity = findEntity(ConfigurationManager.DEFAULT_OWNER, uuid);
        }

        return entity;
    }

}
