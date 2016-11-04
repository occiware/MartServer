/**
 * Copyright (c) 2015-2017 Inria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package org.occiware.mart.server.servlet.impl.parser;

import org.occiware.mart.server.servlet.impl.parser.text.TextOcciParser;
import org.occiware.mart.server.servlet.impl.parser.json.JsonOcciParser;
import org.occiware.mart.server.servlet.facade.IRequestParser;
import org.occiware.mart.server.servlet.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cgourdin
 */
public class ParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserFactory.class);
    // TODO : Make parser as singleton to avoid memory leak.

    /**
     * Build parser class to work with.
     *
     * @param contentType (maybe content type or accept type).
     * @return an object parser.
     */
    public static IRequestParser build(String contentType) {
        if (contentType == null) {
            // Default content type if none on headers.
            return new TextOcciParser();
        }
        switch (contentType) {
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                LOGGER.info("Parser input request: TextOcciParser");
                return new TextOcciParser();

            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
                LOGGER.info("Parser input request: JsonOcciParser");
                return new JsonOcciParser();
            // You can add here all other parsers you need without updating class like GetQuery, PostQuery etc.

            default:
                // Default content type if unknown.
                LOGGER.info("Parser input request: TextOcciParser, warning: contentType/accept is unknown default parser is TextOcciParser");
                return new TextOcciParser();
        }

    }

}
