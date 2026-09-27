package com.nexuscraft.nexushydro;

import org.bukkit.Location;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Holds the one built-in world-spawn zone plus every admin-added zone, and answers the single
 *  question the rest of this plugin actually needs: is this location immune right now? */
final class ProtectedZoneRegistry {

    private ProtectedZone worldSpawnZone;
    private final Map<String, ProtectedZone> customZones = new LinkedHashMap<>();

    void setWorldSpawnZone(ProtectedZone zone) {
        this.worldSpawnZone = zone;
    }

    boolean isProtected(Location loc) {
        if (worldSpawnZone != null && worldSpawnZone.contains(loc)) {
            return true;
        }
        for (ProtectedZone zone : customZones.values()) {
            if (zone.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    boolean add(ProtectedZone zone) {
        String key = zone.name.toLowerCase(Locale.ROOT);
        if (customZones.containsKey(key)) {
            return false;
        }
        customZones.put(key, zone);
        return true;
    }

    boolean remove(String name) {
        return customZones.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    Collection<ProtectedZone> customZones() {
        return List.copyOf(customZones.values());
    }

    void loadCustom(Iterable<ProtectedZone> zones) {
        customZones.clear();
        for (ProtectedZone zone : zones) {
            customZones.put(zone.name.toLowerCase(Locale.ROOT), zone);
        }
    }
}
