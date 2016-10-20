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
package org.occiware.mart.server.servlet.impl.parser.json.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
/**
 * This class aims to validate a json schema (from input query and validate
 * response query).
 *
 * @author Christophe Gourdin
 */
public class ValidatorUtils {

    public static final String JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#";
    public static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";

    public static JsonNode getJsonNode(String jsonText) throws IOException {
        return JsonLoader.fromString(jsonText);
    } 

    public static JsonNode getJsonNode(File jsonFile) throws IOException {
        return JsonLoader.fromFile(jsonFile);
    } 

    public static JsonNode getJsonNode(URL url) throws IOException {
        return JsonLoader.fromURL(url);
    }
    
    public static JsonNode getJsonNode(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in);
        return JsonLoader.fromReader(reader);
    }

    public static JsonNode getJsonNodeFromResource(String resource) throws IOException {
        return JsonLoader.fromResource(resource);
    }

    public static JsonSchema getSchemaNode(String schemaText) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaText);
        return _getSchemaNode(schemaNode);
    }

    public static JsonSchema getSchemaNode(File schemaFile) throws IOException, ProcessingException {
//        final JsonNode schemaNode = getJsonNode(schemaFile);
//        return _getSchemaNode(schemaNode);

        return _getSchemaNodeWithFileURI(schemaFile);
    }

    public static JsonSchema getSchemaNode(URL schemaFile) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaFile);
        return _getSchemaNode(schemaNode);
    } 
    
    public static JsonSchema getSchemaNode(InputStream in) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(in);
        return _getSchemaNode(schemaNode);
    }

    public static JsonSchema getSchemaNodeFromResource(String resource)
            throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNodeFromResource(resource);
        return _getSchemaNode(schemaNode);
    }

    public static void validateJson(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        if (!report.isSuccess()) {
            for (ProcessingMessage processingMessage : report) {
                throw new ProcessingException(processingMessage);
            }
        }
    } 

    public static boolean isJsonValid(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        return report.isSuccess();
    }

    public static boolean isJsonValid(String schemaText, String jsonText) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaText);
        final JsonNode jsonNode = getJsonNode(jsonText);
        return isJsonValid(schemaNode, jsonNode);
    } 

    public static boolean isJsonValid(File schemaFile, File jsonFile) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        return isJsonValid(schemaNode, jsonNode);
    }

    public static boolean isJsonValid(URL schemaURL, URL jsonURL) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaURL);
        final JsonNode jsonNode = getJsonNode(jsonURL);
        return isJsonValid(schemaNode, jsonNode);
    }
    
    public static boolean isJsonValid(InputStream schemaStream, InputStream jsonStream) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaStream);
        final JsonNode jsonNode = getJsonNode(jsonStream);
        
        return isJsonValid(schemaNode, jsonNode);
    }
    
    public static boolean isJsonValid(File schemaFile, InputStream jsonStream) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonStream);
        return isJsonValid(schemaNode, jsonNode);
    }
    
    

    public static void validateJson(String schemaText, String jsonText) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaText);
        final JsonNode jsonNode = getJsonNode(jsonText);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJson(File schemaFile, File jsonFile) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJson(URL schemaDocument, URL jsonDocument) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaDocument);
        final JsonNode jsonNode = getJsonNode(jsonDocument);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJsonResource(String schemaResource, String jsonResource) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaResource);
        final JsonNode jsonNode = getJsonNodeFromResource(jsonResource);
        validateJson(schemaNode, jsonNode);
    }

    private static JsonSchema _getSchemaNode(JsonNode jsonNode) throws ProcessingException {
        final JsonNode schemaIdentifier = jsonNode.get(JSON_SCHEMA_IDENTIFIER_ELEMENT);
        if (null == schemaIdentifier) {
            ((ObjectNode) jsonNode).put(JSON_SCHEMA_IDENTIFIER_ELEMENT, JSON_V4_SCHEMA_IDENTIFIER);
        }

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        return factory.getJsonSchema(jsonNode);
    }
    
    
    private static JsonSchema _getSchemaNodeWithFileURI(File schemaFile) throws ProcessingException {
        
        final URI uri = schemaFile.toURI();
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        
        final JsonSchema schema = factory.getJsonSchema(uri.toString());
        return schema;
    }
}
