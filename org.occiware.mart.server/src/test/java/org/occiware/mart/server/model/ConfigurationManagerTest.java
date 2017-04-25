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
package org.occiware.mart.server.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.occiware.clouddesigner.occi.Configuration;

import static org.junit.Assert.*;

/**
 * Created by christophe on 15/04/2017.
 */
public class ConfigurationManagerTest {

    public String username = "christophe";

    @Before
    public void setUp() throws Exception {
        ConfigurationManager.getConfigurationForOwner(username);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetConfigurationForOwner() throws Exception {
        Configuration configuration = ConfigurationManager.getConfigurationForOwner(username);
        assertNotNull(configuration);
    }




}