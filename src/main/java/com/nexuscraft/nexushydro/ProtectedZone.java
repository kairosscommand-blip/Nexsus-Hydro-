package com.nexuscraft.nexushydro;

import org.bukkit.Location;

/** A spherical region immune to every drain/flood NexusHydro ever performs -- the built-in
 *  world-spawn zone (always present, radius adjustable, the only zone protected out of the box)
 *  plus any further zones an admin opts in later via {@code /nexushydro zone add}.
 *
 *  <p>World spawn is special-cased as "dynamic": its center is re-read from every world's own
 *  {@code World#getSpawnLocation()} on every check rather than stored as a fixed point, so
 *  moving a world's spawn moves this protection with it automatically. Same shape and same
 *  reasoning as NexusIntegrity's own ProtectedZone -- deliberately a separate copy rather than a
 *  shared dependency, since every plugin in this family stands alone as its own jar. */
final class ProtectedZone {

    final String name;
    final String world;
    final boolean dynamicWorldSpawn;
    final double centerX;
    final double centerY;
    final double centerZ;
    final double radius;

    private ProtectedZone(String name, String world, boolean dynamicWorldSpawn,
                           double centerX, double centerY, double centerZ, double radius) {
        this.name = name;
        this.world = world;
        this.dynamicWorldSpawn = dynamicWorldSpawn;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    static ProtectedZone worldSpawn(double radius) {
        return new ProtectedZone("world-spawn", null, true, 0, 0, 0, radius);
    }

    static ProtectedZone fixed(String name, Location center, double radius) {
        return new ProtectedZone(name, center.getWorld().getName(), false,
                center.getX(), center.getY(), center.getZ(), radius);
    }

    boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        if (dynamicWorldSpawn) {
            Location spawn = loc.getWorld().getSpawnLocation();
            return loc.distanceSquared(spawn) <= radius * radius;
        }
        if (!loc.getWorld().getName().equals(world)) {
            return false;
        }
        double dx = loc.getX() - centerX;
        double dy = loc.getY() - centerY;
        double dz = loc.getZ() - centerZ;
        return (dx * dx + dy * dy + dz * dz) <= radius * radius;
    }
}
