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
package org.occiware.mart.server.servlet.facade;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;

/**
 *
 * @author Christophe Gourdin
 */
public interface IPutQuery extends IEntryPoint {
    
    /**
     * Create a new entity.
     * @param path
     * @param entityId
     * @param kind
     * @param mixins
     * @param attributes
     * @return 
     */
    public Response createEntity(final String path, String entityId, final String kind, final List<String> mixins, final Map<String, String> attributes);
    
    
    /**
     * Define a mixin tag on a location
     * @param data
     * @return 
     */
    public Response defineMixinTag(final InputData data);
}
