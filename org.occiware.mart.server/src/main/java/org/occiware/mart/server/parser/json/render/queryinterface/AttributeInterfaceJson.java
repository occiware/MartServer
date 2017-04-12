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
package org.occiware.mart.server.parser.json.render.queryinterface;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.occiware.mart.server.utils.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cgourdin on 30/11/2016.
 */
public class AttributeInterfaceJson {

    private boolean mutable;

    private boolean required;

    private String type = "string";

    private String description = "";

    private Map<String, String> pattern = new HashMap<>();

    /**
     * Can have a default String, Number, boolean type...
     * Be warned that default is a java reserved word, so we use annotation to set the property name manually.
     */
    @JsonProperty("default")
    private Object defaultObj;

    public AttributeInterfaceJson() {
        // Default pattern with string type.
        pattern.put("$schema", Constants.JSON_V4_SCHEMA_IDENTIFIER);
        pattern.put("type", type);
        pattern.put("pattern", '\\' + "S+");
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getPattern() {
        return pattern;
    }

    public void setPattern(Map<String, String> pattern) {
        this.pattern = pattern;
    }

    public Object getDefaultObj() {
        return defaultObj;
    }

    public void setDefaultObj(Object defaultObj) {
        this.defaultObj = defaultObj;
    }

    public void setPatternType(String type) {
        pattern.put("type", type);
    }

    public void setPatternPattern(String pattern) {
        this.pattern.put("pattern", pattern);
    }
}
