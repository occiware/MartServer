package org.occiware.mart.server;

import java.io.File;
import java.io.IOException;

/**
 * Created by cgourdin on 29/03/2017.
 */
public class FrontEndService implements Runnable {

    @Override
    public void run() {
        // Launch frontend node services.
        try {

            String appPath = new File(".").getCanonicalPath();
            String commanddev = appPath + File.separatorChar +  "node" + File.separatorChar + "npm run dev";

            // Check if nodejs is present in node directory.
            File node = new File(appPath + File.separatorChar + "node");
            String[] nodeFiles = node.list();
            if (nodeFiles != null && nodeFiles.length > 0) {
                // Start node with npm command.
                System.out.println("Starting the frontend server on port 3000");
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec(commanddev);


            } else {
                throw new IOException("Node files are not present, please use <mvn clean install> command.");
            }




        } catch(IOException ex) {
            System.err.println("Cannot start OCCInterface node server: " + ex.getMessage());
        }
    }





}
