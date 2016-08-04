/*
 * Copyright 2016 cgourdin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

/**
 *
 * @author Christophe Gourdin
 */
public class EntityRemoveException extends Exception {

    public EntityRemoveException() {
    }

    public EntityRemoveException(String message) {
        super(message);
    }

    public EntityRemoveException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityRemoveException(Throwable cause) {
        super(cause);
    }
    
}
