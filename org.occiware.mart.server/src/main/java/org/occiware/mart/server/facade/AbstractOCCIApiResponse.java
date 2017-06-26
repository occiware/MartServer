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
import org.occiware.mart.server.parser.DefaultParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgourdin on 25/04/2017.
 */
public abstract class AbstractOCCIApiResponse implements OCCIApiResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOCCIApiResponse.class);
    private String username;
    /**
     * Main response message. Used principally for HTTP to render String output (html, json, text etc.).
     */
    private Object response = null;
    private String exceptionMessage = null;
    private Exception exceptionThrown = null;
    private IRequestParser outputParser;

    /**
     * @param username     if null, default to "anonymous" user.
     * @param outputParser if null, use default parser for rendering output.
     */
    public AbstractOCCIApiResponse(String username, IRequestParser outputParser) {
        if (username == null) {
            username = ConfigurationManager.DEFAULT_OWNER;
        }
        if (outputParser == null) {
            outputParser = new DefaultParser(username);
        }
        this.username = username;
        this.outputParser = outputParser;
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
        return this.exceptionThrown;
    }

    @Override
    public void setExceptionThrown(Exception ex) {
        this.exceptionThrown = ex;
    }

    @Override
    public Object getResponseMessage() {
        return response;
    }

    @Override
    public void setResponseMessage(Object responseMessage) {
        this.response = responseMessage;
    }

    @Override
    public void parseResponseMessage(final String message) {
        try {
            setResponseMessage(outputParser.parseMessage(message));

        } catch (ParseOCCIException ex) {
            LOGGER.warn("Parsing message failed : " + ex.getMessage());
            this.response = message;
            this.setExceptionMessage(ex.getMessage());
            this.setExceptionThrown(ex);
            this.setResponseMessage(message);
        }
    }

    @Override
    public IRequestParser getOutputParser() {
        if (this.outputParser == null) {
            this.outputParser = new DefaultParser(username);
        }
        return this.outputParser;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }
}
