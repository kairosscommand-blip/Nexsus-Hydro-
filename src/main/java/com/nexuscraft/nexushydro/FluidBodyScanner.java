package com.nexuscraft.nexushydro;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Answers the one question NexusHydro's whole "real volume" mechanic depends on: is this
 *  connected body of source blocks FINITE (a constructed pool/tank/moat/reservoir, something
 *  with a real, countable amount of fluid in it) or effectively INFINITE (an ocean or other
 *  naturally vast body)?
 *
 *  <p>A bounded breadth-first search over connected same-material fluid blocks that terminates
 *  within budget is finite -- its full block list is collected and can be drained. One that
 *  exhausts the budget without terminating is treated as infinite and left completely alone,
 *  exactly like vanilla's own oceans: this plugin never risks partially draining something it
 *  couldn't fully account for. Same fail-open discipline as NexusIntegrity's own
 *  SupportGraphEngine, just answering the opposite-shaped question. */
final class FluidBodyScanner {

    private static final BlockFace[] NEIGHBOR_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    };

    private final int budget;

    FluidBodyScanner(int budget) {
        this.budget = budget;
    }

    /** Null if {@code seed} isn't a water or lava block. */
    Result scan(Block seed) {
        Material material = seed.getType();
        if (material != Material.WATER && material != Material.LAVA) {
            return null;
        }
        Set<Pos> visited = new HashSet<>();
        List<Block> blocks = new ArrayList<>();
        ArrayDeque<Block> frontier = new ArrayDeque<>();
        frontier.add(seed);
        visited.add(Pos.of(seed));

        while (!frontier.isEmpty()) {
            if (blocks.size() >= budget) {
                // Exhausted the classification budget without the body's edge ever closing --
                // this is effectively boundless (an ocean or similar), so it's left alone,
                // forever, exactly as vanilla's own infinite water always was.
                return new Result(material, List.of(), false);
            }
            Block current = frontier.poll();
            blocks.add(current);
            for (BlockFace face : NEIGHBOR_FACES) {
                Block neighbor = current.getRelative(face);
                if (neighbor.getType() != material) {
                    continue;
                }
                Pos pos = Pos.of(neighbor);
                if (visited.add(pos)) {
                    frontier.add(neighbor);
                }
            }
        }
        return new Result(material, blocks, true);
    }

    record Result(Material material, List<Block> blocks, boolean finite) {
    }

    private record Pos(int x, int y, int z) {
        static Pos of(Block block) {
            return new Pos(block.getX(), block.getY(), block.getZ());
        }
    }
}
