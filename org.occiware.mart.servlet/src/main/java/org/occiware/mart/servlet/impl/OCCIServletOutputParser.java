package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.facade.AbstractOCCIResponse;
import org.occiware.mart.server.facade.OCCIResponse;
import org.occiware.mart.server.utils.Constants;
import org.occiware.mart.servlet.exception.ResponseParseException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by cgourdin on 11/04/2017.
 */
public class OCCIServletOutputParser extends AbstractOCCIResponse implements OCCIResponse {

    public OCCIServletOutputParser(String contentType, String username) {
        super(contentType, username);
    }

    private String getAcceptedTypes() {
        return Constants.MEDIA_TYPE_TEXT_OCCI + ";" + Constants.MEDIA_TYPE_JSON + ";" + Constants.MEDIA_TYPE_JSON_OCCI + ";" + MediaType.TEXT_PLAIN;
    }


    // Specific to jax rs and jersey.

    /**
     * Render a message (error, warning etc.)
     *
     * @param msg
     * @return a jax rs response.
     */
    public Response renderSimpleOutputMessage(String msg, Response.Status status) {
        if (msg != null && !msg.isEmpty()) {

        }
    }

    /**
     * Default with response status ok.
     *
     * @param object
     * @return
     * @throws ResponseParseException
     */
    public Response parseResponse(Object object) throws ResponseParseException {
        return parseResponse(object, Response.Status.OK);
    }

    public Response parseResponse(Object object, Response.Status status) throws ResponseParseException {
        Response response = null;

        String msg;
        if (contentType.equals(Constants.MEDIA_TYPE_TEXT_OCCI)) {
            // Specific because all responses goes to header, not the content.
            response = buildTextOcciResponse(status);
            if (response == null) {
                throw new ResponseParseException("Cannot parse the object to text/occi representation.");
            }

        } else {
            if (object instanceof Response) {
                msg = (String) ((Response) object).getEntity();

                response = Response.fromResponse((Response) object)
                        .header("Server", Constants.OCCI_SERVER_HEADER)
                        .header("Accept", getAcceptedTypes())
                        .type(contentType)
                        .entity(msg)
                        .status(status)
                        .build();
            }

            if (object instanceof String) {
                msg = (String) object;
                response = Response.status(status)
                        .header("Server", Constants.OCCI_SERVER_HEADER)
                        .header("Accept", getAcceptedTypes())
                        .entity(msg)
                        .type(contentType)
                        .build();
            }
        }

        return response;
    }

    /**
     * Specific to TextOCCI renderer.
     *
     * @param status
     * @return
     */
    private Response buildTextOcciResponse(Response.Status status) {

        // TODO : Refactor chain text/occi.


        String attributes = occiParser.getAttributes();
        String categories = occiParser.getCategories();
        String xOCCILocation = occiParser.getxOCCILocations();
        String absoluteEntityLocation = getServerURI().toString() + occiParser.getRelativeLocation();

        // Build links value
        javax.ws.rs.core.Link[] links = renderActionsLink(occiResponse.getKindTerm(), absoluteEntityLocation);

        String msg = "ok \n";

        response = Response.status(status)
                .header("Server", Constants.OCCI_SERVER_HEADER)
                .header(Constants.CATEGORY, categories)
                .header(Constants.X_OCCI_ATTRIBUTE, renderAttributes(entity))
                .header(Constants.X_OCCI_LOCATION, renderXOCCILocationAttr(entity))
                .header("Accept", getAcceptedTypes())
                .type(Constants.MEDIA_TYPE_TEXT_OCCI)
                .entity(msg)
                .links(links)
                .build();
        return response;
    }

    /**
     * Render Link: header (cf spec text rendering).
     *
     * @param entityKindTerm
     * @param entityAbsolutePath
     * @return An array of Link to set to header.
     */
    private javax.ws.rs.core.Link[] renderActionsLink(String entityKindTerm, final String entityAbsolutePath) {
        // LOGGER.info("Entity location : " + entityAbsolutePath);
        javax.ws.rs.core.Link linkAbsoluteEntityPath = javax.ws.rs.core.Link.fromUri(entityAbsolutePath)
                .title(entityKindTerm)
                .build();
        javax.ws.rs.core.Link[] links;
        int linkSize = 1;
        int current = 0;

        // For each actions we add the link like this : <mylocation?action=actionTerm>; \
        //    rel="http://actionScheme#actionTerm"
        List<Action> actionsTmp = entity.getKind().getActions();
        linkSize += actionsTmp.size();
        links = new javax.ws.rs.core.Link[linkSize];
        links[0] = linkAbsoluteEntityPath;
        current++;
        javax.ws.rs.core.Link actionLink;

        // We render the Link header.
        if (!actionsTmp.isEmpty()) {
            // We render the different link here.
            for (Action action : actionsTmp) {

                actionLink = javax.ws.rs.core.Link.fromUri(entityAbsolutePath)
                        .title(action.getTerm())
                        .rel(action.getScheme() + action.getTerm())
                        .build();
                links[current] = actionLink;
                current++;
            }
        }

        return links;
    }

    public Response renderTextOcciResponseOk() {
        response = Response.ok().entity("ok \n")
                .header("Server", Constants.OCCI_SERVER_HEADER)
                .header("interface", sb.toString())
                .type(Constants.MEDIA_TYPE_TEXT_OCCI)
                .header("Accept", getAcceptedTypes())
                .entity("ok")
                .build();
    }

    public Response renderTextOcciResponseEntity() {

//
//        String absoluteEntityLocation = getServerURI().toString() + relativeLocation;
//
//        // Convert all actions to links.
//        javax.ws.rs.core.Link[] links = renderActionsLink(entity, absoluteEntityLocation);
//        String msg = "ok \n";
//        response = Response.status(status)
//                .header("Server", Constants.OCCI_SERVER_HEADER)
//                .header(Constants.CATEGORY, categories)
//                .header(Constants.X_OCCI_ATTRIBUTE, renderAttributes(entity))
//                .header(Constants.X_OCCI_LOCATION, renderXOCCILocationAttr(entity))
//                .header("Accept", getAcceptedTypes())
//                .type(Constants.MEDIA_TYPE_TEXT_OCCI)
//                .entity(msg)
//                .links(links)
//                .build();
//
    }

    public Response renderError() {
        msg = "Error while parsing the response to application/json representation";
        MessageJson msgJson = new MessageJson();
        msgJson.setMessage(msg + " --> " + ex.getMessage());
        msgJson.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        try {
            response = Response.status(status)
                    .header("Server", Constants.OCCI_SERVER_HEADER)
                    .header("Accept", getAcceptedTypes())
                    .entity(msgJson.toStringJson())
                    .type(Constants.MEDIA_TYPE_JSON)
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
            return response;
        } catch (JsonProcessingException e) {
            throw new ResponseParseException(e.getMessage());
        }
    }


    public Response parseEmptyResponse(Response.Status status) {

        return null;
    }

}
