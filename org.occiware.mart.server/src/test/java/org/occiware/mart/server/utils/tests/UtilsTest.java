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
package org.occiware.mart.server.utils.tests;

import org.junit.Test;
import org.occiware.mart.server.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by Christophe Gourdin on 19/11/2016.
 */
public class UtilsTest {

    @Test
    public void getUUIDFromPathTest() {

        String uuidToTest = "f88486b7-0632-482d-a184-a9195733ddd0";
        String uuidResult;
        Map<String, Object> attr = new HashMap<>();
        String path = "/tmp/testuuid/f88486b7-0632-482d-a184-a9195733ddd0";

        uuidResult = Utils.getUUIDFromPath(path, attr);

        assertEquals(uuidToTest, uuidResult);

        path = "/tmp/testuuid/";

        uuidResult = Utils.getUUIDFromPath(path, attr);
        assertNull(uuidResult);

        attr.put("occi.core.id", "urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0");

        uuidResult = Utils.getUUIDFromPath(path, attr);
        assertEquals(uuidToTest, uuidResult);

        attr.put("occi.core.id", "f88486b7-0632-482d-a184-a9195733ddd0");

        uuidResult = Utils.getUUIDFromPath(path, attr);
        assertEquals(uuidToTest, uuidResult);

        attr.put("occi.core.id", "urn:uuid:test/toto/");

        uuidResult = Utils.getUUIDFromPath(path, attr);
        assertNull(uuidResult);
    }


}
