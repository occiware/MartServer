package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import java.util.List;

/**
 * Created by cgourdin on 01/12/2016.
 */
public class MixinInterfacejson extends CategoryInterfaceJson {

    private String location;
    private List<String> depends;
    private List<String> applies;

    /**
     * Related actions schemes.
     */
    private List<String> actions;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getDepends() {
        return depends;
    }

    public void setDepends(List<String> depends) {
        this.depends = depends;
    }

    public List<String> getApplies() {
        return applies;
    }

    public void setApplies(List<String> applies) {
        this.applies = applies;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
