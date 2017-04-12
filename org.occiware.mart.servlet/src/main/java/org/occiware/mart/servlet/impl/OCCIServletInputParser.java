package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.AbstractOCCIRequest;
import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by cgourdin on 11/04/2017.
 */
public class OCCIServletInputParser extends AbstractOCCIRequest implements OCCIRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletInputParser.class);

    private HttpHeaders headers;
    private HttpServletRequest request;

    public OCCIServletInputParser(OCCIResponse response, String contentType, String username) {
        super(response, contentType, username);
    }

    /**
     * Build the data objects for usage in PUT, GET etc. when call findEntity etc.
     */
    @Override
    public void parseInput() throws ParseOCCIException {

        String content = null;


        switch (contentType) {
            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_JSON_OCCI:
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                // For all media type that have content occi build like json, xml, text plain, yml etc..
                if (request == null) {
                    throw new ParseOCCIException("No request to parse.");
                }

                InputStream jsonInput = null;
                LOGGER.info("Parsing input uploaded datas...");
                try {
                    jsonInput = request.getInputStream();
                    if (jsonInput == null) {

                        throw new ParseOCCIException("The input has no content delivered.");
                    }
                    content = Utils.convertInputStreamToString(jsonInput);

                    // for Object occiRequest to be fully completed.
                    getInputParser().parseInputToDatas(content);

                } catch (IOException ex) {
                    throw new ParseOCCIException("The server cant read the json file input --> " + ex.getMessage());
                } finally {
                    Utils.closeQuietly(jsonInput);
                }
                break;
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                // For all media type that have header definition only, known for now is text/occi.
                if (headers == null) {
                    throw new ParseOCCIException("Cannot parse for " + contentType + " cause: no request header.");
                }

                // for object occiRequest to be fully completed, the parameter is Map<String, List<String>> encapsulated on MultivaluedMap.)
                getInputParser().parseInputToDatas(headers.getRequestHeaders());
                break;
            default:
                throw new ParseOCCIException("Cannot parse for " + contentType + " cause: unknown parser");
        }
    }


    @Override
    public void validateRequest() throws ParseOCCIException {
        // Validate this request.
        // Is the kinds exists on this configuration's extensions ?
        // Is the mixins (with attributes ==> not a mixintag) exist on configuration's extensions ?
        // Is this request is an occi compliant request ?
        // TODO : !!!!
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }


}
