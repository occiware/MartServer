package org.occiware.mart.security;

import org.occiware.mart.security.exceptions.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgourdin on 22/06/2017.
 */
public class UserManagement {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateManagement.class);

    public static boolean checkBasicUserAuthorisation(final String username, final String password) throws AuthenticationException {
        LOGGER.info("Checking user accreditation");

        // 1 - Check if app parameter is set for basic authentication.
        


        // Get the user from data store.

        // TODO : TO implement with database (or file properties) and profiling.


        return true;
    }


}
