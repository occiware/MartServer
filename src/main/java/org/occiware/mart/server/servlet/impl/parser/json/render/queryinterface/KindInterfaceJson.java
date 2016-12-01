package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import java.util.List;

/**
 * Created by cgourdin on 01/12/2016.
 */
public class KindInterfaceJson extends CategoryInterfaceJson {

    private String parent;
    private String location;


    /**
     * Related actions schemes.
     */
    private List<String> actions;

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
