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

import org.eclipse.cmf.occi.core.Entity;
import org.occiware.mart.server.exception.ParseOCCIException;

import java.util.List;

/**
 * Default implementation parser.
 * Created by cgourdin on 25/04/2017.
 */
public class DefaultParser extends AbstractRequestParser implements IRequestParser {

    public DefaultParser(String user) {
        super(user);
    }

    @Override
    public Object getInterface(QueryInterfaceData interfaceData, String user) throws ParseOCCIException {
        return null;
    }

    @Override
    public String parseMessage(String message) throws ParseOCCIException {
        return message;
    }

    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {

    }

    @Override
    public Object renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException {
        return null;
    }

    @Override
    public Object renderOutputEntities(List<Entity> entities) throws ParseOCCIException {
        return null;
    }

    @Override
    public Object renderOutputEntity(Entity entity) throws ParseOCCIException {
        return null;
    }
}
