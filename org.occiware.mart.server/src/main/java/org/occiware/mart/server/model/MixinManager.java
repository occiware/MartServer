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
import org.eclipse.emf.ecore.EStructuralFeature;
import org.occiware.clouddesigner.occi.*;


import org.occiware.clouddesigner.occi.util.Occi2Ecore;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by christophe on 22/04/2017.
 */
public class MixinManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinManager.class);

    /**
     * References location for a user mixin. this is used by find method to find
     * the collection of user mixin. Key : Mixin sheme + term, must be unique
     * Value : Location with form of : http://localhost:8080/mymixincollection/
     */
    static Map<String, String> userMixinLocationMap = new ConcurrentHashMap<>();

    /**
     * Get a mixin from configuration object.
     *
     * @param mixinId
     * @param owner
     * @return
     */
    public static Optional<Mixin> findUserMixinOnConfiguration(final String mixinId, final String owner) {
        Mixin mixinToReturn = null;
        Configuration config;
        EList<Mixin> mixins;
        config = ConfigurationManager.getConfigurationForOwner(owner);
        mixins = config.getMixins();
        for (Mixin mixin : mixins) {
            if ((mixin.getScheme() + mixin.getTerm()).equals(mixinId)) {
                mixinToReturn = mixin;
                break;
            }
        }
        return Optional.ofNullable(mixinToReturn);
    }

    /**
     * Search mixin on owner's configuration.
     *
     * @param mixinId
     * @param owner
     * @return a mixin found or empty optional if not found
     */
    public static Optional<Mixin> findMixinOnEntities(final String mixinId, final String owner) {
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
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

        return Optional.ofNullable(mixinToReturn);
    }

    /**
     * Find a mixin on loaded extension on configuration.
     *
     * @param mixinId
     * @param owner
     * @return
     */
    public static Optional<Mixin> findMixinOnExtension(final String mixinId, final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
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

        return Optional.ofNullable(mixinToReturn);
    }

    /**
     * Get used extension with this kind.
     *
     * @param mixin (represent a Mixin Scheme+term)
     * @param owner owner of the configuration
     * @return
     */
    public static Optional<Extension> getExtensionForMixin(final String mixin, final String owner) {
        Extension extRet = null;
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
        EList<Extension> exts = configuration.getUse();
        EList<Mixin> mixins;
        for (Extension ext : exts) {
            mixins = ext.getMixins();
            for (Mixin mixinObj : mixins) {
                if ((mixinObj.getScheme() + mixinObj.getTerm()).equals(mixin)) {
                    extRet = ext;
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
     * Find a mixin for a location and a user.
     *
     * @param locationMixin
     * @param owner
     * @return
     */
    public static Optional<Mixin> getUserMixinFromLocation(final String locationMixin, final String owner) {
        // Search for the mixin id.
        Mixin mixin = null;
        Set<String> keys = userMixinLocationMap.keySet();
        String locationCompare = Utils.getPathWithoutPrefixSuffixSlash(locationMixin);

        for (String key : keys) {
            String locationTmp = userMixinLocationMap.get(key).trim();

            String location = Utils.getPathWithoutPrefixSuffixSlash(locationTmp);

            if (location.equals(locationCompare)) {
                // Search the mixin from this scheme+term.
                Optional<Mixin> optMixin = findUserMixinOnConfiguration(key, owner);
                if (optMixin.isPresent()) {
                    mixin = optMixin.get();
                    break;
                }
            }
        }
        return Optional.ofNullable(mixin);
    }

    /**
     * Is mixin this Id is a mixin tag ?
     *
     * @param mixinTag
     * @param owner
     * @return
     */
    public static boolean isMixinTags(final String mixinTag, final String owner) {
        boolean result = false;
        Optional<Mixin> optMixin = findUserMixinOnConfiguration(mixinTag, owner);

        String location = userMixinLocationMap.get(mixinTag);
        if (location != null && optMixin.isPresent()) {
            result = true;
        }
        return result;
    }

    /**
     * Used by load configuration only. Update all the mixin location map with mixins tag only.
     * @param owner
     */
    public static void updateAllMixinTagReferences(final String owner) {

        List<Mixin> mixinsTags = getAllMixinTagsForOwner(owner);
        String location;
        for (Mixin mixin : mixinsTags) {
            // TODO : Save mixin location map in a file when saving xmi occic document.
            location = mixin.getTerm();
            userMixinLocationMap.put(mixin.getTerm(), location);
        }

    }

    /**
     *
     * @param owner
     * @return
     */
    public static List<Mixin> getAllMixinTagsForOwner(final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
        List<Extension> exts = config.getUse();
        List<Mixin> mixinsConf = config.getMixins();
        List<Mixin> mixinsTags = new LinkedList<>();

        List<Mixin> allMixinsExts = new LinkedList<>();
        boolean found;
        for (Extension ext : exts) {
            allMixinsExts.addAll(ext.getMixins());
        }

        for (Mixin mixin : mixinsConf) {
            found = false;
            String mixinTerm = mixin.getTerm();
            String mixinScheme = mixin.getScheme();
            String mixinId = mixinScheme + mixinTerm;
            for (Mixin mix : allMixinsExts) {
                if ((mix.getScheme() + mix.getTerm()).equals(mixinId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // This is a mixin tag !
                mixinsTags.add(mixin);
            }
        }
        return mixinsTags;

    }

    /**
     * Get all mixins for the configuration used extensions.
     *
     * @param user
     * @return
     */
    public static List<Mixin> getAllConfigurationMixins(final String user) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(user);
        List<Extension> exts = config.getUse();
        List<Mixin> mixinsConf = config.getMixins();

        List<Mixin> allMixins = new LinkedList<>();
        for (Extension ext : exts) {
            allMixins.addAll(ext.getMixins());
        }
        // For mixin tags...
        boolean found;
        for (Mixin mixin : mixinsConf) {
            found = false;
            String mixinTerm = mixin.getTerm();
            String mixinScheme = mixin.getScheme();
            String mixinId = mixinScheme + mixinTerm;
            for (Mixin mix : allMixins) {
                if ((mix.getScheme() + mix.getTerm()).equals(mixinId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allMixins.add(mixin);
            }
        }

        return allMixins;
    }

    /**
     * Create a new mixin without any association.
     *
     * @param mixinId
     * @return
     */
    private static Mixin createMixin(final String mixinId) {
        Mixin mixin = ConfigurationManager.getOcciFactory().createMixin();
        if (mixinId != null) {
            String scheme;
            String term;
            term = mixinId.split("#")[1];
            scheme = mixinId.split("#")[0] + "#";
            mixin.setTerm(term);
            mixin.setScheme(scheme);
        }

        return mixin;
    }

    /**
     * Add mixins to an existing entity (resources or links). Ex of mixin string
     * format : http://schemas.ogf.org/occi/infrastructure/network#ipnetwork
     *
     * @param entity     (OCCI Entity).
     * @param mixins     (List of mixins).
     * @param owner
     * @param updateMode (if updateMode is true, reset existing and replace with
     *                   new ones)
     * @throws ConfigurationException
     */
    public static void addMixinsToEntity(Entity entity, final List<String> mixins, final String owner, final boolean updateMode) throws ConfigurationException {

        Optional<Mixin> optMixin;
        Mixin mixin;
        if (updateMode) {
            entity.getMixins().clear();
        }
        if (mixins != null && !mixins.isEmpty()) {

            for (String mixinStr : mixins) {
                optMixin = findMixinOnExtension(mixinStr, owner);

                // Check if this mixin exist in realm extensions.

                if (!optMixin.isPresent()) {
                    LOGGER.info("Mixin not found on extensions, searching on referenced entities: --> Term : " + mixinStr);
                    // Search the mixin on entities.
                    optMixin = findMixinOnEntities(mixinStr, owner);

                    if (!optMixin.isPresent()) {
                        // Search on the mixin tag.
                        optMixin = findUserMixinOnConfiguration(mixinStr, owner);
                        if (!optMixin.isPresent()) {
                            throw new ConfigurationException("Mixin " + mixinStr + " not found on extension nor on entities, this is maybe a mixin tag to define before.");
                        }
                    }
                    mixin = optMixin.get();
                    LOGGER.info("Mixin found on configuration : --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());

                } else {
                    mixin = optMixin.get();
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
            }
        }
    }

    /**
     * Associate a list of entities with a mixin, replacing existing list if
     * any. if mixin doest exist, this will throw a new exception.
     *
     * @param owner
     * @param mixinId
     * @param entityIds
     * @param updateMode
     * @throws ConfigurationException
     */
    public static void saveMixinForEntities(final String mixinId, final List<String> entityIds,
                                            final boolean updateMode, final String owner) throws ConfigurationException {


        // searching for the mixin to register.
        List<Entity> entities = new ArrayList<>();
        Optional<Entity> optEntity;

        for (String entityId : entityIds) {
            optEntity = EntityManager.findEntityForUuid(entityId, owner);

            if (!optEntity.isPresent()) {
                // Find entity by location.
                optEntity = EntityManager.findEntityFromLocation(entityId, owner);
            }
            if (optEntity.isPresent()) {
                entities.add(optEntity.get());
            }
        }

        if (!entities.isEmpty()) {
            saveMixinForEntitiesModel(mixinId, entities, updateMode, owner);
        }

    }

    /**
     * Associate a list of entities with a mixin, replacing existing list if
     * any. if mixin doest exist, this will throw a new exception.
     *
     * @param mixinId    Mixin term + scheme, this may be a user defined mixin tag.
     * @param entities   List of entity model object.
     * @param updateMode update mode : if false, dissociate mixin from entities that are not in the entities list parameter.
     * @param owner      username for this configuration.
     * @throws ConfigurationException
     */
    public static void saveMixinForEntitiesModel(final String mixinId, final List<Entity> entities,
                                                 final boolean updateMode, final String owner) throws ConfigurationException {

        Optional<Mixin> optMixin = findMixinOnExtension(mixinId, owner);
        // searching for the mixin to register.
        Mixin mixin;

        if (!optMixin.isPresent()) {

            optMixin = findMixinOnEntities(mixinId, owner);
            if (!optMixin.isPresent()) {
                // Search on the mixin tag.
                optMixin = findUserMixinOnConfiguration(mixinId, owner);
                if (!optMixin.isPresent()) {
                    throw new ConfigurationException("Mixin " + mixinId + " not found on extension nor on entities, this is maybe a mixin tag to define before.");
                }
            }
        }
        mixin = optMixin.get();
        LOGGER.info("Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());

        for (Entity entity : entities) {
            if (!entity.getMixins().contains(mixin)) {
                entity.getMixins().add(mixin);
                EntityManager.updateVersion(owner, entity.getId());
            }
        }
        List<Entity> mixinEntities = mixin.getEntities();

        if (!updateMode) {
            boolean found;
            // Remove entities those are not in the input list.
            Iterator<Entity> it = mixinEntities.iterator();
            while (it.hasNext()) {
                found = false;
                Entity entityMixin = it.next();

                for (Entity entity : entities) {
                    if (entityMixin.getId().equals(entity.getId())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // Remove reference mixin of the entity.
                    entityMixin.getMixins().remove(mixin);
                }

            }
        }

    }


    /**
     * Add a user mixin to configuration's Object (user tag).
     *
     * @param id
     * @param title
     * @param location
     * @param owner
     * @throws ConfigurationException
     */
    public static void addUserMixinOnConfiguration(final String id, final String title, final String location, final String owner) throws ConfigurationException {
        if (owner == null || id == null || location == null) {
            return;
        }
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);

        // Check if this mixin already exist on configuration, if this is the case, overwrite mixin definition.
        Optional<Mixin> optMixin = findUserMixinOnConfiguration(id, owner);
        Mixin mixin = null;
        if (!optMixin.isPresent()) {
            mixin = createMixin(id);
            mixin.setTitle(title);
        } else {
            mixin = optMixin.get();
            // Check if this mixin is a mixin extension..
            if (!isMixinTags(id, owner)) {
                throw new ConfigurationException("This mixin : " + id + " is not a mixin tag, but it exist on referenced extension and configuration.");
            }
            LOGGER.info("Overwriting mixin on configuration : " + id);
            configuration.getMixins().remove(mixin);
        }
        LOGGER.info("Adding mixin on configuration : " + id);
        // We add the mixin location to the userMixin map.
        userMixinLocationMap.put(id, location);
        // TODO : check with metamodelv2 how to add a new mixin tag.
        configuration.getMixins().add(mixin);

    }

    /**
     * Dissociate entities from this mixin.
     *
     * @param mixin
     * @param owner
     */
    static void dissociateMixinFromEntities(final Mixin mixin, final String owner) throws ConfigurationException {
        if (mixin == null) {
            throw new ConfigurationException("No mixin to dissociate");
        }
        List<Entity> entities = EntityManager.findAllEntitiesForMixin(mixin.getScheme() + mixin.getTerm(), owner);
        for (Entity entity : entities) {
            entity.getMixins().remove(mixin);
            EntityManager.updateVersion(owner, entity.getId());
        }
        entities.clear();

    }

    /**
     * Dissociate a mixin from an entity.
     *
     * @param mixinId
     * @param entity
     * @param owner
     * @return
     */
    public static void dissociateMixinFromEntity(final String mixinId, Entity entity, final String owner) throws ConfigurationException {

        if (mixinId == null) {
            throw new ConfigurationException("No mixin defined to dissociate from entity");
        }
        // Load the mixin object.
        List<Mixin> mixins = entity.getMixins();
        if (mixins.isEmpty()) {
            return;
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
            // First we remove its attributes if any.
            EList<Attribute> attributesToRemove = myMixin.getAttributes();
            if (!attributesToRemove.isEmpty()) {
                EntityManager.removeEntityAttributes(entity, attributesToRemove);
            }
            entity.getMixins().remove(myMixin);
            EntityManager.updateVersion(owner, entity.getId());
        }

    }

    /**
     * Delete a user mixin from configuration's Object (user tag).
     *
     * @param mixinId
     * @param owner
     * @throws ConfigurationException
     */
    public static void removeUserMixinFromConfiguration(final String mixinId, final String owner) throws ConfigurationException {
        if (mixinId == null) {
            return;
        }

        // Search for userMixin.
        Optional<Mixin> optMixin = findUserMixinOnConfiguration(mixinId, owner);

        Mixin mixin;

        if (!optMixin.isPresent()) {
            LOGGER.info("mixin not found on configurations.");
            throw new ConfigurationException("mixin : " + mixinId + " not found on configuration.");
        }
        mixin = optMixin.get();
        // We remove the mixin location from the userMixin map.
        userMixinLocationMap.remove(mixinId);

        // Delete from configuration.
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
        config.getMixins().remove(mixin);
    }

    /**
     * @param mixins
     * @param kind
     * @return true if all mixins applied.
     */
    public static boolean checkIfMixinAppliedToKind(List<Mixin> mixins, Kind kind) {
        boolean result = true;
        if (mixins.isEmpty()) {
            return true;
        }
        for (Mixin mixin : mixins) {
            if (!mixin.getApplies().contains(kind)) {
                // one or more mixin doesnt apply to this kind.
                result = false;
            }
        }
        return result;
    }

    /**
     * Check if path is on a mixin tag (mixin without attributes and applied and
     * depends).
     *
     * @param path
     * @param owner
     * @return false if the path and request is not on mixin tag.
     */
    public static boolean isMixinTagRequest(final String path, final String owner) {
        Optional<Mixin> optMixin = getUserMixinFromLocation(path, owner);
        return optMixin.isPresent();
    }

    /**
     * Convert entity mixins to List of scheme+term mixins.
     *
     * @param entity
     * @return
     */
    public static List<String> convertEntityMixinsToList(final Entity entity) {
        List<Mixin> mixins = entity.getMixins();
        List<String> mixinsStr = new LinkedList<>();
        for (Mixin mixin : mixins) {
            String mixinStr = mixin.getScheme() + mixin.getTerm();
            mixinsStr.add(mixinStr);
        }
        return mixinsStr;
    }

    public static void clearMixinTagsReferences() {
        userMixinLocationMap.clear();
    }

    /**
     * Get The datatype of an attribute from a list of mixins.
     *
     * @param mixin   a mixin base object.
     * @param attrName the name of the attribute
     * @return a dataType or an optional empty ==> NEVER null values.
     */
    public static Optional<EDataType> getEAttributeType(final Mixin mixin, final String attrName) {
        EDataType eDataType = null;

        String eAttributeName = Occi2Ecore.convertOcciAttributeName2EcoreAttributeName(attrName);
        final EStructuralFeature eStructuralFeature = mixin.eClass().getEStructuralFeature(eAttributeName);
        if (eStructuralFeature != null) {
            if ((eStructuralFeature instanceof EAttribute)) {
                // Obtain the attribute type.
                eDataType = ((EAttribute) eStructuralFeature).getEAttributeType();
            }
        }
        return Optional.ofNullable(eDataType);
    }

    /**
     *
     * @param mixin
     * @return
     */
    public static Collection<Attribute> getAllMixinAttribute(Mixin mixin) {
        List<Attribute> attributes = new ArrayList<>();
        ConfigurationManager.addAllAttributes(attributes, mixin);
        return attributes;
    }

}
