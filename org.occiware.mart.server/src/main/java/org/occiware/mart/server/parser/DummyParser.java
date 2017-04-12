package org.occiware.mart.server.parser;

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.server.facade.OCCIResponse;

import java.util.List;

/**
 * Created by cgourdin on 12/04/2017.
 */
public class DummyParser implements IRequestParser {

    private OCCIRequest occiRequest;
    private OCCIResponse occiResponse;

    public DummyParser(OCCIRequest occiRequest) {
        this.occiRequest = occiRequest;
    }

    public DummyParser(OCCIResponse occiResponse) {
        this.occiResponse = occiResponse;
    }

    @Override
    public Object getInterface(String user) throws ParseOCCIException {
        return "";
    }

    @Override
    public String parseMessage(String message, Integer statusCode) throws ParseOCCIException {
        return message + " --> " + statusCode;
    }

    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {
        // Dummy...
    }

    @Override
    public Object renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException {
        return "";
    }

    @Override
    public Object renderOutputEntities(List<Entity> entities) throws ParseOCCIException {
        return "";
    }

    @Override
    public Object renderOutputEntity(Entity entity) throws ParseOCCIException {
        return "";
    }
}
