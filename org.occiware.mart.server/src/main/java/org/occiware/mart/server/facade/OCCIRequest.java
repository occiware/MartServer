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
package org.occiware.mart.server.facade;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.parser.Data;
import org.occiware.mart.server.parser.IRequestParser;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by Christophe Gourdin on 10/04/2017.
 * This interface is the input point from input protocols like http, dbus and others.
 * This must be implemented with delegation on input protocols modules (servlet, dbus, http).
 */
public interface OCCIRequest {

    public void setContentType(final String contentType);

    /**
     * Embed OCCIResponse instance to return by operations methods.
     *
     * @param occiResponse
     */
    public void setOCCIResponse(OCCIResponse occiResponse);

    public List<Data> getDatas();

    public void setDatas(final List<Data> datas);

    /**
     * Assign a user for all operations with this object. Default is "anonymous".
     */
    public void setUsername(final String username);

    public String getUsername();


    // CRUD on Configuration model.

    /**
     * Create a new entity.
     *
     * @return an OCCI response object.
     */
    public OCCIResponse createEntity();

    /**
     * Create a collection of entities.
     */
    public OCCIResponse createEntities();


    public OCCIResponse applyMixins();


    /**
     * Execute an action on entity.
     *
     * @return
     */
    public OCCIResponse executeAction();

    /**
     * Execute an action asynchronously in a thread task and get his future object.
     *
     * @return
     */
    public Future<OCCIResponse> executeAsyncAction();

    /**
     * Get an entity from location, location property must be set before calling this method.
     *
     * @return
     */
    public OCCIResponse findEntity();

    /**
     * For entity collections query.
     *
     * @return
     */
    public OCCIResponse findEntities();


    /**
     * Update a collection of entities (this is a partial update).
     *
     * @return
     */
    public OCCIResponse updateEntities();


    /**
     * Get the overall model interface.
     *
     * @return OCCIResponse parsed.
     */
    public OCCIResponse getInterface(final String categoryFilter);

    /**
     * Delete an entity
     *
     * @return OCCIResponse
     */
    public OCCIResponse deleteEntity();

    /**
     * Delete a collection of entities.
     *
     * @return OCCIResponse
     */
    public OCCIResponse deleteEntities();

    /**
     * Remove an associated mixin.
     *
     * @return
     */
    public OCCIResponse removeMixinAssociation();

    /**
     * Parse input query, this must be implemented in facade modules.
     * This method give all datas for operations (CRUD and actions).
     */
    public void parseInput() throws ParseOCCIException;

    /**
     * Validate input request not mandatory for implementations (used for servlet and http generic implementation).
     *
     * @throws ParseOCCIException
     */
    public void validateRequest();

    /**
     * Get the current inputParser, this is built on constructor phase.
     *
     * @return
     */
    public IRequestParser getInputParser();

    /**
     * To force specific implementation of a parser.
     */
    public void setInputParser(IRequestParser inputParser);


    /**
     * Initialize MartServer configuration model engine.
     */
    public static void initMart() {
        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);
    }

}
