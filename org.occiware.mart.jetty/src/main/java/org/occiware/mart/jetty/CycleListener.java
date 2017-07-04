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

import org.eclipse.jetty.util.component.LifeCycle;
import org.occiware.mart.server.facade.AppParameters;
import org.occiware.mart.servlet.impl.IniteServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgourdin on 27/06/2017.
 */
public class CycleListener implements LifeCycle.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CycleListener.class);

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        System.out.println("MartServer is starting...");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        LOGGER.info("MartServer is started !");
        loadModels();

    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        LOGGER.error("server failure : " + cause.getMessage());
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        LOGGER.info("MartServer is stopping !");
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
