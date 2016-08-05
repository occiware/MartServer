/**
 * Copyright (c) 2015-2017 Inria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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




/**
 *
 * @author Christophe Gourdin
 */
public class MartServer {

    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig();
        config.packages("org.occiware.mart.server.servlet");
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        Server server = new Server(9090);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");
        
        ConfigurationManager.getConfigurationForOwner(ConfigurationManager.DEFAULT_OWNER);
        
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
