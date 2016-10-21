/*
 * Copyright 2016 Christophe Gourdin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
package org.occiware.mart.server.servlet.impl.parser.json.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.occiware.mart.server.servlet.utils.Constants;

/**
 * Input datas from queries, this container object may has kind, mixins etc.
 * @author Christophe Gourdin
 */
public class InputData {
    private Map<String, String> attrs = new HashMap<>();
    /**
     * Scheme # + term.
     */
    private String kind = null;
    /**
     * Scheme # + term list.
     */
    private List<String> mixins = new ArrayList<>();
    
    private String mixinTagLocation = null;
    /**
     * Action scheme + term.
     */
    private String action = null;
    /**
     * Entity uuid if any.
     */
    private String entityUUID = null;

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, String> attrs) {
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        this.attrs = attrs;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getMixins() {
        return mixins;
    }

    public void setMixins(List<String> mixins) {
        if (mixins == null) {
            mixins = new ArrayList<>();
        }
        this.mixins = mixins;
    }

    public String getMixinTagLocation() {
        return mixinTagLocation;
    }

    public void setMixinTagLocation(String mixinTagLocation) {
        this.mixinTagLocation = mixinTagLocation;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get entity UUID with format urn:uuid:xxxx-xxxx-xxxx-xxxx
     * @return 
     */
    public String getEntityUUID() {
        
        if (!getAttrs().isEmpty()) {
            entityUUID = attrs.get(Constants.OCCI_CORE_ID);

            if (entityUUID != null && entityUUID.startsWith(Constants.URN_UUID_PREFIX)) {
                entityUUID = entityUUID.replace(Constants.URN_UUID_PREFIX, "");
            }
        }
        return entityUUID;
    }

    public void setEntityUUID(String entityUUID) {
        this.entityUUID = entityUUID;
    }
    
    
    
}
