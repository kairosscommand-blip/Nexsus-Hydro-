package com.nexuscraft.nexushydro;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** {@code /nexushydro spawnradius|zone|reload} -- admin-only (also double-checked here, not
 *  just left to plugin.yml's permission default, same belt-and-suspenders convention this whole
 *  family follows; see NexusIntegrity's IntegrityCommand). */
final class HydroCommand implements CommandExecutor {

    private final HydroConfig config;
    private final ProtectedZoneRegistry zones;
    private final Runnable reload;
    private final Runnable saveZones;

    HydroCommand(HydroConfig config, ProtectedZoneRegistry zones, Runnable reload, Runnable saveZones) {
        this.config = config;
        this.zones = zones;
        this.reload = reload;
        this.saveZones = saveZones;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexushydro.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                reload.run();
                sender.sendMessage("§aNexusHydro reloaded.");
            }
            case "spawnradius" -> handleSpawnRadius(sender, args);
            case "zone" -> handleZone(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§7Usage: /nexushydro <spawnradius <blocks>|zone <add <name> <radius>|remove <name>|list>|reload>");
    }

    private void handleSpawnRadius(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nexushydro spawnradius <blocks>");
            return;
        }
        double radius;
        try {
            radius = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cThat's not a number.");
            return;
        }
        if (radius < 0) {
            sender.sendMessage("§cRadius can't be negative.");
            return;
        }
        config.worldSpawnZoneRadius = radius;
        zones.setWorldSpawnZone(config.worldSpawnZoneEnabled ? ProtectedZone.worldSpawn(radius) : null);
        sender.sendMessage("§aWorld-spawn protection radius set to " + radius + " blocks for this session. Update "
                + "'protected-zones.world-spawn.radius' in config.yml to " + (long) radius + " to keep it across a restart.");
    }

    private void handleZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nexushydro zone <add <name> <radius>|remove <name>|list>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> handleZoneAdd(sender, args);
            case "remove" -> handleZoneRemove(sender, args);
            case "list" -> handleZoneList(sender);
            default -> sender.sendMessage("§cUsage: /nexushydro zone <add <name> <radius>|remove <name>|list>");
        }
    }

    private void handleZoneAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player standing where the zone should be centered can add one -- run this "
                    + "in-game at the location you want protected.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /nexushydro zone add <name> <radius>");
            return;
        }
        String name = args[2];
        double radius;
        try {
            radius = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cThat's not a number.");
            return;
        }
        if (radius <= 0) {
            sender.sendMessage("§cRadius must be positive.");
            return;
        }
        Location here = player.getLocation();
        boolean added = zones.add(ProtectedZone.fixed(name, here, radius));
        if (!added) {
            sender.sendMessage("§cA zone named '" + name + "' already exists -- remove it first.");
            return;
        }
        saveZones.run();
        sender.sendMessage("§aProtected zone '" + name + "' added: radius " + radius + " around your current position in '"
                + here.getWorld().getName() + "'. Fluid inside it will never be drained or flooded by NexusHydro.");
    }

    private void handleZoneRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nexushydro zone remove <name>");
            return;
        }
        boolean removed = zones.remove(args[2]);
        if (!removed) {
            sender.sendMessage("§cNo zone named '" + args[2] + "'.");
            return;
        }
        saveZones.run();
        sender.sendMessage("§aProtected zone '" + args[2] + "' removed.");
    }

    private void handleZoneList(CommandSender sender) {
        sender.sendMessage("§7world-spawn (built-in" + (config.worldSpawnZoneEnabled ? "" : ", disabled")
                + "): radius " + (long) config.worldSpawnZoneRadius);
        if (zones.customZones().isEmpty()) {
            sender.sendMessage("§7No additional protected zones on file.");
            return;
        }
        for (ProtectedZone zone : zones.customZones()) {
            sender.sendMessage("§7- " + zone.name + " (" + zone.world + "): radius " + (long) zone.radius);
        }
    }
}
