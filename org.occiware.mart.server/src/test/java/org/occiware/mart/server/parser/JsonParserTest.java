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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.parser.json.render.ActionJson;
import org.occiware.mart.server.parser.json.render.OcciMainJson;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author Christophe Gourdin
 */
public class JsonParserTest {

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


        } catch (IOException ex) {
            Logger.getLogger(JsonParserTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);

        } finally {
            Utils.closeQuietly(in);
        }

    }


    @Test
    public void testJsonInputParserToDatas() {
        File resourcesFileThree = getJsonResourceInput("/testjson/integration/creation/resource3.json");
        try {
            IRequestParser jsonParser = ParserFactory.build(Constants.MEDIA_TYPE_JSON);
            InputStream in = new FileInputStream(resourcesFileThree);
            String content;
            try {
                content = Utils.convertInputStreamToString(in);

                // for Object occiRequest to be fully completed.
                jsonParser.parseInputToDatas(content);

            } catch (IOException ex) {
                throw new ParseOCCIException("The server cant read the json file input --> " + ex.getMessage());
            } finally {
                Utils.closeQuietly(in);
            }

            // Get the input worker and container data object.
            List<OCCIRequestData> occiRequestDatas = jsonParser.getInputDatas();
            assertNotNull(occiRequestDatas);
            assertFalse(occiRequestDatas.isEmpty());
            for (OCCIRequestData data : occiRequestDatas) {
                data.printDataToOutput();
            }

        } catch (IOException | ParseOCCIException ex) {
            Logger.getLogger(JsonParserTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }

    }


    private File getJsonResourceInput(String path) {
        File inputJsonFile = new File(this.getClass().getResource(path).getFile());
        System.out.println(inputJsonFile.getAbsolutePath());
        return inputJsonFile;
    }
}
