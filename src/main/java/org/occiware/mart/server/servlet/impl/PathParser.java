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
package org.occiware.mart.server.servlet.impl;

import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Utils;

import java.util.List;

/**
 * This class is a pojo that defines the query types and give the route to treat the datas.
 *
 * Created by Christophe Gourdin on 19/11/2016.
 */
public class PathParser {

    /**
     * The input data parsed by a parser from input query.
     */
    private InputData data;

    /**
     * Current path where the query is called.
     */
    private String path;

    /**
     * If set this replace the path when this is not a mixinTag definition request.
     */
    private String location;

    /**
     * Defines if mixinTag query the values of xOcciLocations.
     */
    private List<String> xOcciLocations;

    /**
     * Mixin definition request (linked to PUT and GET interface method).
     */
    private boolean mixinTagDefinitionRequest;

    private boolean entityQuery;

    /**
     * Define if this is a collection query.
     */
    private boolean collectionQuery;

    /**
     * If this is a collection query define if category query (compute/).
     */
    private boolean collectionOnCategory;

    /**
     * A query on a collection custom path.
     */
    private boolean collectionCustomPath;

    /**
     * Define if the path is an interface query /-/.
     */
    private boolean interfQuery;

    /**
     * Define if the query is an action query.
     */
    private boolean actionInvocationQuery;

    /**
     *
     * @param data InputData object
     * @param path the query relative path
     */
    public PathParser(InputData data, String path) {
        this.data = data;
        if (data.getLocation() != null) {
            this.location = Utils.getPathWithoutPrefixSuffixSlash(data.getLocation());
        }
        this.path = Utils.getPathWithoutPrefixSuffixSlash(path);
        updateRoutes();
    }

    /**
     * Check if the path with/without datas is a query interface, a mixin tag definition, a resource query etc.
     */
    public void updateRoutes() {

        // Determine if this is an action invocation.
        if (data.getAction() != null && !data.getAction().isEmpty()) {
            actionInvocationQuery = true;
        }

        // Check if interface query.
        if (path.equals("-/") || path.equals(".well-known/org/ogf/occi/-/") || path.endsWith("/-/")) {
            if (location != null && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
                mixinTagDefinitionRequest = true;
            } else {
                interfQuery = true;
            }
        }

        // Is the path is an entity path ?
        //  if a location is defined, this replace the given path.
        if (location != null && !location.isEmpty()) {
            // the attributes is used if occi.core.id is defined for the current data to work with.
            entityQuery = Utils.isEntityUUIDProvided(location, data.getAttrs());
        } else {
            entityQuery = Utils.isEntityUUIDProvided(path, data.getAttrs());
        }

        if (!entityQuery) {
            entityQuery = data.getEntityUUID() != null;
            if (!entityQuery) {
                // Check if a kind is defined in inputdata, if this is the case, it must be an entity query.
                entityQuery = data.getKind() != null;
            }
        }

        if (location != null && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
            mixinTagDefinitionRequest = true;
        }
        if (!entityQuery && !interfQuery && !mixinTagDefinitionRequest) {
            collectionQuery = true;
            // Check if collection category query ex: compute/
            String categoryId;
            if (location != null && !location.isEmpty()) {
                categoryId = Utils.getCategoryFilterSchemeTerm(location, ConfigurationManager.DEFAULT_OWNER);
            } else {
                categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);
            }

            // Check if custom query (bounded path).
            if (categoryId == null) {
                collectionCustomPath = true;
            } else {
                collectionOnCategory = true;
            }
        }
    }

    public InputData getData() {
        return data;
    }

    public void setData(InputData data) {
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getxOcciLocations() {
        return xOcciLocations;
    }

    public void setxOcciLocations(List<String> xOcciLocations) {
        this.xOcciLocations = xOcciLocations;
    }

    public boolean isMixinTagDefinitionRequest() {
        return mixinTagDefinitionRequest;
    }

    public void setMixinTagDefinitionRequest(boolean mixinTagDefinitionRequest) {
        this.mixinTagDefinitionRequest = mixinTagDefinitionRequest;
    }

    public boolean isEntityQuery() {
        return entityQuery;
    }

    public void setEntityQuery(boolean entityQuery) {
        this.entityQuery = entityQuery;
    }

    public boolean isCollectionQuery() {
        return collectionQuery;
    }

    public void setCollectionQuery(boolean collectionQuery) {
        this.collectionQuery = collectionQuery;
    }

    public boolean isCollectionOnCategory() {
        return collectionOnCategory;
    }

    public void setCollectionOnCategory(boolean collectionOnCategory) {
        this.collectionOnCategory = collectionOnCategory;
    }

    public boolean isCollectionCustomPath() {
        return collectionCustomPath;
    }

    public void setCollectionCustomPath(boolean collectionCustomPath) {
        this.collectionCustomPath = collectionCustomPath;
    }

    public boolean isInterfQuery() {
        return interfQuery;
    }

    public void setInterfQuery(boolean interfQuery) {
        this.interfQuery = interfQuery;
    }

    public boolean isActionInvocationQuery() {
        return actionInvocationQuery;
    }

    public void setActionInvocationQuery(boolean actionInvocationQuery) {
        this.actionInvocationQuery = actionInvocationQuery;
    }
}
