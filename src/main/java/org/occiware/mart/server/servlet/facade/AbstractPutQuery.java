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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.occiware.mart.server.servlet.utils.Utils;

/**
 *
 * @author Christophe Gourdin
 */
public abstract class AbstractPutQuery extends AbstractEntryPoint implements IPutQuery {

    @Override
    public abstract Response createEntity(final String path, String entityId, final String kind, final List<String> mixins, final Map<String, String> attributes);

    @Override
    public Response inputQuery(String path, HttpHeaders headers, HttpServletRequest request) {
        Response response = super.inputQuery(path, headers, request);
        
        if (response == null && Utils.isUriListContentTypeUsed(headers)) {
            // We must here return a bad request.
            throw new BadRequestException("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/");
        }
        
        return response;
        
    }

    @Override
    public abstract Response defineMixinTag(String mixinTagKind, String relativeLocationApply);
    
    
    
}
