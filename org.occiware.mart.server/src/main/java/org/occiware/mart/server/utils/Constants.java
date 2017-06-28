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
package org.occiware.mart.server.utils;

import java.util.regex.Pattern;

/**
 * @author cgourdin
 */
public class Constants {

    // Filters and pagination constants.
    public static final int DEFAULT_NUMBER_ITEMS_PER_PAGE = 20;
    public static final String NUMBER_ITEMS_PER_PAGE_KEY = "number";
    public static final String CURRENT_PAGE_KEY = "page";
    public static final int DEFAULT_CURRENT_PAGE = 1;
    public static final int DEFAULT_OPERATOR_VAL = 0; // operator equal by default if 1 : like mode.
    public static final String OPERATOR_KEY = "operator";
    public static final String CATEGORY_KEY = "category";
    public static final String ATTRIBUTE_KEY = "attribute";
    public static final String EXTENSION_NAME_KEY = "extension";
    public static final String VALUE_KEY = "value";


    public static final String PATH_SEPARATOR = "/";
    public static final String CRLF = "\r\n";
    public static final String MEDIA_TYPE_TEXT_OCCI = "text/occi";

    public static final String MEDIA_TYPE_TEXT_URI_LIST = "text/uri-list";
    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String MEDIA_TYPE_JSON_OCCI = "application/occi+json";
    public static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";
    public static final String MEDIA_TYPE_TEXT_PLAIN_OCCI = "text/occi+plain";

    public static final String CATEGORY = "Category";
    public static final String LINK = "Link";
    public static final String X_OCCI_LOCATION = "X-OCCI-Location";

    public static final String URN_UUID_PREFIX = "urn:uuid:";
    public static final String OCCI_CORE_ID = "occi.core.id";
    public static final String OCCI_CORE_TITLE = "occi.core.title";
    public static final String OCCI_CORE_SOURCE = "occi.core.source";
    public static final String OCCI_CORE_TARGET = "occi.core.target";
    public static final String OCCI_CORE_SUMMARY = "occi.core.summary";
    public static final Float OCCI_SERVER_VERSION_NUMBER = 1.2f;
    public static final String OCCI_SERVER_VERSION = "OCCI/1.2";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HEADER_WWW_AUTHENTICATE_BASIC_PARTIAL = "Basic realm=\"";

