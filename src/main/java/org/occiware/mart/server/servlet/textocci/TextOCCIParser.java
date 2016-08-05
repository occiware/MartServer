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
package org.occiware.mart.server.servlet.textocci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import org.occiware.mart.server.servlet.utils.Constants;

/**
 * Service Class to parse input headers and build output compliant to text/occi
 * media type.
 *
 * @author cgourdin
 */
public class TextOCCIParser {

    /**
     * Get attribute category type class like a group kind or a mixin. As it is
     * text/occi, Category must be set on the request header.
     *
     * @param headers
     * @param request
     * @return
     */
    public static String getCategoryClassType(HttpHeaders headers, HttpServletRequest request) {
        MultivaluedMap<String, String> map = headers.getRequestHeaders();
        List<String> values;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equals(Constants.CATEGORY)) {
                // Check the class value.
                // check if this is a kind or a mixin. 
                // As it may have kind and mixins, the value return will be a kind class before.
                // Example of value: 
                // compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";
                String temporaryValue = null;

                for (String value : values) {
                    String line = Constants.CATEGORY + ": " + value;
                    Matcher matcher = Constants.PATTERN_CATEGORY.matcher(line);
                    if (!matcher.find()) {
                        continue;
                    }
                    String term = matcher.group(Constants.GROUP_TERM);
                    String scheme = matcher.group(Constants.GROUP_SCHEME);
                    String categoryClass = matcher.group(Constants.GROUP_CLASS);
                    if (categoryClass.equalsIgnoreCase(Constants.CLASS_KIND)) {
                        return Constants.CLASS_KIND;
                    }
                    if (categoryClass.equalsIgnoreCase(Constants.CLASS_MIXIN)) {
                        temporaryValue = Constants.CLASS_MIXIN;
                        continue;
                    }
                    if (categoryClass.equalsIgnoreCase(Constants.CLASS_ACTION)) {
                        return Constants.CLASS_ACTION;
                    }
                }
                if (temporaryValue != null) {
                    return temporaryValue;
                }
            }
        }
        return null;

    }

    /**
     * Get the kind class value of the category in request.
     *
     * @param headers
     * @param request
     * @return
     */
    public static String getKindFromQuery(HttpHeaders headers, HttpServletRequest request) {
        MultivaluedMap<String, String> map = headers.getRequestHeaders();
        List<String> values;
        // Kind like : http://schemas.ogf.org/occi/infrastructure#compute
        String kind = null;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equals(Constants.CATEGORY)) {
                for (String value : values) {
                    String line = Constants.CATEGORY + ": " + value;
                    Matcher matcher = Constants.PATTERN_CATEGORY.matcher(line);
                    if (!matcher.find()) {
                        continue;
                    }
                    String term = matcher.group(Constants.GROUP_TERM);
                    String scheme = matcher.group(Constants.GROUP_SCHEME);
                    String categoryClass = matcher.group(Constants.GROUP_CLASS);
                    
                    System.out.println("term: " + term);
                    System.out.println("scheme: " + scheme);
                    if (categoryClass.equalsIgnoreCase(Constants.CLASS_KIND)) {
                        kind = scheme + term;
                        break;
                    }
                }
            }
        }
        return kind;
    }
    
    
    

    /**
     * Convert X-OCCI-Attribute to map key --> value.
     *
     * @param headers
     * @param request
     * @return an hash map if there are attributes, none if no attributes.
     */
    public static Map<String, String> convertAttributesInQueryToMap(HttpHeaders headers, HttpServletRequest request) {
        Map<String, String> attrs = new HashMap<>();
        MultivaluedMap<String, String> map = headers.getRequestHeaders();
        List<String> values;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equalsIgnoreCase(Constants.X_OCCI_ATTRIBUTE)) {
                for (String value : values) {
                    // Parse the value:
                    String[] attr = value.split("=");
                    if (attr != null && attr.length > 0) {
                        attr[0] = attr[0].replace("\"", "");
                        attr[1] = attr[1].replace("\"", "");
                        attrs.put(attr[0], attr[1]);
                    } 
                    
                }
            }
        }
        return attrs;
    }

    /**
     * Get all mixin (scheme + term) in a list from header.
     * @param headers
     * @param request
     * @return 
     */
    public static List<String> convertMixinsInQueryToList(HttpHeaders headers, HttpServletRequest request) {
        // Add mixins to a list.
        List<String> mixins = new ArrayList<>();
        MultivaluedMap<String, String> map = headers.getRequestHeaders();
        List<String> values;
        // Kind like : http://schemas.ogf.org/occi/infrastructure#compute
        String mixin = null;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equals(Constants.CATEGORY)) {
                for (String value : values) {
                    String line = Constants.CATEGORY + ": " + value;
                    Matcher matcher = Constants.PATTERN_CATEGORY.matcher(line);
                    if (!matcher.find()) {
                        continue;
                    }
                    String term = matcher.group(Constants.GROUP_TERM);
                    String scheme = matcher.group(Constants.GROUP_SCHEME);
                    String categoryClass = matcher.group(Constants.GROUP_CLASS);
                    
                    System.out.println("term: " + term);
                    System.out.println("scheme: " + scheme);
                    if (categoryClass.equalsIgnoreCase(Constants.CLASS_MIXIN)) {
                        mixin = scheme + term;
                        mixins.add(mixin);
                    }
                }
            }
        }
        return mixins;
    }

}
