package com.nexuscraft.nexushydro;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.List;

/** Every disturbance this plugin reacts to -- a broken block, or an explosion's block list --
 *  never runs a check inline. A newly-air position only gets queued as a breach candidate if
 *  one of its six neighbors is actually a fluid block; everything else is ignored at zero cost.
 *  See {@link HydroQueueProcessor} for the throttled drain that actually evaluates a candidate.
 *  Bucket fills are handled separately (and immediately -- a single-block check is cheap enough
 *  not to need queuing) by {@link BucketDepletion}. */
final class HydroListener implements Listener {

    private static final BlockFace[] NEIGHBOR_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    };

    private final HydroConfig config;
    private final ProtectedZoneRegistry zones;
    private final HydroQueue queue;
    private final BucketDepletion bucketDepletion;

    HydroListener(HydroConfig config, ProtectedZoneRegistry zones, HydroQueue queue, BucketDepletion bucketDepletion) {
        this.config = config;
        this.zones = zones;
        this.queue = queue;
        this.bucketDepletion = bucketDepletion;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        maybeQueueBreach(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        queueAll(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        queueAll(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        bucketDepletion.handle(event.getBlockClicked());
    }

    private void queueAll(List<Block> blocks) {
        if (blocks == null) {
            return;
        }
        for (Block block : blocks) {
            maybeQueueBreach(block);
        }
    }

    private void maybeQueueBreach(Block broken) {
        if (!config.enabled || zones.isProtected(broken.getLocation())) {
            return;
        }
        for (BlockFace face : NEIGHBOR_FACES) {
            Material neighborType = broken.getRelative(face).getType();
            boolean isWater = neighborType == Material.WATER;
            boolean isLava = config.lavaEnabled && neighborType == Material.LAVA;
            if (isWater || isLava) {
                queue.enqueue(broken);
                return;
            }
        }
    }
}
