package org.occiware.mart.server.facade;

/**
 * Created by christophe on 19/04/2017.
 */
public class DummyResponse extends AbstractOCCIResponse implements OCCIResponse {
    public DummyResponse(String contentType) {
        super(contentType);
    }

    public DummyResponse(String contentType, String username) {
        super(contentType, username);
    }

    @Override
    public void setResponse(Object response) {

    }
}
