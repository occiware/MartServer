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

// import org.occiware.clouddesigner.occi.*;
// import org.occiware.clouddesigner.occi.util.OcciHelper;
// import org.occiware.mart.MART;

import org.occiware.clouddesigner.occi.*;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.occiware.mart.MART;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage configurations (OCCI Model).
 *
 * @author Christophe Gourdin
 */
public class ConfigurationManager {

    /**
     * Used for now when no owner defined
     */
    public static final String DEFAULT_OWNER = "anonymous";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

    /**
     * This map reference all occi configurations by users. The first ref string
     * is the user uuid. To be updated for multiusers and multiconfigs.
     */
    private static Map<String, Configuration> configurations = new ConcurrentHashMap<>();

    private static OCCIFactory occiFactory = OCCIFactory.eINSTANCE;

    static {
        MART.initMART();
    }

    /**
     * Get a configuration from the configuration's map.
     *
     * @param owner
     * @return a configuration object for an owner.
     */
    public static Configuration getConfigurationForOwner(final String owner) {
        if (configurations.get(owner) == null) {
            LOGGER.warn("Create configuration for owner : " + owner);
            createConfiguration(owner);
            // Assign all referenced extensions on configuration for this user.
            useAllExtensionForConfigurationInClasspath(owner);

        }
        return configurations.get(owner);
    }


    /**
     * Assign used extensions to configuration object.
     *
     * @param owner the current user.
     */
    public static void useAllExtensionForConfigurationInClasspath(final String owner) {
        Configuration config = getConfigurationForOwner(owner);
        Extension ext;
        List<Extension> extensions = new LinkedList<>();
        Collection<String> extReg = OCCIRegistry.getInstance().getRegisteredExtensions();
        LOGGER.info("Collection: " + extReg + " --> owner : " + owner);
        boolean coreAdded = false;
        if (config.getUse().isEmpty()) {
            for (String extScheme : extReg) {
                // Load the extension and register, include the core as well...
                LOGGER.info("Loading model extension : " + extScheme + " --> owner : " + owner);
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
                LOGGER.info("Extension : " + extension.getName() + " added to user configuration --> " + "owner : " + owner);
                config.getUse().add(extension);
            }
        }
    }


    // Find/Get entity / config section


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
     * Get the location of a category, this is used too with user mixin tag.
     *
     * @param category (kind or mixin) instance.
     * @return a location for a category like this : /categoryTerm/
     * Must never return null value.
     */
    public static Optional<String> getLocation(Category category) {
        if (category == null) {
            LOGGER.warn("Category is null when invoking ConfigurationManager.getLocation(Category) method.");
            return Optional.empty();
        }
        if (category instanceof Mixin) {
            String mixinId = category.getScheme() + category.getTerm();
            if (MixinManager.userMixinLocationMap == null) {
                MixinManager.userMixinLocationMap = new ConcurrentHashMap<>();
            }
            if (MixinManager.userMixinLocationMap.get(mixinId) != null) {
                return Optional.of(MixinManager.userMixinLocationMap.get(mixinId));
            }
        }
        return Optional.of('/' + category.getTerm() + '/');
    }


