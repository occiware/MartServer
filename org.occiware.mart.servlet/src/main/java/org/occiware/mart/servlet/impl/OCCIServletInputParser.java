package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.facade.AbstractOCCIRequest;
import org.occiware.mart.server.facade.OCCIRequest;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.parser.Data;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.server.utils.Utils;
import org.occiware.mart.servlet.utils.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by cgourdin on 11/04/2017.
 *
 */
public class OCCIServletInputParser extends AbstractOCCIRequest implements OCCIRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCCIServletInputParser.class);

    private HeaderPojo headers;
    private HttpServletRequest request;
    private String requestPath;
    private Map<String, String> requestParameters;

    public OCCIServletInputParser(OCCIResponse response, String contentType, String username, HttpServletRequest req, HeaderPojo headers, Map<String, String> requestParameters) {
        super(response, contentType, username);
        this.headers = headers;
        this.request = req;
        this.requestPath = req.getPathInfo();
        this.requestParameters = requestParameters;
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

                InputStream in = null;
                LOGGER.info("Parsing input uploaded datas...");
                try {
                    in = request.getInputStream();
                    if (in == null) {
                        throw new ParseOCCIException("The input has no content delivered.");
                    } else {
                        content = Utils.convertInputStreamToString(in);
                    }
                    // for Object occiRequest to be fully completed.
                    getInputParser().parseInputToDatas(content);

                } catch (IOException ex) {
                    throw new ParseOCCIException("The server cant read the json file input --> " + ex.getMessage());
                } finally {
                    Utils.closeQuietly(in);
                }
                break;
            case Constants.MEDIA_TYPE_TEXT_OCCI:
                // For all media type that have header definition only, known for now is text/occi.
                if (headers == null) {
                    throw new ParseOCCIException("Cannot parse for " + contentType + " cause: no request header.");
                }

                // for object occiRequest to be fully completed, the parameter is Map<String, List<String>> encapsulated on MultivaluedMap.)
                getInputParser().parseInputToDatas(headers);

                break;
            default:
                throw new ParseOCCIException("Cannot parse for " + contentType + " cause: unknown parser");
        }

    }


    @Override
    public void validateRequest() {
        // Validate this request.
        // TODO : in abstract class for models validation..
        // Is the kinds exists on this configuration's extensions ?
        // Is the mixins (with attributes ==> not a mixintag) exist on configuration's extensions ?
        // Is this request is an occi compliant request ?

        // Define the operations to be done with parsed datas.
        for (Data data : this.getDatas()) {
            boolean hasLocationSet = false;
            boolean entityQuery = false;
            boolean mixinTagDefinitionRequest = false;
            boolean interfQuery = false;
            String locationWithoutUUID = null;
            String pathWithoutUUID = null;

            String uuid;
            if (data.getLocation() != null && !data.getLocation().isEmpty()) {
                hasLocationSet = true;
                uuid = ServletUtils.getUUIDFromPath(data.getLocation(), data.getAttrs());
                if (uuid != null) {
                    locationWithoutUUID = data.getLocation().replace(uuid, "");
                } else {
                    locationWithoutUUID = data.getLocation();
                }

            } else {
                // No location attribute is set so use the request relative path directly.
                uuid = ServletUtils.getUUIDFromPath(requestPath, data.getAttrs());
                if (uuid != null) {
                    pathWithoutUUID = requestPath.replace(uuid, "");
                } else {
                    pathWithoutUUID = requestPath;
                }
            }

            // Detect if this is a mixin tag request definition or an interface query (/-/).
            String pathTmp = "/" + requestPath + "/";
            if (pathTmp.equals("/.well-known/org/ogf/occi/-/") || pathTmp.endsWith("/-/")) {
                if (hasLocationSet && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
                    data.setMixinTagDefinitionRequest(true);
                } else {
                    data.setInterfQuery(true);
                }
                continue;
            }
            if (hasLocationSet && data.getMixinTag() != null && !data.getMixinTag().isEmpty()) {
                data.setMixinTagDefinitionRequest(true);
                continue;
            }

            // Determine if this is an action invocation.
            if (data.getAction() != null && !data.getAction().isEmpty() || (requestParameters != null && requestParameters.get("action") != null)) {
                data.setActionInvocationQuery(true);
                continue;
            }
            // Is the path is an entity path ?
            //  if a location is defined, this replace the given path.
            if (hasLocationSet) {
                // the attributes is used if occi.core.id is defined for the current data to work with.
                entityQuery = ServletUtils.isEntityUUIDProvided(data.getLocation(), data.getAttrs());
            } else {
                entityQuery = ServletUtils.isEntityUUIDProvided(requestPath, data.getAttrs());
            }
            if (entityQuery) {
                data.setEntityQuery(true);
                continue;
            }

            // Detect if entity query or collections.
            entityQuery = data.getEntityUUID() != null;
            if (!entityQuery) {
                boolean pathHasEntitiesBehind;
                // Check if location has entities behind.
                List<String> uuids;
                if (hasLocationSet) {
                    uuids = ServletUtils.getEntityUUIDsFromPath(data.getLocation());

                } else {
                    uuids = ServletUtils.getEntityUUIDsFromPath(requestPath);
                }

                // Check if a kind is defined in inputdata, if this is the case, it must be an entity query.
                if (data.getKind() != null && !uuids.isEmpty()) {
                    data.setEntityQuery(true);
                    continue;
                }
            }

            if (!entityQuery) {
                data.setCollectionQuery(true);
            }
        }

    }

    public HeaderPojo getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderPojo headers) {
        this.headers = headers;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
}
