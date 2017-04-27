/**
 * Copyright (c) 2015-2017 Inria
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.mart.server.parser.text;

import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.parser.*;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

/**
 * text/occi rendering are in headers not in body, this is not a text/plain (or
 * text/occi+plain).
 *
 * @author Christophe Gourdin
 */
public class TextOcciParser extends AbstractRequestParser implements IRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextOcciParser.class);


    //*************************
    // Read input content part
    //*************************

    /**
     * Parse the input query in multiple OCCIRequestData objects (or only one if one thing defined).
     *
     * @param contentObj This is the content json.
     * @throws ParseOCCIException
     */
    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {
        if (contentObj == null || !(contentObj instanceof HeaderPojo)) {
            throw new ParseOCCIException("The content to parse has not the good type or has no header content at all.");
        }
        HeaderPojo contentHeader = (HeaderPojo) contentObj;

        // get the kind and mixins from query.
        parseOcciCategories(contentHeader);

        // Get the occi attributes defined in query.
        parseOcciAttributes(contentHeader);


    }


    /**
     * Parse Header input category.
     *
     * @param contentHeader
     * @throws ParseOCCIException
     */
    private void parseOcciCategories(HeaderPojo contentHeader) throws ParseOCCIException {
        Map<String, List<String>> map = contentHeader.getHeaderMap();
        OCCIRequestData data = new OCCIRequestData();

        List<String> values;
        List<String> mixinsToAdd = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equals(Constants.CATEGORY)) {
                // Check the class value.
                // check if this is a kind or a mixin.
                // As it may have kind and mixins, the value return will be a kind class before.
                // Example of value:
                // compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";

                for (String value : values) {
                    String[] valuesArray = value.split(",");

                    for (String line : valuesArray) {
                        if (line.startsWith(" ")) {
                            line = Constants.CATEGORY + ":" + line;
                        } else {
                            line = Constants.CATEGORY + ": " + line;
                        }
                        // String line = Constants.CATEGORY + ": " + value;
                        Matcher matcher = Constants.PATTERN_CATEGORY.matcher(line);
                        if (!matcher.find()) {
                            continue;
                        }
                        String term = matcher.group(Constants.GROUP_TERM);
                        String scheme = matcher.group(Constants.GROUP_SCHEME);
                        String categoryClass = matcher.group(Constants.GROUP_CLASS);

                        if (categoryClass.equalsIgnoreCase(Constants.CLASS_KIND)) {
                            // Assign the kind.
                            data.setKind(scheme + term);
                            continue;
                        }
                        if (categoryClass.equalsIgnoreCase(Constants.CLASS_MIXIN)) {
                            if (matcher.group(Constants.GROUP_LOCATION) != null) {
                                // is a mixin tag.
                                data.setLocation(matcher.group(Constants.GROUP_LOCATION));
                                data.setMixinTag(scheme + term);

                            } else {
                                // is a simple mixin.
                                mixinsToAdd.add(scheme + term);
                            }
                            continue;
                        }
                        if (categoryClass.equalsIgnoreCase(Constants.CLASS_ACTION)) {
                            data.setAction(scheme + term);
                        }
                    }
                }
            }
        }
        if (!mixinsToAdd.isEmpty()) {
            data.setMixins(mixinsToAdd);
        }
        super.getInputDatas().add(data);
    }

    /**
     * Convert X-OCCI-Attribute to map key --> value and add it to data object (read input).
     *
     * @param contentHeader
     * @throws ParseOCCIException
     */
    private void parseOcciAttributes(HeaderPojo contentHeader) throws ParseOCCIException {
        if (super.getInputDatas().isEmpty()) {
            throw new ParseOCCIException("No kind or mixins, so no attributes.");
        }
        OCCIRequestData data = super.getInputDatas().get(0);
        Map<String, List<String>> map = contentHeader.getHeaderMap();

        List<String> values;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            values = entry.getValue();
            if (key.equalsIgnoreCase(Constants.X_OCCI_ATTRIBUTE)) {
                for (String value : values) {
                    // Multiple value on X_OCCI_Attribute header may exist on a same declaration. (source and target for example).
                    // like this --> X-OCCI-Attribute: occi.core.source="/compute/f88486b7-0632-482d-a184-a9195733ddd0", occi.core.target="/network/network1".
                    String[] valuesTmp = value.split(",");
                    for (String valueTmp : valuesTmp) {
                        // Parse the value:
                        String[] attr = valueTmp.split("=");
                        if (attr.length > 1) {
                            attr[0] = attr[0].replace("\"", "");
                            if (attr[0].startsWith(" ")) {
                                attr[0] = attr[0].substring(1); // remove starting space.
                            }
                            attr[1] = attr[1].replace("\"", "");
                            data.getAttrs().put(attr[0], attr[1]);
                        }
                    }

                }
            }
            if (key.equalsIgnoreCase(Constants.X_OCCI_LOCATION)) {
                for (String value : values) {
                    String[] valuesTmp = value.split(",");
                    for (String valueTmp : valuesTmp) {
                        data.addXocciLocation(valueTmp);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Build interface /-/ for accept type : text/occi.
     *
     *
     * @param interfaceData
     * @param user (the authorized username)
     * @return interface to set in header.
     */
    @Override
    public HeaderPojo getInterface(final QueryInterfaceData interfaceData, final String user) throws ParseOCCIException {

        List<Kind> kinds = interfaceData.getKinds();
        List<Mixin> mixins = interfaceData.getMixins();

        StringBuilder sb = renderOcciKindsActions(kinds, true);

        sb.append(renderOcciMixins(mixins, true));

        String msg = sb.toString();
        if (!msg.isEmpty()) {
            if (msg.getBytes().length > 8000) {
                throw new ParseOCCIException("Limit size of header oversized : " + msg.getBytes().length + " > 8000 :");
            }
        } else {
            throw new ParseOCCIException("No interface to render.");
        }
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        List<String> interfaces = new LinkedList<>();
        interfaces.add(msg);
        headerMap.put("interface", interfaces);
        return new HeaderPojo(headerMap);
    }

    @Override
    public String parseMessage(String message) throws ParseOCCIException {
        // This may be rendered in header and in content if this is an error message.
        return message;
    }

    @Override
    public String parseMessageAndStatus(final String message, int status) throws ParseOCCIException {
        return message + "; status=" + status;
    }

    @Override
    public HeaderPojo renderOutputEntity(Entity entity) throws ParseOCCIException {
        if (entity == null) {
            Map<String, List<String>> header = new LinkedHashMap<>();
            return new HeaderPojo(header);
        }
        String categories = renderCategory(entity.getKind(), false);

        // if entity as mixins, update categories as expected.
        List<Mixin> mixinsTmp = entity.getMixins();
        StringBuilder sb = new StringBuilder();
        sb.append(categories);
        for (Mixin mixin : mixinsTmp) {
            sb.append(renderCategory(mixin, false));
        }
        categories = sb.toString();
        if (categories.trim().isEmpty()) {
            throw new ParseOCCIException("No category to render.");
        }

        String entityAttrs = renderAttributes(entity);

        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        List<String> cats = new LinkedList<>();
        List<String> attrs = new LinkedList<>();
        List<String> xOcciLocation = new LinkedList<>();

        cats.add(categories);
        attrs.add(entityAttrs);
        xOcciLocation.add(renderXOCCILocationAttr(entity));

        List<String> links = renderActionLinksHeader(entity);

        headerMap.put(Constants.CATEGORY, cats);
        headerMap.put(Constants.X_OCCI_ATTRIBUTE, attrs);
        headerMap.put(Constants.X_OCCI_LOCATION, xOcciLocation);
        headerMap.put(Constants.LINK, links);

        List<Entity> entities = new ArrayList<>();
        entities.add(entity);

        super.convertEntitiesToOutputData(entities);

        return new HeaderPojo(headerMap);
    }


    @Override
    public HeaderPojo renderOutputEntities(List<Entity> entities) throws ParseOCCIException {
        HeaderPojo result;
        // We render only the first entity found, cause to limit size of header.
        if (entities != null && !entities.isEmpty()) {
            result = renderOutputEntity(entities.get(0));
            return result;
        }
        Map<String, List<String>> header = new LinkedHashMap<>();
        result = new HeaderPojo(header);

        super.convertEntitiesToOutputData(entities);

        return result;
    }


    @Override
    public HeaderPojo renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException {
        HeaderPojo header;
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        if (!locations.isEmpty()) {
            // String absLocation = getServerURI().toString() + location;
            // responseBuilder.header(Constants.X_OCCI_LOCATION, absLocation);
            headerMap.put(Constants.X_OCCI_LOCATION, locations);
            header = new HeaderPojo(headerMap);
            return header;
        } else {
            header = new HeaderPojo(headerMap);
        }

        super.convertLocationsToOutputDatas(locations);

        return header;
    }

    /**
     * Render Link: header (cf spec text rendering).
     *
     * @param entity
     * @return An array of Link to set to header.
     */
    private List<String> renderActionLinksHeader(final Entity entity) {
        String location = EntityManager.getLocation(entity);
        List<String> actionLinks = new LinkedList<>();
        LOGGER.info("Entity location : " + location);
        int linkSize = 1;
        int current = 0;
        String actionLink;
        List<Action> actionsTmp = entity.getKind().getActions();
        for (Action action : actionsTmp) {
            actionLink = location + entity.getId() + "?action=" + action.getTerm() + ";" + "rel=\""+action.getScheme()+action.getTerm() + "\"";
            actionLinks.add(actionLink);
        }

        // For each actions we add the link like this : <mylocation?action=actionTerm>; \
        //    rel="http://actionScheme#actionTerm"
        return actionLinks;
    }



    /**
     * Get text/occi for occi Kinds and actions.
     *
     * @param kinds
     * @param detailed
     * @return
     */
    private StringBuilder renderOcciKindsActions(List<Kind> kinds, boolean detailed) {
        StringBuilder sb = new StringBuilder();

        for (Kind kind : kinds) {
            if (!kind.getScheme().equals(Constants.OCCI_CORE_SCHEME)) {
                renderOcciKind(kind, detailed, sb);
            }
        }
        return sb;
    }

    /**
     * Get text/occi for occi mixins and dependencies.
     *
     * @param mixins
     * @param detailed
     * @return
     */
    private StringBuilder renderOcciMixins(List<Mixin> mixins, boolean detailed) {
        StringBuilder sb = new StringBuilder();
        for (Mixin mixin : mixins) {
            renderOcciMixin(detailed, sb, mixin);

        }
        return sb;
    }

    private void renderOcciMixin(boolean detailed, StringBuilder sb, Mixin mixin) {
        sb.append(mixin.getTerm()).append(";").append(Constants.CRLF)
                .append("scheme=\"").append(mixin.getScheme()).append("\";").append(Constants.CRLF)
                .append("class=\"mixin\"").append(";");
        if (detailed) {
            sb.append(Constants.CRLF);
            sb.append("title=\"").append(mixin.getTitle()).append('\"').append(";").append(Constants.CRLF);
            List<Mixin> mixinsDep = mixin.getDepends();
            if (!mixinsDep.isEmpty()) {
                sb.append("rel=\"");
                String sep = "";
                for (Mixin md : mixinsDep) {
                    sb.append(sep).append(md.getScheme()).append(md.getTerm());
                    sep = " ";
                }
                sb.append('\"').append(";").append(Constants.CRLF);
            }
            appendCategoryLocation(sb, ConfigurationManager.getLocation(mixin));
            appendAttributes(sb, mixin.getAttributes());
            appendActions(sb, mixin.getActions());
        }
    }

    private void appendCategoryLocation(StringBuilder sb, String location) {
        sb.append("location=\"");
        sb.append(location);
        sb.append('\"');
        sb.append(";");
        sb.append(Constants.CRLF);
    }

    /**
     * Append attributes in text/occi format for kinds, mixins and entities
     * definition.
     *
     * @param sb
     * @param attributes
     */
    private void appendAttributes(StringBuilder sb, List<Attribute> attributes) {
        if (!attributes.isEmpty()) {
            sb.append("attributes=\"");
            String sep = "";
            for (Attribute attribute : attributes) {
                sb.append(sep).append(attribute.getName());
                if (attribute.isRequired() || !attribute.isMutable()) {
                    sb.append('{');
                    if (!attribute.isMutable()) {
                        sb.append("immutable");
                        if (attribute.isRequired()) {
                            sb.append(' ');
                        }
                    }
                    if (attribute.isRequired()) {
                        sb.append("required");
                    }
                    sb.append('}');
                }
                sep = " ";
            }
            sb.append('\"').append(";").append(Constants.CRLF);
        }
    }

    /**
     * Append action to string builder.
     *
     * @param sb
     * @param actions
     */
    private void appendActions(StringBuilder sb, List<Action> actions) {
        if (!actions.isEmpty()) {
            sb.append("actions=\"");
            String sep = "";
            for (Action action : actions) {
                sb.append(sep).append(action.getScheme()).append(action.getTerm());
                sep = " ";
            }
            sb.append('\"').append(";").append(Constants.CRLF);
        }
    }

//    /**
//     * Get the kind on header, for text/occi.
//     *
//     * @param headers
//     * @return
//     */
//    public String getKindFromHeader(HttpHeaders headers) {
//        String kind = null;
//
//        List<String> kindsVal = Utils.getFromValueFromHeaders(headers, Constants.CATEGORY);
//        // Search for Class="kind" value.
//        String[] vals;
//        boolean kindVal;
//        for (String line : kindsVal) {
//            kindVal = false;
//            vals = line.split(";");
//            // Check class="kind".
//            for (String val : vals) {
//                if (val.contains("class=\"" + Constants.CLASS_KIND + "\"")) {
//                    kindVal = true;
//                }
//            }
//            if (kindVal) {
//                // Get the kind value.
//                for (String val : vals) {
//                    if (val.contains(Constants.CATEGORY)) {
//                        String category = val.trim();
//
//                        // Get the value.
//                        kind = category.split(":")[1];
//                        LOGGER.info("Kind value is : " + kind);
//                    }
//                }
//            }
//        }
//        return kind;
//    }

    /**
     * Render a category with its definitions attributes etc.
     *
     * @param kind
     * @param detailed
     * @return
     */
    private String renderCategory(Kind kind, boolean detailed) {
        StringBuilder sb = new StringBuilder();

        renderOcciKind(kind, detailed, sb);
        return sb.toString();
    }

    private void renderOcciKind(Kind kind, boolean detailed, StringBuilder sb) {
        sb.append(kind.getTerm()).append(";").append(Constants.CRLF)
                .append("scheme=\"").append(kind.getScheme()).append("\";").append(Constants.CRLF)
                .append("class=\"kind\"").append(";");
        if (detailed) {
            sb.append(Constants.CRLF);
            sb.append("title=\"").append(kind.getTitle()).append('\"').append(";").append(Constants.CRLF);
            Kind parent = kind.getParent();
            if (parent != null) {
                sb.append("rel=\"").append(parent.getScheme()).append(parent.getTerm()).append('\"').append(";").append(Constants.CRLF);
            }
            appendCategoryLocation(sb, ConfigurationManager.getLocation(kind));
            appendAttributes(sb, kind.getAttributes());
            appendActions(sb, kind.getActions());
        }
    }

    /**
     * As kind we render here a mixin.
     *
     * @param mixin
     * @param detailed
     * @return
     */
    private String renderCategory(Mixin mixin, boolean detailed) {
        StringBuilder sb = new StringBuilder();

        renderOcciMixin(detailed, sb, mixin);

        return sb.toString();
    }


    /**
     * Return a string like :
     * "http://myabsolutepathserver:xxxx/myentitylocationrelativepath". This
     * will added to header with X-OCCI-Location name field.
     *
     * @param entity
     * @return
     */
    private String renderXOCCILocationAttr(final Entity entity) {
        return EntityManager.getLocation(entity);
    }

    /**
     * Render attributes used for GET request on entity.
     *
     * @param entity
     * @return
     */
    private String renderAttributes(Entity entity) {
        StringBuilder attributes = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        List<AttributeState> attrStates = entity.getAttributes();
        String coreId = Constants.OCCI_CORE_ID + "=\"" + Constants.URN_UUID_PREFIX + entity.getId() + "\"," + Constants.CRLF;
        sb.append(coreId);
        if (entity instanceof Link) {
            Link link = (Link) entity;
            String source = Constants.OCCI_CORE_SOURCE + "=\"" + EntityManager.getLocation(link.getSource()) + "\"" + "," + Constants.CRLF;
            sb.append(source);
            String target = Constants.OCCI_CORE_TARGET + "=\"" + EntityManager.getLocation(link.getTarget()) + "\"" + "," + Constants.CRLF;
            sb.append(target);
        }

        for (AttributeState attribute : attrStates) {
            String name = attribute.getName();
            if (name.equals(Constants.OCCI_CORE_ID) || name.equals(Constants.OCCI_CORE_SOURCE) || name.equals(Constants.OCCI_CORE_TARGET)) {
                continue;
            }
            String value = null;

            String valStr = EntityManager.getAttrValueStr(entity, name);
            Number valNumber = EntityManager.getAttrValueNumber(entity, name);

            if (valStr != null) {
                value = "\"" + valStr + "\"";
            } else if (valNumber != null) {
                value = "" + valNumber;
            } else {
                if (attribute.getValue() != null) {
                    value = "\"" + attribute.getValue() + "\"";
                }
            }
            // if value is null, it wont display in header.
            if (value == null) {
                continue;
            }

            attributes.append(attribute.getName()).append('=').append(value).append(",").append(Constants.CRLF);
        }

        if (attributes.length() > 0) {
            // To remove the last comma.
            attributes = new StringBuilder(attributes.substring(0, attributes.length() - 3));
            sb.append(attributes);
        }
        return sb.toString();
    }

}
