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

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.occiware.mart.server.facade.AppParameters;
import org.occiware.mart.server.exception.ApplicationConfigurationException;
import org.occiware.mart.server.utils.logging.LoggerConfig;
import org.occiware.mart.servlet.MainServlet;

import java.net.URISyntaxException;


/**
 * @author Christophe Gourdin
 */
public class MartServer {

    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";
    private static int port;
    private static int httpsPort;
    private static String logDirectoryPath;
    private static String httpProtocol;
    private static Server server;

    public static void main(String[] args) throws URISyntaxException {

        String configFilePath = null;
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
        }

        AppParameters appParameters = AppParameters.getInstance();
        try {
            appParameters.loadParametersFromConfigFile(configFilePath);
        } catch (ApplicationConfigurationException ex) {
            throw new RuntimeException("Cannot load configuration parameters : " + ex.getMessage());
        }
        readConfigParameters(appParameters);

        ServletHandler handler = new ServletHandler();
        server = new Server(port);
        server.setHandler(handler);

        handler.addServletWithMapping(MainServlet.class, "/*");

        if (httpProtocol.equalsIgnoreCase(HTTPS_PROTOCOL)) {
            // Configure https protocol
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(MartServer.class.getResource(
                    "/keystore.jks").toExternalForm());
            sslContextFactory.setKeyStorePassword("martserver");
            sslContextFactory.setKeyManagerPassword("martserver");
            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(httpsPort);
            Connector[] connectors = server.getConnectors();
            // connector[0] => http connector built via server constructor.
            server.setConnectors(new Connector[]{connectors[0], sslConnector});
        }

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
     * Read the config parameters and set values to this server.
     * @param parameters
     */
    public static void readConfigParameters(AppParameters parameters) {
        // setDefaultServerConfigValues();
        String portStr = parameters.getConfig().get(AppParameters.KEY_PORT);
        String httpsPortStr = parameters.getConfig().get(AppParameters.KEY_HTTPS_PORT);
        httpProtocol = parameters.getConfig().get(AppParameters.KEY_HTTPS_PORT);
        logDirectoryPath = parameters.getConfig().get(AppParameters.KEY_LOG_DIRECTORY);
        try {
            port = Integer.valueOf(portStr);
        } catch (Exception e) {
            System.out.println(AppParameters.KEY_PORT + " --< key is not set properly : " + e.getMessage());
            System.out.println("Back to default port : " + port);
        }
        try {
            httpsPort = Integer.valueOf(httpsPortStr);
        } catch (Exception e) {
            System.out.println(AppParameters.KEY_HTTPS_PORT + " --< key is not set properly : " + e.getMessage());
            System.out.println("Back to default port : " + httpsPort);
        }
    }

    public static void stopServer() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            server.destroy();
        }
    }

}
