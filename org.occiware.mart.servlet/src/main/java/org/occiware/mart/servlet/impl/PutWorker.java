package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class PutWorker extends ServletEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PutWorker.class);

    public PutWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    /**
     * Put request authorized only the creation of mixin tag and entity (only one).
     * @return
     */
    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();
        // Root request are not allowed by PUT method ==> 405 http error.
        if (occiRequest.getRequestPath().trim().isEmpty() || occiRequest.getRequestPath().equals("/")) {
            return occiResponse.parseMessage("This url : " + occiRequest.getRequestPath() + " is not supported by HTTP PUT method.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }
        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        if (datas.isEmpty()) {
            return occiResponse.parseMessage("No content to put.", HttpServletResponse.SC_BAD_REQUEST);
        }
        // Check if PUT has more than one content data.
        if (datas.size() > 1 && !controlIfDatasHasMixinTagsOnly()) {
            return occiResponse.parseMessage("Content has more than one entity to put, this is not authorized on HTTP PUT request, please use POST request for entity collection creation.", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        // There is content so check it.
        occiRequest.validateInputDataRequest();
        if (occiResponse.hasExceptions()) {
            // Validation failed.
            return occiResponse.getHttpResponse();
        }

        if (occiRequest.isActionInvocationQuery()) {
            LOGGER.warn("Querying action invocation on PUT method.");
            return occiResponse.parseMessage("You cannot use an action with PUT method.", HttpServletResponse.SC_BAD_REQUEST);
        }
        boolean isMixinTags = false;
        for (OCCIRequestData data : datas) {
            if (occiRequest.isInterfQuery() && data.getMixinTag() == null) {
                return occiResponse.parseMessage("you cannot use interface query on PUT method, only mixin tag content are authorized with this url type : /-/", HttpServletResponse.SC_BAD_REQUEST);
            }

            // determine if this is a mixin tag definition request.
            if (occiRequest.isInterfQuery() && data.getMixinTag() != null) {
                occiRequest.createMixinTag(data.getMixinTagTitle(), data.getMixinTag(), data.getLocation(), data.getXocciLocations());
                isMixinTags = true;
                if (occiResponse.hasExceptions()) {
                    return resp;
                }
                continue;
            }

            // This is a feature that override the PUT request path if data.getLocation() return null (no location set on request content).
            if (data.getLocation() == null) {
                // Location has not been set on content, this is set on request path like /mycompute/myentity/.
                data.setLocation(occiRequest.getRequestPath());
            }

            // This is an entity creation query.
            occiRequest.createEntity(data.getEntityTitle(), data.getEntitySummary(), data.getKind(), data.getMixins(), data.getAttrsValStr(), data.getLocation());
        }
        
        if (isMixinTags) {
            // All ok, mixin tags defined.
            occiResponse.parseResponseMessage("ok");
        }

        return resp;
    }

    /**
     * True if all content data are mixin tags
     * @return
     */
    private boolean controlIfDatasHasMixinTagsOnly() {
        boolean result = false;
        List<OCCIRequestData> datas = occiRequest.getContentDatas();
        for (OCCIRequestData data : datas) {
            if (data.getMixinTag() == null) {
                result = false;
                break;
            } else {
                result = true;
            }
        }
        return result;
    }


}
