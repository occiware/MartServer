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

import org.occiware.mart.security.exceptions.AuthenticationException;
import org.occiware.mart.server.facade.AppParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cgourdin on 22/06/2017.
 */
public class UserManagement {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateManagement.class);
    private static final String PREFIX_KEY_USERNAME = "user";
    private static final String PREFIX_KEY_PASSWORD = "password";
    private static final String PREFIX_KEY_PROFILE = "profile";
    private static Map<String, UserProfile> userProfileMap = new ConcurrentHashMap<>();

    /**
     * Check a user with a basic authentication (username + password).
     *
     * @param username
     * @param password
     * @return true if user is validated and false if authentication failed.
     * @throws AuthenticationException if a configuration or other errors is thrown (IOException etc.).
     */
    public static boolean checkBasicUserAuthorisation(final String username, final String password) throws AuthenticationException {
        LOGGER.info("Checking user accreditation");

        // Get the user profile if it exist we use it, if not we load it from file or anything else.
        UserProfile user = getUserProfile(username);

        // Get the parameter on mode used.
        AppParameters appParameters = AppParameters.getInstance();
        String userMode = appParameters.getConfig().get(AppParameters.KEY_USERS_MODE);
        if (userMode.equals(AppParameters.USERS_NONE_MODE)) {
            // In this mode, all is authenticated and all is authorized.
            if (user == null) {
                user = new UserProfile();
                user.setUsername(username);
                user.setPassword(password);
                user.setProfile("c,r,u,d,a");
                userProfileMap.put(username, user);
            }
            return true;
        }


        if (user == null && userMode.equals(AppParameters.USERS_FILE_MODE)) {
            // In this mode, the application use a file for profile and authentication.
            // Load the user from file if not found, authentication is failed.
            user = loadUserProfileFromFile(username, appParameters);
        }

        // check the password combination.
        if (user != null && user.getPassword().equals(password)) {
            return true;
        }

        return false;
    }


    public static UserProfile getUserProfile(final String username) {
        return userProfileMap.get(username);
    }

    public static UserProfile loadUserProfileFromFile(final String username, final AppParameters parameters) throws AuthenticationException {

        UserProfile userProfile = new UserProfile();
        String userFile = parameters.getConfig().get(AppParameters.KEY_USERS_FILE_PATH);
        LOGGER.info("Loading user from file : " + userFile);
        // Load the file as it is a property file.
        // Read the properties...
        Properties prop = new Properties();
        InputStream in;
        String message;

        try {
            in = new FileInputStream(new File(userFile));
            prop.load(in);
            try {
                in.close();
            } catch (IOException e) {
                    /* ignore */
            }
            // Find user index.
            Set<String> propertyNames = prop.stringPropertyNames();
            String usernamePropertyKey = null;

            for (String property : propertyNames) {
                if (property.contains(PREFIX_KEY_USERNAME)) {
                    // Check if this is the key for the given username.
                    String value = prop.getProperty(property);
                    if (value.equals(username)) {
                        usernamePropertyKey = property;
                        break;
                    }
                }
            }
            if (usernamePropertyKey == null || usernamePropertyKey.trim().isEmpty()) {
                throw new AuthenticationException("Username : " + username + " doesnt exist.");
            }
            String indexStr = usernamePropertyKey.replace(PREFIX_KEY_USERNAME, "");
            if (indexStr.trim().isEmpty()) {
                message = "Username : " + username + " has no index, check the configuration file to add an index ex: user1 for the property key.";
                LOGGER.warn(message);
                throw new AuthenticationException(message);
            }
            int index;
            try {
                index = Integer.valueOf(indexStr);
            } catch (NumberFormatException ex) {
                message = "User index is not defined as a number; check the configuration file to add index correctly, ex: user1 for the property key.";
                LOGGER.warn(message);
                throw new AuthenticationException(message);
            }
            // Read user informations.
            String password = prop.getProperty(PREFIX_KEY_PASSWORD + index);
            String profile = prop.getProperty(PREFIX_KEY_PROFILE + index);
            if (password == null) {
                message = "No password property defined for username : " + username + " , please update user configuration file with a property like : password1=mypassword";
                LOGGER.warn(message);
                throw new AuthenticationException(message);
            }
            if (profile == null) {
                message = "No profile property defined for username : " + username + ", please update user configuration file with a property like : profile1=c,r,u,d,a";
                LOGGER.warn(message);
                throw new AuthenticationException(message);
            }

            // Set the user profile object and update reference map.
            userProfile.setUsername(username);
            userProfile.setPassword(password);
            userProfile.setProfile(profile);

            // Update reference map.
            userProfileMap.put(username, userProfile);

        } catch (IOException ex) {
            LOGGER.error("Cannot load the file : " + userFile + " , message: " + ex.getMessage());
            throw new AuthenticationException(ex.getMessage(), ex);
        }
        return userProfile;
    }


}