package com.nexuscraft.nexushydro;

import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pending "this location just opened up next to a fluid body, go check whether it's a dam
 *  breach" work -- a dedup-aware queue of coordinates, not live block references. Nothing here
 *  ever runs a check itself; see {@link HydroQueueProcessor} for the throttled drain that
 *  actually does. Same shape and reasoning as NexusIntegrity's own IntegrityQueue. */
final class HydroQueue {

    private final ArrayDeque<HydroKey> pending = new ArrayDeque<>();
    private final Set<HydroKey> queuedSet = new LinkedHashSet<>();

    void enqueue(Block block) {
        HydroKey key = HydroKey.of(block);
        if (queuedSet.add(key)) {
            pending.add(key);
        }
    }

    HydroKey poll() {
        HydroKey key = pending.poll();
        if (key != null) {
            queuedSet.remove(key);
        }
        return key;
    }

    boolean isEmpty() {
        return pending.isEmpty();
    }

    int size() {
        return pending.size();
    }
}