    public static final String X_OCCI_ATTRIBUTE = "X-OCCI-Attribute";
    public static final String CLASS_ACTION = "action";
    public static final String CLASS_KIND = "kind";
    public static final String CLASS_MIXIN = "mixin";
    //regular expression groups
    public static final String GROUP_TERM = "term";
    public static final String GROUP_SCHEME = "scheme";
    public static final String GROUP_CLASS = "class";
    public static final String GROUP_LOCATION = "location";
    public static final String OCCI_CORE_SCHEME = "http://schemas.ogf.org/occi/core#";
    public static final String JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#";
    private static final String OCCI_SERVER_NAME = "OCCIWare MART Server v1.0";
    public static final String OCCI_SERVER_HEADER = OCCI_SERVER_NAME + " " + OCCI_SERVER_VERSION;
    private static final String GROUP_TITLE = "title";
    private static final String GROUP_REL = "rel";
    private static final String GROUP_ATTRIBUTES = "attributes";
    private static final String GROUP_ACTIONS = "actions";
    private static final String GROUP_URI = "uri";
    private static final String GROUP_SELF = "self";
    private static final String GROUP_CATEGORY = "category";
    //regular expressions
    private static final String REGEXP_LOALPHA = "[a-z]";
    private static final String REGEXP_ALPHA = "[a-zA-Z]";
    private static final String REGEXP_DIGIT = "[0-9]";
    private static final String REGEXP_INT = REGEXP_DIGIT + "+";
    private static final String REGEXP_FLOAT = REGEXP_INT + "\\." + REGEXP_INT;
    private static final String REGEXP_NUMBER = REGEXP_FLOAT + "|" + REGEXP_INT;
    private static final String REGEXP_BOOL = "\\b(?<!\\|)true(?!\\|)\\b|\\b(?<!\\|)false(?!\\|)\\b";
    private static final String REGEXP_QUOTED_STRING = "([^\"\\\\]|\\.)*";
    private static final String REGEXP_URI = "(?x-mi:([a-zA-Z][\\-+.a-zA-Z\\d]*):(?:((?:[\\-_.!~*'()a-zA-Z\\d;?:@&=+$,]|%[a-fA-F\\d]{2})(?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*)|(?:(?:\\/\\/(?:(?:(?:((?:[\\-_.!~*'()a-zA-Z\\d;:&=+$,]|%[a-fA-F\\d]{2})*)@)?(?:((?:(?:[a-zA-Z0-9\\-.]|%[0-9a-fA-F][0-9a-fA-F])+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|\\[(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?:(?:[a-fA-F\\d]{1,4}:)*[a-fA-F\\d]{1,4})?::(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}))?)\\]))(?::(\\d*))?))?|((?:[\\-_.!~*'()a-zA-Z\\d$,;:@&=+]|%[a-fA-F\\d]{2})+))|(?!\\/\\/))(\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*)?)(?:\\?((?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*))?)(?:\\#((?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*))?)";
    private static final String REGEXP_URI_REF = "(?:[a-zA-Z][\\-+.a-zA-Z\\d]*:(?:(?:\\/\\/(?:(?:(?:[\\-_.!~*'()a-zA-Z\\d;:&=+$,]|%[a-fA-F\\d]{2})*@)?(?:(?:[a-zA-Z0-9\\-.]|%[0-9a-fA-F][0-9a-fA-F])+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|\\[(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?:(?:[a-fA-F\\d]{1,4}:)*[a-fA-F\\d]{1,4})?::(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}))?)\\])(?::\\d*)?|(?:[\\-_.!~*'()a-zA-Z\\d$,;:@&=+]|%[a-fA-F\\d]{2})+)(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*)?|\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*)(?:\\?(?:(?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*))?|(?:[\\-_.!~*'()a-zA-Z\\d;?:@&=+$,]|%[a-fA-F\\d]{2})(?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*)|(?:\\/\\/(?:(?:(?:[\\-_.!~*'()a-zA-Z\\d;:&=+$,]|%[a-fA-F\\d]{2})*@)?(?:(?:[a-zA-Z0-9\\-.]|%[0-9a-fA-F][0-9a-fA-F])+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|\\[(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?:(?:[a-fA-F\\d]{1,4}:)*[a-fA-F\\d]{1,4})?::(?:(?:[a-fA-F\\d]{1,4}:)*(?:[a-fA-F\\d]{1,4}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}))?)\\])(?::\\d*)?|(?:[\\-_.!~*'()a-zA-Z\\d$,;:@&=+]|%[a-fA-F\\d]{2})+)(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*)?|\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*|(?:[\\-_.!~*'()a-zA-Z\\d;@&=+$,]|%[a-fA-F\\d]{2})+(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*(?:\\/(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*(?:;(?:[\\-_.!~*'()a-zA-Z\\d:@&=+$,]|%[a-fA-F\\d]{2})*)*)*)?)(?:\\?(?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*)?)?(?:#(?:[\\-_.!~*'()a-zA-Z\\d;\\/?:@&=+$,\\[\\]]|%[a-fA-F\\d]{2})*)?";
    private static final String REGEXP_TERM = "(" + REGEXP_ALPHA + "|" + REGEXP_DIGIT + ")(" + REGEXP_LOALPHA + "|" + REGEXP_DIGIT + "|-|_)*";
    private static final String REGEXP_SCHEME = REGEXP_URI + "#";
    private static final String REGEXP_TYPE_IDENTIFIER = REGEXP_SCHEME + REGEXP_TERM;
    private static final String REGEXP_CLASS = "\\b(?<!\\|)action(?!\\|)\\b|\\b(?<!\\|)mixin(?!\\|)\\b|\\b(?<!\\|)kind(?!\\|)\\b";
    private static final String REGEXP_TYPE_IDENTIFIER_LIST = REGEXP_TYPE_IDENTIFIER + "(\\s+" + REGEXP_TYPE_IDENTIFIER + ")*";
    private static final String REGEXP_ATTRIBUTE_COMPONENT = REGEXP_LOALPHA + "(" + REGEXP_LOALPHA + "|" + REGEXP_DIGIT + "|-|_)*";
    private static final String REGEXP_ATTRIBUTE_NAME = REGEXP_ATTRIBUTE_COMPONENT + "(\\." + REGEXP_ATTRIBUTE_COMPONENT + ")*";
    private static final String REGEXP_ATTRIBUTE_PROPERTIES = "\\{(?:required immutable|immutable required|required|immutable)\\}";
    private static final String REGEXP_ATTRIBUTE_DEF = "(?:" + REGEXP_ATTRIBUTE_NAME + ")(?:" + REGEXP_ATTRIBUTE_PROPERTIES + ")?";
    private static final String REGEXP_ATTRIBUTE_LIST = REGEXP_ATTRIBUTE_DEF + "(\\s+" + REGEXP_ATTRIBUTE_DEF + ")*";
    private static final String REGEXP_ATTRIBUTE_REPR = REGEXP_ATTRIBUTE_NAME + "=(\"" + REGEXP_QUOTED_STRING + "\"|" + REGEXP_NUMBER + "|" + REGEXP_BOOL + ");?";
    private static final String REGEXP_ACTION = REGEXP_TYPE_IDENTIFIER;
    private static final String REGEXP_ACTION_LIST = REGEXP_ACTION + "(\\s+" + REGEXP_ACTION + ")*";
    private static final String REGEXP_RESOURCE_TYPE = REGEXP_TYPE_IDENTIFIER + "(\\s+" + REGEXP_TYPE_IDENTIFIER + ")*";
    private static final String REGEXP_LINK_INSTANCE = REGEXP_URI_REF;
    private static final String REGEXP_LINK_TYPE = REGEXP_TYPE_IDENTIFIER + "(\\s+" + REGEXP_TYPE_IDENTIFIER + ")*";
    private static final String REGEXP_CATEGORY = "(?<" + GROUP_TERM + ">" + REGEXP_TERM + ")" // term (mandatory)
            + ";\\s*scheme=\"(?<" + GROUP_SCHEME + ">" + REGEXP_SCHEME + ")(?:" + REGEXP_TERM + ")?\"" // scheme (mandatory)
            + ";\\s*class=\"?(?<" + GROUP_CLASS + ">" + REGEXP_CLASS + ")\"?" // class (mandatory)
            + "(;\\s*title=\"(?<" + GROUP_TITLE + ">" + REGEXP_QUOTED_STRING + ")\")?" // title (optional)
            + "(;\\s*rel=\"(?<" + GROUP_REL + ">" + REGEXP_TYPE_IDENTIFIER_LIST + ")\")?" // rel (optional)
            + "(;\\s*location=\"(?<" + GROUP_LOCATION + ">" + REGEXP_URI_REF + ")\")?" // location (optional)
            + "(;\\s*attributes=\"(?<" + GROUP_ATTRIBUTES + ">" + REGEXP_ATTRIBUTE_LIST + ")\")?" // attributes (optional)
            + "(;\\s*actions=\"(?<" + GROUP_ACTIONS + ">" + REGEXP_ACTION_LIST + ")\")?" // actions (optional)
            + ";?"; // additional semicolon at the end (not specified, for interoperability)
    public static final Pattern PATTERN_CATEGORY = Pattern.compile("Category: " + REGEXP_CATEGORY);
    private static final String REGEXP_ATTRIBUTES = "(" + REGEXP_ATTRIBUTE_DEF + ")";
    public static final Pattern PATTERN_ATTRIBUTES = Pattern.compile(REGEXP_ATTRIBUTES);
    private static final String REGEXP_LINK = "\\<(?<" + GROUP_URI + ">" + REGEXP_URI_REF + ")\\>" // uri (mandatory)
            + ";\\s*rel=\"(?<" + GROUP_REL + ">" + REGEXP_RESOURCE_TYPE + ")\"" // rel (mandatory)
            + "(;\\s*self=\"(?<" + GROUP_SELF + ">" + REGEXP_LINK_INSTANCE + ")\")?" // self (optional)
            + "(;\\s*category=\"(?<" + GROUP_CATEGORY + ">(;?\\s*(" + REGEXP_LINK_TYPE + "))+)\")?" // category (optional)
            + "(;\\s*(?<" + GROUP_ATTRIBUTES + ">(;?\\s*" + REGEXP_ATTRIBUTE_REPR + ")*))?" // attributes (optional)
            + ";?"; // additional semicolon at the end (not specified, for interoperability)
    public static final Pattern PATTERN_LINK = Pattern.compile(REGEXP_LINK);
    private static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";
    public static final String RESERVED_URI_SAVE_MODEL = "/mart/save/";
    public static final String RESERVED_URI_LOAD_MODEL = "/mart/load/";
    public static final String RESERVED_URI_VALIDATE_MODEL = "/mart/validate/";

}
