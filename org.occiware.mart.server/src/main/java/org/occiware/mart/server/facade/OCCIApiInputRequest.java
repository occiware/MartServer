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

import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.utils.CollectionFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by cgourdin on 24/04/2017.
 */
public interface OCCIApiInputRequest {

    // Methods for operation on OCCI core runtime.

    /**
     * @param categoryFilter  a category scheme+term for filtering interface.
     * @param extensionFilter an extension scheme+name for filtering interface.
     * @return
     */
    public OCCIApiResponse getModelsInterface(final String categoryFilter, final String extensionFilter);


    /**
     * Create a new entity Resource or Link or overwrite it totally if it already exist.
     *
     * @param kind       kind scheme+term.
     * @param mixins     List of mixins and mixin tags.
     * @param attributes Attributes map in String format type.
     * @param location   the location like /mylocation/myentity.
     * @return a response object defined by implementation.
     */
    public OCCIApiResponse createEntity(final String title, final String summary, final String kind, final List<String> mixins,
                                        final Map<String, String> attributes, final String location);

    OCCIApiResponse createEntities(List<OCCIRequestData> datas);

    /**
     * Update an entity (partial update). This take new mixins association but doesnt remove association, to make that use removeMixinAssociations() method.
     *
     *
     * @param title
     * @param summary
     *@param mixins     a list of mixins to associate with entity.
     * @param attributes attributes to update.
     * @param location   an entity location.    @return a response object defined by implementation.
     */
    public OCCIApiResponse updateEntity(String title, String summary, final List<String> mixins, final Map<String, String> attributes, final String location);

    /**
     * Delete an entity with location provided.
     *
     * @param location the location like /mylocation/myentity.
     * @return a response object defined by implementation.
     */
    public OCCIApiResponse deleteEntity(final String location);


    OCCIApiResponse deleteEntities(String location, CollectionFilter filter);

    OCCIApiResponse findEntity(String location);

    /**
     * Location may be /mylocation/myentity or a category collection location like /compute/
     *
     * @param location the location like /mylocation/myentity.
     * @param filter   filter the output entities if this is a collection so may be null if none.
     * @return a response object defined by implementation.
     */
    public OCCIApiResponse findEntities(final String location, CollectionFilter filter);

    /**
     * Find a collection of entities locations.
     *
     * @param location the collection location like /mylocation/myentity.
     * @param filter   filter the output entities if this is a collection so may be null if none.
     * @return a response object defined by implementation, container & parser must have locations entities.
     */
    public OCCIApiResponse findEntitiesLocations(final String location, CollectionFilter filter);

    // Mixin part.

    /**
     * Create a mixin tag and if locations not null and not empty, associate it with entities location.
     *
     * @param title     a title for this mixin tag.
     * @param mixinTag  mixin scheme+term.
     * @param location  mixin tag location.
     * @param locations an optional list of entities location, if set this associate the mixin on these locations.  @return a response object defined by implementation.
     */
    public OCCIApiResponse createMixinTag(final String title, final String mixinTag, String location, final List<String> locations);

    OCCIApiResponse replaceMixinTagCollection(String mixinTag, List<String> locations);

    OCCIApiResponse associateMixinToEntities(String mixin, String mixinTagLocation, List<String> xlocations);

    /**
     * Remove definitively the mixin tag and remove all its association.
     *
     * @param mixinTag Mixin tag scheme + term.
     * @return a response object.
     */
    public OCCIApiResponse deleteMixinTag(final String mixinTag);

    /**
     * Remove mixin association on listed entities locations.
     *
     * @param mixin     the mixin scheme + term.
     * @param locations the location of the entity where to remove the mixin.
     * @return a response object defined by implementation.
     */
    public OCCIApiResponse removeMixinAssociations(final String mixin, final List<String> locations);


    /**
     * Execute an action on the listed entities location with action attributes defined.
     *
     * @param action           the action category scheme+term.
     * @param actionAttributes the action attributes in a map of String, String.
     * @param locations        a
     * @return
     */
    public OCCIApiResponse executeActionOnEntities(final String action, final Map<String, String> actionAttributes, final List<String> locations);


    OCCIApiResponse executeActionOnCategory(String action, Map<String, String> actionsAttrs, String categoryTerm);

    OCCIApiResponse executeActionOnMixinTag(String action, Map<String, String> actionAttrs, String mixinTag);

    // Helper methods.
    boolean isCategoryLocation(String location);

    boolean isCategoryTerm(String categoryTerm);

    Optional<String> getCategorySchemeTerm(String categoryTerm);

    Optional<String> getCategorySchemeTermFromLocation(String location);

    Optional<String> getMixinTagSchemeTermFromLocation(String location);

    boolean isMixinTagLocation(String location);

    boolean isEntityLocation(String location);

    OCCIApiResponse validateInputDataRequest();

    IRequestParser getInputParser();

    void setInputParser(IRequestParser inputParser);

    String createUUID();

    public OCCIApiResponse loadModelFromDisk();

    public OCCIApiResponse saveModelToDisk();

    OCCIApiResponse saveAllModelsToDisk();

    OCCIApiResponse loadAllModelsFromDisk();

    OCCIApiResponse validateModel();

}
