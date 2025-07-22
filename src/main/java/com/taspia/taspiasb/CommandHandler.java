package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final TaspiaSB plugin;
    private final RewardsManager rewardsManager;
    private final PlayerDataManager playerDataManager;
    private final CustomBossBarManager customBossBarManager;
    private final PersonalBeaconManager personalBeaconManager;
    private final PersonalLightningManager personalLightningManager;
    private final IslandLevelManager islandLevelManager;

    public CommandHandler(TaspiaSB plugin, RewardsManager rewardsManager, PlayerDataManager playerDataManager, CustomBossBarManager customBossBarManager, PersonalBeaconManager personalBeaconManager, PersonalLightningManager personalLightningManager, IslandLevelManager islandLevelManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
        this.customBossBarManager = customBossBarManager;
        this.personalBeaconManager = personalBeaconManager;
        this.personalLightningManager = personalLightningManager;
        this.islandLevelManager = islandLevelManager;
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
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar|zone|cutscene|pbeacon|plightning|islevel>");
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
            case "cutscene":
                return handleCutsceneCommand(sender, args);
            case "pbeacon":
                return handlePersonalBeaconCommand(sender, args);
            case "plightning":
                return handlePersonalLightningCommand(sender, args);
            case "islevel":
                return handleIslandLevelCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar|zone|cutscene|pbeacon|plightning|islevel>");
                return true;
        }
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("taspiasb.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
            return true;
        }

        // Reload each manager individually from server config (no merged config)
        rewardsManager.reloadRewards();
        playerDataManager.reloadPlayerData();
        islandLevelManager.reloadBlockUnlocks();
        plugin.getDatabaseManager().reload();
        
        sender.sendMessage(ChatColor.GREEN + "TaspiaSB configuration reloaded from server config.");
        sender.sendMessage(ChatColor.GRAY + "✓ Rewards reloaded");
        sender.sendMessage(ChatColor.GRAY + "✓ Player data reloaded");
        sender.sendMessage(ChatColor.GRAY + "✓ Island block unlocks reloaded");
        sender.sendMessage(ChatColor.GRAY + "✓ MySQL configuration reloaded");
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
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb zone <unlock|lock> <zone_id> <player>");
            return true;
        }

        // Check base permission for zone commands
        if (!sender.hasPermission("taspiasb.zone")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use zone commands.");
            return true;
        }

        String action = args[1].toLowerCase();
        String zoneId = args[2].toLowerCase();
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
        return true;
    }

    private boolean handleZoneLock(CommandSender sender, Player targetPlayer, String zoneId) {
        if (!playerDataManager.isZoneUnlocked(targetPlayer, zoneId)) {
            sender.sendMessage(ChatColor.YELLOW + "Zone '" + zoneId + "' is already locked for " + targetPlayer.getName());
            return true;
        }

        playerDataManager.setZoneUnlocked(targetPlayer, zoneId, false);
        sender.sendMessage(ChatColor.GREEN + "Zone '" + zoneId + "' has been locked for " + targetPlayer.getName());
        return true;
    }

    private boolean handleCutsceneCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb cutscene <cutscene_id> <player> <true/false>");
            return true;
        }

        // Check base permission for cutscene commands
        if (!sender.hasPermission("taspiasb.cutscene")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use cutscene commands.");
            return true;
        }

        String cutsceneId = args[1].toLowerCase();
        String targetPlayerName = args[2];
        String completionStatus = args[3].toLowerCase();

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
            return true;
        }

        if (!completionStatus.equals("true") && !completionStatus.equals("false")) {
            sender.sendMessage(ChatColor.RED + "Completion status must be 'true' or 'false'");
            return true;
        }

        boolean isCompleted = Boolean.parseBoolean(completionStatus);
        return handleCutsceneSet(sender, targetPlayer, cutsceneId, isCompleted);
    }

    private boolean handleCutsceneSet(CommandSender sender, Player targetPlayer, String cutsceneId, boolean completed) {
        boolean currentStatus = playerDataManager.isCutsceneCompleted(targetPlayer, cutsceneId);
        
        if (currentStatus == completed) {
            String statusText = completed ? "completed" : "not completed";
            sender.sendMessage(ChatColor.YELLOW + "Cutscene '" + cutsceneId + "' is already " + statusText + " for " + targetPlayer.getName());
            return true;
        }

        playerDataManager.setCutsceneCompleted(targetPlayer, cutsceneId, completed);
        String statusText = completed ? "completed" : "not completed";
        String actionText = completed ? "marked as completed" : "marked as not completed";
        sender.sendMessage(ChatColor.GREEN + "Cutscene '" + cutsceneId + "' has been " + actionText + " for " + targetPlayer.getName());
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
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb plightning <player> <world> <x> <y> <z>");
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

        Location location = new Location(world, x, y, z);

        boolean success = personalLightningManager.spawnPersonalLightning(targetPlayer, location);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Personal lightning spawned for " + targetPlayer.getName() + 
                " at " + worldName + ":" + x + ", " + y + ", " + z);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to spawn personal lightning. Please check the console for errors.");
        }
        return true;
    }

    private boolean handleIslandLevelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("taspiasb.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb islevel <info|refresh|clear|check|blocks|reload|debug>");
            sender.sendMessage(ChatColor.YELLOW + "  info - Show cache statistics");
            sender.sendMessage(ChatColor.YELLOW + "  refresh - Refresh cache for all online players");
            sender.sendMessage(ChatColor.YELLOW + "  clear - Clear the entire cache");
            sender.sendMessage(ChatColor.YELLOW + "  check <player> - Check a specific player's cached level");
            sender.sendMessage(ChatColor.YELLOW + "  blocks <level> - Show unlocked blocks at level");
            sender.sendMessage(ChatColor.YELLOW + "  reload - Reload block unlock configuration");
            sender.sendMessage(ChatColor.YELLOW + "  debug - Show config debug information");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "info":
                int cacheSize = islandLevelManager.getCacheSize();
                sender.sendMessage(ChatColor.GREEN + "Island Level Cache Info:");
                sender.sendMessage(ChatColor.YELLOW + "  Cached players: " + cacheSize);
                sender.sendMessage(ChatColor.YELLOW + "  Online players: " + Bukkit.getOnlinePlayers().size());
                break;

            case "refresh":
                islandLevelManager.refreshAllCache();
                sender.sendMessage(ChatColor.GREEN + "Island level cache refreshed for all online players.");
                break;

            case "clear":
                islandLevelManager.clearCache();
                sender.sendMessage(ChatColor.GREEN + "Island level cache cleared.");
                break;

            case "check":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /taspiasb islevel check <player>");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player '" + args[2] + "' not found or not online.");
                    return true;
                }
                
                int cachedLevel = islandLevelManager.getCachedIslandLevel(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Cached island level for " + target.getName() + ": " + cachedLevel);
                break;

            case "blocks":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /taspiasb islevel blocks <level>");
                    return true;
                }
                
                try {
                    int level = Integer.parseInt(args[2]);
                    Set<Material> unlockedBlocks = islandLevelManager.getUnlockedBlocks(level);
                    
                    if (unlockedBlocks.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "No blocks unlocked at level " + level + " or below.");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Blocks unlocked at level " + level + " or below:");
                        for (Material material : unlockedBlocks) {
                            int requiredLevel = islandLevelManager.getRequiredLevelForBlock(material);
                            sender.sendMessage(ChatColor.YELLOW + "  - " + material.name() + 
                                             ChatColor.GRAY + " (unlocked at level " + requiredLevel + ")");
                        }
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid level number: " + args[2]);
                }
                break;

            case "reload":
                islandLevelManager.reloadBlockUnlocks();
                sender.sendMessage(ChatColor.GREEN + "Block unlock configuration reloaded successfully.");
                break;
                
                                case "debug":
                        sender.sendMessage(ChatColor.YELLOW + "=== Island Level Config Debug ===");
                        
                        // Show server config file status
                        File serverConfigFile = new File(plugin.getDataFolder(), "config.yml");
                        if (!serverConfigFile.exists()) {
                            sender.sendMessage(ChatColor.RED + "Server config file does not exist!");
                            sender.sendMessage(ChatColor.GRAY + "Expected path: " + serverConfigFile.getAbsolutePath());
                            sender.sendMessage(ChatColor.YELLOW + "Using default config only (no block unlocks)");
                            break;
                        }
                        
                        sender.sendMessage(ChatColor.GREEN + "Server config file exists: " + serverConfigFile.getAbsolutePath());
                        
                        // Load server config directly (not merged)
                        try {
                            FileConfiguration serverConfig = YamlConfiguration.loadConfiguration(serverConfigFile);
                            ConfigurationSection levelsSection = serverConfig.getConfigurationSection("levels");
                            
                            if (levelsSection == null) {
                                sender.sendMessage(ChatColor.RED + "No 'levels' section found in server config!");
                                break;
                            }
                            
                            sender.sendMessage(ChatColor.GRAY + "Levels in server config: " + levelsSection.getKeys(false));
                            sender.sendMessage(ChatColor.GRAY + "Total blocks configured: " + islandLevelManager.getBlockRequiredLevels().size());
                            
                            sender.sendMessage(ChatColor.YELLOW + "\n=== Server Config Block Unlocks ===");
                            levelsSection.getKeys(false).stream()
                                .sorted((a, b) -> {
                                    try {
                                        return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                                    } catch (NumberFormatException e) {
                                        return a.compareTo(b);
                                    }
                                })
                                .forEach(levelKey -> {
                                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                                    if (levelSection != null && levelSection.contains("island-block-unlocks")) {
                                        List<String> blocks = levelSection.getStringList("island-block-unlocks");
                                        if (!blocks.isEmpty()) {
                                            sender.sendMessage(ChatColor.AQUA + "Level " + levelKey + ": " + 
                                                ChatColor.WHITE + String.join(", ", blocks));
                                        }
                                    }
                                });
                                
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Error reading server config: " + e.getMessage());
                        }
                        
                        // Show MySQL database status
                        sender.sendMessage(ChatColor.YELLOW + "\n=== MySQL Database Status ===");
                        DatabaseManager dbManager = plugin.getDatabaseManager();
                        if (dbManager.isEnabled()) {
                            sender.sendMessage(ChatColor.GREEN + "MySQL fallback: ENABLED");
                            sender.sendMessage(ChatColor.GRAY + dbManager.getConnectionInfo());
                        } else {
                            sender.sendMessage(ChatColor.RED + "MySQL fallback: DISABLED");
                        }
                        
                        break;

            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb islevel <info|refresh|clear|check|blocks|reload|debug>");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("collect")) {
            // /collect has no arguments
            return completions;
        }
        
        if (command.getName().equalsIgnoreCase("taspiasb")) {
            return getTaskSBTabCompletions(sender, args);
        }
        
        return completions;
    }
    
    private List<String> getTaskSBTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: main subcommands
            List<String> subcommands = Arrays.asList("reload", "npctalk", "bossbar", "zone", "cutscene", "pbeacon", "plightning", "islevel");
            return filterCompletions(subcommands, args[0]);
        }
        
        if (args.length >= 2) {
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "npctalk":
                    return getNpcTalkCompletions(args);
                    
                case "bossbar":
                    return getBossbarCompletions(args);
                    
                case "zone":
                    return getZoneCompletions(args);
                    
                case "cutscene":
                    return getCutsceneCompletions(args);
                    
                case "pbeacon":
                    return getPersonalBeaconCompletions(sender, args);
                    
                case "plightning":
                    return getPersonalLightningCompletions(sender, args);
                    
                case "islevel":
                    return getIslandLevelCompletions(args);
                    
                case "reload":
                    // No additional arguments
                    return completions;
                    
                default:
                    return completions;
            }
        }
        
        return completions;
    }
    
    private List<String> getNpcTalkCompletions(String[] args) {
        if (args.length == 2) {
            // NPC IDs - you can expand this list based on your NPCs
            return filterCompletions(Arrays.asList("wizard_of_alibon", "merchant", "guard"), args[1]);
        }
        return new ArrayList<>();
    }
    
    private List<String> getBossbarCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            return filterCompletions(Arrays.asList("add", "remove", "list", "colors"), args[1]);
        }
        
        if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (Arrays.asList("add", "remove", "list").contains(action)) {
                // Player names
                return getOnlinePlayerNames(args[2]);
            }
        }
        
        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            // Boss bar IDs for removal - get from CustomBossBarManager if player exists
            if (args.length > 2 && !args[2].isEmpty()) {
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer != null) {
                    Set<String> bossBarIds = customBossBarManager.getPlayerBossBars(targetPlayer).keySet();
                    return filterCompletions(new ArrayList<>(bossBarIds), args[3]);
                }
            }
            return filterCompletions(Arrays.asList("<boss_bar_id>"), args[3]);
        }
        
        if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            // Boss bar colors - get dynamically from CustomBossBarManager
            String availableColors = customBossBarManager.getAvailableColors();
            List<String> colors = Arrays.asList(availableColors.split(", "));
            return filterCompletions(colors, args[4]);
        }
        
        return completions;
    }
    
    private List<String> getZoneCompletions(String[] args) {
        if (args.length == 2) {
            return filterCompletions(Arrays.asList("unlock", "lock"), args[1]);
        }
        
        if (args.length == 3) {
            // Zone IDs - you can expand this based on your zones
            return filterCompletions(Arrays.asList("spawn", "pvp", "mining", "farming"), args[2]);
        }
        
        if (args.length == 4) {
            // Player names
            return getOnlinePlayerNames(args[3]);
        }
        
        return new ArrayList<>();
    }
    
    private List<String> getCutsceneCompletions(String[] args) {
        if (args.length == 2) {
            // Cutscene IDs - you can expand this based on your cutscenes
            return filterCompletions(Arrays.asList("intro", "tutorial", "boss_fight"), args[1]);
        }
        
        if (args.length == 3) {
            // Player names
            return getOnlinePlayerNames(args[2]);
        }
        
        if (args.length == 4) {
            return filterCompletions(Arrays.asList("true", "false"), args[3]);
        }
        
        return new ArrayList<>();
    }
    
    private List<String> getPersonalBeaconCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            return filterCompletions(Arrays.asList("add", "remove", "list", "colors"), args[1]);
        }
        
        if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (Arrays.asList("add", "remove", "list").contains(action)) {
                // Player names
                return getOnlinePlayerNames(args[2]);
            }
        }
        
        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            // Beacon IDs - get from PersonalBeaconManager if player exists
            if (args.length > 2 && !args[2].isEmpty()) {
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer != null) {
                    Set<String> beaconIds = personalBeaconManager.getPlayerBeacons(targetPlayer).keySet();
                    return filterCompletions(new ArrayList<>(beaconIds), args[3]);
                }
            }
            return filterCompletions(Arrays.asList("<beacon_id>"), args[3]);
        }
        
        if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            // Beacon ID
            return filterCompletions(Arrays.asList("<beacon_id>"), args[3]);
        }
        
        if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            // X coordinate for beacon
            return getCoordinateSuggestions(sender, "x", args[4]);
        }
        
        if (args.length == 6 && args[1].equalsIgnoreCase("add")) {
            // Y coordinate for beacon
            return getCoordinateSuggestions(sender, "y", args[5]);
        }
        
        if (args.length == 7 && args[1].equalsIgnoreCase("add")) {
            // Z coordinate for beacon
            return getCoordinateSuggestions(sender, "z", args[6]);
        }
        
        if (args.length == 8 && args[1].equalsIgnoreCase("add")) {
            // Beacon colors - get them dynamically from PersonalBeaconManager
            String availableColors = personalBeaconManager.getAvailableColors();
            List<String> colors = Arrays.asList(availableColors.split(", "));
            return filterCompletions(colors, args[7]);
        }
        
        return completions;
    }
    
    private List<String> getPersonalLightningCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Player names
            return getOnlinePlayerNames(args[1]);
        }
        
        if (args.length == 3) {
            // World names - get all loaded worlds
            List<String> worldNames = Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.toList());
            return filterCompletions(worldNames, args[2]);
        }
        
        if (args.length == 4) {
            // X coordinate - suggest player's current X if they exist
            return getCoordinateSuggestions(sender, "x", args[3]);
        }
        
        if (args.length == 5) {
            // Y coordinate - suggest player's current Y if they exist
            return getCoordinateSuggestions(sender, "y", args[4]);
        }
        
        if (args.length == 6) {
            // Z coordinate - suggest player's current Z if they exist
            return getCoordinateSuggestions(sender, "z", args[5]);
        }
        
        return new ArrayList<>();
    }
    
    private List<String> getIslandLevelCompletions(String[] args) {
        if (args.length == 2) {
            return filterCompletions(Arrays.asList("info", "refresh", "clear", "check", "blocks", "reload", "debug"), args[1]);
        }
        
        if (args.length == 3) {
            String action = args[1].toLowerCase();
            
            if (action.equals("check")) {
                // Player names
                return getOnlinePlayerNames(args[2]);
            }
            
            if (action.equals("blocks")) {
                // Level numbers - suggest some common levels
                return filterCompletions(Arrays.asList("5", "10", "15", "20", "25", "30", "50", "100"), args[2]);
            }
        }
        
        return new ArrayList<>();
    }
    
    private List<String> getOnlinePlayerNames(String partialName) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialName.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<String> filterCompletions(List<String> options, String partial) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<String> getCoordinateSuggestions(CommandSender sender, String coordinate, String partial) {
        List<String> suggestions = new ArrayList<>();
        
        // If sender is a player, suggest their current coordinates
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            
            String value;
            switch (coordinate.toLowerCase()) {
                case "x":
                    value = String.valueOf(loc.getBlockX());
                    break;
                case "y":
                    value = String.valueOf(loc.getBlockY());
                    break;
                case "z":
                    value = String.valueOf(loc.getBlockZ());
                    break;
                default:
                    value = "0";
            }
            
            suggestions.add(value);
            
            // Add some common variations
            if (coordinate.equalsIgnoreCase("y")) {
                suggestions.add("64");  // Sea level
                suggestions.add("100"); // Common building height
                suggestions.add("200"); // High altitude
            }
        }
        
        // Add placeholder if no player location available
        suggestions.add("<" + coordinate + ">");
        
        return filterCompletions(suggestions, partial);
    }

} 