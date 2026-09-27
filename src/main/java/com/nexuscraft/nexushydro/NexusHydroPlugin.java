package com.nexuscraft.nexushydro;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Real volume for fluids -- see README.md for the full design and reasoning. This plugin never
 *  reimplements vanilla's own fluid spreading/animation; it only ever decides when a new source
 *  block is allowed to appear or disappear, and lets vanilla's real tick-based physics do the
 *  actual flowing from there. */
public final class NexusHydroPlugin extends JavaPlugin {

    private HydroConfig config;
    private final ProtectedZoneRegistry zones = new ProtectedZoneRegistry();
    private HydroQueue queue;
    private HydroQueueProcessor processor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new HydroConfig(this);
        config.load(getLogger());
        refreshWorldSpawnZone();
        loadZones();

        queue = new HydroQueue();
        processor = new HydroQueueProcessor(this, config, zones, queue);
        processor.start();

        BucketDepletion bucketDepletion = new BucketDepletion(config, zones);
        getServer().getPluginManager().registerEvents(
                new HydroListener(config, zones, queue, bucketDepletion), this);

        getCommand("nexushydro").setExecutor(
                new HydroCommand(config, zones, this::reload, this::saveZones));

        getLogger().info("NexusHydro enabled -- world-spawn protection radius "
                + (long) config.worldSpawnZoneRadius + ", " + zones.customZones().size()
                + " additional protected zone(s) on file.");
    }

    @Override
    public void onDisable() {
        if (processor != null) {
            processor.stop();
        }
        saveZones();
        getLogger().info("NexusHydro disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getLogger());
        refreshWorldSpawnZone();
        processor.start();
    }

    private void refreshWorldSpawnZone() {
        zones.setWorldSpawnZone(config.worldSpawnZoneEnabled
                ? ProtectedZone.worldSpawn(config.worldSpawnZoneRadius)
                : null);
    }

    private void loadZones() {
        File file = new File(getDataFolder(), "protected_zones.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<ProtectedZone> loaded = ProtectedZonePersistence.loadFrom(data, getLogger());
        zones.loadCustom(loaded);
    }

    private void saveZones() {
        getDataFolder().mkdirs();
        YamlConfiguration data = new YamlConfiguration();
        ProtectedZonePersistence.saveTo(data, zones.customZones());
        try {
            data.save(new File(getDataFolder(), "protected_zones.yml"));
        } catch (IOException e) {
            getLogger().warning("Failed to save protected_zones.yml: " + e.getMessage());
        }
    }
}
