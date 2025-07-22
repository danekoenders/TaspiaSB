package com.taspia.taspiasb;

import com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI;
import com.alonsoaliaga.alonsolevels.api.events.LevelChangeEvent;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class IslandLevelManager implements Listener {
    
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> islandTopLevel;
    private final Map<Integer, Set<Material>> levelBlockUnlocks;
    private final Map<Material, Integer> blockRequiredLevels;
    
    public IslandLevelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.islandTopLevel = new HashMap<>();
        this.levelBlockUnlocks = new HashMap<>();
        this.blockRequiredLevels = new HashMap<>();
        
        loadBlockUnlocksFromConfig();
        plugin.getLogger().info("IslandLevelManager initialized with direct AlonsoLevels API");
        plugin.getLogger().info("Loaded block unlocks for " + levelBlockUnlocks.size() + " levels");
    }
    
    /**
     * Cache the highest island level when a player joins
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        try {
            Island island = SuperiorSkyblockAPI.getPlayer(uuid).getIsland();
            if (island == null) {
                plugin.getLogger().info("Player " + player.getName() + " joined but has no island");
                return;
            }
            
            List<SuperiorPlayer> members = island.getIslandMembers(true);
            int highest = members.stream()
                    .mapToInt(sp -> {
                        try {
                            return getPlayerLevel(sp.getUniqueId());
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to get level for player " + sp.getName() + ": " + e.getMessage());
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            
            islandTopLevel.put(uuid, highest);
            plugin.getLogger().info("Cached highest island level for " + player.getName() + ": " + highest + 
                                   " (Island members: " + members.size() + ")");
                                   
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error caching island level for player " + player.getName(), e);
        }
    }
    
    /**
     * Update the cache when a player's level changes
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChange(LevelChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        try {
            Island island = SuperiorSkyblockAPI.getPlayer(uuid).getIsland();
            if (island == null) {
                plugin.getLogger().info("Level change for " + player.getName() + " but player has no island");
                return;
            }
            
            List<SuperiorPlayer> members = island.getIslandMembers(true);
            int highest = members.stream()
                    .mapToInt(sp -> {
                        try {
                            return getPlayerLevel(sp.getUniqueId());
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to get level for player " + sp.getName() + ": " + e.getMessage());
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            
            // Update for all members of the island
            for (SuperiorPlayer sp : members) {
                islandTopLevel.put(sp.getUniqueId(), highest);
            }
            
            plugin.getLogger().info("Updated island level cache for " + members.size() + " members after " + 
                                   player.getName() + "'s level change. New highest level: " + highest + 
                                   " (Old: " + event.getOldLevel() + ", New: " + event.getNewLevel() + ")");
                                   
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error updating island level cache for player " + player.getName(), e);
        }
    }
    
    /**
     * Check block placement restrictions based on island level
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location loc = event.getBlock().getLocation();
        String world = loc.getWorld().getName();
        
        // Check if we're in an island world
        if (!world.toLowerCase().contains("islandworld")) {
            return;
        }
        
        Material blockType = event.getBlock().getType();
        
        // Check if this block requires a specific level
        if (!blockRequiredLevels.containsKey(blockType)) {
            // Block is not restricted, allow placement
            return;
        }
        
        int requiredLevel = blockRequiredLevels.get(blockType);
        int topLevel = islandTopLevel.getOrDefault(uuid, 0);
        
        plugin.getLogger().info("Block placement check for " + player.getName() + 
                               ": " + blockType.name() + " (Required: " + requiredLevel + 
                               ", Island Top Level: " + topLevel + ", World: " + world + ")");
        
        if (topLevel < requiredLevel) {
            event.setCancelled(true);
            player.sendMessage("§cJe eiland heeft nog niet het juiste level voor dit blok! " +
                              "Vereist level: §6" + requiredLevel + "§c, Hoogste level op eiland: §6" + topLevel);
            
            plugin.getLogger().info("Blocked " + player.getName() + " from placing " + blockType.name() + 
                                   " (insufficient island level: " + topLevel + " < " + requiredLevel + ")");
        }
    }
    
    /**
     * Optional: Clean up cache when player leaves
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Integer removedLevel = islandTopLevel.remove(uuid);
        if (removedLevel != null) {
            plugin.getLogger().info("Removed cached island level for " + event.getPlayer().getName() + 
                                   " (was: " + removedLevel + ")");
        }
    }
    
    /**
     * Load block unlock configuration from server config.yml only (not default)
     */
    private void loadBlockUnlocksFromConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // Check if server config exists
        if (!configFile.exists()) {
            plugin.getLogger().info("No server config file found, using defaults only for initial creation");
            return;
        }
        
        // Load ONLY the server config file (not merged with default)
        FileConfiguration serverConfig = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Loading block unlocks from server config file: " + configFile.getAbsolutePath());
        
        ConfigurationSection levelsSection = serverConfig.getConfigurationSection("levels");
        if (levelsSection == null) {
            plugin.getLogger().warning("No 'levels' section found in server config.yml!");
            plugin.getLogger().warning("Available sections: " + serverConfig.getKeys(false));
            return;
        }
        
        levelBlockUnlocks.clear();
        blockRequiredLevels.clear();
        
        plugin.getLogger().info("Found levels in server config: " + levelsSection.getKeys(false));
        
        for (String levelKey : levelsSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(levelKey);
                ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                
                if (levelSection != null && levelSection.contains("island-block-unlocks")) {
                    List<String> blockNames = levelSection.getStringList("island-block-unlocks");
                    Set<Material> materials = new HashSet<>();
                    
                    for (String blockName : blockNames) {
                        try {
                            Material material = Material.valueOf(blockName.toUpperCase());
                            materials.add(material);
                            
                            // Map each block to its required level
                            if (!blockRequiredLevels.containsKey(material) || blockRequiredLevels.get(material) > level) {
                                blockRequiredLevels.put(material, level);
                            }
                            
                            plugin.getLogger().info("Loaded block unlock: " + material.name() + " at level " + level);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material name in config: " + blockName + " at level " + level);
                        }
                    }
                    
                    if (!materials.isEmpty()) {
                        levelBlockUnlocks.put(level, materials);
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid level number in config: " + levelKey);
            }
        }
        
        plugin.getLogger().info("Loaded " + blockRequiredLevels.size() + " block restrictions across " + levelBlockUnlocks.size() + " levels");
    }
    
    /**
     * Get the cached island top level for a player
     * @param uuid Player UUID
     * @return Cached top level or 0 if not cached
     */
    public int getCachedIslandLevel(UUID uuid) {
        return islandTopLevel.getOrDefault(uuid, 0);
    }
    
    /**
     * Manually update cache for a player (for admin commands or debugging)
     * @param uuid Player UUID
     * @param level New level to cache
     */
    public void updateCache(UUID uuid, int level) {
        islandTopLevel.put(uuid, level);
        plugin.getLogger().info("Manually updated cache for " + uuid + " to level " + level);
    }
    
    /**
     * Get cache size for monitoring
     * @return Current cache size
     */
    public int getCacheSize() {
        return islandTopLevel.size();
    }
    
    /**
     * Clear the entire cache (for debugging/admin purposes)
     */
    public void clearCache() {
        int size = islandTopLevel.size();
        islandTopLevel.clear();
        plugin.getLogger().info("Cleared island level cache (" + size + " entries removed)");
    }
    
    /**
     * Force refresh cache for all online players
     */
    public void refreshAllCache() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            try {
                UUID uuid = player.getUniqueId();
                Island island = SuperiorSkyblockAPI.getPlayer(uuid).getIsland();
                if (island == null) return;
                
                List<SuperiorPlayer> members = island.getIslandMembers(true);
                int highest = members.stream()
                        .mapToInt(sp -> {
                            try {
                                return getPlayerLevel(sp.getUniqueId());
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .max()
                        .orElse(0);
                
                islandTopLevel.put(uuid, highest);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error refreshing cache for " + player.getName(), e);
            }
        });
        plugin.getLogger().info("Refreshed island level cache for all online players");
    }
    
    /**
     * Get all blocks unlocked up to a certain level
     * @param level The maximum level to check
     * @return Set of all unlocked materials
     */
    public Set<Material> getUnlockedBlocks(int level) {
        Set<Material> unlockedBlocks = new HashSet<>();
        
        for (Map.Entry<Integer, Set<Material>> entry : levelBlockUnlocks.entrySet()) {
            if (entry.getKey() <= level) {
                unlockedBlocks.addAll(entry.getValue());
            }
        }
        
        return unlockedBlocks;
    }
    
    /**
     * Get the required level for a specific material
     * @param material The block material  
     * @return Required level, or 0 if no restriction
     */
    public int getRequiredLevelForBlock(Material material) {
        return blockRequiredLevels.getOrDefault(material, 0);
    }
    
    /**
     * Check if a block is unlocked at a specific level
     * @param material The block material
     * @param level The level to check
     * @return True if the block is unlocked at this level
     */
    public boolean isBlockUnlocked(Material material, int level) {
        int requiredLevel = getRequiredLevelForBlock(material);
        return requiredLevel == 0 || level >= requiredLevel;
    }
    
    /**
     * Reload block unlocks from server config only
     */
    public void reloadBlockUnlocks() {
        // Don't call plugin.reloadConfig() as that merges with defaults
        // Instead, reload directly from server config file
        loadBlockUnlocksFromConfig();
        plugin.getLogger().info("Reloaded block unlocks configuration from server config");
    }
    
    /**
     * Get the block required levels map for debugging
     * @return Map of materials to required levels
     */
    public Map<Material, Integer> getBlockRequiredLevels() {
        return new HashMap<>(blockRequiredLevels);
    }
    
    /**
     * Get a player's level using AlonsoLevels API
     * @param uuid Player UUID
     * @return Player level or 0 if player not loaded
     */
    private int getPlayerLevel(UUID uuid) {
        try {
            int level = AlonsoLevelsAPI.getLevel(uuid);
            // API returns -1 if player is not loaded
            return level == -1 ? 0 : level;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get level for player " + uuid + ": " + e.getMessage());
            return 0;
        }
    }
} 