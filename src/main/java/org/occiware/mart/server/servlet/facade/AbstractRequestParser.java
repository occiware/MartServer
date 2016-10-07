/*
 * Copyright 2016 cgourdin.
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
package org.occiware.mart.server.servlet.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.exception.ResponseParseException;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Constants;

/**
 *
 * @author cgourdin
 */
public abstract class AbstractRequestParser implements IRequestParser {

    private Map<String, String> attrs = new HashMap<>();
    /**
     * Scheme # + term.
     */
    private String kind = null;
    /**
     * Scheme # + term list.
     */
    private List<String> mixins = new ArrayList<>();
    
    /**
     * Action scheme + term.
     */
    private String action = null;
    
    private String entityUUID = null;
    
    // For interface, used in configurations.
    protected List<Kind> kindsConf = null;
    protected List<Mixin> mixinsConf = null;
    
    @Override
    public void parseInputQuery(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException, AttributeParseException {
        // get the kind and mixins from query.
        parseOcciCategories(headers, request);
        // Get the occi attributes defined in query.
        parseOcciAttributes(headers, request);
    }

    @Override
    public abstract void parseOcciCategories(HttpHeaders headers, HttpServletRequest request) throws CategoryParseException;

    @Override
    public abstract void parseOcciAttributes(HttpHeaders headers, HttpServletRequest request) throws AttributeParseException;

    @Override
    public String getKind() {
        return kind;
    }
    
    @Override
    public String getAction() {
        return action;
    }

    @Override
    public List<String> getMixins() {
        if (mixins == null) {
            mixins = new ArrayList<>();
        }
        return mixins;
    }
    
    @Override
    public Map<String, String> getOcciAttributes() {
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        
        return attrs;
    }
    
    @Override
    public abstract Response parseResponse(Object object) throws ResponseParseException;

    @Override
    public abstract Response parseResponse(Object object, Response.Status status) throws ResponseParseException;

    @Override
    public String getEntityUUID() {
        if (!getOcciAttributes().isEmpty()) {
            entityUUID = attrs.get(Constants.OCCI_CORE_ID);
            
            if (entityUUID != null && entityUUID.startsWith(Constants.URN_UUID_PREFIX)) {
                entityUUID = entityUUID.replace(Constants.URN_UUID_PREFIX, "");
            }
        }
        return entityUUID;
    }

    @Override
    public void setOcciAttributes(Map<String, String> attributes) {
        attrs = attributes;
    }

    @Override
    public void setMixins(List<String> mixins) {
        this.mixins = mixins;
    }

    @Override
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public Response getInterface(final String categoryFilter, final String user) {
        // Give all kinds from each extension registered and use by the configuration model of the user.
        kindsConf = ConfigurationManager.getAllConfigurationKind(user);
        // Give all mixins from each extension registered and use by the configuration model of the user.
        mixinsConf = ConfigurationManager.getAllConfigurationMixins(user);
        
        if (categoryFilter != null) {
            Iterator it = kindsConf.iterator();
            Iterator itMix = mixinsConf.iterator();
            List<Action> actions;
            boolean hasActionFilter = false;
            while (it.hasNext()) {
                Kind kindTmp = (Kind) it.next();
                
                // Check the action kind, if action found for this kind, we keep it.
                 actions = kindTmp.getActions();
                for (Action actionTmp : actions) {
                    if (actionTmp.getTerm().equalsIgnoreCase(categoryFilter )) {
                        hasActionFilter = true;
                        break;
                    }
                }
                
                if (!kindTmp.getTerm().equalsIgnoreCase(categoryFilter) && !hasActionFilter) {
                    it.remove();
                }
            }
            while (itMix.hasNext()) {
                Mixin mixinTmp = (Mixin) itMix.next();
                if (!mixinTmp.getTerm().equalsIgnoreCase(categoryFilter)) {
                    itMix.remove();
                }
            }
        }
        
        return null;
    }

    @Override
    public List<Mixin> getMixinsConf() {
        if (mixinsConf == null) {
            mixinsConf = new LinkedList<>();
        }
        return mixinsConf;
    }

    @Override
    public List<Kind> getKindsConf() {
        if (kindsConf == null) {
            kindsConf = new LinkedList<>();
        }
        return kindsConf;
        
    }

    
    
}
