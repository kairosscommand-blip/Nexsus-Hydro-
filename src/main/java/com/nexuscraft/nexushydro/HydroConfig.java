package com.nexuscraft.nexushydro;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/** Parses config.yml's behavior toggles and tuning knobs. Same "config drives defaults, code
 *  never hardcodes them" split this whole Nexus family uses (compare NexusIntegrity's
 *  IntegrityConfig). */
final class HydroConfig {

    private final Plugin plugin;

    boolean enabled = true;

    int maxBlocksToClassify = 4000;

    int maxBlocksPerFlood = 2000;
    int minRemainingSourceBlocks = 4;

    double finiteSourceDepletionChance = 0.15;

    boolean lavaEnabled = true;
    int maxBlocksPerLavaSettle = 800;

    int queueDrainIntervalTicks = 4;
    int maxChecksPerDrain = 20;

    boolean worldSpawnZoneEnabled = true;
    double worldSpawnZoneRadius = 32;

    HydroConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(Logger log) {
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("enabled", true);

        ConfigurationSection classification = c.getConfigurationSection("classification");
        if (classification != null) {
            maxBlocksToClassify = Math.max(50, classification.getInt("max-blocks-to-classify", maxBlocksToClassify));
        }

        ConfigurationSection drainage = c.getConfigurationSection("drainage");
        if (drainage != null) {
            maxBlocksPerFlood = Math.max(1, drainage.getInt("max-blocks-per-flood", maxBlocksPerFlood));
            minRemainingSourceBlocks = Math.max(0, drainage.getInt("min-remaining-source-blocks", minRemainingSourceBlocks));
        }

        ConfigurationSection bucket = c.getConfigurationSection("bucket");
        if (bucket != null) {
            finiteSourceDepletionChance = clamp01(bucket.getDouble("finite-source-depletion-chance", finiteSourceDepletionChance));
        }

        ConfigurationSection lava = c.getConfigurationSection("lava");
        if (lava != null) {
            lavaEnabled = lava.getBoolean("enabled", lavaEnabled);
            maxBlocksPerLavaSettle = Math.max(1, lava.getInt("max-blocks-per-settle", maxBlocksPerLavaSettle));
        }

        ConfigurationSection performance = c.getConfigurationSection("performance");
        if (performance != null) {
            queueDrainIntervalTicks = Math.max(1, performance.getInt("queue-drain-interval-ticks", queueDrainIntervalTicks));
            maxChecksPerDrain = Math.max(1, performance.getInt("max-checks-per-drain", maxChecksPerDrain));
        }

        ConfigurationSection zones = c.getConfigurationSection("protected-zones");
        if (zones != null) {
            ConfigurationSection worldSpawn = zones.getConfigurationSection("world-spawn");
            if (worldSpawn != null) {
                worldSpawnZoneEnabled = worldSpawn.getBoolean("enabled", worldSpawnZoneEnabled);
                worldSpawnZoneRadius = Math.max(0, worldSpawn.getDouble("radius", worldSpawnZoneRadius));
            }
        }
    }

    /** The volume-move budget for whichever fluid a given breach involves -- water uses
     *  {@link #maxBlocksPerFlood}, lava uses its own smaller, separately-tunable
     *  {@link #maxBlocksPerLavaSettle} since moving lava around is riskier than moving water. */
    int maxBlocksPerMove(org.bukkit.Material material) {
        return material == org.bukkit.Material.LAVA ? maxBlocksPerLavaSettle : maxBlocksPerFlood;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
