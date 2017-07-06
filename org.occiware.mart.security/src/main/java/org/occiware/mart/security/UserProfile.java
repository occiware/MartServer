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
package org.occiware.mart.security;

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
import org.occiware.mart.server.parser.json.JsonOcciParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cgourdin on 05/07/2017.
 */
public class UserProfile {

    private String username = null;
    private String password = null;

    private static final String PROFILE_CREATE_ENTITY = "c";
    private static final String PROFILE_RETRIEVE_ENTITY = "r";
    private static final String PROFILE_UPDATE_ENTITY = "u";
    private static final String PROFILE_DELETE_ENTITY = "d";
    private static final String PROFILE_ALL_ACTIONS = "a";

    /**
     * True authorize all creation.
     */
    private boolean createEntity = false;
    /**
     * True authorize all delete operation on entities.
     */
    private boolean deleteEntity = false;
    /**
     * True authorize all entity updates.
     */
    private boolean updateEntity = false;
    /**
     * True authorize all retrieve for all entity.
     */
    private boolean retrieveEntity = false;
    /**
     * Authorize all actions if true.
     */
    private boolean allActions = false;

    /**
     * List of the authorized action if allActions is false.
     */
    private List<String> authorizedActions = new ArrayList<>();

    public void loadUserProfile() {
        // TODO : to implement.
    }

    public void saveUserProfile() {
        // TODO : to implement.
    }

    public void updateProfile() {
        // TODO : to implement.
    }

    /**
     * @return a json formatted profile.
     */
    public String toJsonUserProfile() {
        // TODO : to implement...
        return JsonOcciParser.EMPTY_JSON;
    }

    /**
     * This method assign a profile to the current user.
     *
     * @param profile must be never null.
     */
    public void setProfile(final String profile) {

        if (profile == null || profile.isEmpty()) {
            return;
        }

        String[] profileTab = profile.split(",");

        String current;
        for (int i = 0; i < profileTab.length; i++) {
            current = profileTab[i].trim();
            switch (current) {
                case PROFILE_CREATE_ENTITY:
                    setCreateEntity(true);
                    break;
                case PROFILE_RETRIEVE_ENTITY:
                    setRetrieveEntity(true);
                    break;
                case PROFILE_UPDATE_ENTITY:
                    setUpdateEntity(true);
                    break;
                case PROFILE_DELETE_ENTITY:
                    setDeleteEntity(true);
                    break;

                case PROFILE_ALL_ACTIONS:
                    setAllActions(true);

                    break;
                default:
                    if (!current.isEmpty() && !isAllActions()) {
                        // Add to the actions authorized list.
                        authorizedActions.add(current);
                    }
                    break;
            }

        }

    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isCreateEntity() {
        return createEntity;
    }

    public void setCreateEntity(boolean createEntity) {
        this.createEntity = createEntity;
    }

    public boolean isDeleteEntity() {
        return deleteEntity;
    }

    public void setDeleteEntity(boolean deleteEntity) {
        this.deleteEntity = deleteEntity;
    }

    public boolean isUpdateEntity() {
        return updateEntity;
    }

    public void setUpdateEntity(boolean updateEntity) {
        this.updateEntity = updateEntity;
    }

    public boolean isRetrieveEntity() {
        return retrieveEntity;
    }

    public void setRetrieveEntity(boolean retrieveEntity) {
        this.retrieveEntity = retrieveEntity;
    }

    public boolean isAllActions() {
        return allActions;
    }

    public void setAllActions(boolean allActions) {
        this.allActions = allActions;
    }

    public List<String> getAuthorizedActions() {
        if (authorizedActions == null) {
            authorizedActions = new ArrayList<>();
        }
        return authorizedActions;
    }

    public void setAuthorizedActions(List<String> authorizedActions) {
        this.authorizedActions = authorizedActions;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *
     * @param actionTerm
     * @return true if this action is authorized to be executed.
     */
    public boolean isActionAuthorized(final String actionTerm) {
       if (allActions) {
           return true;
       }
       if (actionTerm == null || actionTerm.trim().isEmpty()) {
           return false;
       }
       if (authorizedActions.contains(actionTerm)) {
           // Action is authorized.
           return true;
       } else {
           return false;
       }

    }

}
