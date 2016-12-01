package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Represents the full interface for all models.
 * Created by cgourdin on 01/12/2016.
 */
public class GlobalModelInterfaceJson {


    /**
     * For each models we have kinds, mixins and actions.
     */
    private List<ModelInterfaceJson> model;

    public List<ModelInterfaceJson> getModel() {
        return model;
    }

    public void setModel(List<ModelInterfaceJson> model) {
        this.model = model;
    }

    public String toStringJson() throws JsonProcessingException {
        String result;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        return result;
    }
}
