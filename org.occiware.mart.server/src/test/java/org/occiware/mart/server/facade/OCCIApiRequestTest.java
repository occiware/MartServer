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
package org.occiware.mart.server.facade;

import org.junit.Test;
import org.occiware.mart.server.parser.DefaultParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.ParserFactory;
import org.occiware.mart.server.utils.Constants;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * Created by cgourdin on 25/04/2017.
 */
public class OCCIApiRequestTest {

    private String username = "christophe";

    @Test
    public void createEntityTest() {

        // First create entity on "/".
        String location = "/";
        createEntity(location);
    }

    private void createEntity(String location) {
        String kind = "http://schemas.ogf.org/occi/infrastructure#compute";
        IRequestParser parser = new DefaultParser();
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON));
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
        IRequestParser parser = new DefaultParser();
        OCCIApiResponse occiResponse = new DefaultOCCIResponse(username, ParserFactory.build(Constants.MEDIA_TYPE_JSON));
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
        occiRequest.findEntities("/compute/", null);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertFalse(occiResponse.hasExceptions());
        System.out.println("Result : ");
        System.out.println(occiResponse.getResponseMessage().toString());

        // not find..
        System.out.println("Check with a test path, it must not find a resource.");
        occiRequest.findEntities("/test/", null);
        assertNotNull(occiResponse);
        assertNotNull(occiResponse.getResponseMessage());
        assertTrue(occiResponse.hasExceptions());

        System.out.println("response : ");
        System.out.println(occiResponse.getResponseMessage().toString());
        System.out.println("Exception message: " + occiResponse.getExceptionMessage());

    }




}
