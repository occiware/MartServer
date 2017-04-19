package org.occiware.mart.server.facade;

import org.occiware.mart.server.exception.ParseOCCIException;

/**
 * Created by christophe on 19/04/2017.
 */
public class DummyRequest extends AbstractOCCIRequest implements OCCIRequest {

    public DummyRequest(OCCIResponse response, String contentType) {
        super(response, contentType);
    }

    public DummyRequest(OCCIResponse response, String contentType, String username) {
        super(response, contentType, username);
    }

    public DummyRequest(OCCIResponse response, String contentType, String location, String username) {
        super(response, contentType, location, username);
    }

    @Override
    public void parseInput() throws ParseOCCIException {
        // DO nothing...
    }

    @Override
    public void validateRequest() {
        // DO nothing...
    }
}
