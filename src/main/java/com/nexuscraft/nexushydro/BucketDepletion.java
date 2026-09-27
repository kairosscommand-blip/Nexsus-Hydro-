package com.nexuscraft.nexushydro;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.concurrent.ThreadLocalRandom;

/** The "no more free infinite duplication" half of NexusHydro: scooping a bucket from a FINITE
 *  body's source block (vanilla lets this happen for free, forever, no matter how many times)
 *  has a real chance of actually using up that source block, letting vanilla's own flow settle
 *  the body afterward. Oceans and other bodies whose {@link FluidBodyScanner} search saturates
 *  its budget are classified INFINITE and stay free to scoop from forever, exactly like vanilla
 *  intends. */
final class BucketDepletion {

    private final HydroConfig config;
    private final ProtectedZoneRegistry zones;

    BucketDepletion(HydroConfig config, ProtectedZoneRegistry zones) {
        this.config = config;
        this.zones = zones;
    }

    void handle(Block clicked) {
        if (!config.enabled || clicked == null) {
            return;
        }
        // Real Bukkit fires PlayerBucketFillEvent for lava buckets too, but a scoop-able lava
        // pool is already a rare, deliberate build (unlike ocean water, which is everywhere) --
        // only water gets the depletion mechanic. Lava's own real-volume mechanic is the dam
        // breach/settle path in HydroQueueProcessor instead.
        if (clicked.getType() != Material.WATER) {
            return;
        }
        if (zones.isProtected(clicked.getLocation())) {
            return;
        }
        FluidBodyScanner.Result result = new FluidBodyScanner(config.maxBlocksToClassify).scan(clicked);
        if (result == null || !result.finite()) {
            return;
        }
        if (result.blocks().size() <= config.minRemainingSourceBlocks) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < config.finiteSourceDepletionChance) {
            clicked.setType(Material.AIR);
        }
    }
}
