/** 
 * Copyright 2016 - Christophe Gourdin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.occiware.mart.server.servlet.impl.parser.json.render;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * Class mapping from collection input queries and used to output.
 * For single query like an action invocation, use ActionJson object directly.
 * Same for resource etc.
 * @author christophe
 */
public class OcciMainJson {
    
    private List<ResourceJson> resources = null;
    
    private List<LinkJson> links = null;
    
    private List<KindJson> kinds = null;
    
    private List<MixinJson> mixins = null;
    
    private List<ActionJson> actions = null;

    // For only one action invocation.
    private String action;
    
    private Map<String, Object> attributes;
    
    public List<ResourceJson> getResources() {
        return resources;
    }

    public void setResources(List<ResourceJson> resources) {
        this.resources = resources;
    }

    public List<LinkJson> getLinks() {
        return links;
    }

    public void setLinks(List<LinkJson> links) {
        this.links = links;
    }

    public List<MixinJson> getMixins() {
        return mixins;
    }

    public void setMixins(List<MixinJson> mixins) {
        this.mixins = mixins;
    }

    public List<ActionJson> getActions() {
        return actions;
    }

    public void setActions(List<ActionJson> actions) {
        this.actions = actions;
    }

    public List<KindJson> getKinds() {
        return kinds;
    }

    public void setKinds(List<KindJson> kinds) {
        this.kinds = kinds;
    }
    
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String toStringJson() throws JsonProcessingException {
        String result;
        ObjectMapper mapper = new ObjectMapper();
        result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        return result;
    }
    
    
    
}
