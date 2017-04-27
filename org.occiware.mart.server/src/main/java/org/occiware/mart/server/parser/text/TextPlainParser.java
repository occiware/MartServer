package org.occiware.mart.server.parser.text;

import org.occiware.clouddesigner.occi.Entity;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.parser.AbstractRequestParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.QueryInterfaceData;

import java.util.List;

/**
 * Created by cgourdin on 26/04/2017.
 */
public class TextPlainParser extends AbstractRequestParser implements IRequestParser {
    @Override
    public Object getInterface(QueryInterfaceData interfaceData, String user) throws ParseOCCIException {
        return null;
    }

    @Override
    public String parseMessage(String message) throws ParseOCCIException {
        return null;
    }

    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {

    }

    @Override
    public Object renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException {
        return null;
    }

    @Override
    public Object renderOutputEntities(List<Entity> entities) throws ParseOCCIException {
        return null;
    }

    @Override
    public Object renderOutputEntity(Entity entity) throws ParseOCCIException {
        return null;
    }

    @Override
    public String parseMessageAndStatus(String message, int status) throws ParseOCCIException {
        return null;
    }
}
