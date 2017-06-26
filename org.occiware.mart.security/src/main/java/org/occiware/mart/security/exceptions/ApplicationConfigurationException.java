package org.occiware.mart.security.exceptions;

/**
 * Created by cgourdin on 26/06/2017.
 */
public class ApplicationConfigurationException extends Exception {

    public ApplicationConfigurationException() {
    }

    public ApplicationConfigurationException(String message) {
        super(message);
    }

    public ApplicationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationConfigurationException(Throwable cause) {
        super(cause);
    }
}
