package org.occiware.mart.server.servlet.impl;

import junit.framework.TestCase;
import org.junit.Test;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.model.ConfigurationManager;

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
        InputData data = new InputData();
        PathParser pathParser = new PathParser(data, path);
        pathParser.updateRoutes();
        assertFalse(pathParser.isActionInvocationQuery());
        assertFalse(pathParser.isEntityQuery());
        assertTrue(pathParser.isCollectionQuery());
        assertFalse(pathParser.isInterfQuery());
        assertTrue(pathParser.isCollectionOnCategory());
        assertFalse(pathParser.isCollectionCustomPath());

    }

    private void collectionCustomTest(String path) {
        InputData data = new InputData();
        PathParser pathParser = new PathParser(data, path);
        pathParser.updateRoutes();
        assertFalse(pathParser.isActionInvocationQuery());
        assertFalse(pathParser.isEntityQuery());
        assertTrue(pathParser.isCollectionQuery());
        assertFalse(pathParser.isInterfQuery());
        assertFalse(pathParser.isCollectionOnCategory());
        assertTrue(pathParser.isCollectionCustomPath());
        assertFalse(pathParser.isMixinTagDefinitionRequest());
    }

    private void entityRequestTest(String path, String uuidAttr, String kind) {
        InputData data = new InputData();
        Map<String, Object> attrs = new HashMap<>();

        if (uuidAttr == null) {
            data.setAttrObjects(attrs);
        } else {
            attrs.put("occi.core.id", uuidAttr);
            data.setAttrObjects(attrs);
        }
        if (kind != null) {
            data.setKind(kind);
        }

        PathParser pathParser = new PathParser(data, path);
        pathParser.updateRoutes();
        assertFalse(pathParser.isActionInvocationQuery());
        assertTrue(pathParser.isEntityQuery());
        assertFalse(pathParser.isCollectionQuery());
        assertFalse(pathParser.isInterfQuery());


    }


    private void actionInvocationTest() {
        InputData data = new InputData();
        data.setAction("myactionscheme#term");
        String path = "";
        PathParser pathParser = new PathParser(data, path);
        pathParser.updateRoutes();
        boolean actionInvocation = pathParser.isActionInvocationQuery();
        assertTrue(actionInvocation);

    }


    private void mixinTagDefinitionTest(String path) {
        InputData data = new InputData();
        data.setLocation("/mymixin/mymixin2/");
        data.setMixinTag("myMixinTagScheme#mymixin");
        data.setMixinTagTitle("mymixin title");
        PathParser pathParser = new PathParser(data, path);
        pathParser.updateRoutes();
        boolean mixinTagRes = pathParser.isMixinTagDefinitionRequest();
        assertTrue(mixinTagRes);
        assertFalse(pathParser.isInterfQuery());
    }



}