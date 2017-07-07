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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cgourdin on 05/07/2017.
 */
public class UserProfile {

    private String username = null;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password = null;

    private static final String PROFILE_CREATE_ENTITY = "c";
    private static final String PROFILE_RETRIEVE_ENTITY = "r";
    private static final String PROFILE_UPDATE_ENTITY = "u";
    private static final String PROFILE_DELETE_ENTITY = "d";
    private static final String PROFILE_ALL_ACTIONS = "a";

    private static final String PROFILE_LIST_USERS = "lu";
    private static final String PROFILE_DELETE_USERS = "du";
    private static final String PROFILE_CREATE_USERS = "cu";
    private static final String PROFILE_UPDATE_USERS = "uu";

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

    private boolean updateUser = false;
    private boolean createUser = false;
    private boolean listUser = false;
    private boolean deleteUser = false;


    /**
     * List of the authorized action if allActions is false.
     */
    private List<String> authorizedActions = new ArrayList<>();

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

    public boolean isUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(boolean updateUser) {
        this.updateUser = updateUser;
    }

    public boolean isCreateUser() {
        return createUser;
    }

    public void setCreateUser(boolean createUser) {
        this.createUser = createUser;
    }

    public boolean isListUser() {
        return listUser;
    }

    public void setListUser(boolean listUser) {
        this.listUser = listUser;
    }

    public boolean isDeleteUser() {
        return deleteUser;
    }

    public void setDeleteUser(boolean deleteUser) {
        this.deleteUser = deleteUser;
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
                case PROFILE_CREATE_USERS:
                    setCreateUser(true);
                    break;
                case PROFILE_LIST_USERS:
                    setListUser(true);
                    break;
                case PROFILE_UPDATE_USERS:
                    setUpdateUser(true);
                    break;
                case PROFILE_DELETE_USERS:
                    setDeleteUser(true);
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

    /**
     *
     * @return a profile in string format like : c,r,u,d,a.
     */
    public String profileToString() {
        StringBuilder profile = new StringBuilder();

        if (isCreateEntity()) {
            profile.append(PROFILE_CREATE_ENTITY + ",");
        }
        if (isRetrieveEntity()) {
            profile.append(PROFILE_RETRIEVE_ENTITY + ",");
        }
        if (isUpdateEntity()) {
            profile.append(PROFILE_UPDATE_ENTITY + ",");
        }
        if (isDeleteEntity()) {
            profile.append(PROFILE_DELETE_ENTITY + ",");
        }
        if (isAllActions()) {
            profile.append(PROFILE_ALL_ACTIONS + ",");
        }
        if (isCreateUser()) {
            profile.append(PROFILE_CREATE_USERS + ",");
        }
        if (isUpdateUser()) {
            profile.append(PROFILE_UPDATE_USERS + ",");
        }
        if (isListUser()) {
            profile.append(PROFILE_LIST_USERS + ",");
        }
        if (isDeleteUser()) {
            profile.append(PROFILE_DELETE_USERS + ",");
        }

        if (!authorizedActions.isEmpty()) {
            for (String actionTerm : authorizedActions) {
                profile.append(actionTerm).append(",");
            }
        }
        // Remove end comma.
        if (profile.toString().endsWith(",")) {
            profile = new StringBuilder(profile.substring(0, profile.length() - 1));
        }
        return profile.toString();
    }


    public String toStringJson() throws JsonProcessingException {
        String result;
        String passwordTmp = password;

        // remove all data password.
        this.setPassword(null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        this.setPassword(passwordTmp);
        return result;
    }


}
