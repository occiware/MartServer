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
package org.occiware.mart.server.parser;


import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Mixin;
import org.occiware.clouddesigner.occi.Resource;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.EntityManager;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by cgourdin on 25/04/2017.
 */
public abstract class AbstractRequestParser implements IRequestParser {

    private List<OCCIRequestData> inputDatas = new LinkedList<>();
    private List<OCCIRequestData> outputDatas = new LinkedList<>();
    private String username;
    private QueryInterfaceData interfaceData;
    private URI serverURI;
    public AbstractRequestParser(String username) {
        this.username = username;
    }

    @Override
    public abstract Object getInterface(QueryInterfaceData interfaceData, String user) throws ParseOCCIException;

    @Override
    public abstract String parseMessage(String message) throws ParseOCCIException;

    @Override
    public abstract void parseInputToDatas(Object contentObj) throws ParseOCCIException;

    @Override
    public abstract Object renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException;

    @Override
    public abstract Object renderOutputEntity(Entity entity) throws ParseOCCIException;

    @Override
    public List<OCCIRequestData> getInputDatas() {
        if (inputDatas == null) {
            inputDatas = new LinkedList<>();
        }
        return inputDatas;
    }

    @Override
    public void setInputDatas(List<OCCIRequestData> inputDatas) {
        this.inputDatas = inputDatas;
    }

    @Override
    public List<OCCIRequestData> getOutputDatas() {
        if (outputDatas == null) {
            outputDatas = new LinkedList<>();
        }
        return outputDatas;
    }

    @Override
    public void setOutputDatas(List<OCCIRequestData> outputDatas) {
        this.outputDatas = outputDatas;
    }

    /**
     * Convert an entity model object to a container object.
     *
     * @param entities a list of entity model.
     */
    @Override
    public void convertEntitiesToOutputData(List<Entity> entities) {

        OCCIRequestData data;
        List<Mixin> mixins;
        List<String> mixinsToRender = new LinkedList<>();
        this.outputDatas.clear();
        for (Entity entity : entities) {
            mixins = entity.getMixins();

            data = new OCCIRequestData();
            data.setEntityUUID(entity.getId());
            data.setEntityTitle(entity.getTitle());
            // Add summary to data container if any.
            if (entity instanceof Resource) {
                data.setEntitySummary(((Resource) entity).getSummary());
            }
            data.setAttrs(EntityManager.convertEntityAttributesToMap(entity));
            data.setLocation(EntityManager.getLocation(entity, username));
            data.setKind(entity.getKind().getScheme() + entity.getKind().getTerm());

            if (!entity.getMixins().isEmpty()) {
                for (Mixin mixin : mixins) {
                    // Check if mixin tag.
                    if (mixin.getAttributes().isEmpty()) {
                        // this mixin is a mixin tag.
                        data.setMixinTag(mixin.getScheme() + mixin.getTerm());

                    }
                    mixinsToRender.add(mixin.getScheme() + mixin.getTerm());
                }
                if (!mixinsToRender.isEmpty()) {
                    data.setMixins(mixinsToRender);
                }
            }
            this.outputDatas.add(data);
        }
    }

    /**
     * Convert a list of locations to output datas container object.
     *
     * @param locations a list of locations (entities, etc).
     */
    @Override
    public void convertLocationsToOutputDatas(List<String> locations) {
        OCCIRequestData data;
        this.outputDatas.clear();
        for (String location : locations) {
            data = new OCCIRequestData();
            data.setLocation(location);
            this.outputDatas.add(data);
        }
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public URI getServerURI() {
        return serverURI;
    }
    @Override
    public void setServerURI(URI serverURI) {
        this.serverURI = serverURI;
    }

}
