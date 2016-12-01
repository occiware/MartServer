package org.occiware.mart.server.servlet.impl.parser.json.render.queryinterface;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by cgourdin on 01/12/2016.
 */
public class ModelInterfaceJson {

    private String id;
    private List<KindInterfaceJson> kinds = new LinkedList<>();
    private List<MixinInterfacejson> mixins = new LinkedList<>();
    private List<ActionInterfaceJson> actions = new LinkedList<>();

    public List<KindInterfaceJson> getKinds() {
        return kinds;
    }

    public void setKinds(List<KindInterfaceJson> kinds) {
        this.kinds = kinds;
    }

    public List<MixinInterfacejson> getMixins() {
        return mixins;
    }

    public void setMixins(List<MixinInterfacejson> mixins) {
        this.mixins = mixins;
    }

    public List<ActionInterfaceJson> getActions() {
        return actions;
    }

    public void setActions(List<ActionInterfaceJson> actions) {
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
