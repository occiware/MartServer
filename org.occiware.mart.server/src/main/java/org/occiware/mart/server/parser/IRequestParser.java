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

import java.net.URI;
import java.util.List;

/**
 * Utility interface for parsing request input and parsing request output.
 *
 * @author cgourdin
 */
public interface IRequestParser {

    /**
     * Get the model interface description.
     *
     * @param interfaceData
     * @param user          (the authorized username)
     * @return an Object, this may be a String (for json or plain/text), this may be a Header Map object (for text/occi) or specific objects if other implementations (sextuple etc.).
     * @throws ParseOCCIException if anything goes wrong (in Model@runtime or in Parser).
     */
    public Object getInterface(QueryInterfaceData interfaceData, final String user) throws ParseOCCIException;

    /**
     * To render message in appropriate format. if there is an error, this method will be used to render a response message.
     *
     * @param message
     * @return
     */
    public String parseMessage(final String message) throws ParseOCCIException;

    /**
     * Parse input request to OCCIRequest datas (this set datas on OCCIRequest object).
     *
     * @param contentObj, may be a String or other objects.
     * @throws ParseOCCIException
     */
    public void parseInputToDatas(final Object contentObj) throws ParseOCCIException;

    public Object renderOutputEntitiesLocations(final List<String> locations) throws ParseOCCIException;

    public Object renderOutputEntities(final List<Entity> entities) throws ParseOCCIException;

    public Object renderOutputEntity(final Entity entity) throws ParseOCCIException;

    List<OCCIRequestData> getInputDatas();

    public void setInputDatas(List<OCCIRequestData> inputDatas);

    public List<OCCIRequestData> getOutputDatas();

    public void setOutputDatas(List<OCCIRequestData> outputDatas);

    public void convertEntitiesToOutputData(List<Entity> entities);

    public void convertLocationsToOutputDatas(List<String> locations);

    public void setUsername(final String username);
    public String getUsername();
    URI getServerURI();
    void setServerURI(URI serverURI);
}
