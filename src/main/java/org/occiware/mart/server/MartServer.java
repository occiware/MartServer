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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.occiware.mart.server.servlet.model.ConfigurationManager;
import org.occiware.mart.server.servlet.utils.LoggerConfig;


/**
 *
 * @author Christophe Gourdin
 */
class MartServer {

    public static void main(String[] args) {
        // TODO : Read a property file to configure the server on other port etc.
        ResourceConfig config = new ResourceConfig();
        config.packages("org.occiware.mart.server.servlet");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(ConfigurationManager.DEFAULT_OWNER);

        // Initialize logger appenders.
        LoggerConfig.initAppenders();

        try {
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

}
