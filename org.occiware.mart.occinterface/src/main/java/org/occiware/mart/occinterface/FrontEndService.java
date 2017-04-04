package org.occiware.mart.occinterface;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

/**
 * Created by cgourdin on 03/04/2017.
 */
public class FrontEndService {

    public static void main(String[] args) throws Exception {
        System.out.println("Launch the frontend server.");

        // Note that if you set this to port 0
        // Create a basic Jetty server object that will listen on port 8080.
        // then a randomly available port will be assigned that you can either
        // look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(3000);

        // Create the ResourceHandler. It is the object that will actually
        // handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other
        // handlers as you will see in other examples.
        ResourceHandler resourceHandler = new ResourceHandler();
        // Configure the ResourceHandler. Setting the resource base indicates
        // where the files should be served out of.
        // In this example it is the current directory but it can be configured
        // to anything that the jvm has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });
        resourceHandler.setResourceBase("./src/main/frontend/occinterface");

        // Add the ResourceHandler to the server.
        GzipHandler gzip = new GzipHandler();
        server.setHandler(gzip);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler,
                new DefaultHandler() });
        gzip.setHandler(handlers);

        // Start things up! By using the server.join() the server thread will
        // join with the current thread.
        // See
        // "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html#join()"
        // for more details.
        server.start();
        server.join();

    }


}
