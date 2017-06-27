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
package org.occiware.mart.server.integration;

import org.junit.Test;
import org.occiware.mart.server.exception.ApplicationConfigurationException;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.facade.*;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.parser.DefaultParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.ParserFactory;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by cgourdin on 25/04/2017.
 */
public class OCCIApiRequestIT {

    private String username = "christophe";

    @Test
    public void createEntityTest() {

        // First create entity on "/myentity/".
        ConfigurationManager.getConfigurationForOwner(username);

        String location = "/myentity/";
        createEntity(location);

        // Test save config.
        saveConfiguration();

        // Test load configuration.
        loadConfiguration();

    }

    private void saveConfiguration() {
        AppParameters parameters = AppParameters.getInstance();
        try {
            parameters.loadParametersFromConfigFile(null);
        } catch (ApplicationConfigurationException ex) {
            System.err.println("Cannot load application parameters...");
        }

        IRequestParser parser = new DefaultParser(username);
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        OCCIApiInputRequest occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);
        occiRequest.saveModelToDisk();

        if (occiResponse.hasExceptions()) {
            System.out.println("Exception thrown : " + occiResponse.getExceptionMessage());
        }
        assertFalse(occiResponse.hasExceptions());

    }

    private void loadConfiguration() {

        AppParameters parameters = AppParameters.getInstance();
        // parameters are already loaded.
        if (!parameters.isConfigLoaded()) {
            try {
                parameters.loadParametersFromConfigFile(null);
            } catch (ApplicationConfigurationException ex) {
                System.err.println("Cannot load application parameters...");
            }
        }

        IRequestParser parser = new DefaultParser(username);
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        OCCIApiInputRequest occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);

        occiRequest.loadModelFromDisk();

        if (occiResponse.hasExceptions()) {
            System.out.println("Exception thrown : " + occiResponse.getExceptionMessage());
        }
        assertFalse(occiResponse.hasExceptions());

    }


    private void createEntity(String location) {
        String kind = "http://schemas.ogf.org/occi/infrastructure#compute";
        IRequestParser parser = new DefaultParser(username);
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        OCCIApiInputRequest occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);

        Map<String, String> attrs = new HashMap<>();
        List<String> mixins = new LinkedList<>();
        attrs.put("occi.compute.cores", "2");
        attrs.put("occi.compute.memory", "2.0");
        occiRequest.createEntity("entity title", "resource summary", kind, mixins, attrs, location);

        assertNotNull(occiResponse);
        assertFalse(occiResponse.hasExceptions());
        assertNotNull(occiResponse.getResponseMessage());
        assertTrue(occiResponse.getResponseMessage() instanceof String);
        System.out.println(occiResponse.getResponseMessage().toString());
    }

    @Test
    public void findEntityTest() {
        String location = "/";
        IRequestParser parser = new DefaultParser(username);
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        OCCIApiInputRequest occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);

        occiRequest.findEntities(location, null);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertFalse(occiResponse.hasExceptions());
        System.out.println("Result : ");
        System.out.println(occiResponse.getResponseMessage().toString());

        // create a new entity on /myres/
        createEntity("/myres/");


        // Find a collection on category /compute/.
        System.out.println("Find on category location like /compute/");

        CollectionFilter filter = new CollectionFilter();
        filter.setOperator(0);
        filter.setNumberOfItemsPerPage(10);
        filter.setCurrentPage(1);
        filter.setCategoryFilter("compute");
        occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);
        occiRequest.findEntities("/compute/", filter);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertFalse(occiResponse.hasExceptions());
        System.out.println("Result : ");
        System.out.println(occiResponse.getResponseMessage().toString());
        occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);
        // not find..
        System.out.println("Check with a test path, it must not find a resource.");
        filter.setFilterOnEntitiesPath("/test/");
        occiRequest.findEntities("/test/", filter);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertFalse(occiResponse.hasExceptions()); // Must be empty collection.

        System.out.println("response : ");
        System.out.println(occiResponse.getResponseMessage().toString());
        System.out.println("Exception message: " + occiResponse.getExceptionMessage());
        occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);

        System.out.println("With location /compute/ without filter collection.");
        occiRequest.findEntities("/compute/", null);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertFalse(occiResponse.hasExceptions());
        System.out.println("Result : ");
        System.out.println(occiResponse.getResponseMessage().toString());

        occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON, username));
        occiRequest = new DefaultOCCIRequest(username, occiResponse, parser);

        // Find entities without filter set.
        // Must be not found.
        occiRequest.findEntities("/test/", null);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertNotNull(occiResponse.hasExceptions());

        System.out.println("response : ");
        System.out.println(occiResponse.getResponseMessage().toString());
        System.out.println("Exception message: " + occiResponse.getExceptionMessage());


    }


}
