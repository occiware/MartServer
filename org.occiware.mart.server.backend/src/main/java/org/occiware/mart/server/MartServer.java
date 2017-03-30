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
package org.occiware.mart.server;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.model.exceptions.ConfigurationException;
import org.occiware.mart.server.servlet.utils.LoggerConfig;
import org.occiware.mart.server.servlet.utils.Utils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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

        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);

        // Initialize logger appenders.
        LoggerConfig.initAppenders(logDirectoryPath);

        // Thread thFrontEnd = new Thread(new FrontEndService());
        // thFrontEnd.start();

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

    /**
     * start an occi interface frontend.
     */
    private static void startOcciInterfaceServer() {





        Server wserver = new Server(3000);

        // Create the ResourceHandler. It is the object that will actually
        // handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other
        // handlers as you will see in other examples.
        ResourceHandler resource_handler = new ResourceHandler();
        // Configure the ResourceHandler. Setting the resource base indicates
        // where the files should be served out of.
        // In this example it is the current directory but it can be configured
        // to anything that the jvm has access to.
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[] { "index.html" });
        resource_handler.setResourceBase("./webapp");

        GzipHandler gzip = new GzipHandler();

        wserver.setHandler(gzip);
        HandlerList handlers = new HandlerList();

        handlers.setHandlers(new Handler[] { resource_handler,
                new DefaultHandler() });
        gzip.setHandler(handlers);

        try {
            System.out.println("Starting server occiinterface http.");
            wserver.start();
            wserver.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

//    public static class FrontEndProxyServlet extends ProxyServlet {
//        @Override
//        public void init(ServletConfig config) throws ServletException {
//            super.init(config);
//            System.out.println(">> init done !");
//        }
//
//        @Override
//        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
//            System.out.println(">>> got a request !");
//            super.service(req, res);
//        }
//
//    }

//
//    public static class MyProxyServlet extends ProxyServlet {
//        @Override
//        public void init(ServletConfig config) throws ServletException {
//            super.init(config);
//            System.out.println(">> init done !");
//        }
//
//        @Override
//        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
//            System.out.println(">>> got a request !");
//            super.service(req, res);
//        }
//    }

}
