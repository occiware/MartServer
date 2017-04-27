package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.exception.ResourceNotFoundException;
import org.occiware.mart.server.facade.AbstractOCCIApiResponse;
import org.occiware.mart.server.facade.OCCIApiResponse;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.parser.IRequestParser;
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
public class OCCIServletOutputResponse extends AbstractOCCIApiResponse implements OCCIApiResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletOutputResponse.class);
    private HttpServletResponse httpResponse;
    private final String acceptType;

    public OCCIServletOutputResponse(String acceptType, String username, HttpServletResponse response, IRequestParser parser) {
        super(username, parser);
        this.httpResponse = response;
        this.acceptType = acceptType;
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
    public void setResponseMessage(Object response) {
        super.setResponseMessage(response);
        if (response == null) {
            LOGGER.warn("Response has no values.");
            response = "";
        }

        buildServerHeaders();

        switch (acceptType) {
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
            } else if (getExceptionThrown() instanceof ResourceNotFoundException) {
                httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                // All others are models and runtime operation exception.
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            writeContentToResponse(getExceptionMessage());

        } else {
            // Default to OK response ==> 200.
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
        httpResponse.setContentType(acceptType);
    }

    /**
     * Parse a message using output parser.
     *
     * @param message
     * @param httpStatus
     * @return
     */
    public HttpServletResponse parseMessage(final String message, final int httpStatus) {

        try {
            setResponseMessage(getOutputParser().parseMessageAndStatus(message, httpStatus));

        } catch (ParseOCCIException ex) {
            LOGGER.warn("Parsing message failed : " + ex.getMessage());
            this.setExceptionMessage(ex.getMessage());
            this.setExceptionThrown(ex);
            this.setResponseMessage(message);
        }
        httpResponse.setStatus(httpStatus);
        return httpResponse;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }
}
