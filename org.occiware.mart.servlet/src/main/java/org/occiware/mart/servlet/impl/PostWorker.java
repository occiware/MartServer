package org.occiware.mart.servlet.impl;

import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.HeaderPojo;
import org.occiware.mart.server.utils.CollectionFilter;
import org.occiware.mart.server.utils.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
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
        
        if (occiRequest.isInterfQuery()) {
            return occiResponse.parseMessage("you cannot use interface query on POST method", HttpServletResponse.SC_BAD_REQUEST);
        }

        for (OCCIRequestData data : datas) {

            // Action on entities.
            if (occiRequest.isActionInvocationQuery()) {

                occiRequest.findEntitiesLocations(occiRequest.getRequestPath(), buildCollectionFilter());

                if (!occiResponse.hasExceptions()) {


                    // finally execute the action on entities found previously.
                    List<OCCIRequestData> locationsOut = occiResponse.getOutputParser().getOutputDatas();
                    List<String> locations = new LinkedList<>();
                    for (OCCIRequestData locationData : locationsOut) {
                        locations.add(locationData.getLocation());
                    }
                    occiRequest.executeActionOnEntities(data.getAction(), data.getAttrsValStr(), locations);
                }
                return occiResponse.getHttpResponse();
            }

            // Entities update attributes and/or mixin and mixinTag association.
            if (occiRequest.isEntityLocation(occiRequest.getRequestPath())) {
                // Update entity(ies).
                occiRequest.updateEntity(data.getMixins(), data.getAttrsValStr(), data.getLocation(), data.getMixinTag(), data.getXocciLocations());
            }

        }

        resp = occiResponse.getHttpResponse();

        return resp;
    }
}
