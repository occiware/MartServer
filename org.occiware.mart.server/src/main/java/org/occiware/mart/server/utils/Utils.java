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
package org.occiware.mart.server.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Utility class for rest queries.
 *
 * @author cgourdin
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static int uniqueInt = 1;

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

    public static synchronized int getUniqueInt() {
        return uniqueInt++;
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
     * Parse a string to a number without knowning its type output.
     *
     * @param str value to convert.
     * @param instanceClass can be null, represent the class type of the value to convert (like Integer etc.)
     * @throws NumberFormatException if the value cannot be converted.
     * @return a non null number object.
     *
     */
    public static Number parseNumber(String str, Class<?> instanceClass) throws NumberFormatException {
        Number number;
        if (instanceClass == null) {

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
                            number = new BigDecimal(str);
                        }
                    }
                }
            }
        } else {
            if (instanceClass == Integer.class) {
                number = Integer.parseInt(str);
            } else if (instanceClass == Long.class) {
                number = Long.parseLong(str);
            } else if (instanceClass == Float.class) {
                number = Float.parseFloat(str);
            } else if (instanceClass == Double.class) {
                number = Double.parseDouble(str);
            } else if (instanceClass == Short.class) {
                number = Short.parseShort(str);
            } else if (instanceClass == Byte.class) {
                number = Byte.parseByte(str);
            } else if (instanceClass == BigDecimal.class) {
                number = new BigDecimal(str);
            } else {
                throw new NumberFormatException("Unknown format.");
            }

        }

        return number;
    }





}
