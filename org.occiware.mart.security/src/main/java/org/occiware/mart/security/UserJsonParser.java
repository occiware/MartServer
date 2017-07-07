package org.occiware.mart.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.occiware.mart.security.exceptions.ParseUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cgourdin on 06/07/2017.
 */
public class UserJsonParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserJsonParser.class);

    /**
     * Render into a list of User profile object from json input content.
     * @param content
     * @return
     * @throws ParseUserException
     */
    public UsersCollection parseInput(final String content) throws ParseUserException {
        String message;
        ObjectMapper mapper = new ObjectMapper();
        // Define those attributes have only non null values.
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        UsersCollection usersCollection;

        if (content == null || content.trim().isEmpty()) {
            return new UsersCollection();
        }
        List<UserProfile> profiles;

        try {
            usersCollection = mapper.readValue(content, UsersCollection.class);
            return usersCollection;
        } catch (IOException ex) {
        }

        try {
            UserProfile userProfile = mapper.readValue(content, UserProfile.class);
            profiles = new ArrayList<>();
            profiles.add(userProfile);
            usersCollection = new UsersCollection();
            usersCollection.setUsers(profiles);
            return usersCollection;
        } catch (IOException ex) {
            message = "Unknown user json content, " + ex.getMessage();
            LOGGER.error(message);
            throw new ParseUserException(message);
        }
    }

    /**
     * Render in json format the user profile.
     * @param usersCollection list of users object.
     * @return
     * @throws ParseUserException
     */
    public String parseOutput(UsersCollection usersCollection) throws ParseUserException {
        String render = "{ }";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (usersCollection == null || usersCollection.getUsers().isEmpty()) {
            return render;
        }
        List<UserProfile> profiles = usersCollection.getUsers();
        try {
            render = usersCollection.toStringJson();
            if (profiles.size() == 1) {
                // One user to render.
                LOGGER.info("One user profile to render.");
                render = profiles.get(0).toStringJson();
            }
        } catch (JsonProcessingException ex) {
            throw new ParseUserException(ex.getMessage(), ex);
        }
        return render;
    }

}
