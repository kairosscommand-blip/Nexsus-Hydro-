package com.nexuscraft.nexushydro;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Drains {@link HydroQueue} on a timer, budgeted per tick -- same reasoning as
 *  NexusIntegrity's IntegrityQueueProcessor: a mass disturbance near a reservoir just takes a
 *  few extra drains to fully settle instead of freezing the server for one tick.
 *
 *  <p>This is the actual "real volume" mechanic: when a breach candidate turns out to be a
 *  genuine dam -- a finite fluid body with a lower, reachable empty pocket on the other side of
 *  the gap that just opened -- fluid actually moves. The body's own highest source blocks
 *  (never inside a protected zone) convert to air, and an equal number of the target pocket's
 *  lowest positions (also never inside a protected zone) become real source blocks of the same
 *  material. Vanilla's own tick-based fluid physics takes it from there -- this plugin never
 *  animates the flow itself, it only ever decides that a real transfer of volume should happen
 *  and where it starts and ends. A breach that only opens onto space at the same level (or
 *  higher) as the body isn't a dam at all -- that's just pond-edge expansion vanilla's own flow
 *  already handles correctly with no help needed here. */
final class HydroQueueProcessor {

    private static final BlockFace[] NEIGHBOR_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    };

    private final Plugin plugin;
    private final HydroConfig config;
    private final ProtectedZoneRegistry zones;
    private final HydroQueue queue;

    private BukkitTask task;

    HydroQueueProcessor(Plugin plugin, HydroConfig config, ProtectedZoneRegistry zones, HydroQueue queue) {
        this.plugin = plugin;
        this.config = config;
        this.zones = zones;
        this.queue = queue;
    }

    void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::drain,
                config.queueDrainIntervalTicks, config.queueDrainIntervalTicks);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void drain() {
        int checked = 0;
        while (checked < config.maxChecksPerDrain && !queue.isEmpty()) {
            HydroKey key = queue.poll();
            checked++;
            if (key == null) {
                continue;
            }
            Block breach = key.resolve();
            if (breach == null || !breach.getType().isAir()) {
                // Something already claimed this gap -- a player rebuilt it, vanilla's own flow
                // already filled it in -- nothing left here to evaluate.
                continue;
            }
            evaluateBreach(breach);
        }
    }

    private void evaluateBreach(Block breach) {
        FluidBodyScanner.Result body = findAdjacentBody(breach);
        if (body == null || !body.finite()) {
            return;
        }
        if (body.material() == Material.LAVA && !config.lavaEnabled) {
            return;
        }

        int moveBudget = config.maxBlocksPerMove(body.material());
        List<Block> target = new FloodTargetScanner(moveBudget).scan(breach);
        if (target.isEmpty()) {
            return;
        }

        int bodyMinY = Integer.MAX_VALUE;
        for (Block b : body.blocks()) {
            bodyMinY = Math.min(bodyMinY, b.getY());
        }
        int targetMinY = target.get(0).getY();
        if (targetMinY >= bodyMinY) {
            // Nowhere lower to go -- lateral pond-edge expansion, not a dam breach.
            return;
        }

        int floor = config.minRemainingSourceBlocks;
        int available = Math.max(0, body.blocks().size() - floor);
        if (available <= 0) {
            return;
        }

        List<Block> drainable = new ArrayList<>(body.blocks());
        drainable.removeIf(b -> zones.isProtected(b.getLocation()));
        drainable.sort(Comparator.comparingInt(Block::getY).reversed());

        List<Block> fillable = new ArrayList<>(target);
        fillable.removeIf(b -> zones.isProtected(b.getLocation()));

        int moveCount = Math.min(available, Math.min(moveBudget, Math.min(drainable.size(), fillable.size())));
        for (int i = 0; i < moveCount; i++) {
            fillable.get(i).setType(body.material());
            drainable.get(i).setType(Material.AIR);
        }
    }

    private FluidBodyScanner.Result findAdjacentBody(Block breach) {
        FluidBodyScanner scanner = new FluidBodyScanner(config.maxBlocksToClassify);
        for (BlockFace face : NEIGHBOR_FACES) {
            Block neighbor = breach.getRelative(face);
            Material type = neighbor.getType();
            if (type == Material.WATER || type == Material.LAVA) {
                FluidBodyScanner.Result result = scanner.scan(neighbor);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
