package org.occiware.mart.server.utils.tests;
import org.junit.Test;
import org.occiware.mart.server.servlet.utils.Utils;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.junit.Assert.*;
/**
 * Created by Christophe Gourdin on 19/11/2016.
 */
public class UtilsTest {

    @Test
    public void getUUIDFromPathTest() {

        String uuidToTest = "f88486b7-0632-482d-a184-a9195733ddd0";
        String uuidResult;
        Map<String, String> attr = new HashMap<>();
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
