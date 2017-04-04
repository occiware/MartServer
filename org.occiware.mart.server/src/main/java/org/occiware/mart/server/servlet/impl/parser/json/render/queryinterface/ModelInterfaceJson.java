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
