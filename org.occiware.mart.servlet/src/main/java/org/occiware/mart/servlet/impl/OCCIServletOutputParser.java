package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.AbstractOCCIResponse;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by cgourdin on 11/04/2017.
 * Implementation for Servlet output with parser (text/occi, application/json etc.)
 */
public class OCCIServletOutputParser extends AbstractOCCIResponse implements OCCIResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletOutputParser.class);
    private HttpServletResponse httpResponse;

    public OCCIServletOutputParser(String contentType, String username, HttpServletResponse response) {
        super(contentType, username);
        this.httpResponse = response;
    }

    private String getAcceptedTypes() {
        return Constants.MEDIA_TYPE_TEXT_OCCI + ";" + Constants.MEDIA_TYPE_JSON + ";" + Constants.MEDIA_TYPE_JSON_OCCI + ";" + Constants.MEDIA_TYPE_TEXT_PLAIN;
    }

    /**
     * This method is called by output parser (json - text/occi - text/plain) for each thing to parse in output response.
     *
     * @param response
     */
    @Override
    public void setResponse(Object response) {

        if (response == null) {
            LOGGER.warn("Response has no values.");
            response = "";
        }

        buildServerHeaders();

        switch (contentType) {
            case Constants.MEDIA_TYPE_JSON_OCCI:
            case Constants.MEDIA_TYPE_JSON:
            case Constants.MEDIA_TYPE_TEXT_PLAIN:
                // Set the response content.
                writeContentToResponse((String) response);

                break;
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                // Set the header values only.
                if (response instanceof String) {
                    httpResponse.setHeader("message", (String) response);
                    writeContentToResponse((String) response);

                } else if (response instanceof HeaderPojo) {

                    // Write headers
                    HeaderPojo headerPojo = (HeaderPojo) response;

                    Map<String, List<String>> respMap = headerPojo.getHeaderMap();
                    String key;
                    List<String> values;
                    for (Map.Entry<String, List<String>> entry : respMap.entrySet()) {
                        key = entry.getKey();
                        values = entry.getValue();

                        if (values.size() == 1) {
                            httpResponse.setHeader(key, values.get(0));
                        } else {
                            for (String value : values) {
                                // Add multiple header for same key.
                                // Used for same keys and locations rendering collection.
                                httpResponse.addHeader(key, value);
                            }
                        }
                    }
                }
                break;
        }

        // Define status.
        if (hasExceptions()) {
            if (getExceptionThrown() instanceof ParseOCCIException) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                // All others are models and runtime operation exception.
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            writeContentToResponse(getExceptionMessage());

        } else {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
        }

    }

    /**
     * Method to write content directly on httpResponse object.
     *
     * @param content
     */
    private void writeContentToResponse(final String content) {
        try {
            PrintWriter respWriter = httpResponse.getWriter();
            respWriter.println(content);

        } catch (IOException ex) {
            LOGGER.error("Cannot write content output response to httpServletResponse.");
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Assign generic server header to httpResponse object.
     */
    private void buildServerHeaders() {
        httpResponse.setHeader("server", Constants.OCCI_SERVER_HEADER);
        httpResponse.setHeader("accept", getAcceptedTypes());
        httpResponse.setContentType(contentType);
    }

    /**
     * Parse a message using output parser.
     *
     * @param message
     * @param httpStatus
     * @return
     */
    public HttpServletResponse parseMessage(String message, int httpStatus) {

        try {
            getOutputParser().parseMessage(message, httpStatus);
            httpResponse.setStatus(httpStatus);
        } catch (ParseOCCIException ex) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        return httpResponse;
    }
}
