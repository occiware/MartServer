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
import org.occiware.mart.server.exception.ParseOCCIException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by cgourdin on 25/04/2017.
 */
public abstract class AbstractRequestParser implements IRequestParser {

    private List<OCCIRequestData> inputDatas = new LinkedList<>();
    private QueryInterfaceData interfaceData;


    @Override
    public abstract Object getInterface(QueryInterfaceData interfaceData, String user) throws ParseOCCIException;

    @Override
    public abstract String parseMessage(String message) throws ParseOCCIException;

    @Override
    public abstract void parseInputToDatas(Object contentObj) throws ParseOCCIException;

    @Override
    public abstract Object renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException;

    @Override
    public abstract Object renderOutputEntities(List<Entity> entities) throws ParseOCCIException;

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



}
