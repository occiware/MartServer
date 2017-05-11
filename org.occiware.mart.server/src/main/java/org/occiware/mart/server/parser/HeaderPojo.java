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
package org.occiware.mart.server.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cgourdin on 12/04/2017.
 * This object gives a map for header rendering (used only with text/occi).
 */
public class HeaderPojo {

    private Map<String, List<String>> headerMap = new LinkedHashMap<>();

    public HeaderPojo(Map<String, List<String>> headerMap) {
        this.headerMap = headerMap;
    }

    public Map<String, List<String>> getHeaderMap() {
        return headerMap;
    }

    public void put(final String key, final String value) {
        if (headerMap.get(key) != null) {
            List<String> values = headerMap.get(key);
            if (!values.isEmpty() && !values.contains(value)) {
                values.add(value);
            }
        }
    }

    public void put(final String key, final List<String> values) {
        if (headerMap.containsKey(key)) {
            for (String value : values) {
                put(key, value);
            }
        } else {
            headerMap.put(key, values);
        }

    }

    /**
     * Get the first value or null if key doesnt exist.
     *
     * @param key
     */
    public String getFirst(final String key) {
        String val = null;
        List<String> values = headerMap.get(key);
        if (values != null && !values.isEmpty()) {
            val = values.get(0);
        }
        return val;
    }

    public List<String> getValues(final String key) {
        return headerMap.get(key);
    }

}
