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
package org.occiware.mart.servlet.utils;

import org.apache.commons.io.IOUtils;
import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Utility class for rest queries.
 *
 * @author cgourdin
 */
public class ServletUtils {

    private static final String REGEX_CONTROL_UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletUtils.class);
    private static int uniqueInt = 1;

    /**
     * Client OCCI user agent version control.
     *
     * @param headers
     * @return null if no response, not null if failed response.
     */
    public static boolean checkClientOCCIVersion(HeaderPojo headers) {
        boolean result = true;
        String val = headers.getFirst(Constants.HEADER_USER_AGENT);
        if (val != null) {

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

        return result;
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
    public static boolean isEntityUUIDProvided(final String id, final Map<String, Object> attr) {
        String[] uuids = id.split("/");
        boolean match = false;

        for (String uuid : uuids) {
            if (uuid.matches(REGEX_CONTROL_UUID)) {
                match = true;
                break;
            }
        }
        String occiCoreId = (String) attr.get("occi.core.id");
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
    public static String getUUIDFromPath(final String path, final Map<String, Object> attr) {
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
        String occiCoreId = (String)attr.get(Constants.OCCI_CORE_ID);
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

        return null;
    }

    /**
     * Check if the uuid is a valid one.
     *
     * @param uuid
     * @return true if the uuid provided is valid false elsewhere.
     */
    public static boolean isUUIDValid(final String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        return uuid.matches(REGEX_CONTROL_UUID);
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
        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
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
        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
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

        List<Kind> kinds = KindManager.getAllConfigurationKind(user);
        List<Mixin> mixins = MixinManager.getAllConfigurationMixins(user);
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
            mixinTmp = MixinManager.findMixinOnExtension(ConfigurationManager.DEFAULT_OWNER, mixinId);
            if (mixinTmp == null) {
                mixinTmp = MixinManager.findUserMixinOnConfiguration(mixinId, ConfigurationManager.DEFAULT_OWNER);
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

        Map<String, String> entitiesPath = EntityManager.getEntitiesLocation();
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
        Mixin mixin = MixinManager.getUserMixinFromLocation(path, owner);
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
        String categoryId = ServletUtils.getCategoryFilterSchemeTerm(path, ConfigurationManager.DEFAULT_OWNER);

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


    /**
     * Get headers from http javax servlet request.
     * @param request
     * @return
     */
    public static HeaderPojo getRequestHeaders(HttpServletRequest request) {

        HeaderPojo headerPojo;
        Map<String, List<String>> map = new LinkedHashMap<>();

        Enumeration headerNames = request.getHeaderNames();
        List<String> elements = new LinkedList<>();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(key);
            while(values.hasMoreElements()) {
                elements.add(values.nextElement());
            }
            map.put(key, elements);
        }
        headerPojo = new HeaderPojo(map);

        return headerPojo;
    }

    public static URI getServerURI(HttpServletRequest req) {
        String protocol = req.getScheme();
        String hostname = req.getServerName();
        int port = req.getServerPort();
        URI serverURI = null;
        try {
            serverURI = new URI(protocol + "://" + hostname + ":" + port);
        } catch (URISyntaxException ex) {
            LOGGER.error("Cannot parse server URI.");
        }

        return serverURI;
    }





}
