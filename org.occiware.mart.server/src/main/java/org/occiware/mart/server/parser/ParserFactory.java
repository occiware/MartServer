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

import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.occiware.mart.server.parser.text.TextOcciParser;
import org.occiware.mart.server.parser.text.TextPlainParser;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cgourdin
 */
public class ParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserFactory.class);

    /**
     * Build parser class to work with.
     *
     * @param parserType (maybe content type or accept type, or other custom type).
     * @return an IRequestParser parser.
     */
    public static IRequestParser build(String parserType, String user) {
        if (user == null) {
            user = ConfigurationManager.DEFAULT_OWNER;
        }
        if (parserType == null) {
            // Default content type if none on headers.
            return new TextOcciParser(user);
        }
        switch (parserType) {
            case Constants.MEDIA_TYPE_TEXT_OCCI:
            case Constants.MEDIA_TYPE_TEXT_URI_LIST:
                LOGGER.info("Parser request: TextOcciParser");
                return new TextOcciParser(user);

            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
                LOGGER.info("Parser request: JsonOcciParser");
                return new JsonOcciParser(user);

            case Constants.MEDIA_TYPE_TEXT_PLAIN:
            case Constants.MEDIA_TYPE_TEXT_PLAIN_OCCI:
                LOGGER.info("Parser request: TextPlainParser");
                return new TextPlainParser(user);

            // You can add here all other parsers you need.

            default:
                // Default content type if unknown.
                LOGGER.warn("Parser request: DefaultParser, warning: contentType/accept is unknown default parser is DefaultParser");
                return new DefaultParser(user);
        }
    }
}