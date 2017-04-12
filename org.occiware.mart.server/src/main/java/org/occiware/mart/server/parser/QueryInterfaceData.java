package org.occiware.mart.server.parser;

import org.occiware.clouddesigner.occi.Kind;
import org.occiware.clouddesigner.occi.Mixin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cgourdin on 11/04/2017.
 * This object is the base to render query interface.
 */
public class QueryInterfaceData {
    private List<Kind> kinds;
    private List<Mixin> mixins;
    private String categoryFilter;

    public List<Kind> getKinds() {
        if (kinds == null) {
            kinds = new ArrayList<>();
        }
        return kinds;
    }

    public void setKinds(List<Kind> kinds) {
        if (kinds == null) {
            this.kinds = new ArrayList<>();
        } else {
            this.kinds = kinds;
        }
    }

    public List<Mixin> getMixins() {
        if (mixins == null) {
            this.mixins = new ArrayList<>();
        }
        return mixins;
    }

    public void setMixins(List<Mixin> mixins) {
        if (mixins == null) {
            this.mixins = new ArrayList<>();
        } else {
            this.mixins = mixins;
        }
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }


}
