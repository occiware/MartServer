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
import org.occiware.clouddesigner.occi.*;
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
    public static Mixin findUserMixinOnConfiguration(final String mixinId, final String owner) {
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
        return mixinToReturn;
    }

    /**
     * Search mixin on owner's configuration.
     *
     * @param owner
     * @param mixinId
     * @return a mixin found or null if not found
     */
    static Mixin findMixinOnEntities(final String owner, final String mixinId) {
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

        return mixinToReturn;
    }

    /**
     * Get used extension with this kind.
     *
     * @param owner owner of the configuration
     * @param mixin (represent a Mixin Scheme+term)
     * @return
     */
    public static Extension getExtensionForMixin(String owner, String mixin) {
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

        return extRet;
    }

    /**
     * Find a mixin for a location and a user.
     *
     * @param locationMixin
     * @param owner
     * @return
     */
    public static Mixin getUserMixinFromLocation(final String locationMixin, final String owner) {
        // Search for the mixin id.
        Mixin mixin = null;
        Set<String> keys = userMixinLocationMap.keySet();
        String locationCompare = Utils.getPathWithoutPrefixSuffixSlash(locationMixin);

        for (String key : keys) {
            String locationTmp = userMixinLocationMap.get(key).trim();

            String location = Utils.getPathWithoutPrefixSuffixSlash(locationTmp);

            if (location.equals(locationCompare)) {
                // Search the mixin from this scheme+term.
                mixin = findUserMixinOnConfiguration(key, owner);
                break;
            }

        }
        return mixin;

    }

    /**
     * Is mixin this Id is a mixin tag ?
     *
     * @param owner
     * @param mixinTag
     * @return
     */
    public static boolean isMixinTags(final String owner, final String mixinTag) {
        boolean result = false;

        Mixin mixin = findUserMixinOnConfiguration(mixinTag, owner);
        String location = userMixinLocationMap.get(mixinTag);
        if (location != null && mixin != null) {
            result = true;
        }
        return result;
    }

    /**
     * Get all mixins for the configuration used extensions.
     *
     * @param user
     * @return
     */
    public static List<Mixin> getAllConfigurationMixins(String user) {
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
                        // Search on the mixin tag.
                        mixin = findUserMixinOnConfiguration(mixinStr, owner);
                        if (mixin == null) {
                            throw new ConfigurationException("Mixin " + mixinStr + " not found on extension nor on entities, this is maybe a mixin tag to define before.");
                        }
                    }
                    LOGGER.info("Mixin found on configuration : --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());

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
        Mixin mixin = findMixinOnExtension(owner, mixinId);

        if (mixin == null) {
            mixin = findMixinOnEntities(owner, mixinId);
            if (mixin == null) {
                // Search on the mixin tag.
                mixin = findUserMixinOnConfiguration(mixinId, owner);
                if (mixin == null) {
                    throw new ConfigurationException("Mixin " + mixinId + " not found on extension nor on entities, this is maybe a mixin tag to define before.");
                }
            }
        }
        LOGGER.info("Mixin --> Term : " + mixin.getTerm() + " --< Scheme : " + mixin.getScheme());
        List<Entity> entities = new ArrayList<>();
        for (String entityId : entityIds) {
            Entity entity = EntityManager.findEntityForUuid(entityId, owner);

            if (entity != null && !entity.getMixins().contains(mixin)) {
                entity.getMixins().add(mixin);
                EntityManager.updateVersion(owner, entityId);
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
        Mixin mixin = findUserMixinOnConfiguration(id, owner);
        if (mixin == null) {
            mixin = createMixin(id);
            mixin.setTitle(title);
        } else {
            // Check if this mixin is a mixin extension..
            if (!isMixinTags(owner, id)) {
                throw new ConfigurationException("This mixin : " + id + " is not a mixin tag, but it exist on referenced extension and configuration.");
            }
            LOGGER.info("Overwriting mixin on configuration : " + id);
            configuration.getMixins().remove(mixin);
        }
        LOGGER.info("Adding mixin on configuration : " + id);
        // We add the mixin location to the userMixin map.
        userMixinLocationMap.put(id, location);

        configuration.getMixins().add(mixin);

    }

    /**
     * Dissociate entities from this mixin.
     *
     * @param mixin
     * @param owner
     */
    static void dissociateMixinFromEntities(final Mixin mixin, final String owner) {
        if (mixin == null) {
            return;
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
     * @param owner
     * @param mixinId
     * @param entity
     * @return
     */
    public static boolean dissociateMixinFromEntity(final String owner, final String mixinId, Entity entity) {
        boolean result = false;
        if (mixinId == null) {
            return false;
        }
        // Load the mixin object.
        List<Mixin> mixins = entity.getMixins();
        if (mixins.isEmpty()) {
            return true;
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
            result = true;
        }
        return result;
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
        Mixin mixin = findUserMixinOnConfiguration(mixinId, owner);

        if (mixin == null) {
            LOGGER.info("mixin not found on configurations.");
            throw new ConfigurationException("mixin : " + mixinId + " not found on configuration.");

        }

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
     * Load a list of mixin from used extensions models.
     *
     * @param mixins
     * @return
     * @throws ConfigurationException
     */
    public static List<Mixin> loadMixinFromSchemeTerm(List<String> mixins) throws ConfigurationException {
        List<Mixin> mixinModel = new LinkedList<>();

        Mixin mixinTmp;
        for (String mixinId : mixins) {
            mixinTmp = findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, mixinId);
            if (mixinTmp == null) {
                mixinTmp = findUserMixinOnConfiguration(mixinId, ConfigurationManager.DEFAULT_OWNER);
                if (mixinTmp == null) {
                    throw new ConfigurationException("Mixin : " + mixinId + " not found on used extensions models");
                }
            } else {
                mixinModel.add(mixinTmp);
            }
        }
        return mixinModel;
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
        boolean result;
        Mixin mixin = getUserMixinFromLocation(path, owner);
        result = mixin != null;
        return result;
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
}
