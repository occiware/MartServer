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

/**
 * Created by cgourdin on 24/04/2017.
 */
public interface OCCIApiResponse {


    /**
     * If the operation launch an exeception, the facade must manage the callback.
     * To be implemented in an abstract method. Not the final one.
     *
     * @return
     */
    public boolean hasExceptions();


    /**
     * If an exception is set on this object, the facade may use this callback.
     * To be implemented in an abstract method. Not the final one.
     *
     * @return
     */
    public String getExceptionMessage();


    public void setExceptionMessage(final String message);

    public Exception getExceptionThrown();

    public void setExceptionThrown(final Exception ex);

    /**
     * @return a parsed response message.
     */
    public Object getResponseMessage();

    /**
     * May be string or other object type, depends on implementation.
     *
     * @param responseMessage
     */
    public void setResponseMessage(Object responseMessage);

    /**
     * Parse a response message, used when an exception is thrown or to say "ok" to output message.
     *
     * @param message a String simple message to be parsed on output message object.
     */
    public void parseResponseMessage(String message);

    public IRequestParser getOutputParser();

    public String getUsername();

    public void setUsername(final String username);


}