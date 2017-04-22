package org.occiware.mart.server.model;

import org.eclipse.emf.common.util.EList;
import org.occiware.clouddesigner.occi.*;
import org.occiware.clouddesigner.occi.util.OcciHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
     * @param owner
     * @param id
     * @return
     */
    static Kind findKindFromEntities(final String owner, final String id) {
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

        return kindToReturn;

    }

    /**
     * Get used extension with this kind.
     *
     * @param owner owner of the configuration
     * @param kind  (represent a Kind Scheme+term)
     * @return
     */
    public static Extension getExtensionForKind(String owner, String kind) {
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

        return extRet;
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
}
