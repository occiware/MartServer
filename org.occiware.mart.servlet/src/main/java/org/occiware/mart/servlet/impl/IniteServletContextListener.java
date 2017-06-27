package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ApplicationConfigurationException;
import org.occiware.mart.server.facade.*;
import org.occiware.mart.server.parser.json.JsonOcciParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Created by cgourdin on 27/06/2017.
 */
@WebListener
public class IniteServletContextListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IniteServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        AppParameters appParameters = AppParameters.getInstance();
        try {
            if (!appParameters.isConfigLoaded()) {
                // try to load configuration default.
                appParameters.loadParametersFromConfigFile(null);
            }
            // Load the configuration object.
            String tmpUser = "anonymous";

            // ensure that parameters are here.
            if (appParameters.isConfigLoaded()) {

                OCCIApiResponse occiResponse = new DefaultOCCIResponse(tmpUser, new JsonOcciParser(tmpUser));
                OCCIApiInputRequest occiApiInputRequest = new DefaultOCCIRequest(tmpUser, occiResponse, new JsonOcciParser(tmpUser));
                String loadOnStartStr = appParameters.getConfig().get(AppParameters.KEY_LOAD_ON_START);
                boolean loadOnStart = Boolean.valueOf(loadOnStartStr);
                if (loadOnStart) {
                    LOGGER.info("Loading models...");
                    occiApiInputRequest.loadAllModelsFromDisk();
                }
            }

        } catch (ApplicationConfigurationException ex) {
            System.err.println("Exception : " + ex.getMessage());
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        LOGGER.info("Leaving...");
        String tmpUser = "anonymous";

        // ensure that parameters are here.
        AppParameters appParameters = AppParameters.getInstance();
        if (appParameters.isConfigLoaded()) {

            OCCIApiResponse occiResponse = new DefaultOCCIResponse(tmpUser, new JsonOcciParser(tmpUser));
            OCCIApiInputRequest occiApiInputRequest = new DefaultOCCIRequest(tmpUser, occiResponse, new JsonOcciParser(tmpUser));
            String saveOnTerminateStr = appParameters.getConfig().get(AppParameters.KEY_SAVE_ON_TERMINATE);
            boolean saveOnTerminate = Boolean.valueOf(saveOnTerminateStr);
            if (saveOnTerminate) {
                LOGGER.info("saving models..");
                occiApiInputRequest.saveAllModelsToDisk();
            }
        }
    }
}
