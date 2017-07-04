package org.occiware.mart.server.facade;


import java.io.File;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgourdin on 04/07/2017.
 */
public class OSGILoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSGILoader.class);
    private OSGILoader() {

    }

    public void installPlugins(List<File> files) throws Exception {
        // Use the standard Java "ServiceLoader" approach to finding FrameworkFactory implementations in
        // the classpath. The OSGi implementation (whatever it is) should have the appropriate entry
        // in its MANIFEST.MF for the ServiceLoader to find..
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

        // Start up the core OSGi stuff; the result is an environment with exactly one OSGI bundle
        // in it : the osgi "system" bundle.
        Map<String, String> config = initConfig();
        Framework framework = frameworkFactory.newFramework(config);
        framework.start();

        // Tell OSGi to load a bunch of bundles (aka jarfiles)
        BundleContext context = framework.getBundleContext();
        List<Bundle> installedBundles = new LinkedList<>();

        for (File file : files) {
            LOGGER.info("Loading jar : " + file.toURI().toString());
            installedBundles.add(context.installBundle(file.toURI().toString()));
        }
        // And now "start" the loaded bundles - except for "fragment" ones
        for (Bundle bundle : installedBundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
//                    bundle.start();
                LOGGER.info("Plugin : " + bundle.getLocation() + " installed !");
                LOGGER.info("Plugin state : " + bundle.getState());
            }
        }

        // and finally leave the bundles alone to deal with events..
        framework.waitForStop(1000);

        // context = FrameworkUtil.getBundle( OSGILoader.class ).getBundleContext();
        // Bundle b = context.installBundle( file.toURI().toString() );
        // b.start();
    }

//    public void uninstallPlugin(File file) throws Exception {
//        context = FrameworkUtil.getBundle( OSGILoader.class ).getBundleContext();
//        Bundle b = context.getBundle( file.toURI().toString() );
//        b.uninstall();
//    }
    private static Map<String, String> initConfig() {
        Map<String, String> config = new HashMap<>();

        // Allow OSGi bundles to import this package from the standard java classpath classloader
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.occiware.mart.server.facade");

        // See the org.osgi.framework.Constants class for other useful things that can be put
        // into the OSGi configuration map.

        return config;
    }

    /**
     * @return the instance of an AppParameters singleton object.
     */
    public static OSGILoader getInstance() {
        return OSGILoaderHolder.instance;
    }

    /**
     * Singleton holder, this for multi-threaded environmnent (and avoid synchronize method).
     */
    private static class OSGILoaderHolder {
        private static OSGILoader instance = new OSGILoader();
    }

}
