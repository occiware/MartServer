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
package org.occiware.mart.server.servlet.exception;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

/**
 *
 * @author Christophe Gourdin
 */
public class EntityConflictException extends ClientErrorException {

    public EntityConflictException(Response.Status status) {
        super(status);
    }

    public EntityConflictException(String message, Response.Status status) {
        super(message, status);
    }

    public EntityConflictException(int status) {
        super(status);
    }

    public EntityConflictException(String message, int status) {
        super(message, status);
    }

    public EntityConflictException(Response response) {
        super(response);
    }

    public EntityConflictException(String message, Response response) {
        super(message, response);
    }

    public EntityConflictException(Response.Status status, Throwable cause) {
        super(status, cause);
    }

    public EntityConflictException(String message, Response.Status status, Throwable cause) {
        super(message, status, cause);
    }

    public EntityConflictException(int status, Throwable cause) {
        super(status, cause);
    }

    public EntityConflictException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }

    public EntityConflictException(Response response, Throwable cause) {
        super(response, cause);
    }

    public EntityConflictException(String message, Response response, Throwable cause) {
        super(message, response, cause);
    }

    
    
    
    
}
