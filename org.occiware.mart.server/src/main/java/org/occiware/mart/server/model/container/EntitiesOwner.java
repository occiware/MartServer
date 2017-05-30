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
package org.occiware.mart.server.model.container;

import org.occiware.clouddesigner.occi.Entity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cgourdin on 03/05/2017.
 * Subclass container used by EntityManager object.
 */
public class EntitiesOwner {

    private String owner;
    /**
     * key: location, value: entity object.
     */
    private Map<String, Entity> entitiesByLocation = new ConcurrentHashMap<>();
    /**
     * key: uuid, value : entity object.
     */
    private Map<String, Entity> entitiesByUuid = new ConcurrentHashMap<>();

    /**
     * Constructor to use with configuration manager when building a new configuration for a user.
     *
     * @param owner
     */
    public EntitiesOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Map<String, Entity> getEntitiesByLocation() {
        return entitiesByLocation;
    }

    public void setEntitiesByLocation(Map<String, Entity> entitiesByLocation) {
        this.entitiesByLocation = entitiesByLocation;
    }

    public Map<String, Entity> getEntitiesByUuid() {
        return entitiesByUuid;
    }

    public void setEntitiesByUuid(Map<String, Entity> entitiesByUuid) {
        this.entitiesByUuid = entitiesByUuid;
    }

    // Helpers methods to manage the entities maps.
    public void putEntity(final String location, final Entity entity) {
        String loc = manageSlashes(location);
        entitiesByLocation.put(loc, entity);
        entitiesByUuid.put(entity.getId(), entity);
    }

    public Entity getEntityByUuid(final String uuid) {
        return entitiesByUuid.get(uuid);
    }

    public Entity getEntityByLocation(final String location) {
        String loc = manageSlashes(location);
        return entitiesByLocation.get(loc);
    }

    public void removeEntity(final String location, final Entity entity) {
        String loc = manageSlashes(location);
        entitiesByLocation.remove(loc);
        entitiesByUuid.remove(entity.getId());
    }

    public String getEntityLocation(final Entity entity) {
        if (entity == null) {
            return null;
        }
        String location = null;
        for (Map.Entry<String, Entity> entry : entitiesByLocation.entrySet()) {
            if (entry.getValue().getId().equals(entity.getId())) {
                location = entry.getKey();
                break;
            }
        }
        return location;
    }

    public void removeEntity(final Entity entity) {
        if (entity == null) {
            return;
        }
        String location = getEntityLocation(entity);
        if (location != null) {
            removeEntity(location, entity);
        }
    }

    /**
     * Get entity location using its uuid only.
     *
     * @param uuid universal identifier of the entity to locate.
     * @return a location (like /myresources/myentities null if no location found).
     */
    public String getEntityLocation(final String uuid) {
        if (uuid == null) {
            return null;
        }
        Entity entity = getEntityByUuid(uuid);
        if (entity == null) {
            return null;
        }
        return getEntityLocation(entity);
    }

    /**
     * Format the location to a location like /mylocation/myentity.
     * @param location a location to format
     * @return a formatted location for entity exclusively.
     */
    public String manageSlashes(String location) {
        String loc = location;
        // Remove leading slash as entities MUST has a format location like : /myentity/entity1
        if (loc.endsWith("/")) {
            loc = loc.substring(0, loc.length() - 1);
        }
        if (!loc.startsWith("/")) {
            // Add a leading slash if no leading slash.
            loc = "/" + loc;
        }
        return loc;
    }

}
