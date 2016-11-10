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
package org.occiware.mart.server.servlet.tests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.Test;
import org.occiware.mart.server.servlet.exception.AttributeParseException;
import org.occiware.mart.server.servlet.exception.CategoryParseException;
import org.occiware.mart.server.servlet.impl.parser.json.JsonOcciParser;
import org.occiware.mart.server.servlet.impl.parser.json.render.ActionJson;
import org.occiware.mart.server.servlet.impl.parser.json.render.OcciMainJson;
import org.occiware.mart.server.servlet.impl.parser.json.utils.InputData;
import org.occiware.mart.server.servlet.impl.parser.json.utils.ValidatorUtils;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Utils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 *
 * @author Christophe Gourdin
 */
public class JsonTest {


    // @Test
    public void testJsonValidator() {

        try {
            // Get the resource.json inputstream.
            File resIn = getJsonResourceOneInput();
            assertNotNull(resIn);
            File schemaIn = getJsonSchemaControl();
            assertNotNull(schemaIn);
            boolean result = ValidatorUtils.isJsonValid(schemaIn, resIn);

            // Utils.closeQuietly(resIn);
            // Utils.closeQuietly(schemaIn);
            if (result) {
                System.out.println("Valid!");
            } else {
                System.out.println("NOT valid!");
            }
            assertTrue(result);
        } catch (ProcessingException | IOException ex) {
            Logger.getLogger(JsonTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);

        }

    }

    private File getJsonSchemaControl() {
        InputStream in = null;
        // File inputSchemaJsonFile = new File(this.getClass().getResource("/jsonschemas/OCCI-schema.json").getFile());
        File inputSchemaJsonFile = new File(this.getClass().getResource("/jsonschemas/OCCI-schema.json").getFile());
        System.out.println(inputSchemaJsonFile.getAbsolutePath());
//        try {
//          in = new FileInputStream(inputSchemaJsonFile);
//        } catch (FileNotFoundException ex) {
//        }
//        try {
//            List<String> lines = IOUtils.readLines(in, "UTF8");
//            for (String line : lines ) {
//                System.out.println("Line: "+ line);
//            }
//        } catch (IOException ex) {
//        }
        assertNotNull(inputSchemaJsonFile);
        return inputSchemaJsonFile;
    }

    private File getJsonResourceOneInput() {
        InputStream in = null;
        File inputJsonFile = new File(this.getClass().getResource("/testjson/integration/creation/resource1.json").getFile());
        System.out.println(inputJsonFile.getAbsolutePath());
//        try {
//            in = new FileInputStream(inputJsonFile);
//        } catch (FileNotFoundException ex) {
//        }
//        
        // in = this.getClass().getResourceAsStream("test/resources/testjson/resources.json");
        assertNotNull(inputJsonFile);
//        try {
//            List<String> lines = IOUtils.readLines(in, "UTF8");
//            for (String line : lines ) {
//                System.out.println("Line: "+ line);
//            }
//        } catch (IOException ex) {
//        }

        return inputJsonFile;
    }

    // @Test
    public void testJsonInterface() {
        JsonOcciParser parser = new JsonOcciParser();
        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);
        Response response = parser.getInterface(null, ConfigurationManager.DEFAULT_OWNER);
        assertNotNull(response);
        assertTrue(response.hasEntity());

        System.out.println(response.getEntity());
    }

    @Test
    public void testJsonInputObject() {
        // load the input stream resources test json file.

        InputStream in = null;
        File resourcesFile = getJsonResourceInput("/testjson/integration/creation/resource1.json");
        File resourcesFileTwo = getJsonResourceInput("/testjson/integration/creation/resource2.json");
        File resourcesFileThree = getJsonResourceInput("/testjson/integration/creation/resource3.json");
        File actionInvocFile = getJsonResourceInput("/testjson/action_invocation.json");

        assertNotNull(resourcesFile);
        try {
            in = new FileInputStream(resourcesFile);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            OcciMainJson occiMain = mapper.readValue(in, OcciMainJson.class);
            assertNotNull(occiMain.getResources());
            Utils.closeQuietly(in);
            // Load resource2.json.
            in = new FileInputStream(resourcesFileTwo);
            OcciMainJson myRes2 = mapper.readValue(in, OcciMainJson.class);
            assertNotNull(myRes2);
            Utils.closeQuietly(in);
            // Load resource3.json
            in = new FileInputStream(resourcesFileThree);
            OcciMainJson myRes3 = mapper.readValue(in, OcciMainJson.class);
            assertNotNull(myRes3);
            Utils.closeQuietly(in);
            in = new FileInputStream(actionInvocFile);
            ActionJson actionInvoc = mapper.readValue(in, ActionJson.class);
            assertNotNull(actionInvoc);

            // Check if method parseMainInput works.
            JsonOcciParser occiParser = new JsonOcciParser();
            occiParser.parseMainInput(myRes3);
            List<InputData> datas = occiParser.getInputDatas();
            assertNotNull(datas);
            assertFalse(datas.isEmpty());

        } catch (CategoryParseException | AttributeParseException | IOException ex) {
            Logger.getLogger(JsonTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);

        } finally {
            Utils.closeQuietly(in);
        }

    }

    private File getJsonResourceInput(String path) {
        InputStream in = null;
        File inputJsonFile = new File(this.getClass().getResource(path).getFile());
        System.out.println(inputJsonFile.getAbsolutePath());
        return inputJsonFile;
    }
}
