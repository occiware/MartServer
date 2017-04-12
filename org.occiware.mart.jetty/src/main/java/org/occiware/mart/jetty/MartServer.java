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
package org.occiware.mart.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.occiware.mart.server.exception.ConfigurationException;
import org.occiware.mart.server.utils.LoggerConfig;
import org.occiware.mart.server.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


/**
 * @author Christophe Gourdin
 */
public class MartServer {

    private static final String KEY_PORT = "server.port";
    private static final String KEY_PROTOCOL = "server.protocol";
    private static final String KEY_LOG_DIRECTORY = "server.log.directory";
    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    private static String configFilePath;
    private static int port;
    private static String logDirectoryPath;
    private static String httpProtocol;

    public static void main(String[] args) {

        setDefaultServerConfigValues();

        if (args.length > 0) {
            // check if a configuration properties is given on path.
            if (args[0].equalsIgnoreCase("--help")) {
                System.out.println("You can set a config directory file path (absolute path) in argument");
                System.out.println("ex: mvn exec:java -Dexec.args=\"/mydirectory/martserverconfigfilename.config\"");
                System.out.println("Minimum config file content: ");
                System.out.println("server.port=8080\n" +
                        "server.log.directory=/logging/application/logs\n" +
                        "server.protocol=http");
                return;
            }

            configFilePath = args[0];

        } else {

            // Read the server file configuration in /~/martserver.config
            configFilePath = System.getProperty("user.home") + File.separator + "martserver.config";
        }
        readFileConfig();

        ResourceConfig config = new ResourceConfig();
        config.packages("org.occiware.mart.server.servlet");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        // ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        // ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);

        // Initialize logger appenders.
        LoggerConfig.initAppenders(logDirectoryPath);

        try {
            System.out.println("Starting OCCI REST MartServer...");
            server.start();
            server.join();
        } catch (Exception ex) {
            System.err.println("Exception thrown : " + ex.getClass().getSimpleName());
            ex.printStackTrace();
        } finally {
            System.out.println("Destroying server...");
            try {
                server.stop();
            } catch (Exception ex) {
                System.out.println("Failed to stop the server");
            }
            server.destroy();
        }
    }

    /**
     * Read the configuration file.
     */
    public static void readFileConfig() {
        setDefaultServerConfigValues();
        if (configFilePath == null) {
            System.out.println("No configuration file detected, default configuration is assumed.");
            return;
        }

        File configFile = new File(configFilePath);
        Path pathConfig = configFile.toPath();

        if (Files.exists(pathConfig) && Files.isRegularFile(pathConfig)) {
            // Read the properties...
            Properties prop = new Properties();
            try {
                InputStream in = new FileInputStream(configFilePath);
                prop.load(in);
                Utils.closeQuietly(in);

                // Read values.
                System.out.println("Loading configuration file : " + configFilePath);
                if (prop.containsKey(KEY_PORT)) {
                    try {
                        String portStr = prop.getProperty(KEY_PORT);
                        if (portStr == null) {
                            throw new ConfigurationException(KEY_PORT + " must be set !");
                        }
                        port = Integer.valueOf(portStr);
                        if (port == 0) {
                            throw new ConfigurationException(KEY_PORT + "must be set !");
                        }

                    } catch (Exception e) {
                        System.out.println(KEY_PORT + " --< key is not set properly : " + e.getMessage());
                        System.out.println("Back to default port : " + port);
                    }
                } else {
                    System.out.println(KEY_PORT + " --< key is not set.");
                    System.out.println("Back to default port : " + port);
                }
                if (prop.containsKey(KEY_LOG_DIRECTORY)) {
                    String logPath = prop.getProperty(KEY_LOG_DIRECTORY);
                    Path logDir = new File(logPath).toPath();
                    if (Files.exists(logDir) && Files.isDirectory(logDir)) {
                        logDirectoryPath = logPath;
                    } else {
                        System.out.println("Directory : " + logDirectoryPath + " doesnt exist on your file system.");
                        System.out.println("Default to log directory : " + logDirectoryPath);
                    }
                } else {
                    System.out.println("Default to log directory : " + logDirectoryPath);
                }
                if (prop.containsKey(KEY_PROTOCOL)) {
                    String protocol = prop.getProperty(KEY_PROTOCOL);
                    if (protocol == null || (!protocol.equalsIgnoreCase(HTTP_PROTOCOL) && !protocol.equalsIgnoreCase(HTTPS_PROTOCOL))) {
                        System.out.println(KEY_PROTOCOL + " is not set, assume default protocol : " + httpProtocol);
                    } else {
                        httpProtocol = protocol;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Cannot find configuration file for Mart server, setting default values.");
            }
        }


    }

    /**
     * Default configuration server values.
     */
    private static void setDefaultServerConfigValues() {
        port = 8080;
        logDirectoryPath = Paths.get("logs").toAbsolutePath().toString() + FileSystems.getDefault().getSeparator();
        httpProtocol = HTTP_PROTOCOL;
    }


}
