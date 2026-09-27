package com.nexuscraft.nexushydro;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** YAML round trip for the admin-added protected zones -- a flat list of small maps under a
 *  {@code zones} key in {@code protected_zones.yml}. The built-in world-spawn zone is never
 *  stored here (it's config-driven and always present); this file only ever holds zones added
 *  with {@code /nexushydro zone add}. */
final class ProtectedZonePersistence {

    private ProtectedZonePersistence() {
    }

    static List<ProtectedZone> loadFrom(ConfigurationSection root, Logger log) {
        List<ProtectedZone> zones = new ArrayList<>();
        if (root == null) {
            return zones;
        }
        for (Map<?, ?> raw : root.getMapList("zones")) {
            String name = str(raw, "name");
            String worldName = str(raw, "world");
            Double x = dbl(raw, "x");
            Double y = dbl(raw, "y");
            Double z = dbl(raw, "z");
            Double radius = dbl(raw, "radius");
            if (name == null || worldName == null || x == null || y == null || z == null || radius == null) {
                log.warning("nexushydro: skipped a malformed entry in protected_zones.yml.");
                continue;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                log.warning("nexushydro: protected zone '" + name + "' names an unknown world '"
                        + worldName + "' -- skipped (that world may just not be loaded yet).");
                continue;
            }
            zones.add(ProtectedZone.fixed(name, new Location(world, x, y, z), radius));
        }
        return zones;
    }

    static void saveTo(ConfigurationSection root, Collection<ProtectedZone> zones) {
        List<Map<String, Object>> raw = new ArrayList<>();
        for (ProtectedZone zone : zones) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", zone.name);
            entry.put("world", zone.world);
            entry.put("x", zone.centerX);
            entry.put("y", zone.centerY);
            entry.put("z", zone.centerZ);
            entry.put("radius", zone.radius);
            raw.add(entry);
        }
        root.set("zones", raw);
    }

    private static String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Double dbl(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
