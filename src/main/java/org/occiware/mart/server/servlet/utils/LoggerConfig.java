package org.occiware.mart.server.servlet.utils;

import org.apache.log4j.*;
import org.apache.log4j.varia.LevelRangeFilter;

import java.nio.file.FileSystems;

/**
 * Configure logger appender application.
 *
 * @author Christophe Gourdin
 */
public class LoggerConfig {

    /**
     * Initialize appender on root logger.
     * Warning, All old appenders is removed in this method.
     *
     * @param logDirectoryPath
     */
    public static void initAppenders(String logDirectoryPath) {
        // Re-initialize the configuration logger..
        Logger.getRootLogger().getLoggerRepository().resetConfiguration();

        ConsoleAppender console = new ConsoleAppender();

        RollingFileAppender rollingInfoAppender;
        RollingFileAppender rollingDebugAppender;
        RollingFileAppender rollingWarnAppender;
        RollingFileAppender rollingErrorAppender;
        RollingFileAppender rollingFatalAppender;

        final String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %m%n";
        console.setLayout(new PatternLayout(pattern));
        // Assign on all level the console appender.
        console.setThreshold(Level.INFO);
        console.activateOptions();
        console.setEncoding("UTF8");

        Logger.getRootLogger().addAppender(console);
        String name = "DebugLogger";
        String prefix = "mart_server_";
        String filename = prefix + "debug.log";
        rollingDebugAppender = createAppender(Level.DEBUG, pattern, name, logDirectoryPath, filename);
        name = "InfoLogger";
        filename = prefix + "info.log";
        rollingInfoAppender = createAppender(Level.INFO, pattern, name, logDirectoryPath, filename);
        name = "WarnLogger";
        filename = prefix + "warn.log";
        rollingWarnAppender = createAppender(Level.WARN, pattern, name, logDirectoryPath, filename);

        name = "ErrorLogger";
        filename = prefix + "error.log";
        rollingErrorAppender = createAppender(Level.ERROR, pattern, name, logDirectoryPath, filename);

        name = "FatalLogger";
        filename = prefix + "fatal.log";
        rollingFatalAppender = createAppender(Level.FATAL, pattern, name, logDirectoryPath, filename);


        // Assign appenders to root logger.
        Logger.getRootLogger().addAppender(rollingDebugAppender);
        Logger.getRootLogger().addAppender(rollingInfoAppender);
        Logger.getRootLogger().addAppender(rollingWarnAppender);
        Logger.getRootLogger().addAppender(rollingErrorAppender);
        Logger.getRootLogger().addAppender(rollingFatalAppender);

        // Assign level appender for other libraries.
        // This following example show how to use external quartz lib with rolling appender.
//        name = PackageFilter.QUARTZ_LOGGER_NAME;
//        filename = "log/jobs.log";
//
//        RollingFileAppender rollingQuartzAppender = createAppender(Level.INFO, pattern, name, filename);
//        Logger logQuartz = Logger.getLogger(PackageFilter.QUARTZ_LOGGER_NAME);
//        logQuartz.setLevel(Level.INFO);
//        logQuartz.removeAllAppenders();
//        logQuartz.addAppender(rollingQuartzAppender);
//        logQuartz.setAdditivity(false);

    }


    /**
     * Add appenders to rolling files with a maximum size of 2 Mo.
     *
     * @param level
     * @param pattern
     * @param name
     * @param logDirectoryPath
     * @param filename
     * @return
     */
    private static RollingFileAppender createAppender(final Level level, final String pattern, final String name, final String logDirectoryPath, final String filename) {
        RollingFileAppender rollingAppender = new RollingFileAppender();
        String maxFileSize = "2048KB";
        int maxBackupIndex = 300;

        // LevelRangeFilter
        LevelRangeFilter rangeFilter = new LevelRangeFilter();
        rangeFilter.setLevelMax(level);
        rangeFilter.setLevelMin(level);
        rollingAppender.addFilter(rangeFilter);
        if (!name.equals(PackageFilter.QUARTZ_LOGGER_NAME)) {
            // Add a filter to exclude org.quartz from appender.
            PackageFilter packFilter = new PackageFilter();
            rollingAppender.addFilter(packFilter);
        }

        rollingAppender.setName(name);
        String logFilename;
        if (logDirectoryPath.endsWith("/")) {
            logFilename = logDirectoryPath + filename;
        } else {
            logFilename = logDirectoryPath + FileSystems.getDefault().getSeparator() + filename;
        }

        System.out.println("Mart server will log in path : " + logFilename);
        rollingAppender.setFile(logFilename);
        rollingAppender.setLayout(new PatternLayout(pattern));
        rollingAppender.setMaxFileSize(maxFileSize);
        rollingAppender.setMaxBackupIndex(maxBackupIndex);
        rollingAppender.setEncoding("UTF8");
        rollingAppender.setThreshold(level);
        rollingAppender.setAppend(true);
        rollingAppender.activateOptions();


        return rollingAppender;
    }

}
