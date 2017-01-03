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
package org.occiware.mart.server.servlet.utils;

import org.apache.commons.io.IOUtils;
import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Utility class for rest queries.
 *
 * @author cgourdin
 */
public class Utils {

    private static final String REGEX_CONTROL_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static int uniqueInt = 1;

    /**
     * Client OCCI user agent version control.
     *
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
     * Check if text/uri-list is used.
     *
     * @param headers
     * @return true if this content-type is an uri-list otherwise false.
     */
    public static boolean isUriListContentTypeUsed(HttpHeaders headers) {
        boolean result = false;
        // Find media type produce as Content-Type: text/uri-list.
        List<String> vals = Utils.getFromValueFromHeaders(headers, Constants.HEADER_CONTENT_TYPE);

        for (String val : vals) {
            if (val.toLowerCase().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Return the Content-type from http header.
     *
     * @param headers
     * @return
     */
    public static String findContentTypeFromHeader(HttpHeaders headers) {
        List<String> vals = Utils.getFromValueFromHeaders(headers, Constants.HEADER_CONTENT_TYPE);
        String contentType = null;
        for (String val : vals) {
            if (val != null && !val.isEmpty()) {
                contentType = val;
                break;
            }
        }
        return contentType;
    }

    /**
     * Return the accept media type from header.
     *
     * @param headers
     * @return
     */
    public static String findAcceptTypeFromHeader(HttpHeaders headers) {
        List<String> vals = Utils.getFromValueFromHeaders(headers, Constants.HEADER_ACCEPT);
        String contentType = null;
        for (String val : vals) {
            if (val != null && !val.isEmpty()) {
                contentType = val;
                break;
            }
        }
        return contentType;
    }

    /**
     * Get a list of values for a header key.
     *
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
                /* ignore */
            }
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
                /* ignore */
            }
        }
    }

    /**
     * Serialize an object to make an MD5 hash after call getMd5Digest Method.
     *
     * @param obj
     * @return
     * @throws IOException
     */
    private static byte[] serialize(Object obj) throws IOException {
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
    private static String getMd5Digest(byte[] bytes) {
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
        return "0" + number.toString(16);
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
     *                object).
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
     * @param id,  an uuid or a path like foo/bar/myuuid
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
     * Search for UUID on a entityId String before attribute occi.core.id.
     *
     * @param path
     * @param attr
     * @return the UUID provided may return null if uuid not found.
     */
    public static String getUUIDFromPath(final String path, final Map<String, String> attr) {
        String[] uuids = path.split("/");
        String uuidToReturn = null;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                uuidToReturn = uuid;
                break;
            }
        }
        if (uuidToReturn != null) {
            return uuidToReturn;
        }

        // Check with occi.core.id attribute.
        String occiCoreId = attr.get(Constants.OCCI_CORE_ID);
        if (occiCoreId == null) {
            return null;
        }
        occiCoreId = occiCoreId.replace(Constants.URN_UUID_PREFIX, "");
        if (!occiCoreId.isEmpty()) {
            String[] spls = {"/", ":"};
            for (String spl : spls) {
                uuids = occiCoreId.split(spl);
                for (String uuid : uuids) {
                    if (uuid.matches(REGEX_CONTROL_UUID)) {
                        return uuid;
                    }
                }
            }
        }

        return uuidToReturn;
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
            String value;
            int index = 0;
            for (Map.Entry<String, String> entry : actionAttributes.entrySet()) {
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

    /**
     * Check if the path contains a category referenced on extensions used by
     * configuration.
     *
     * @param path
     * @param user
     * @return a category term if found on configuration, if not found return
     * null.
     */
    public static String getCategoryFilter(final String path, final String user) {
        List<Kind> kinds = ConfigurationManager.getAllConfigurationKind(user);
        List<Mixin> mixins = ConfigurationManager.getAllConfigurationMixins(user);
        String term;

        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                if (path.contains(term) || path.contains(term.toLowerCase())) {
                    return term;

                }
            }

            term = kind.getTerm();
            if (path.contains(term) || path.contains(term.toLowerCase())) {
                return term;
            }

        }
        for (Mixin mixin : mixins) {
            term = mixin.getTerm();
            if (path.contains(term) || path.contains(term.toLowerCase())) {
                return term;
            }
        }

        return null;
    }

    /**
     * Check if the path equals a category referenced on extensions used by
     * configuration. Remove leading slash and ending slash before proceed.
     *
     * @param path
     * @param user
     * @return a category term if found on configuration, if not found return
     * null.
     */
    public static String getCategoryFilterSchemeTerm(final String path, final String user) {
        List<Kind> kinds = ConfigurationManager.getAllConfigurationKind(user);
        List<Mixin> mixins = ConfigurationManager.getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;

        if (path == null) {
            return null;
        }

        String pathTerm = path;
        if (pathTerm.startsWith("/")) {
            pathTerm = pathTerm.substring(1);
        }
        if (pathTerm.endsWith("/")) {
            pathTerm = pathTerm.substring(0, pathTerm.length() - 1);
        }

        for (Kind kind : kinds) {
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (pathTerm.equals(term) || pathTerm.equals(term.toLowerCase())) {
                    return id;

                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (pathTerm.equals(term) || path.equals(term.toLowerCase())) {
                return id;
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (pathTerm.equals(term) || pathTerm.equals(term.toLowerCase())) {
                return id;
            }
        }

        return null;
    }

    /**
     * Return true if categoryFilter is a scheme + term.
     *
     * @param categoryFilter
     * @param user
     * @return
     */
    public static boolean checkIfCategorySchemeTerm(String categoryFilter, String user) {

        List<Kind> kinds = ConfigurationManager.getAllConfigurationKind(user);
        List<Mixin> mixins = ConfigurationManager.getAllConfigurationMixins(user);
        String term;
        String scheme;
        String id;
        for (Kind kind : kinds) {
            // Check actions.
            for (Action action : kind.getActions()) {
                term = action.getTerm();
                scheme = action.getScheme();
                id = scheme + term;
                if (categoryFilter.equalsIgnoreCase(id)) {
                    return true;

                }
            }

            term = kind.getTerm();
            scheme = kind.getScheme();
            id = scheme + term;
            if (categoryFilter.equalsIgnoreCase(id)) {
                return true;
            }

        }
        for (Mixin mixin : mixins) {

            term = mixin.getTerm();
            scheme = mixin.getScheme();
            id = scheme + term;
            if (categoryFilter.equalsIgnoreCase(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param mixins
     * @param kind
     * @return true if all mixins applied.
     */
    public static boolean checkIfMixinAppliedToKind(List<Mixin> mixins, Kind kind) {
        boolean result = true;
        if (mixins.isEmpty()) {
            return true;
        }
        for (Mixin mixin : mixins) {
            if (!mixin.getApplies().contains(kind)) {
                // one or more mixin doesnt apply to this kind.
                result = false;
            }
        }
        return result;
    }

    /**
     * Load a list of mixin from used extensions models.
     *
     * @param mixins
     * @return
     * @throws ConfigurationException
     */
    public static List<Mixin> loadMixinFromSchemeTerm(List<String> mixins) throws ConfigurationException {
        List<Mixin> mixinModel = new LinkedList<>();

        Mixin mixinTmp;
        for (String mixinId : mixins) {
            mixinTmp = ConfigurationManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, mixinId);
            if (mixinTmp == null) {
                mixinTmp = ConfigurationManager.findUserMixinOnConfiguration(mixinId, ConfigurationManager.DEFAULT_OWNER);
                if (mixinTmp == null) {
                    throw new ConfigurationException("Mixin : " + mixinId + " not found on used extensions models");
                }
            } else {
                mixinModel.add(mixinTmp);
            }
        }
        return mixinModel;
    }

    public static synchronized int getUniqueInt() {
        return uniqueInt++;
    }

    /**
     * Entity request is defined by a known path relative to an entity. This
     * method search for an entity or entities for the path. if entities found
     * for the same path, this is a collection request and not an entity request
     * => return false..
     *
     * @param path
     * @param attrs
     * @return true if the path is an entity request path, false elsewhere.
     */
    public static boolean isEntityRequest(String path, Map<String, String> attrs) {
        if (isEntityUUIDProvided(path, attrs)) {
            return true;
        }

        // this path has no uuid provided, must search on all entities path.
        List<String> entitiesUuid = getEntityUUIDsFromPath(path);

        // This is a collection request or no entities on paths. Other entities are declared on the same path.
        return entitiesUuid.size() == 1;

    }

    /**
     * Get all entities registered on the same path.
     *
     * @param path
     * @return a List of String uuids
     */
    public static List<String> getEntityUUIDsFromPath(final String path) {
        List<String> entitiesUUID = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return entitiesUUID;
        }
        String pathCompare = path;
        if (!path.equals("/") && path.endsWith("/")) {
            // Delete ending slash.
            pathCompare = path.substring(0, path.length() - 1);
        }
        // Remove leading "/".
        if (path.startsWith("/")) {
            pathCompare = path.substring(1);
        }

        Map<String, String> entitiesPath = ConfigurationManager.getEntitiesRelativePath();
        String uuid;
        String pathTmp;
        for (Map.Entry<String, String> entry : entitiesPath.entrySet()) {
            uuid = entry.getKey();
            pathTmp = entry.getValue();

            if (!pathTmp.equals("/") && pathTmp.endsWith("/")) {
                pathTmp = pathTmp.substring(0, pathTmp.length() - 1);
            }
            // Remove leading "/".
            if (pathTmp.startsWith("/")) {
                pathTmp = pathTmp.substring(1);
            }

            if (pathCompare.equals(pathTmp)) {
                entitiesUUID.add(uuid);
            } else if ((pathCompare.startsWith(pathTmp) || pathTmp.startsWith(pathCompare)) && !pathTmp.isEmpty()) {
                entitiesUUID.add(uuid);
            }
        }
        return entitiesUUID;
    }

    /**
     * Check if path is on a mixin tag (mixin without attributes and applied and
     * depends).
     *
     * @param path
     * @param owner
     * @return false if the path and request is not on mixin tag.
     */
    public static boolean isMixinTagRequest(final String path, final String owner) {
        boolean result;
        Mixin mixin = ConfigurationManager.getUserMixinFromLocation(path, owner);
        result = mixin != null;
        return result;
    }

    /**
     * Is that path is on a category ? like compute/
     *
     * @param path
     * @return
     */
    public static boolean isCollectionOnCategory(String path) {
        String categoryId = Utils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);

        return categoryId != null;

    }

    /**
     * Parse a string to a number without knowning its type output.
     *
     * @param str
     * @param instanceClassType can be null.
     * @return a non null number object.
     */
    public static Number parseNumber(String str, String instanceClassType) {
        Number number;
        if (instanceClassType == null) {

            try {
                number = Float.parseFloat(str);

            } catch (NumberFormatException e) {
                try {
                    number = Double.parseDouble(str);
                } catch (NumberFormatException e1) {
                    try {
                        number = Integer.parseInt(str);
                    } catch (NumberFormatException e2) {
                        try {
                            number = Long.parseLong(str);
                        } catch (NumberFormatException e3) {
                            throw e3;
                        }
                    }
                }
            }
        } else {
            switch (instanceClassType) {
                // We know here the instanceClass.

                case "int":
                case "Integer":
                    // Convert to integer.
                    try {
                        number = Integer.parseInt(str);
                    } catch (NumberFormatException ex) {
                        throw ex;
                    }
                    break;
                case "float":
                case "Float":
                    try {
                        number = Float.parseFloat(str);
                    } catch (NumberFormatException ex) {
                        throw ex;
                    }
                    break;
                case "BigDecimal":
                case "double":
                case "Double":
                    try {
                        number = Double.parseDouble(str);
                    } catch (NumberFormatException ex) {
                        throw ex;
                    }

                    break;
                case "Long":
                case "long":
                    try {
                        number = Long.parseLong(str);
                    } catch (NumberFormatException ex) {
                        throw ex;
                    }
                    break;
                default:
                    throw new NumberFormatException("Unknown format.");
            }

        }

        return number;
    }

    /**
     * Convert an input stream to a String object.
     *
     * @param jsonInput
     * @return
     * @throws IOException
     */
    public static String convertInputStreamToString(InputStream jsonInput) throws IOException {
        String contentStr;
        StringBuilder content = new StringBuilder();
        try {
            List<String> lines = IOUtils.readLines(jsonInput, "UTF8");
            for (String line : lines) {
                content.append(line);
            }
//            if (content.toString().isEmpty()) {
//                return content.toString();
//                // throw new IOException("No input text file defined.");
//            }
        } catch (IOException ex) {
            LOGGER.error("This stream is not a text stream.");
            throw new IOException("The input file is not a text file or has unknown characters.");
        }
        contentStr = content.toString();
        return contentStr;
    }

    /**
     * @param path
     * @return
     */
    public static String getPathWithoutPrefixSuffixSlash(final String path) {
        String pathTmp = path;

        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        if (path.startsWith("/")) {
            pathTmp = pathTmp.substring(1);
        }
        if (path.endsWith("/")) {
            pathTmp = pathTmp.substring(0, pathTmp.length() - 1);
        }
        pathTmp.replaceAll("\\s+", "");

        return pathTmp;
    }
}
