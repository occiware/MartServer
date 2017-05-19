package org.occiware.mart.server.parser.text;

import org.occiware.clouddesigner.occi.*;
import org.occiware.mart.server.exception.ParseOCCIException;
import org.occiware.mart.server.model.ConfigurationManager;
import org.occiware.mart.server.model.EntityManager;
import org.occiware.mart.server.model.KindManager;
import org.occiware.mart.server.model.MixinManager;
import org.occiware.mart.server.parser.AbstractRequestParser;
import org.occiware.mart.server.parser.IRequestParser;
import org.occiware.mart.server.parser.OCCIRequestData;
import org.occiware.mart.server.parser.QueryInterfaceData;
import org.occiware.mart.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by cgourdin on 26/04/2017.
 * text/plain occi compliant parser.
 */
public class TextPlainParser extends AbstractRequestParser implements IRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextPlainParser.class);

    public TextPlainParser(String user) {
        super(user);
    }


    @Override
    public void parseInputToDatas(Object contentObj) throws ParseOCCIException {
        String message;
        if (contentObj == null || !(contentObj instanceof String)) {
            throw new ParseOCCIException("The object parameter must be a String content object");
        }

        String content = (String) contentObj;
        if (content.isEmpty()) {
            // No content input.
            super.getInputDatas().clear();
            // parse nothing, there is no content.
            return;
        }

        // Read the contents data line by line.
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            // Read the first line containing a category.
            String line = reader.readLine();
            LOGGER.debug("Content received : ");
            LOGGER.debug("line=" + line);
            Kind kind = parseInputKind(line);

            String lastLineRead = line;

            // Parse all declared mixins
            List<Mixin> mixins = new LinkedList<>();
            lastLineRead = parseInputMixins(reader, mixins);
            List<String> mixinsStr = new LinkedList<>();
            for (Mixin mixin : mixins) {
                mixinsStr.add(mixin.getScheme() + mixin.getTerm());
            }

            OCCIRequestData data = new OCCIRequestData();
            data.setKind(kind.getScheme() + kind.getTerm());
            data.setMixins(mixinsStr);

            List<String> xOcciLocations = new LinkedList<>();

            // Parse attributes and x occi location fields.
            parseInputAttributes(reader, lastLineRead, data);

            // put data to data content list.
            super.getInputDatas().add(data);

        } catch (IOException ex) {
            throw new ParseOCCIException("Error while parsing text/plain content : " + ex.getMessage(), ex);
        }


    }

    /**
     * Parse all the attributes content.
     *
     * @param reader to read all attributes lines.
     * @return a map of String, Object attributes where key is the attribute name and value is an object.
     * @throws ParseOCCIException
     * @throws IOException
     */
    private Map<String, Object> parseInputAttributes(BufferedReader reader, String line, OCCIRequestData data) throws ParseOCCIException, IOException {
        String message;
        String location;
        Map<String, Object> resultMap = new HashMap<>();
        boolean isXocciAttribute;
        boolean isXocciLocation;
        while (line != null) {
            isXocciAttribute = false;
            isXocciLocation = false;
            location = null;
            if (line.startsWith(Constants.X_OCCI_ATTRIBUTE)) {
                isXocciAttribute = true;
            }
            if (line.startsWith(Constants.X_OCCI_LOCATION)) {
                isXocciLocation = true;
            }

            if (!isXocciAttribute && !isXocciLocation) {
                message = "Attribute is not well formed, it must be X-OCCI-Attributes or X-OCCI-Location !";
                LOGGER.warn(message);
                throw new ParseOCCIException(message);
            }
            if (isXocciAttribute) {
                line = line.replaceAll(Constants.X_OCCI_ATTRIBUTE, "");
                line = line.replaceAll(": ", "");
                String[] parts = line.split("=");
                String value = parts[1];
                String key = parts[0];
                if (key == null) {
                    continue;
                }
                switch (key) {
                    case Constants.OCCI_CORE_ID:
                        data.setEntityUUID(value);
                        break;
                    case Constants.OCCI_CORE_TITLE:
                        data.setEntityTitle(value);
                        break;
                    case Constants.OCCI_CORE_SUMMARY:
                        data.setEntitySummary(value);
                        break;
                    default:
                        resultMap.put(key, value);
                        break;
                }
            }

            if (isXocciLocation) {
                line = line.replaceAll(Constants.X_OCCI_LOCATION, "");
                line = line.replaceAll(": ", "");
                location = line;
                if (!location.startsWith("/")) {
                    location = "/" + location;
                }
                data.getXocciLocations().add(location);
            }
            line = reader.readLine();
            LOGGER.debug("line=" + line);
        }

        return resultMap;

    }

    /**
     * Parse input mixins.
     *
     * @param reader to read each lines that contains category mixins.
     * @param mixins
     * @return a list of parsed mixins model.
     * @throws ParseOCCIException
     * @throws IOException
     */
    private String parseInputMixins(BufferedReader reader, List<Mixin> mixins) throws ParseOCCIException, IOException {
        String line = reader.readLine();
        String lastLineRead = line;
        String term;
        String scheme;
        String categoryClass;
        Matcher matcher;
        String message;
        if (line != null) {
            matcher = Constants.PATTERN_CATEGORY.matcher(line);
            // While this is a category line...
            while (matcher.find()) {
                term = matcher.group(Constants.GROUP_TERM);
                scheme = matcher.group(Constants.GROUP_SCHEME);
                categoryClass = matcher.group(Constants.GROUP_CLASS);
                Mixin mixin = null;
                switch (categoryClass) {
                    case Constants.CLASS_MIXIN:
                        Optional<Mixin> mixinOpt = MixinManager.findMixinOnExtension(scheme + term, getUser());
                        if (!mixinOpt.isPresent()) {
                            // Search on user mixin tag.
                            mixinOpt = MixinManager.findUserMixinOnConfiguration(scheme + term, getUser());
                        }
                        if (!mixinOpt.isPresent()) {
                            message = "mixin " + scheme + term + " not found !";
                            throw new ParseOCCIException(message);
                        }
                        mixin = mixinOpt.get();
                        // Add the mixin to the list of mixins.
                        mixins.add(mixin);
                        break;
                    default:
                        message = "category class : " + categoryClass + " is not a mixin !";
                        LOGGER.warn(message);
                        throw new ParseOCCIException(message);
                }

                line = reader.readLine();
                lastLineRead = line;
                LOGGER.debug("line=" + line);
                matcher = Constants.PATTERN_CATEGORY.matcher(line);
            }
        }
        return lastLineRead;

    }


    /**
     * Parse a line to retrieve the kind from extensions and user configuration.
     *
     * @param line
     * @return
     * @throws ParseOCCIException
     */
    private Kind parseInputKind(final String line) throws ParseOCCIException {
        String message;
        if (line == null) {
            message = "No category provided !!!";
            LOGGER.warn(message);
            throw new ParseOCCIException(message);
        }

        Matcher matcher = Constants.PATTERN_CATEGORY.matcher(line);
        if (!matcher.find()) {
            message = "Not a category : " + line;
            LOGGER.debug(message);
            throw new ParseOCCIException(message);
        }

        String term = matcher.group(Constants.GROUP_TERM);
        String scheme = matcher.group(Constants.GROUP_SCHEME);
        String categoryClass = matcher.group(Constants.GROUP_CLASS);

        Kind kind;
        switch (categoryClass) {
            case Constants.CLASS_KIND:
                Optional<Kind> kindOpt = KindManager.findKindFromExtension(scheme + term, getUser());
                if (!kindOpt.isPresent()) {
                    message = "kind " + scheme + term + " not found!";
                    LOGGER.error(message);
                    throw new ParseOCCIException(message);
                }
                kind = kindOpt.get();
                break;
            default:
                message = "The category class : " + categoryClass + " is not a kind";
                LOGGER.error(message);
                throw new ParseOCCIException(message);
        }

        return kind;
    }


    /**
     * Build interface /-/ for accept type : text/plain.
     *
     * @param interfaceData
     * @param user          (the authorized username)
     * @return a String containing the interface.
     * @throws ParseOCCIException
     */
    @Override
    public String getInterface(final QueryInterfaceData interfaceData, final String user) throws ParseOCCIException {
        List<Kind> kinds = interfaceData.getKinds();
        List<Mixin> mixins = interfaceData.getMixins();
        StringBuilder sb = renderCategories(kinds, mixins);
        String msg = sb.toString();
        if (msg.isEmpty()) {
            throw new ParseOCCIException("No interface to render");
        }
        return msg;
    }

    @Override
    public String parseMessage(final String message) throws ParseOCCIException {
        return "message: " + message;
    }

    @Override
    public String renderOutputEntitiesLocations(List<String> locations) throws ParseOCCIException {
        String render;
        StringBuilder sb = new StringBuilder();
        for (String location : locations) {
            sb.append(Constants.X_OCCI_LOCATION).append(": ").append(location);
        }
        render = sb.toString();

        super.convertLocationsToOutputDatas(locations);

        return render;
    }

    /**
     * Render a collection of entities ==> render only locations.
     *
     * @param entities
     * @return
     * @throws ParseOCCIException
     */
    @Override
    public String renderOutputEntities(List<Entity> entities) throws ParseOCCIException {
        String render;
        StringBuilder sb = new StringBuilder();
        // Render only locations.
        String location;
        for (Entity entity : entities) {
            location = EntityManager.getLocation(entity, getUser());
            sb.append(Constants.X_OCCI_LOCATION).append(": ").append(location).append(Constants.CRLF);
        }

        render = sb.toString();

        super.convertEntitiesToOutputData(entities);

        return render;
    }

    @Override
    public String renderOutputEntity(Entity entity) throws ParseOCCIException {
        String render;
        StringBuilder sb = new StringBuilder();

        if (entity == null) {
            throw new ParseOCCIException("No entity to render");
        }

        // Render an entity.

        sb.append(Constants.CATEGORY).append(": ").append(asCategory(entity.getKind(), false)).append(Constants.CRLF);

        for (Mixin mixin : entity.getMixins()) {
            sb.append(Constants.CATEGORY).append(": ").append(asCategory(mixin, false));
        }

        sb.append(Constants.X_OCCI_ATTRIBUTE).append(": ").append(Constants.OCCI_CORE_ID)
                .append("=\"").append(entity.getId()).append('\"').append(Constants.CRLF);
        if (entity instanceof Link) {
            Link link = (Link) entity;

            sb.append(Constants.X_OCCI_ATTRIBUTE).append(": ")
                    .append(Constants.OCCI_CORE_SOURCE).append("=\"")
                    .append(EntityManager.getLocation(link.getSource(), getUser())).append('\"').append(Constants.CRLF);
            sb.append(Constants.X_OCCI_ATTRIBUTE).append(": ")
                    .append(Constants.OCCI_CORE_TARGET).append("=\"")
                    .append(EntityManager.getLocation(link.getTarget(), getUser())).append('\"').append(Constants.CRLF);
        }

        for (AttributeState attribute : entity.getAttributes()) {

            String name = attribute.getName();
            if (name.equals(Constants.OCCI_CORE_ID) || name.equals(Constants.OCCI_CORE_SOURCE) || name.equals(Constants.OCCI_CORE_TARGET)) {
                continue;
            }
            String value = null;
            Optional<String> optValStr = EntityManager.getAttrValueStr(entity, name);
            Optional<Number> optValNumber = EntityManager.getAttrValueNumber(entity, name);

            if (optValStr.isPresent()) {
                value = "\"" + optValStr.get() + "\"";
            } else if (optValNumber.isPresent()) {
                value = "" + optValNumber.get();
            } else {
                if (attribute.getValue() != null) {
                    value = "\"" + attribute.getValue() + "\"";
                }
            }
            // if value is null, it wont display.
            if (value == null) {
                continue;
            }
            sb.append(Constants.X_OCCI_ATTRIBUTE).append(": ").append(attribute.getName()).append('=').append(value).append(Constants.CRLF);
        }

        render = sb.toString();
        List<Entity> entities = new ArrayList<>();
        entities.add(entity);

        super.convertEntitiesToOutputData(entities);

        return render;
    }


    /**
     * Render to text/plain all categories Kind and Mixin to text/plain format.
     *
     * @param kinds  A list of kinds to render
     * @param mixins A list of mixins to render
     * @return a StringBuilder object, contains text/plain rendering.
     */
    private StringBuilder renderCategories(List<Kind> kinds, List<Mixin> mixins) {
        StringBuilder sb = new StringBuilder();
        for (Kind kind : kinds) {
            sb.append(Constants.CATEGORY).append(": ").append(asCategory(kind, true));
            for (Action action : kind.getActions()) {
                sb.append(Constants.CATEGORY).append(": ").append(asString(action));
            }
        }
        for (Mixin mixin : mixins) {
            sb.append(Constants.CATEGORY).append(": ").append(asCategory(mixin, true));
            for (Action action : mixin.getActions()) {
                sb.append(Constants.CATEGORY).append(": ").append(asString(action));
            }
        }
        return sb;
    }


    /**
     * Render a category Kind to text/plain.
     *
     * @param kind     a kind model to render
     * @param detailed if detailed information on kind
     * @return a StringBuilder object, contains text/plain rendering of the kind.
     */
    private StringBuilder asCategory(Kind kind, boolean detailed) {
        StringBuilder sb = new StringBuilder();
        sb.append(kind.getTerm())
                .append(";scheme=\"").append(kind.getScheme()).append("\";class=\"kind\"");
        if (detailed) {
            sb.append(";title=\"").append(kind.getTitle()).append('\"');
            Kind parent = kind.getParent();
            if (parent != null) {
                sb.append(";rel=\"").append(parent.getScheme()).append(parent.getTerm()).append('\"');
            }
            sb.append(";location=\"").append(ConfigurationManager.getLocation(kind)).append('\"');
            appendAttributes(sb, kind.getAttributes());
            appendActions(sb, kind.getActions());
        }
        return sb;
    }

    /**
     * Render a category Mixin to text/plain.
     *
     * @param mixin    a mixin model to render
     * @param detailed if detailed information about mixin
     * @return a StringBuilder object, contains text/plain rendering of the mixin.
     */
    private StringBuilder asCategory(Mixin mixin, boolean detailed) {
        StringBuilder sb = new StringBuilder();
        sb.append(mixin.getTerm())
                .append(";scheme=\"").append(mixin.getScheme()).append("\";class=\"mixin\"");
        if (detailed) {
            sb.append(";title=\"").append(mixin.getTitle()).append('\"');
            List<Mixin> mixins = mixin.getDepends();
            if (mixins.size() != 0) {
                sb.append(";rel=\"");
                String sep = "";
                for (Mixin md : mixins) {
                    sb.append(sep).append(md.getScheme()).append(md.getTerm());
                    sep = " ";
                }
                sb.append('\"');
            }
            sb.append(";location=\"").append(ConfigurationManager.getLocation(mixin)).append('\"');
            appendAttributes(sb, mixin.getAttributes());
            appendActions(sb, mixin.getActions());
        }
        return sb;
    }

    private String asString(Action action) {
        StringBuilder sb = new StringBuilder();
        sb.append(action.getTerm())
                .append(";scheme=\"").append(action.getScheme()).append("\";class=\"action\"")
                .append(";title=\"").append(action.getTitle()).append('\"');
        appendAttributes(sb, action.getAttributes());
        return sb.toString();
    }

    private void appendAttributes(StringBuilder sb, List<Attribute> attributes) {
        if (attributes.size() != 0) {
            sb.append(";attributes=\"");
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
            sb.append('\"');
        }
    }

    private void appendActions(StringBuilder sb, List<Action> actions) {
        if (actions.size() != 0) {
            sb.append(";actions=\"");
            String sep = "";
            for (Action action : actions) {
                sb.append(sep).append(action.getScheme()).append(action.getTerm());
                sep = " ";
            }
            sb.append('\"');
        }
    }
}