    /**
     * Find category id from category term value for a user configuration.
     *
     * @param categoryTerm
     * @param user
     * @return a String, scheme + term or optional empty if not found on configuration.
     */
    public static Optional<String> findCategorySchemeTermFromTerm(final String categoryTerm, final String user) {
        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;
        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (categoryTerm.equalsIgnoreCase(term)) {
                    return Optional.of(id);
                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (categoryTerm.equalsIgnoreCase(term)) {
                return Optional.of(id);
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (categoryTerm.equalsIgnoreCase(term)) {
                return Optional.of(id);
            }
        }

        return Optional.empty();
    }


    // Create model or add object to model section.

    /**
     * Create a new configuration (empty ==> without any resources and link and
     * extension) for a user.
     *
     * @param owner
     */
    private static void createConfiguration(final String owner) {
        Configuration configuration = occiFactory.createConfiguration();
        configurations.put(owner, configuration);
        LOGGER.debug("Configuration for user " + owner + " created");
        EntityManager.buildEntitiesOwner(owner);
    }

    /**
     * Add all the attributes of a given Kind instance and all its parent kinds.
     *
     * @param attributes the collection where attributes will be added.
     * @param kind       the given Kind instance.
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
     * @param mixin      the given Mixin instance.
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


    // Update entity / config section


    // Remove objects section.


    /**
     * Load kinds and mixins for an interface to render with filtering on extension name and category.
     *
     * @param categoryFilter
     * @param interfData
     * @throws ConfigurationException
     */
    public static void applyFilterOnInterface(final String categoryFilter, QueryInterfaceData interfData, final String username) throws ConfigurationException {

        List<Kind> kinds = KindManager.getAllConfigurationKind(username);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(username);
        if (categoryFilter != null) {
            Iterator it = kinds.iterator();
            Iterator itMix = mixins.iterator();
            List<Action> actions;
            boolean hasActionFilter = false;
            while (it.hasNext()) {
                Kind kindTmp = (Kind) it.next();

                // Check the action kind, if action found for this kind, we keep it.
                actions = kindTmp.getActions();
                for (Action actionTmp : actions) {
                    if (actionTmp.getTerm().equalsIgnoreCase(categoryFilter)) {
                        hasActionFilter = true;
                        break;
                    }
                }

                if (!kindTmp.getTerm().equalsIgnoreCase(categoryFilter) && !hasActionFilter) {
                    it.remove();
                }
            }
            while (itMix.hasNext()) {
                Mixin mixinTmp = (Mixin) itMix.next();
                if (!mixinTmp.getTerm().equalsIgnoreCase(categoryFilter)) {
                    itMix.remove();
                }
            }
        }
        interfData.setCategoryFilter(categoryFilter);
        interfData.setKinds(kinds);
        interfData.setMixins(mixins);

    }

    /**
     * Find action model object from the used extensions.
     *
     * @param actionId action scheme + term.
     * @param owner    owner of the configuration.
     * @return an action model object, return empty optional if not found.
     */
    public static Optional<Action> findActionOnExtensions(final String actionId, final String owner) {
        Action action = null;
        List<Kind> kinds = KindManager.getAllConfigurationKind(owner);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(owner);
        if (actionId == null) {
            return Optional.empty();
        }
        for (Kind kind : kinds) {
            for (Action actionModel : kind.getActions()) {
                if ((actionModel.getScheme() + actionModel.getTerm()).equals(actionId)) {
                    action = actionModel;
                    break;
                }
            }
            if (action != null) {
                break;
            }
        }
        if (action == null) {
            // Search on mixins actions.
            for (Mixin mixin : mixins) {
                for (Action actionModel : mixin.getActions()) {
                    if ((actionModel.getScheme() + actionModel.getTerm()).equals(actionId)) {
                        action = actionModel;
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(action);
    }

    /**
     * Create a new uuid.
     *
     * @return new uuid, never return null value.
     */
    public static String createUUID() {
        return UUID.randomUUID().toString();

    }

    /**
     * Check if the path contains a category referenced on extensions used by
     * configuration.
     *
     * @param path
     * @param user
     * @return a category term if found on configuration, if not found return
     * empty optional.
     */
    public static Optional<String> getCategoryFilter(final String path, final String user) {
        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
        String term;

        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                if (path.contains(term) || path.contains(term.toLowerCase())) {
                    return Optional.of(term);

                }
            }

            term = kind.getTerm();
            if (path.contains(term) || path.contains(term.toLowerCase())) {
                return Optional.of(term);
            }

        }
        for (Mixin mixin : mixins) {
            term = mixin.getTerm();
            if (path.contains(term) || path.contains(term.toLowerCase())) {
                return Optional.of(term);
            }
        }

        return Optional.empty();
    }

    /**
     * Check if the path equals a category referenced on extensions used by
     * configuration. Remove leading slash and ending slash before proceed.
     *
     * @param path
     * @param user
     * @return a category term if found on configuration, if not found return
     * empty optional.
     */
    public static Optional<String> getCategoryFilterSchemeTerm(final String path, final String user) {
        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;

        if (path == null) {
            return Optional.empty();
        }

        String pathTerm = path;
        if (pathTerm.startsWith("/")) {
            pathTerm = pathTerm.substring(1);
        }
        if (pathTerm.endsWith("/")) {
            pathTerm = pathTerm.substring(0, pathTerm.length() - 1);
        }

        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (pathTerm.equals(term) || pathTerm.equals(term.toLowerCase())) {
                    return Optional.of(id);

                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (pathTerm.equals(term) || path.equals(term.toLowerCase())) {
                return Optional.of(id);
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (pathTerm.equals(term) || pathTerm.equals(term.toLowerCase())) {
                return Optional.of(id);
            }
        }

        return Optional.empty();
    }

    /**
     * Return true if categoryFilter is a scheme + term.
     *
     * @param categoryFilter kind or mixin or action scheme + term
     * @param user           a username (owner)
     * @return true if category is on user's configuration.
     */
    public static boolean checkIfCategorySchemeTerm(final String categoryFilter, final String user) {

        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;
        for (Kind kind : kinds) {
            // Check actions.
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (categoryFilter.equalsIgnoreCase(id)) {
                    return true;

                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (categoryFilter.equalsIgnoreCase(id)) {
                return true;
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (categoryFilter.equalsIgnoreCase(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is that location path is on a category ? like compute/
     *
     * @param location the location path : like /compute/ or entity path.
     * @return
     */
    public static boolean isCollectionOnCategory(final String location, final String owner) {
        Optional<String> optCategoryId = getCategoryFilterSchemeTerm(location, owner);
        return optCategoryId.isPresent();
    }

    public static OCCIFactory getOcciFactory() {
        return occiFactory;
    }

}
