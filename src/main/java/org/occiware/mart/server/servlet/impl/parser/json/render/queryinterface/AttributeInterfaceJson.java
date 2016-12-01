package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.occiware.mart.server.servlet.impl.parser.json.utils.ValidatorUtils;

import java.util.HashMap;
import java.util.Map;

/**
 *
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
        pattern.put("$schema", ValidatorUtils.JSON_V4_SCHEMA_IDENTIFIER);
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
