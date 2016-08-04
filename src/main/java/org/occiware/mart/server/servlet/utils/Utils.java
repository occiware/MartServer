package org.occiware.mart.server.servlet.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.occiware.clouddesigner.occi.Action;
import org.occiware.clouddesigner.occi.AttributeState;
import org.occiware.clouddesigner.occi.Entity;
import org.occiware.clouddesigner.occi.Link;
import org.occiware.clouddesigner.occi.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for rest queries.
 * @author cgourdin
 */
public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static final String REGEX_CONTROL_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    
    
    /**
     * Client OCCI user agent version control.
     * @param headers
     * @return null if no response, not null if failed response.
     */
    public static Response checkClientOCCIVersion(HttpHeaders headers) {
        boolean result = true;
        List<String> vals = getFromValueFromHeaders(headers, Constants.HEADER_USER_AGENT);
        for (String val : vals) {
            if (val.contains("OCCI/") || val.contains("occi/")) {
                // Check if version is compliant with supported occi version 1.2 currently.
                if (!val.contains(Constants.OCCI_SERVER_VERSION)) {
                    // Check if client version is > 1.2.
                    int index = val.indexOf("OCCI/") + 5;
                    String versionStr = val.substring(index);
                    if (!versionStr.isEmpty()) {
                        
                        // Check version number.
                        try {
                            Float version = Float.valueOf(versionStr);
                            if (version > Constants.OCCI_SERVER_VERSION_NUMBER) {
                                result = false;
                            }
                        } catch (NumberFormatException ex) {
                            // Version is unparseable.
                        }
                        
                        
                    }
                    
                    
                }
            } 
        }
        if (!result) {
            System.out.println("Version is not compliant, max: OCCI v1.2");
            return Response
                    .status(Response.Status.NOT_IMPLEMENTED).entity("The requested version is not implemented.").build();
        }
        return null;
    }
    /**
     * Get a list of values for a header key.
     * @param headers
     * @param key
     * @return 
     */
    public static List<String> getFromValueFromHeaders(HttpHeaders headers, String key) {
        MultivaluedMap<String, String> headersVal = headers.getRequestHeaders();
        List<String> vals = headersVal.get(key);
        if (vals == null) {
            vals = new ArrayList<>();
        }
        return vals;
    }

    public static String createUUID() {
        return UUID.randomUUID().toString();

    }

    /**
     * Simple copy a stream with a buffer of 1024 bytes into an outputstream.
     *
     * @param in
     * @param os
     * @return a String representation of copied bytes, null if outputstream is
     * not a ByteArrayOutputStream.
     * @throws IOException
     *
     */
    public static String copyStream(InputStream in, OutputStream os) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        os.flush();
        if (os instanceof ByteArrayOutputStream) {
            return new String(((ByteArrayOutputStream) os).toByteArray(), "UTF-8");
        }
        return null;
    }

    /**
     * Close quietly an inputstream without exception thrown.
     *
     * @param in
     */
    public static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }
    
    public static void closeQuietly(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
    }

    public static void closeQuietly(Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException e) {
                /* ignore */
            }
        }
    }
    
    
    /**
     * Close quietly an outputstream without exception thrown.
     *
     * @param os
     */
    public static void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }

    /**
     * Serialize an object to make an MD5 hash after call getMd5Digest Method.
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static byte[] serialize(Object obj) throws IOException {
        byte[] byteArray = null;
        ByteArrayOutputStream baos;
        ObjectOutputStream out = null;
        try {
            // These objects are closed in the finally.
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            byteArray = baos.toByteArray();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return byteArray;
    }

    /**
     * Create a MD5 hash.
     *
     * @param bytes (array of bytes).
     * @return
     */
    public static String getMd5Digest(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "1";
            // throw new RuntimeException("MD5 cryptographic algorithm is not
            // available.", e);
        }
        byte[] messageDigest = md.digest(bytes);
        BigInteger number = new BigInteger(1, messageDigest);
        // prepend a zero to get a "proper" MD5 hash value
        StringBuilder sb = new StringBuilder().append('0');
        sb.append(number.toString(16));
        return sb.toString();
    }

    /**
     * Create an eTag (Serial number, serialize an object) for dbus interaction.
     *
     * @param obj
     * @return an eTag number.
     */
    public static Long createEtagNumber(Object obj) {
        String eTag;
        try {
            eTag = getMd5Digest(serialize(obj));
        } catch (IOException ioe) {
            LOGGER.warn("IOException thrown : {0}", ioe.getMessage());
            eTag = "1";
        }

        StringBuilder sb = new StringBuilder();
        for (char c : eTag.toCharArray()) {
            sb.append((int) c);
        }
        return new Long(sb.toString());
    }

    /**
     * Serialize a string (entity id for example with an owner)
     *
     * @param id
     * @param owner
     * @param version (version number, will increment with each update on this
     * object).
     * @return
     */
    public static Long createEtagNumber(final String id, final String owner, final int version) {
        String eTag;
        if (id == null) {
            eTag = "1";
        } else {
            try {
                eTag = getMd5Digest(serialize(id + owner + version));
            } catch (IOException ioe) {
                LOGGER.warn("IOException thrown : {0}", ioe.getMessage());
                eTag = "1";
            }
        }
        StringBuilder sb = new StringBuilder();

        for (char c : eTag.toCharArray()) {
            sb.append((int) c);
        }
        String result = sb.toString().substring(0, 7);

        return new Long(result);
    }

    /**
     * Check if an UUID is provided on a String or attribute occi.core.id.
     *
     * @param id
     * @param attr
     * @return true if provided or false if not provided
     */
    public static boolean isEntityUUIDProvided(final String id, final Map<String, String> attr) {
        String[] uuids = id.split("/");
        boolean match = false;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                match = true;
                break;
            }
        }
        String occiCoreId = attr.get("occi.core.id");
        if (!match && occiCoreId != null && !occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }

        }

        return match;
    }

    /**
     * Search for UUID on a String or attribute occi.core.id.
     *
     * @param id
     * @param attr
     * @return the UUID provided may return null if uuid not found.
     */
    public static String getUUIDFromId(final String id, final Map<String, String> attr) {
        String[] uuids = id.split("/");
        String uuidToReturn = null;
        boolean match = false;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                uuidToReturn = uuid;
                break;
            }
        }
        String occiCoreId = attr.get("occi.core.id");
        if (!match && occiCoreId != null && !occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        uuidToReturn = uuid;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }

        }

        return uuidToReturn;
    }

    /**
     * Return a relative path from an full entityId with uuid provided.
     *
     * @param id
     * @param uuid
     * @return
     */
    public static String getRelativePathFromId(final String id, final String uuid) {

        String relativePathPart = "";

        relativePathPart = id.replace(uuid, "");
        if (relativePathPart.endsWith("/")) {
            relativePathPart = relativePathPart.substring(0, relativePathPart.length() - 1);
        }

        return relativePathPart;
    }

    /**
     * Helper for converting action attributes parameters in array.
     *
     * @param actionAttributes
     * @return parameters for an action null if none.
     */
    public static String[] getActionParametersArray(Map<String, String> actionAttributes) {
        String[] actionParameters = null;
        if (actionAttributes != null && !actionAttributes.isEmpty()) {
            actionParameters = new String[actionAttributes.size()];
            String key;
            String value;
            int index = 0;
            for (Map.Entry<String, String> entry : actionAttributes.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                actionParameters[index] = value;
                index++;
            }
        }

        return actionParameters;
    }

    /**
     * Print on logger an entity.
     *
     * @param entity
     */
    public static void printEntity(Entity entity) {

        StringBuilder builder = new StringBuilder("");
        if (entity instanceof Resource) {
            builder.append("Entity is a resource. \n");
        }
        if (entity instanceof Link) {
            builder.append("Entity is a link.\n");
        }
        builder.append("id : ").append(entity.getId()).append(" \n");
        builder.append("kind : ").append(entity.getKind().getScheme()).append(entity.getKind().getTerm()).append(" \n ");
        if (!entity.getMixins().isEmpty()) {
            builder.append("mixins : ").append(entity.getMixins().toString()).append(" \n ");
        } else {
            builder.append("entity has no mixins" + " \n ");
        }
        builder.append("Entity attributes : " + " \n ");
        if (entity.getAttributes().isEmpty()) {
            builder.append("no attributes found." + " \n ");
        }
        for (AttributeState attribute : entity.getAttributes()) {
            builder.append("--> name : ").append(attribute.getName()).append(" \n ");
            builder.append("-- value : ").append(attribute.getValue()).append(" \n ");
        }
        if (entity.getKind().getActions().isEmpty()) {
            builder.append("entity has no action \n ");
        } else {
            builder.append("entity has actions available : \n ");
            for (Action action : entity.getKind().getActions()) {
                builder.append(action.getTitle()).append("--> ").append(action.getScheme()).append(action.getTerm()).append(" \n ");
            }
        }
        LOGGER.info(builder.toString());

    }

    private static int uniqueInt = 1;

    public static synchronized int getUniqueInt() {
        return uniqueInt++;
    }
    
    /**
     * Get the kind on header, for text/occi.
     * @param headers
     * @return 
     */
    public static String getKindFromHeader(HttpHeaders headers) {
        String kind = null;
        
        List<String> kindsVal = getFromValueFromHeaders(headers, Constants.CATEGORY);
        // Search for Class="kind" value.
        String[] vals;
        boolean kindVal;
        for (String line : kindsVal) {
            kindVal = false;
            vals = line.split(";");
            // Check class="kind".
            for (String val : vals) {
                if (val.contains("class=\"" + Constants.CLASS_KIND + "\"")) {
                    kindVal = true;
                }
            }
            if (kindVal) {
                // Get the kind value.
                for (String val : vals) {
                    if (val.contains(Constants.CATEGORY)) {
                        String category = val.trim();
                        
                        // Get the value.
                        kind = category.split(":")[1];
                        LOGGER.info("Kind value is : " + kind);
                    }
                }
            }
        }
        return kind;
    }
    
    
}
