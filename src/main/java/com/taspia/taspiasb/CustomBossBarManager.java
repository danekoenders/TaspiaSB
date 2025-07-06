package com.taspia.taspiasb;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomBossBarManager implements Listener {
    
    // Map structure: PlayerUUID -> (BossBarID -> BossBar)
    private final Map<UUID, Map<String, BossBar>> customBossBars = new HashMap<>();

    public boolean addCustomBossBar(Player player, String id, String message, String color) {
        // Parse color
        BarColor barColor = parseColor(color);
        if (barColor == null) {
            return false; // Invalid color
        }
        
        // Remove existing boss bar with same ID if present (before getting map reference)
        removeCustomBossBar(player, id);
        
        // Get or create player's boss bar map (after potential cleanup)
        Map<String, BossBar> playerBossBars = customBossBars.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        // Create new custom boss bar
        BossBar customBossBar = Bukkit.createBossBar(message, barColor, BarStyle.SOLID);
        customBossBar.setProgress(1.0);
        customBossBar.addPlayer(player);
        customBossBar.setVisible(true);
        
        // Store in the map
        playerBossBars.put(id, customBossBar);
        return true;
    }

    public boolean removeCustomBossBar(Player player, String id) {
        Map<String, BossBar> playerBossBars = customBossBars.get(player.getUniqueId());
        if (playerBossBars == null) {
            return false;
        }
        
        BossBar existingBossBar = playerBossBars.remove(id);
        if (existingBossBar != null) {
            existingBossBar.removePlayer(player);
            
            // Clean up empty player map
            if (playerBossBars.isEmpty()) {
                customBossBars.remove(player.getUniqueId());
            }
            return true;
        }
        return false;
    }

    public void removeAllCustomBossBars(Player player) {
        Map<String, BossBar> playerBossBars = customBossBars.remove(player.getUniqueId());
        if (playerBossBars != null) {
            playerBossBars.values().forEach(bossBar -> bossBar.removePlayer(player));
        }
    }

    private BarColor parseColor(String color) {
        try {
            return BarColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getAvailableColors() {
        StringBuilder colors = new StringBuilder();
        for (BarColor color : BarColor.values()) {
            if (colors.length() > 0) {
                colors.append(", ");
            }
            colors.append(color.name().toLowerCase());
        }
        return colors.toString();
    }

    public Map<String, BossBar> getPlayerBossBars(Player player) {
        return customBossBars.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Restore all boss bars for this player when they rejoin
        restoreBossBars(player);
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up all custom boss bars when player quits
        removeAllCustomBossBars(player);
    }
    
    private void restoreBossBars(Player player) {
        Map<String, BossBar> playerBossBars = customBossBars.get(player.getUniqueId());
        if (playerBossBars != null) {
            for (BossBar bossBar : playerBossBars.values()) {
                // Re-add the player to their boss bars
                bossBar.addPlayer(player);
                bossBar.setVisible(true);
            }
        }
    }

    public void shutdown() {
        // Clean up all custom boss bars
        customBossBars.values().forEach(playerBossBars -> {
            playerBossBars.values().forEach(bossBar -> {
                bossBar.setVisible(false);
                bossBar.removeAll();
            });
        });
        customBossBars.clear();
    }
} 