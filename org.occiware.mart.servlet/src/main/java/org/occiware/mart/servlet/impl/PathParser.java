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
package org.occiware.mart.servlet.impl;

import java.util.List;
import java.util.Map;

/**
 * This class is a pojo that defines the query types and give the route to treat the datas.
 * <p>
 * Created by Christophe Gourdin on 19/11/2016.
 */
public class PathParser {

    /**
     * The input data parsed by a parser from input query.
     */
    private Data data;

    /**
     * Current path where the query is called.
     */
    private String path;

    /**
     * If set this replace the path when this is not a mixinTag definition request.
     */
    private String location;

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

    private String categoryId;

    /**
     * @param data Data object
     * @param path the query relative path
     */
    public PathParser(Data data, String path, Map<String, String> requestParameters) {
        this.data = data;
        if (data.getLocation() != null) {
            this.location = Utils.getPathWithoutPrefixSuffixSlash(data.getLocation());
        }
        this.path = Utils.getPathWithoutPrefixSuffixSlash(path);
        updateRoutes(requestParameters);
    }

    /**
     * Check if the path with/without datas is a query interface, a mixin tag definition, a resource query etc.
     *
     * @param requestParameters
     */
    public void updateRoutes(Map<String, String> requestParameters) {

        boolean hasLocationSet = false;

        String locationWithoutUUID = null;
        String pathWithoutUUID = null;

        String uuid;
        if (location != null && !location.isEmpty()) {
            hasLocationSet = true;
            uuid = Utils.getUUIDFromPath(location, data.getAttrs());
            if (uuid != null) {
                locationWithoutUUID = location.replace(uuid, "");
            } else {
                locationWithoutUUID = location;
            }

        } else {
            uuid = Utils.getUUIDFromPath(path, data.getAttrs());
            if (uuid != null) {
                pathWithoutUUID = path.replace(uuid, "");
            } else {
                pathWithoutUUID = path;
            }
        }

        if (hasLocationSet) {
            categoryId = Utils.getCategoryFilterSchemeTerm(locationWithoutUUID, ConfigurationManager.DEFAULT_OWNER);
            if (categoryId == null) {
                // For mixin tag location for example: /mymixin/mymixintag/
                Mixin mixin = ConfigurationManager.getUserMixinFromLocation(locationWithoutUUID, ConfigurationManager.DEFAULT_OWNER);
                if (mixin != null) {
                    categoryId = mixin.getScheme() + mixin.getTerm();
                }
            }


        } else {

            categoryId = Utils.getCategoryFilterSchemeTerm(pathWithoutUUID, ConfigurationManager.DEFAULT_OWNER);
            if (categoryId == null) {
                // For mixin tag location for example: /mymixin/mymixintag/
                Mixin mixin = ConfigurationManager.getUserMixinFromLocation(pathWithoutUUID, ConfigurationManager.DEFAULT_OWNER);
                if (mixin != null) {
                    categoryId = mixin.getScheme() + mixin.getTerm();
                }
            }
        }

        // Determine if this is an action invocation.
        if (data.getAction() != null && !data.getAction().isEmpty() || (requestParameters != null && requestParameters.get("action") != null)) {
            actionInvocationQuery = true;
        }

        String pathTmp = "/" + path + "/";
        // Check if interface query.
        if (pathTmp.equals("/.well-known/org/ogf/occi/-/") || pathTmp.endsWith("/-/")) {
            if (hasLocationSet && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
                mixinTagDefinitionRequest = true;
            } else {
                interfQuery = true;
            }
        }

        // Is the path is an entity path ?
        //  if a location is defined, this replace the given path.
        if (hasLocationSet) {
            // the attributes is used if occi.core.id is defined for the current data to work with.
            entityQuery = Utils.isEntityUUIDProvided(location, data.getAttrs());
        } else {
            entityQuery = Utils.isEntityUUIDProvided(path, data.getAttrs());
        }

        if (!entityQuery) {
            entityQuery = data.getEntityUUID() != null;
            if (!entityQuery) {
                boolean pathHasEntitiesBehind;
                // Check if location has entities behind.
                List<String> uuids;
                if (hasLocationSet) {
                    uuids = Utils.getEntityUUIDsFromPath(location);

                } else {
                    uuids = Utils.getEntityUUIDsFromPath(path);
                }

                // Check if a kind is defined in inputdata, if this is the case, it must be an entity query.
                if (categoryId == null && data.getKind() != null && !uuids.isEmpty()) {
                    entityQuery = true;
                }
            }
        }

        if (location != null && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
            mixinTagDefinitionRequest = true;
        }
        if (!entityQuery && !interfQuery && !mixinTagDefinitionRequest) {
            collectionQuery = true;

            // Check if custom query (bounded path).
            if (categoryId == null) {
                collectionCustomPath = true;
            } else {
                collectionOnCategory = true;
            }
        }
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
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

    public boolean isMixinTagDefinitionRequest() {
        return mixinTagDefinitionRequest;
    }

    public boolean isEntityQuery() {
        return entityQuery;
    }

    public boolean isCollectionQuery() {
        return collectionQuery;
    }

    public boolean isCollectionOnCategory() {
        return collectionOnCategory;
    }

    public boolean isCollectionCustomPath() {
        return collectionCustomPath;
    }

    public boolean isInterfQuery() {
        return interfQuery;
    }

    public boolean isActionInvocationQuery() {
        return actionInvocationQuery;
    }

    public String getCategoryId() {
        return categoryId;
    }

}
