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
 * Created by christophe on 19/04/2017.
 */
public class DefaultOCCIRequest extends AbstractOCCIApiInputRequest implements OCCIApiInputRequest {

    public DefaultOCCIRequest(String username, OCCIApiResponse occiApiResponse, IRequestParser inputParser) {
        super(username, occiApiResponse, inputParser);
    }

}
