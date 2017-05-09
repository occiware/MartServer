package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by cgourdin on 13/04/2017.
 */
public class PostWorker extends ServletEntry {
    public PostWorker(URI serverURI, HttpServletResponse resp, HeaderPojo headers, HttpServletRequest req, String path) {
        super(serverURI, resp, headers, req, path);
    }

    public HttpServletResponse executeQuery() {

        HttpServletResponse resp = buildInputDatas();

        if (occiResponse.hasExceptions()) {
            return resp;
        }
        if (getContentType().equals(Constants.MEDIA_TYPE_TEXT_URI_LIST)) {
            return occiResponse.parseMessage("You cannot use Content-Type: text/uri-list that way, use a get collection request like http://yourhost:8080/compute/", HttpServletResponse.SC_BAD_REQUEST);
        }

        // There is content so check it.
        occiRequest.validateInputDataRequest();
        if (occiResponse.hasExceptions()) {
            // Validation failed.
            return occiResponse.getHttpResponse();
        }

        List<OCCIRequestData> datas = occiRequest.getContentDatas();

        if (datas.isEmpty()) {
            return occiResponse.parseMessage("No content to post.", HttpServletResponse.SC_BAD_REQUEST);
        }

        OCCIRequestData contentData;

        // Partial update of the entity.
        if (occiRequest.isOnEntityLocation() && !occiRequest.isActionInvocationQuery()) {
            contentData = datas.get(0);
            if (contentData.getLocation() == null) {
                // override with request path.
                contentData.setLocation(occiRequest.getRequestPath());
            }
            // Update entity.
            occiRequest.updateEntity(contentData.getMixins(), contentData.getAttrsValStr(), contentData.getLocation());
            return resp;
        }

        // Mixin tag definition (this may be multiple definitions.
        // Defines if datas has only mixinTags definition.

        if (occiRequest.isInterfQuery() && datas.size() >= 1 && !occiRequest.isActionInvocationQuery()) {
            boolean isMixinTags = true;
            for (OCCIRequestData data : datas) {
                if (data.getMixinTag() == null) {
                    isMixinTags = false;
                    break;
                }
            }

            if (!isMixinTags) {
                return occiResponse.parseMessage("Request is malformed, to define mixin tags, the content body must be only mixin tags contents.", HttpServletResponse.SC_BAD_REQUEST);
            }
            // Define the mixintags.
            for (OCCIRequestData data : datas) {
                occiRequest.createMixinTag(data.getMixinTagTitle(), data.getMixinTag(), data.getLocation(), data.getXocciLocations());
                if (occiResponse.hasExceptions()) {
                    return resp;
                }
            }
            // All ok, mixin tags defined.
            occiResponse.parseResponseMessage("ok");
            return resp;
        }

        // Mixin tag association ==> Add entity to a mixin tag collection defined by user. like on /my_stuff/ --> entities location on xOcciLocations values.
        if (occiRequest.isOnMixinTagLocation()) {

            // Add mixin tag defined to entities.
            // curl -v -X POST http://localhost:8080/my_stuff/ -H 'X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0'
            List<String> xOcciLocations;
            for (OCCIRequestData data : datas) {
                xOcciLocations = data.getXocciLocations();
                if (data.getMixinTag() != null) {
                    occiRequest.associateMixinToEntities(data.getMixinTag(), data.getLocation(), xOcciLocations);
                } else {
                    occiRequest.associateMixinToEntities(null, occiRequest.getRequestPath(), xOcciLocations);
                }
            }
            return resp;
        }
        // TODO : the following :

        // Create entity (or entity create collection) on /category/ (like /compute/ etc.) ==> for kind and mixin (not mixin tags).

        
        // Action invocation on entity location path.

        // Action invocation on entity collection.

        // Action invocation on mixin tag defined collection.







//        List<OCCIRequestData> entityDatas = occiRequest.getContentDatas();
//
//
//
//        List<OCCIRequestData> otherDatas = new LinkedList<>();
//
//        if (occiRequest.isOnCollectionLocation()) {
//            // Build a list of entities data to work with occi request api.
//            Iterator<OCCIRequestData> it = entityDatas.iterator();
//            OCCIRequestData data;
//            while (it.hasNext()) {
//                data = it.next();
//                if (data.getKind() == null) {
//                    otherDatas.add(data);
//                    it.remove();
//                }
//            }
//            if (!entityDatas.isEmpty()) {
//                occiRequest.createEntities(entityDatas);
//            }
//            if (occiResponse.hasExceptions()) {
//                return resp;
//            }
//
//
//
//        }
//
//
//
//        for (OCCIRequestData data : datas) {
//            if (occiRequest.isInterfQuery() && data.getMixinTag() == null) {
//                return occiResponse.parseMessage("you cannot use interface query on POST method, only mixin tag definition is allowed with POST method and interface query /-/", HttpServletResponse.SC_BAD_REQUEST);
//            }
//
//            // Action on entities.
//            if (occiRequest.isActionInvocationQuery()) {
//
//                occiRequest.findEntitiesLocations(occiRequest.getRequestPath(), buildCollectionFilter());
//
//                if (!occiResponse.hasExceptions()) {
//
//                    // finally execute the action on entities found previously.
//                    List<OCCIRequestData> locationsOut = occiResponse.getOutputParser().getOutputDatas();
//                    List<String> locations = new LinkedList<>();
//                    for (OCCIRequestData locationData : locationsOut) {
//                        locations.add(locationData.getLocation());
//                    }
//                    occiRequest.executeActionOnEntities(data.getAction(), data.getAttrsValStr(), locations);
//                }
//                return occiResponse.getHttpResponse();
//            }
//
//            // determine if this is a mixin tag definition request, this is authorized only on POST method (see OCCI spec http_protocol page 8).
//            if (occiRequest.isInterfQuery() && data.getMixinTag() != null) {
//                occiRequest.createMixinTag(data.getMixinTagTitle(), data.getMixinTag(), data.getLocation(), data.getXocciLocations());
//                isMixinTags = true;
//                if (occiResponse.hasExceptions()) {
//                    return resp;
//                }
//                continue;
//            }
//
//            // Entities update attributes and/or mixin and mixinTag association.
//            if (occiRequest.isOnCollectionLocation() && data.getKind() != null) {
//                if (data.getLocation() == null) {
//                    return occiResponse.parseMessage("No location set for entity : " + data.getEntityTitle() + " , id: " + data.getEntityUUID(), HttpServletResponse.SC_BAD_REQUEST);
//                }
//                occiRequest.createEntity(data.getEntityTitle(), data.getEntitySummary(), data.getKind(), data.getMixins(), data.getAttrsValStr(), data.getLocation());
//
//            } else if (occiRequest.isOnEntityLocation()) {
//                if (data.getLocation() == null) {
//                    // override with request path.
//                    data.setLocation(occiRequest.getRequestPath());
//                }
//                // Update entity.
//                occiRequest.updateEntity(data.getMixins(), data.getAttrsValStr(), data.getLocation());
//            }
//            if (occiResponse.hasExceptions()) {
//                return resp;
//            }
//
//        }
//
//        if (occiRequest.isOnCollectionLocation() && !occiResponse.hasExceptions()) {
//            // Return the same as input data.
//            occiResponse.getOutputParser().setOutputDatas(datas);
//
//        }
//
//
//
//        if (isMixinTags && !occiResponse.hasExceptions()) {
//            // All ok, mixin tags defined.
//            occiResponse.parseResponseMessage("ok");
//            return resp;
//        }

        if (occiResponse.hasExceptions()) {
            return resp;
        }

        // If we are here this is an unknown request.
        occiResponse.parseMessage("The request is malformed", HttpServletResponse.SC_BAD_REQUEST);

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
