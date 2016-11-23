package org.occiware.mart.server.servlet.utils;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Specific filter for root logger, this avoid to log org.quartz and and other classes to standard logs (info.log etc.).
 * @author Christophe Gourdin
 */
public class PackageFilter extends Filter {

    /**
     * Example for quartz lib.
     */
    public static final String QUARTZ_LOGGER_NAME = "org.quartz";
    /**
     * Example for jclouds lib.
     */
    public static final String JCLOUD_BLOBSTORE_LOGGER_NAME = "jclouds.blobstore";

    public static final String COM_MCHANGE = "com.mchange";
    
    @Override
    public int decide(LoggingEvent event) {

        if (event.getLoggerName().contains(QUARTZ_LOGGER_NAME) || event.getLoggerName().contains(JCLOUD_BLOBSTORE_LOGGER_NAME) 
                || event.getLoggerName().contains(COM_MCHANGE)) {
            return DENY;
        } 
        return NEUTRAL;
    }
    
}
