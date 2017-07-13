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
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by christophe on 22/04/2017.
 */
public class KindManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KindManager.class);


    /**
     * Check if an attribute is a kind attribute.
     *
     * @param kind
     * @param attributeName
     */
    public static boolean isKindAttribute(final Kind kind, String attributeName) {
        boolean result = false;
        EList<Attribute> attrs = kind.getAttributes();
        for (Attribute attr : attrs) {
            if (attr.getName().equals(attributeName)) {
                result = true;
                break;
            }
        }
        return result;

    }

    /**
     * Search for a kind.
     *
     * @param id    kind id scheme + term.
     * @param owner owner of the configuration
     * @return an optional kind object model or optional empty if kind is not found.
     */
    static Optional<Kind> findKindFromEntities(final String id, final String owner) {
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
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

        return Optional.ofNullable(kind);

    }

    /**
     * Search for a kind from referenced extension model.
     *
     * @param kindId
     * @param owner
     * @return
     */
    public static Optional<Kind> findKindFromExtension(final String kindId, final String owner) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(owner);
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

            Collection<String> extReg = OCCIRegistry.getInstance().getRegisteredExtensions();

            extReg.removeAll(extUsed);
            Extension ext;
            for (String extScheme : extReg) {
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

        return Optional.ofNullable(kindToReturn);

    }

    /**
     * Get used extension with this kind.
     *
     * @param kind  (represent a Kind Scheme+term)
     * @param owner owner of the configuration
     * @return
     */
    public static Optional<Extension> getExtensionForKind(String kind, String owner) {
        Extension extRet = null;
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(owner);
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

        return Optional.ofNullable(extRet);
    }

    /**
     * Get all kinds for the configuration used extensions.
     *
     * @param user
     * @return
     */
    public static List<Kind> getAllConfigurationKind(final String user) {
        Configuration config = ConfigurationManager.getConfigurationForOwner(user);
        List<Extension> exts = config.getUse();
        List<Kind> allKinds = new LinkedList<>();
        for (Extension ext : exts) {
            allKinds.addAll(ext.getKinds());
        }
        return allKinds;
    }

    /**
     * Return all attributes for an entity kind (main attributes).
     *
     * @param kind
     * @return a collection of attributes.
     */
    public static Collection<Attribute> getKindAttributes(final Kind kind) {
        List<Attribute> attributes = new ArrayList<>();
        if (kind != null) {
            ConfigurationManager.addAllAttributes(attributes, kind);
        }
        return attributes;
    }
}
