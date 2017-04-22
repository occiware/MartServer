package org.occiware.mart.server.exception;

/**
 * Created by christophe on 20/04/2017.
 */
public class ModelValidatorException extends Exception {
    public ModelValidatorException() {
    }

    public ModelValidatorException(String message) {
        super(message);
    }

    public ModelValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelValidatorException(Throwable cause) {
        super(cause);
    }
}
