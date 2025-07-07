package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    
    private final TaspiaSB plugin;
    private final RewardsManager rewardsManager;
    private final PlayerDataManager playerDataManager;
    private final CustomBossBarManager customBossBarManager;
    private final PersonalBeaconManager personalBeaconManager;
    private final PersonalLightningManager personalLightningManager;

    public CommandHandler(TaspiaSB plugin, RewardsManager rewardsManager, PlayerDataManager playerDataManager, CustomBossBarManager customBossBarManager, PersonalBeaconManager personalBeaconManager, PersonalLightningManager personalLightningManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
        this.customBossBarManager = customBossBarManager;
        this.personalBeaconManager = personalBeaconManager;
        this.personalLightningManager = personalLightningManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("collect")) {
            return handleCollectCommand(sender);
        } else if (command.getName().equalsIgnoreCase("taspiasb")) {
            return handleTaspiaSBCommand(sender, args);
        }
        return false;
    }

    private boolean handleCollectCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission for collect command
        if (!player.hasPermission("taspiasb.collect")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use the collect command.");
            return true;
        }

        // Use PlaceholderAPI to get the player's level
        String levelPlaceholder = "%alonsolevels_level%";
        String levelString = PlaceholderAPI.setPlaceholders(player, levelPlaceholder);
        int playerLevel;
        try {
            playerLevel = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage("Could not determine your level.");
            return true;
        }

        RewardsGUI rewardsGUI = new RewardsGUI(plugin, rewardsManager, playerDataManager);
        rewardsGUI.openGUI(player, playerLevel);

        return true;
    }

    private boolean handleTaspiaSBCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar|zone|pbeacon|plightning>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReloadCommand(sender);
            case "npctalk":
                return handleNPCTalkCommand(sender, args);
            case "bossbar":
                return handleBossBarCommand(sender, args);
            case "zone":
                return handleZoneCommand(sender, args);
            case "pbeacon":
                return handlePersonalBeaconCommand(sender, args);
            case "plightning":
                return handlePersonalLightningCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar|zone|pbeacon|plightning>");
                return true;
        }
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("taspiasb.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
            return true;
        }

        plugin.reloadConfig();
        rewardsManager.reloadRewards();
        sender.sendMessage(ChatColor.GREEN + "TaspiaSB configuration reloaded.");
        return true;
    }

    private boolean handleNPCTalkCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return true; // Invalid usage but no error message needed
        }
        
        String id = args[1];
        
        // Check dynamic permission based on NPC ID
        String requiredPermission = "taspiasb.npctalk." + id;
        if (!sender.hasPermission(requiredPermission)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to talk to NPC: " + id);
            return true;
        }
        
        // Command registered but no functionality implemented yet
        return true;
    }

    private boolean handleBossBarCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb bossbar <add|remove|list|colors> <player> [parameters...]");
            return true;
        }

        // Check base permission for boss bar commands
        if (!sender.hasPermission("taspiasb.bossbar")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use boss bar commands.");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "add":
                return handleBossBarAdd(sender, args);
            case "remove":
                return handleBossBarRemove(sender, args);
            case "list":
                return handleBossBarList(sender, args);
            case "colors":
                return handleBossBarColors(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb bossbar <add|remove|list|colors> <player> [parameters...]");
                return true;
        }
    }

    private boolean handleBossBarAdd(CommandSender sender, String[] args) {
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb bossbar add <player> <id> <color> <message>");
            sender.sendMessage(ChatColor.GRAY + "Use '/taspiasb bossbar colors' to see available colors.");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        String id = args[3];
        String color = args[4];

        // Join all remaining arguments as the message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 5; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = ChatColor.translateAlternateColorCodes('&', messageBuilder.toString());

        boolean success = customBossBarManager.addCustomBossBar(targetPlayer, id, message, color);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Boss bar '" + id + "' added for " + targetPlayer.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid color '" + color + "'. Use '/taspiasb bossbar colors' to see available colors.");
        }
        return true;
    }

    private boolean handleBossBarRemove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb bossbar remove <player> <id>");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        String id = args[3];
        boolean success = customBossBarManager.removeCustomBossBar(targetPlayer, id);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Boss bar '" + id + "' removed for " + targetPlayer.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Boss bar '" + id + "' not found for " + targetPlayer.getName());
        }
        return true;
    }

    private boolean handleBossBarColors(CommandSender sender) {
        String availableColors = customBossBarManager.getAvailableColors();
        sender.sendMessage(ChatColor.YELLOW + "Available boss bar colors: " + ChatColor.WHITE + availableColors);
        return true;
    }

    private boolean handleBossBarList(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb bossbar list <player>");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        var playerBossBars = customBossBarManager.getPlayerBossBars(targetPlayer);
        if (playerBossBars.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " has no active boss bars.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " has " + playerBossBars.size() + " active boss bar(s):");
            for (String id : playerBossBars.keySet()) {
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + id);
            }
        }
        return true;
    }

    private boolean handleZoneCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb zone <unlock|lock> <zone_id> <player>");
            sender.sendMessage(ChatColor.GRAY + "Available zones: alibon, airship_port, dryland");
            return true;
        }

        // Check base permission for zone commands
        if (!sender.hasPermission("taspiasb.zone")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use zone commands.");
            return true;
        }

        String action = args[1].toLowerCase();
        String zoneId = args[2].toLowerCase();

        if (!playerDataManager.isValidZone(zoneId)) {
            sender.sendMessage(ChatColor.RED + "Invalid zone ID: " + zoneId);
            sender.sendMessage(ChatColor.GRAY + "Available zones: alibon, airship_port, dryland");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb zone <unlock|lock> <zone_id> <player>");
            return true;
        }

        String targetPlayerName = args[3];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        switch (action) {
            case "unlock":
                return handleZoneUnlock(sender, targetPlayer, zoneId);
            case "lock":
                return handleZoneLock(sender, targetPlayer, zoneId);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb zone <unlock|lock> <zone_id> <player>");
                return true;
        }
    }

    private boolean handleZoneUnlock(CommandSender sender, Player targetPlayer, String zoneId) {
        if (playerDataManager.isZoneUnlocked(targetPlayer, zoneId)) {
            sender.sendMessage(ChatColor.YELLOW + "Zone '" + zoneId + "' is already unlocked for " + targetPlayer.getName());
            return true;
        }

        playerDataManager.setZoneUnlocked(targetPlayer, zoneId, true);
        sender.sendMessage(ChatColor.GREEN + "Zone '" + zoneId + "' has been unlocked for " + targetPlayer.getName());
        targetPlayer.sendMessage(ChatColor.GREEN + "You have unlocked the zone: " + ChatColor.YELLOW + zoneId);
        return true;
    }

    private boolean handleZoneLock(CommandSender sender, Player targetPlayer, String zoneId) {
        if (!playerDataManager.isZoneUnlocked(targetPlayer, zoneId)) {
            sender.sendMessage(ChatColor.YELLOW + "Zone '" + zoneId + "' is already locked for " + targetPlayer.getName());
            return true;
        }

        playerDataManager.setZoneUnlocked(targetPlayer, zoneId, false);
        sender.sendMessage(ChatColor.GREEN + "Zone '" + zoneId + "' has been locked for " + targetPlayer.getName());
        targetPlayer.sendMessage(ChatColor.RED + "You have lost access to the zone: " + ChatColor.YELLOW + zoneId);
        return true;
    }

    private boolean handlePersonalBeaconCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb pbeacon <add|remove|list|colors> [parameters...]");
            return true;
        }

        // Check base permission for personal beacon commands
        if (!sender.hasPermission("taspiasb.pbeacon")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use personal beacon commands.");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "add":
                return handlePersonalBeaconAdd(sender, args);
            case "remove":
                return handlePersonalBeaconRemove(sender, args);
            case "list":
                return handlePersonalBeaconList(sender, args);
            case "colors":
                return handlePersonalBeaconColors(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb pbeacon <add|remove|list|colors> [parameters...]");
                sender.sendMessage(ChatColor.GRAY + "Add usage: /taspiasb pbeacon add <player> <id> <world> <x> <y> <z> <color>");
                return true;
        }
    }

    private boolean handlePersonalBeaconAdd(CommandSender sender, String[] args) {
        if (args.length < 8) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb pbeacon add <player> <id> <world> <x> <y> <z> <color>");
            sender.sendMessage(ChatColor.GRAY + "Use '/taspiasb pbeacon colors' to see available colors.");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        String id = args[3];
        String worldName = args[4];
        
        // Validate world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found.");
            return true;
        }
        
        // Parse coordinates
        int x, y, z;
        try {
            x = Integer.parseInt(args[5]);
            y = Integer.parseInt(args[6]);
            z = Integer.parseInt(args[7]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use integers.");
            return true;
        }

        String color = args[8];
        Location location = new Location(world, x, y, z);

        boolean success = personalBeaconManager.addPersonalBeacon(targetPlayer, id, location, color);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Personal beacon '" + id + "' added for " + targetPlayer.getName() + " at " + worldName + ":" + x + ", " + y + ", " + z);
            targetPlayer.sendMessage(ChatColor.GREEN + "A personal beacon '" + id + "' has been created for you in world '" + worldName + "'!");
        } else {
            // Check if it's a color issue or limit issue
            if (personalBeaconManager.getPlayerBeacons(targetPlayer).size() >= 20) {
                sender.sendMessage(ChatColor.RED + "Failed to add personal beacon. Player has reached the maximum limit of 20 beacons.");
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid color '" + color + "'. Use '/taspiasb pbeacon colors' to see available colors.");
            }
        }
        return true;
    }

    private boolean handlePersonalBeaconRemove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb pbeacon remove <player> <id>");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        String id = args[3];
        boolean success = personalBeaconManager.removePersonalBeacon(targetPlayer, id);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Personal beacon '" + id + "' removed for " + targetPlayer.getName());
            targetPlayer.sendMessage(ChatColor.YELLOW + "Your personal beacon '" + id + "' has been removed.");
        } else {
            sender.sendMessage(ChatColor.RED + "Personal beacon '" + id + "' not found for " + targetPlayer.getName());
        }
        return true;
    }

    private boolean handlePersonalBeaconList(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb pbeacon list <player>");
            return true;
        }

        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        var playerBeacons = personalBeaconManager.getPlayerBeacons(targetPlayer);
        if (playerBeacons.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " has no active personal beacons.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " has " + playerBeacons.size() + " active personal beacon(s):");
            for (PersonalBeaconManager.PersonalBeacon beacon : playerBeacons.values()) {
                Location loc = beacon.getLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + beacon.getId() + 
                    ChatColor.GRAY + " at " + ChatColor.WHITE + worldName + ":" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() +
                    ChatColor.GRAY + " (" + ChatColor.WHITE + beacon.getColor().name().toLowerCase() + ChatColor.GRAY + ")");
            }
        }
        return true;
    }

    private boolean handlePersonalBeaconColors(CommandSender sender) {
        String availableColors = personalBeaconManager.getAvailableColors();
        sender.sendMessage(ChatColor.YELLOW + "Available personal beacon colors: " + ChatColor.WHITE + availableColors);
        return true;
    }

    private boolean handlePersonalLightningCommand(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb plightning <player> <world> <x> <y> <z> <sound>");
            sender.sendMessage(ChatColor.GRAY + "Sound: true/false");
            return true;
        }

        // Check base permission for personal lightning commands
        if (!sender.hasPermission("taspiasb.plightning")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use personal lightning commands.");
            return true;
        }

        return handlePersonalLightningSpawn(sender, args);
    }

    private boolean handlePersonalLightningSpawn(CommandSender sender, String[] args) {
        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        String worldName = args[2];
        
        // Validate world
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found.");
            return true;
        }
        
        // Parse coordinates
        double x, y, z;
        try {
            x = Double.parseDouble(args[3]);
            y = Double.parseDouble(args[4]);
            z = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers.");
            return true;
        }

        // Parse sound setting
        boolean playSound;
        String soundArg = args[6].toLowerCase();
        if (soundArg.equals("true") || soundArg.equals("1") || soundArg.equals("yes")) {
            playSound = true;
        } else if (soundArg.equals("false") || soundArg.equals("0") || soundArg.equals("no")) {
            playSound = false;
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid sound setting. Use true/false, yes/no, or 1/0.");
            return true;
        }

        Location location = new Location(world, x, y, z);

        boolean success = personalLightningManager.spawnPersonalLightning(targetPlayer, location, playSound);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Personal lightning spawned for " + targetPlayer.getName() + 
                " at " + worldName + ":" + x + ", " + y + ", " + z + " (sound: " + playSound + ")");
            targetPlayer.sendMessage(ChatColor.GOLD + "⚡ " + ChatColor.YELLOW + "Personal lightning has been spawned for you!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to spawn personal lightning. Please check the console for errors.");
        }
        return true;
    }

} 