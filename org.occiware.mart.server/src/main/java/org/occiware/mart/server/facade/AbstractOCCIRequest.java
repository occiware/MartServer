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

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.parser.Data;
import org.occiware.mart.server.parser.DummyParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.parser.text.TextOcciParser;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by cgourdin on 10/04/2017.
 */
public abstract class AbstractOCCIRequest implements OCCIRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOCCIRequest.class);

    protected String contentType;
    private List<Data> datas = new LinkedList<>();
    private OCCIResponse occiResponse;

    private String location = null;

    private String username = ConfigurationManager.DEFAULT_OWNER;

    /**
     * Input parser.
     */
    private IRequestParser inputParser;


    /**
     * @param response
     * @param contentType
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType) {
        this.occiResponse = response;
        this.contentType = contentType;
        this.inputParser = buildParser();

    }

    /**
     * @param response
     * @param contentType
     * @param username
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType, final String username) {
        this.occiResponse = response;
        this.contentType = contentType;
        if (username == null || username.isEmpty()) {
            this.username = ConfigurationManager.DEFAULT_OWNER;
        } else {
            this.username = username;
        }
        this.inputParser = buildParser();

    }

    /**
     * @param response
     * @param contentType
     * @param location
     * @param username
     */
    public AbstractOCCIRequest(OCCIResponse response, final String contentType, final String location, final String username) {
        this.occiResponse = response;
        this.occiResponse.setUsername(username);
        this.contentType = contentType;
        this.location = location;

        if (username == null || username.isEmpty()) {
            this.username = ConfigurationManager.DEFAULT_OWNER;
        } else {
            this.username = username;
        }
        this.inputParser = buildParser();
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setOCCIResponse(OCCIResponse occiResponse) {
        this.occiResponse = occiResponse;
        this.occiResponse.setUsername(username);
    }

    @Override
    public List<Data> getDatas() {
        if (datas == null) {
            datas = new LinkedList<>();
        }
        return datas;
    }

    @Override
    public void setDatas(List<Data> datas) {
        if (datas == null) {
            this.datas = new LinkedList<>();
        } else {
            this.datas = datas;
        }
    }

    @Override
    public OCCIResponse findEntity() {

        for (Data data : datas) {

            Entity entity;
            try {
                if (data.getEntityUUID() == null || data.getEntityUUID().trim().isEmpty()) {
                    // Try to find entity by its location
                    entity = ConfigurationManager.findEntityFromLocation(location);
                } else {
                    // Try to find entity by its id.
                    entity = ConfigurationManager.findEntity(username, location);
                }
                if (entity == null) {
                    throw new ConfigurationException("Entity on location: " + data.getLocation() + " doesnt exist.");
                }
                // Load entity attributes, mixins and kind to output.
                entity.occiRetrieve(); // Refresh values.
                Data outputData = new Data();
                Map<String, Object> attrs = Utils.convertEntityAttributesToMap(entity);
                List<String> mixins = Utils.convertEntityMixinsToList(entity);
                String kind = entity.getKind().getScheme() + entity.getKind().getTerm();
                outputData.setKind(kind);
                outputData.setMixins(mixins);
                outputData.setAttrs(attrs);
                occiResponse.getDatas().add(outputData);

                IRequestParser outputParser = occiResponse.getOutputParser();
                if (outputParser != null) {
                    // Launch the parsing output (for json, text/occi and other implementations).
                    outputParser.renderOutputEntity(entity);
                }

            } catch (ConfigurationException | ParseOCCIException ex) {
                occiResponse.setExceptionMessage(ex.getMessage());
                occiResponse.setExceptionThrown(ex);
            }
            // We take the first one.
            break;
        }
        return this.occiResponse;
    }

    @Override
    public OCCIResponse findEntities() {
        return null;
    }


    @Override
    public OCCIResponse getInterface(final String categoryFilter) {
        QueryInterfaceData interfData = new QueryInterfaceData();

        interfData.setCategoryFilter(categoryFilter);
        try {
            ConfigurationManager.applyFilterOnInterface(categoryFilter, interfData, username);
            occiResponse.setQueryInterfaceData(interfData);
            // Render output.
            occiResponse.getOutputParser().getInterface(username);

        } catch (ConfigurationException | ParseOCCIException ex) {
            occiResponse.setExceptionMessage(ex.getMessage());
            occiResponse.setExceptionThrown(ex);
        }

        return this.occiResponse;
    }

    @Override
    public OCCIResponse createEntity() {
        return null;
    }

    @Override
    public OCCIResponse createEntities() {
        return null;
    }

    @Override
    public OCCIResponse applyMixins() {
        return null;
    }

    @Override
    public OCCIResponse executeAction() {
        return null;
    }

    @Override
    public Future<OCCIResponse> executeAsyncAction() {
        return null;
    }

    @Override
    public OCCIResponse updateEntities() {
        return null;
    }

    @Override
    public OCCIResponse deleteEntity() {
        return null;
    }

    @Override
    public OCCIResponse deleteEntities() {
        return null;
    }

    @Override
    public OCCIResponse removeMixinAssociation() {
        return null;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }


    @Override
    public abstract void parseInput() throws ParseOCCIException;

    @Override
    public abstract void validateRequest() throws ParseOCCIException;


    private IRequestParser buildParser() {
        if (contentType == null) {
            // Default content type if none on headers.
            return new JsonOcciParser(this);
        }
        switch (contentType) {
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                LOGGER.info("Parser request: TextOcciParser");
                return new TextOcciParser(this);

            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
                LOGGER.info("Parser request: JsonOcciParser");
                return new JsonOcciParser(this);
            // You can add here all other parsers you need without updating class like GetQuery, PostQuery etc.
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                // TODO : plain text parser.
                return new DummyParser(this);

            case Constants.MEDIA_TYPE_TEXT_URI_LIST:
                // TODO : test uri list parser and combined parsers (json + uri list for example).
                return new DummyParser(this);

            default:
                // No parser.
                LOGGER.warn("The parser for " + contentType + " doesnt exist !");
                return new DummyParser(this);
            // throw new ParseOCCIException("The parser for " + contentType + " doesnt exist !");
        }

    }

    @Override
    public IRequestParser getInputParser() {
        return this.inputParser;
    }

    @Override
    public void setInputParser(IRequestParser inputParser) {
        this.inputParser = inputParser;
    }

}
