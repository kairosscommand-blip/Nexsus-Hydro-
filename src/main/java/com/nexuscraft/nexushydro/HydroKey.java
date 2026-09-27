package com.nexuscraft.nexushydro;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

/** A coordinate, not a live block reference -- every breach candidate queued for later
 *  evaluation is stored as one of these, so the same location queued twice (a break, then an
 *  explosion moments later) only ever gets evaluated once. {@link #resolve()} re-reads whatever
 *  is actually at that coordinate at drain time, which is exactly the correct behavior: what
 *  matters is the current state of the world when the check finally runs. Same shape as
 *  NexusIntegrity's own BlockKey. */
record HydroKey(String world, int x, int y, int z) {

    static HydroKey of(Block block) {
        return new HydroKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    Block resolve() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : w.getBlockAt(x, y, z);
    }
}
