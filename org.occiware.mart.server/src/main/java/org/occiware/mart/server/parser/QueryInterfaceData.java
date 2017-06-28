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
package org.occiware.mart.server.parser;

import org.eclipse.cmf.occi.core.Kind;
import org.eclipse.cmf.occi.core.Mixin;

import java.util.ArrayList;
import java.util.List;

// import org.occiware.clouddesigner.occi.Kind;
// import org.occiware.clouddesigner.occi.Mixin;

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
