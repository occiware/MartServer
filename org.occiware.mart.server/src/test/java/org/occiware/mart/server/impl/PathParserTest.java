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
package org.occiware.mart.server.impl;

import junit.framework.TestCase;
import org.junit.Test;
import org.occiware.mart.server.parser.Data;
import org.occiware.mart.server.model.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christophe on 19/11/2016.
 */
public class PathParserTest extends TestCase {

    @Test
    public void testUpdateRoutes() throws Exception {

        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);

        actionInvocationTest();

        mixinTagDefinitionTest("/-/");
        mixinTagDefinitionTest("mymixins/");

        entityRequestTest("myres/", "c7d55bf4-7057-5113-85c8-141871bf7636", "mykind#term");
        entityRequestTest("myres/c7d55bf4-7057-5113-85c8-141871bf7636", null, "mykind#term");
        entityRequestTest("/c7d55bf4-7057-5113-85c8-141871bf7636/", null, "mykind#term");
        entityRequestTest("", "urn:uuid:c7d55bf4-7057-5113-85c8-141871bf7636", "mykind#term");
        entityRequestTest("", "c7d55bf4-7057-5113-85c8-141871bf7636", "mykind#term");
        // Check if a kind is on attrs but no id.
        // entityRequestTest("/myres/", null, "mykind#term"); // its a collection query on custom path...


        collectionCategoryTest("compute/");
        // this is not working : collectionCategoryTest("compute/c7d55bf4-7057-5113-85c8-141871bf7636");
        // because this is an entity request.
        collectionCategoryTest("/networkinterface/");
        collectionCategoryTest("networkinterface/");
        collectionCategoryTest("ipnetwork");
        // This is not working : collectionCategoryTest("/mycustompath/mypath");
        //  because this is a custom path request.

        collectionCustomTest("/mycustompath/mypath");
        collectionCustomTest("/mycustompath/mypath");
        collectionCustomTest("/my/second/area");
    }

    private void collectionCategoryTest(String path) {
        Data data = new Data();
//        Map<String, String> requestParameters = new HashMap<>();
//        PathParser pathParser = new PathParser(data, path, requestParameters);
//        pathParser.updateRoutes(requestParameters);
//        assertFalse(pathParser.isActionInvocationQuery());
//        assertFalse(pathParser.isEntityQuery());
//        assertTrue(pathParser.isCollectionQuery());
//        assertFalse(pathParser.isInterfQuery());
//        assertTrue(pathParser.isCollectionOnCategory());
//        assertFalse(pathParser.isCollectionCustomPath());

    }

    private void collectionCustomTest(String path) {
        Data data = new Data();
//        Map<String, String> requestParameters = new HashMap<>();
//        PathParser pathParser = new PathParser(data, path, requestParameters);
//        pathParser.updateRoutes(requestParameters);
//        assertFalse(pathParser.isActionInvocationQuery());
//        assertFalse(pathParser.isEntityQuery());
//        assertTrue(pathParser.isCollectionQuery());
//        assertFalse(pathParser.isInterfQuery());
//        assertFalse(pathParser.isCollectionOnCategory());
//        assertTrue(pathParser.isCollectionCustomPath());
//        assertFalse(pathParser.isMixinTagDefinitionRequest());
    }

    private void entityRequestTest(String path, String uuidAttr, String kind) {
        Data data = new Data();
//        Map<String, Object> attrs = new HashMap<>();
//
//        if (uuidAttr == null) {
//            data.setAttrObjects(attrs);
//        } else {
//            attrs.put("occi.core.id", uuidAttr);
//            data.setAttrObjects(attrs);
//        }
//        if (kind != null) {
//            data.setKind(kind);
//        }
//        Map<String, String> requestParameters = new HashMap<>();
//        PathParser pathParser = new PathParser(data, path, requestParameters);
//        pathParser.updateRoutes(requestParameters);
//        assertFalse(pathParser.isActionInvocationQuery());
//        assertTrue(pathParser.isEntityQuery());
//        assertFalse(pathParser.isCollectionQuery());
//        assertFalse(pathParser.isInterfQuery());


    }


    private void actionInvocationTest() {
        Data data = new Data();
//        data.setAction("myactionscheme#term");
//        String path = "";
//        Map<String, String> requestParameters = new HashMap<>();
//        PathParser pathParser = new PathParser(data, path, requestParameters);
//        pathParser.updateRoutes(requestParameters);
//        boolean actionInvocation = pathParser.isActionInvocationQuery();
//        assertTrue(actionInvocation);

    }


    private void mixinTagDefinitionTest(String path) {
        Data data = new Data();
//        data.setLocation("/mymixin/mymixin2/");
//        data.setMixinTag("myMixinTagScheme#mymixin");
//        data.setMixinTagTitle("mymixin title");
//        Map<String, String> requestParameters = new HashMap<>();
//        PathParser pathParser = new PathParser(data, path, requestParameters);
//        pathParser.updateRoutes(requestParameters);
//        boolean mixinTagRes = pathParser.isMixinTagDefinitionRequest();
//        assertTrue(mixinTagRes);
//        assertFalse(pathParser.isInterfQuery());
    }


}