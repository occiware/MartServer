package org.occiware.mart.jetty;

import org.eclipse.jetty.util.component.LifeCycle;
import org.occiware.mart.servlet.impl.IniteServletContextListener;

/**
 * Created by cgourdin on 27/06/2017.
 */
public class CycleListener implements LifeCycle.Listener {

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        System.out.println("MartServer is starting...");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        System.out.println("MartServer is started !");
        loadModels();

    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        System.err.println("server failure : " + cause.getMessage());
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        System.err.println("MartServer is stopping !");
        saveModels();
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        System.err.println("MartServer is stopped !");
    }

    /**
     * Reused jee listener to load models on startup.
     */
    public static void loadModels() {
        IniteServletContextListener engine = new IniteServletContextListener();
        engine.contextInitialized(null);
    }


    public static void saveModels() {
        IniteServletContextListener engine = new IniteServletContextListener();
        engine.contextDestroyed(null);
    }
}
