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
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Entity;

/**
 *
 * @author Christophe Gourdin
 */
public interface IPostQuery extends IEntryPoint {

    /**
     * Update the entity attributes and call occiUpdate().
     *
     * @param path
     * @param entity
     * @return
     */
    public Response updateEntity(String path, Entity entity);

    /**
     * Update the entities attributes and call occiUpdate() for each entity.
     *
     * @param path
     * @param entities
     * @return
     */
    public Response updateEntityCollection(String path, List<Entity> entities);

    /**
     * Update mixin tag association location.
     *
     * @param mixinTagKind
     * @param relativeLocationApply
     * @return
     */
    public Response updateMixinTagAssociation(String mixinTagKind, String relativeLocationApply);

    /**
     * Execute an action on an entity.
     *
     * @param actionKind
     * @param entity
     * @return a response builder to build a response after calling this method.
     */
    public Response executeAction(String action, Entity entity);

    /**
     * Same as executeAction but on a list of entities.
     *
     * @param actionKind
     * @param entity
     * @return
     */
    public Response executeActionsOnEntities(String actionKind, List<Entity> entity);

}
