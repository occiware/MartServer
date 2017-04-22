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

import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.parser.DummyParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.parser.text.TextOcciParser;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cgourdin on 10/04/2017.
 */
public abstract class AbstractOCCIResponse implements OCCIResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOCCIRequest.class);
    /**
     * Really the acceptType in output (http for example, it may be used for a factory of parsers.).
     */
    protected String contentType;
    /**
     * Collections of contentDatas to render on output.
     */
    private List<ContentData> contentDatas;
    /**
     * Main response message. Used principally for HTTP to render String output (html, json, text etc.).
     */
    private Object response = null;

    private String exceptionMessage = null;

    private Exception exceptionThrown = null;

    private QueryInterfaceData interfData = null;

    private String username = ConfigurationManager.DEFAULT_OWNER;

    private IRequestParser outputParser;


    public AbstractOCCIResponse(String contentType) {
        this.contentType = contentType;
        this.outputParser = buildParser();
    }

    public AbstractOCCIResponse(String contentType, String username) {
        this.contentType = contentType;
        if (username == null) {
            this.username = ConfigurationManager.DEFAULT_OWNER;
        }
        this.username = username;
        this.outputParser = buildParser();
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Object getResponse() {
        return response;
    }

    @Override
    public boolean hasExceptions() {
        return exceptionMessage != null || exceptionThrown != null;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public void setExceptionMessage(String message) {
        this.exceptionMessage = message;
    }

    @Override
    public Exception getExceptionThrown() {
        return exceptionThrown;
    }

    @Override
    public void setExceptionThrown(Exception ex) {
        this.exceptionThrown = ex;
    }

    @Override
    public List<ContentData> getContentDatas() {
        return contentDatas;
    }

    @Override
    public void setContentDatas(List<ContentData> contentDatas) {
        this.contentDatas = contentDatas;
    }

    @Override
    public QueryInterfaceData getQueryInterfaceData() {
        return this.interfData;
    }

    @Override
    public void setQueryInterfaceData(QueryInterfaceData interfData) {
        this.interfData = interfData;
    }


    /**
     * Build output parser.
     *
     * @return an output parser.
     */
    private IRequestParser buildParser() {
        if (contentType == null) {
            // Default content type if none on headers.
            LOGGER.warn("No content type in output, so no parsers available.");
            return new DummyParser(this);
        }
        switch (contentType) {
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                LOGGER.info("Parser request: TextOcciParser");
                return new TextOcciParser(this);

            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
                LOGGER.info("Parser request: JsonOcciParser");
                return new JsonOcciParser(this);
            // You can add here all other parsers you need without updating facade classes.
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                return new DummyParser(this);

            default:
                // No parser, this could be setted via setOutputParser method.
                LOGGER.warn("The parser for " + contentType + " doesnt exist !");
                return new DummyParser(this);
            // throw new ParseOCCIException("The parser for " + contentType + " doesnt exist !");
        }
    }

    @Override
    public IRequestParser getOutputParser() {
        return this.outputParser;
    }

    @Override
    public void setOutputParser(IRequestParser outputParser) {
        this.outputParser = outputParser;
    }
}
