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

import org.occiware.mart.server.parser.ContentData;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.QueryInterfaceData;

import java.util.List;

/**
 * Created by cgourdin on 10/04/2017.
 * This interface is the output point to response protocols like http, dbus and others.
 * This must be implemented with delegation on output protocols modules (servlet, dbus, http etc.).
 */
public interface OCCIResponse {

    // Generics.

    public void setContentType(final String contentType);


    public List<ContentData> getContentDatas();

    /**
     * For collections management.
     *
     * @param contentDatas
     */
    public void setContentDatas(final List<ContentData> contentDatas);

    public QueryInterfaceData getQueryInterfaceData();

    public void setQueryInterfaceData(final QueryInterfaceData interfData);

    /**
     * Return a response message to be used by the facade (servlet, dbus, http etc.).
     *
     * @return
     */
    public Object getResponse();

    public void setResponse(Object response);


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
     * Assign a user for all operations with this object. Default is "anonymous".
     */
    public void setUsername(final String username);


    public IRequestParser getOutputParser();

    public void setOutputParser(IRequestParser outputParser);



}
