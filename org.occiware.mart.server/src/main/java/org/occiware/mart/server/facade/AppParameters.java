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

import org.occiware.mart.server.exception.ApplicationConfigurationException;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

/**
 * Created by cgourdin on 26/06/2017.
 * This class is a singleton that manage all the application parameters.
 * These parameters reflect on how the application works and give directory for model to load, for model to save, proxy ldap etc.
 */
public class AppParameters {


    public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final String KEY_PORT = "server.http.port";
    public static final String KEY_HTTPS_PORT = "server.https.port";
    public static final String KEY_PROTOCOL = "server.protocol";
    public static final String KEY_LOG_DIRECTORY = "server.log.directory";
    // Paths.get("logs").toAbsolutePath().toString() + FileSystems.getDefault().getSeparator();
    public static final String KEY_MODEL_DIRECTORY = "server.model.directory";
    //Paths.get("models").toAbsolutePath().toString() + FileSystems.getDefault().getSeparator();
    public static final String KEY_ADMIN_USERNAME = "admin.username";
    public static final String KEY_ADMIN_PASSWORD = "admin.password";
    public static final String KEY_SAVE_ON_TERMINATE = "server.save.onterminate";
    public static final String KEY_LOAD_ON_START = "server.load.onstart";
    public static final String KEY_PLUGINS_DIRECTORY = "server.plugins.directory";
    private static final Logger LOGGER = LoggerFactory.getLogger(AppParameters.class);
    private static final String DEFAULT_LOG_DIRECTORY = System.getProperty("user.home") + FILE_SEPARATOR + "logs" + FILE_SEPARATOR;
    private static final String DEFAULT_MODEL_DIRECTORY = System.getProperty("user.home") + FILE_SEPARATOR + "models" + FILE_SEPARATOR;
    private static final String DEFAULT_PLUGINS_DIRECTORY = System.getProperty("user.home") + FILE_SEPARATOR + "martserver-plugins" + FILE_SEPARATOR;

    private HashMap<String, String> config = new HashMap<>();
    private boolean configLoaded = false;

    /**
     * Private constructor as this class is a singleton.
     */
    private AppParameters() {
    }

    /**
     * @return the instance of an AppParameters singleton object.
     */
    public static AppParameters getInstance() {
        return AppParametersHolder.instance;
    }

