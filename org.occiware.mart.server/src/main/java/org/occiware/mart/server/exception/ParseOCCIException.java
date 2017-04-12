package org.occiware.mart.server.exception;

/**
 * Created by cgourdin on 11/04/2017.
 */
public class ParseOCCIException extends Exception {
    public ParseOCCIException() {
    }

    public ParseOCCIException(String message) {
        super(message);
    }

    public ParseOCCIException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseOCCIException(Throwable cause) {
        super(cause);
    }
}
