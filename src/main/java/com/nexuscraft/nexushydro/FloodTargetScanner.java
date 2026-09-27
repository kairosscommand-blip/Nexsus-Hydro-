package com.nexuscraft.nexushydro;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded breadth-first search over connected AIR blocks starting from a breach point -- the
 *  "is there an empty pocket this newly-opened gap leads into, and how big is it" half of
 *  NexusHydro's dam-breach mechanic. Same fail-open discipline as {@link FluidBodyScanner}: a
 *  search that exhausts its budget just stops there and returns whatever it found, rather than
 *  ever running an unbounded flood-fill. Results come back sorted lowest-Y-first, since a real
 *  flood fills from the bottom up. */
final class FloodTargetScanner {

    private static final BlockFace[] NEIGHBOR_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    };

    private final int budget;

    FloodTargetScanner(int budget) {
        this.budget = budget;
    }

    List<Block> scan(Block seed) {
        if (!seed.getType().isAir()) {
            return List.of();
        }
        Set<Pos> visited = new HashSet<>();
        List<Block> found = new ArrayList<>();
        ArrayDeque<Block> frontier = new ArrayDeque<>();
        frontier.add(seed);
        visited.add(Pos.of(seed));

        while (!frontier.isEmpty() && found.size() < budget) {
            Block current = frontier.poll();
            found.add(current);
            for (BlockFace face : NEIGHBOR_FACES) {
                Block neighbor = current.getRelative(face);
                if (!neighbor.getType().isAir()) {
                    continue;
                }
                Pos pos = Pos.of(neighbor);
                if (visited.add(pos)) {
                    frontier.add(neighbor);
                }
            }
        }
        found.sort(Comparator.comparingInt(Block::getY));
        return found;
    }

    private record Pos(int x, int y, int z) {
        static Pos of(Block block) {
            return new Pos(block.getX(), block.getY(), block.getZ());
        }
    }
}
