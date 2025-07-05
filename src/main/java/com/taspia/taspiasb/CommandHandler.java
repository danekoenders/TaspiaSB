package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    
    private final TaspiaSB plugin;
    private final RewardsManager rewardsManager;
    private final PlayerDataManager playerDataManager;
    private final CustomBossBarManager customBossBarManager;

    public CommandHandler(TaspiaSB plugin, RewardsManager rewardsManager, PlayerDataManager playerDataManager, CustomBossBarManager customBossBarManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
        this.customBossBarManager = customBossBarManager;
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
            sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReloadCommand(sender);
            case "npctalk":
                return handleNPCTalkCommand(sender, args);
            case "bossbar":
                return handleBossBarCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /taspiasb <reload|npctalk|bossbar>");
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
} 