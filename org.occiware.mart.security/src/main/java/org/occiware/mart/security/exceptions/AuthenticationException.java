package org.occiware.mart.security.exceptions;

/**
 * Created by cgourdin on 22/06/2017.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException() {
    }

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(Throwable cause) {
        super(cause);
    }



}