    /**
     * This method load the properties config file and set config map attributes.
     */
    public void loadParametersFromConfigFile(final String configFilePath) throws ApplicationConfigurationException {


        InputStream in;
        try {
            if (configFilePath == null || configFilePath.trim().isEmpty()) {
                LOGGER.warn("No configuration file given on application top, using packaged configuration file.");
                in = this.getClass().getResourceAsStream("/config.properties");

            } else {
                LOGGER.info("Configuration file : " + configFilePath);
                in = new FileInputStream(configFilePath);
            }
        } catch (IOException ex) {
            String message = "Error while loading configuration file : " + ex.getClass().getName() + " --> " + ex.getMessage();
            LOGGER.error(message);
            throw new ApplicationConfigurationException(message, ex);
        }

        // Read the properties...
        Properties prop = new Properties();
        try {
            prop.load(in);
            try {
                in.close();
            } catch (IOException e) {
                    /* ignore */
            }
            // Default port.
            int port = 8080;

            // Read values.
            LOGGER.info("Reading application parameters...");
            if (prop.containsKey(KEY_PORT)) {
                try {
                    String portStr = prop.getProperty(KEY_PORT);
                    if (portStr == null) {
                        throw new ApplicationConfigurationException(KEY_PORT + " must be set !");
                    }
                    port = Integer.valueOf(portStr);
                    if (port == 0) {
                        throw new ApplicationConfigurationException(KEY_PORT + "must be set !");
                    }

                } catch (Exception e) {
                    LOGGER.warn(KEY_PORT + " --< key is not set properly : " + e.getMessage());
                    LOGGER.warn("Back to default port : " + port);
                    LOGGER.error(e.getMessage());
                }
                config.put(KEY_PORT, "" + port);

            } else {
                LOGGER.info(KEY_PORT + " --< key is not set.");
                LOGGER.info("Back to default port : " + port);
            }

            // LOG directory parameter.
            if (prop.containsKey(KEY_LOG_DIRECTORY)) {
                String logPath = prop.getProperty(KEY_LOG_DIRECTORY);
                if (logPath == null || logPath.trim().isEmpty()) {
                    logPath = DEFAULT_LOG_DIRECTORY;
                }

                Path logDir = new File(logPath).toPath();
                if (Files.exists(logDir) && Files.isDirectory(logDir)) {
                    config.put(KEY_LOG_DIRECTORY, logPath);

                } else {
                    LOGGER.warn("Creating directory for logs : " + logPath);
                    try {
                        Files.createDirectory(logDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating log directory : " + ex.getMessage());
                        throw new ApplicationConfigurationException(ex.getMessage());
                    }
                    LOGGER.warn("Directory : " + logPath + " doesnt exist on your file system or is not a directory.");
                    config.put(KEY_LOG_DIRECTORY, logPath);
                }
            } else {
                LOGGER.warn("Default to log directory : " + DEFAULT_LOG_DIRECTORY);
                config.put(KEY_LOG_DIRECTORY, DEFAULT_LOG_DIRECTORY);
                Path logDir = new File(DEFAULT_LOG_DIRECTORY).toPath();
                if (Files.exists(logDir) && Files.isDirectory(logDir)) {
                    config.put(KEY_LOG_DIRECTORY, DEFAULT_LOG_DIRECTORY);

                } else {
                    try {
                        Files.createDirectory(logDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating models directory : " + ex.getMessage() + " --> " + DEFAULT_LOG_DIRECTORY);
                        throw new ApplicationConfigurationException(ex.getMessage() + " --> " + DEFAULT_LOG_DIRECTORY);
                    }
                    config.put(KEY_LOG_DIRECTORY, DEFAULT_LOG_DIRECTORY);
                }

            }
            LOGGER.info("Log directory is : " + config.get(KEY_LOG_DIRECTORY));

            // Protocol part (currently only http(s) and standalone use).
            if (prop.containsKey(KEY_PROTOCOL)) {
                String protocol = prop.getProperty(KEY_PROTOCOL);

                if (protocol == null || (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https"))) {
                    LOGGER.warn(KEY_PROTOCOL + " is not set, assume default protocol : " + "http");
                    config.put(KEY_PROTOCOL, "http");
                } else {
                    config.put(KEY_PROTOCOL, protocol);
                }
            } else {
	        	LOGGER.warn(KEY_PROTOCOL + " is not provided in file, assume default protocol : " + "http");
	            config.put(KEY_PROTOCOL, "http");
	        }
            
            int httpsPort = 8181;
            if (prop.containsKey(KEY_HTTPS_PORT)) {
                try {
                    String portStr = prop.getProperty(KEY_HTTPS_PORT);
                    if (portStr == null || portStr.trim().isEmpty()) {
                        throw new ApplicationConfigurationException(KEY_HTTPS_PORT + " must be set !");
                    }
                    httpsPort = Integer.valueOf(portStr);
                    if (httpsPort == 0) {
                        LOGGER.warn(KEY_HTTPS_PORT + " must be set, this must be an integer value like 8181.");
                        throw new ApplicationConfigurationException(KEY_HTTPS_PORT + " must be set, this must be an integer value like 8181.");
                    }

                } catch (Exception e) {

                    LOGGER.warn(KEY_HTTPS_PORT + " --< key is not set properly : " + e.getMessage());
                    LOGGER.warn("Back to default port : " + httpsPort);

                }
            } else {
                LOGGER.warn(KEY_HTTPS_PORT + " --< key is not set.");
                LOGGER.warn("Back to default port : " + httpsPort);
            }
            config.put(KEY_HTTPS_PORT, "" + httpsPort);

            // Main administrator to remove after when usermanager will be ready or when the administrator(s) will be set.
            if (prop.containsKey(KEY_ADMIN_USERNAME)) {
                String adminuser = prop.getProperty(KEY_ADMIN_USERNAME);
                if (adminuser == null || adminuser.trim().isEmpty()) {
                    throw new ApplicationConfigurationException(KEY_ADMIN_USERNAME + "is not set, please set a default admin.");
                }
                config.put(KEY_ADMIN_USERNAME, adminuser);
            }
            if (prop.containsKey(KEY_ADMIN_PASSWORD)) {
                String adminpassword = prop.getProperty(KEY_ADMIN_PASSWORD);
                if (adminpassword == null || adminpassword.trim().isEmpty()) {
                    throw new ApplicationConfigurationException(KEY_ADMIN_PASSWORD + " is not set, please set a password for admin.");
                }
            }

            // Model directory for load and save models.
            if (prop.containsKey(KEY_MODEL_DIRECTORY)) {
                String modelPath = prop.getProperty(KEY_MODEL_DIRECTORY);
                if (modelPath == null || modelPath.trim().isEmpty()) {
                    modelPath = DEFAULT_MODEL_DIRECTORY;
                }
                Path modelDir = new File(modelPath).toPath();
                if (Files.exists(modelDir) && Files.isDirectory(modelDir)) {
                    config.put(KEY_MODEL_DIRECTORY, modelPath);

                } else {
                    try {
                        Files.createDirectory(modelDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating models directory : " + ex.getMessage());
                        throw new ApplicationConfigurationException(ex.getMessage());
                    }
                    config.put(KEY_MODEL_DIRECTORY, modelPath);
                }
            } else {
                LOGGER.warn("Default to model directory : " + DEFAULT_MODEL_DIRECTORY);
                config.put(KEY_MODEL_DIRECTORY, DEFAULT_MODEL_DIRECTORY);
                Path modelDir = new File(DEFAULT_MODEL_DIRECTORY).toPath();
                if (Files.exists(modelDir) && Files.isDirectory(modelDir)) {
                    config.put(KEY_MODEL_DIRECTORY, DEFAULT_MODEL_DIRECTORY);

                } else {
                    try {
                        Files.createDirectory(modelDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating models directory : " + ex.getMessage() + " --> " + DEFAULT_MODEL_DIRECTORY);
                        throw new ApplicationConfigurationException(ex.getMessage() + " --> " + DEFAULT_MODEL_DIRECTORY);
                    }
                    config.put(KEY_MODEL_DIRECTORY, DEFAULT_MODEL_DIRECTORY);
                }
            }
            LOGGER.info("Model directory is : " + config.get(KEY_MODEL_DIRECTORY));

            if (prop.containsKey(KEY_SAVE_ON_TERMINATE)) {
                String saveOnTermStr = prop.getProperty(KEY_SAVE_ON_TERMINATE);
                if (saveOnTermStr == null || saveOnTermStr.trim().isEmpty()) {
                    saveOnTermStr = "false";
                }

                boolean saveOnTerm = Boolean.valueOf(saveOnTermStr);
                config.put(KEY_SAVE_ON_TERMINATE, "" + saveOnTerm);
            } else {
                config.put(KEY_SAVE_ON_TERMINATE, "false");
            }
            if (prop.containsKey(KEY_LOAD_ON_START)) {
                String saveOnTermStr = prop.getProperty(KEY_LOAD_ON_START);
                if (saveOnTermStr == null || saveOnTermStr.trim().isEmpty()) {
                    saveOnTermStr = "false";
                }

                boolean saveOnTerm = Boolean.valueOf(saveOnTermStr);
                config.put(KEY_LOAD_ON_START, "" + saveOnTerm);
            } else {
                config.put(KEY_LOAD_ON_START, "false");
            }

            // Plugins directory for extension models and connector, must be jar file format.
            if (prop.containsKey(KEY_PLUGINS_DIRECTORY)) {
                String pluginPath = prop.getProperty(KEY_PLUGINS_DIRECTORY);
                if (pluginPath == null || pluginPath.trim().isEmpty()) {
                    pluginPath = DEFAULT_PLUGINS_DIRECTORY;
                }

                Path pluginDir = new File(pluginPath).toPath();
                if (Files.exists(pluginDir) && Files.isDirectory(pluginDir)) {
                    config.put(KEY_PLUGINS_DIRECTORY, pluginPath);

                } else {
                    LOGGER.warn("Creating directory for plugins : " + pluginPath);
                    try {
                        Files.createDirectory(pluginDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating plugins directory : " + ex.getMessage());
                        throw new ApplicationConfigurationException(ex.getMessage());
                    }
                    LOGGER.warn("Directory : " + pluginPath + " doesnt exist on your file system or is not a directory.");
                    config.put(KEY_PLUGINS_DIRECTORY, pluginPath);
                }
            } else {
                LOGGER.warn("Default to plugins directory : " + DEFAULT_PLUGINS_DIRECTORY);
                config.put(KEY_PLUGINS_DIRECTORY, DEFAULT_PLUGINS_DIRECTORY);
                Path pluginDir = new File(DEFAULT_PLUGINS_DIRECTORY).toPath();
                if (Files.exists(pluginDir) && Files.isDirectory(pluginDir)) {
                    config.put(KEY_PLUGINS_DIRECTORY, DEFAULT_PLUGINS_DIRECTORY);

                } else {
                    try {
                        Files.createDirectory(pluginDir);
                    } catch (IOException ex) {
                        LOGGER.error("Error while creating models directory : " + ex.getMessage() + " --> " + DEFAULT_PLUGINS_DIRECTORY);
                        throw new ApplicationConfigurationException(ex.getMessage() + " --> " + DEFAULT_PLUGINS_DIRECTORY);
                    }
                    config.put(KEY_PLUGINS_DIRECTORY, DEFAULT_PLUGINS_DIRECTORY);
                }

            }
            LOGGER.info("Plugin directory is : " + config.get(KEY_PLUGINS_DIRECTORY));

        } catch (IOException ex) {
            LOGGER.warn("Cannot find configuration file for Mart server, setting default values.");
            LOGGER.error("Error while reading configuration file :--> Exception : " + ex.getClass().getName() + " --> " + ex.getMessage());
            config.put(KEY_PORT, "8080");
            config.put(KEY_HTTPS_PORT, "8181");
            config.put(KEY_PROTOCOL, "http");
            config.put(KEY_LOG_DIRECTORY, DEFAULT_LOG_DIRECTORY);
            config.put(KEY_MODEL_DIRECTORY, DEFAULT_MODEL_DIRECTORY);
            config.put(KEY_PLUGINS_DIRECTORY, DEFAULT_PLUGINS_DIRECTORY);
            config.put(KEY_LOAD_ON_START, "false");
            config.put(KEY_SAVE_ON_TERMINATE, "false");
        } finally {
            Utils.closeQuietly(in);
        }
        configLoaded = true;
    }

    /**
     * @return a config map with attributes (or empty config map).
     */
    public Map<String, String> getConfig() {
        return config;
    }

    public boolean isConfigLoaded() {
        return configLoaded;
    }

    /**
     * Add plugins directory on classpath. This is not using OSGI.
     * @throws Exception any exception thrown on reading plugins directory and java exception from jars loaded on classpath.
     */
    public void addPluginsToClasspath() throws Exception {
        //need to do add path to Classpath with reflection since the URLClassLoader.addURL(URL url) method is protected:
        // Detect in plugins directory...
        String pluginsFolder = getInstance().getConfig().get(KEY_PLUGINS_DIRECTORY);
        if (pluginsFolder == null) {
            LOGGER.warn("plugins directory is not set.");
            return;
        }
        List<String> files = fileList(pluginsFolder);
        for (String filename : files) {
            LOGGER.info("Plugin jar detected: " + filename);
            File f = new File(filename);
            URI u = f.toURI();
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> urlClass = URLClassLoader.class;
            Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(urlClassLoader, new Object[]{u.toURL()});
        }
    }


    /**
     * Add plugins using OSGI bundle.
     * @throws Exception
     */
    public void addPluginsUsingOSGI() throws Exception {
        String pluginsFolder = config.get(KEY_PLUGINS_DIRECTORY);
        if (pluginsFolder == null) {
            LOGGER.warn("plugins directory is not set.");
            return;
        }
        OSGILoader osgiLoader = OSGILoader.getInstance();
        List<String> filesStr = fileList(pluginsFolder);
        List<File> files = new ArrayList<>();
        for (String filename : filesStr) {
            LOGGER.info("Plugin jar detected: " + filename);
            File f = new File(filename);
            if (f.exists()) {
                // Install and start osgi bundle.
                files.add(f);
            }
        }
        if (!files.isEmpty()) {
            osgiLoader.installPlugins(files);
        }
    }

    /**
     * List files on a local directory (use java nio).
     * @param directory
     * @return
     * @throws Exception
     */
    public static List<String> fileList(String directory) throws Exception {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
            for (Path path : directoryStream) {
                fileNames.add(path.toString());
            }
        } catch (IOException ex) {
            System.out.println("Exception thrown : " + ex.getClass().getName() + " , message: " + ex.getMessage());
            throw ex;
        }
        return fileNames;
    }

    /**
     * Singleton holder, this for multi-threaded environmnent (and avoid synchronize method).
     */
    private static class AppParametersHolder {
        private static AppParameters instance = new AppParameters();
    }
}
