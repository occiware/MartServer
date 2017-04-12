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
package org.occiware.mart.servlet.facade;

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.parser.Data;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Christophe Gourdin
 */
public abstract class AbstractPostQuery extends AbstractEntryPoint implements IPostQuery {

    @Override
    public abstract Response executeActionsOnEntities(String actionKind, List<Entity> entity, Data data);

    @Override
    public abstract Response executeAction(String action, Entity entity, Data data);

    @Override
    public abstract Response updateMixinTagAssociation(String mixinTagKind, String relativeLocationApply);

    @Override
    public abstract Response updateEntityCollection(String path, List<Entity> entities, Data data);

    @Override
    public abstract Response updateEntity(String path, Entity entity, Data data);

}
