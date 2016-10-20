/*
 * Copyright 2016 Christophe Gourdin.
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
package org.occiware.mart.server.servlet.tests;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import static org.junit.Assert.*;
import org.apache.commons.io.IOUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.occiware.mart.server.servlet.impl.parser.json.JsonOcciParser;
import org.occiware.mart.server.servlet.impl.parser.json.utils.ValidatorUtils;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.Utils;
/**
 *
 * @author Christophe Gourdin
 */
public class JsonTest {
    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
    
    
    // @Test
    public void testJsonValidator() {
        
        try {
            // Get the resource.json inputstream.
            File resIn = getJsonResourceInput();
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
    
    public File getJsonSchemaControl() {
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
    
    public File getJsonResourceInput() {
        InputStream in = null;
        File inputJsonFile = new File(this.getClass().getResource("/testjson/actions.json").getFile());
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
    
    @Test
    public void testJsonInterface() {
        JsonOcciParser parser = new JsonOcciParser();
        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);
        Response response = parser.getInterface(null, ConfigurationManager.DEFAULT_OWNER);
        assertNotNull(response);
        assertTrue(response.hasEntity());
        
        System.out.println(response.getEntity());
    }
    
    
}
