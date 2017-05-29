package org.occiware.mart.security.exceptions;

/**
 * Created by cgourdin on 24/05/2017.
 */
public class ApplicationSecurityException extends Exception {
    public ApplicationSecurityException() {
    }

    public ApplicationSecurityException(String message) {
        super(message);
    }

    public ApplicationSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationSecurityException(Throwable cause) {
        super(cause);
    }
}
