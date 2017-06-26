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
