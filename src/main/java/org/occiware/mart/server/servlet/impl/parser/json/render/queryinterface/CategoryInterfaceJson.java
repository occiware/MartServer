package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cgourdin on 01/12/2016.
 */
public class CategoryInterfaceJson {

    private String term;
    private String scheme;
    private String title;
    /**
     * Attributes field : key: attribute name, value : attribute definition.
     */
    private Map<String, AttributeInterfaceJson> attributes = new HashMap<>();


    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, AttributeInterfaceJson> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, AttributeInterfaceJson> attributes) {
        this.attributes = attributes;
    }

    public String toStringJson() throws JsonProcessingException {
        String result;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        return result;
    }

}
