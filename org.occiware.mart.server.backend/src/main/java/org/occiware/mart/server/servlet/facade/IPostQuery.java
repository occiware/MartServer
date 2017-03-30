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
package org.occiware.mart.server.servlet.facade;

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Christophe Gourdin
 */
interface IPostQuery extends IEntryPoint {

    /**
     * Update the entity attributes and call occiUpdate().
     *
     * @param path
     * @param entity
     * @param data
     * @return
     */
    Response updateEntity(String path, Entity entity, InputData data);

    /**
     * Update the entities attributes and call occiUpdate() for each entity.
     *
     * @param path
     * @param entities
     * @param data
     * @return
     */
    Response updateEntityCollection(String path, List<Entity> entities, InputData data);

    /**
     * Update mixin tag association location.
     *
     * @param mixinTagKind
     * @param relativeLocationApply
     * @return
     */
    Response updateMixinTagAssociation(String mixinTagKind, String relativeLocationApply);

    /**
     * Execute an action on an entity.
     *
     * @param action
     * @param entity
     * @param data
     * @return a response builder to build a response after calling this method.
     */
    Response executeAction(String action, Entity entity, InputData data);


    Response executeActionsOnEntities(String actionKind, List<Entity> entities, InputData data);
}
