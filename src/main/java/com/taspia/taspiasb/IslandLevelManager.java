package com.taspia.taspiasb;

import com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI;
import com.alonsoaliaga.alonsolevels.api.events.LevelChangeEvent;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
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
    private final DatabaseManager databaseManager;
    private final Map<UUID, Integer> islandHighestLevel; // Island UUID -> Highest Level
    private final Map<Integer, Set<Material>> levelBlockUnlocks;
    private final Map<Material, Integer> blockRequiredLevels;

    public IslandLevelManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.islandHighestLevel = new HashMap<>();
        this.levelBlockUnlocks = new HashMap<>();
        this.blockRequiredLevels = new HashMap<>();

        loadBlockUnlocksFromConfig();
        plugin.getLogger().info("IslandLevelManager initialized with hybrid level system (API + MySQL fallback)");
        plugin.getLogger().info("Loaded block unlocks for " + levelBlockUnlocks.size() + " levels");
        plugin.getLogger().info("MySQL fallback enabled: " + databaseManager.isEnabled());
        plugin.getLogger().info("Island-based caching system enabled");
    }
    
    /**
     * Cache the highest island level when a player joins
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        try {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerUuid);
            if (superiorPlayer == null) {
                plugin.getLogger().warning("Could not get SuperiorPlayer for " + player.getName());
                return;
            }
            
            Island island = superiorPlayer.getIsland();
            if (island == null) {
                plugin.getLogger().info("Player " + player.getName() + " joined but has no island");
                return;
            }
            
            // Update the cache for this island
            updateIslandCache(island);
            
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
        UUID playerUuid = player.getUniqueId();
        
        try {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerUuid);
            if (superiorPlayer == null) {
                plugin.getLogger().warning("Could not get SuperiorPlayer for " + player.getName());
                return;
            }
            
            Island island = superiorPlayer.getIsland();
            if (island == null) {
                plugin.getLogger().info("Player " + player.getName() + " level changed but has no island");
                return;
            }
            
            // Update the cache for this island
            updateIslandCache(island);
            
            plugin.getLogger().info("Updated island level cache after " + player.getName() + 
                                   "'s level change (Old: " + event.getOldLevel() + ", New: " + event.getNewLevel() + ")");
                                   
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error updating island level cache for player " + player.getName(), e);
        }
    }
    
    /**
     * Check block placement restrictions based on island level at placement location
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        Location blockLocation = event.getBlock().getLocation();
        String world = blockLocation.getWorld().getName();
        
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
        
        try {
            // Get the island at the block placement location (not player's island)
            Island placementIsland = SuperiorSkyblockAPI.getIslandAt(blockLocation);
            if (placementIsland == null) {
                // No island at this location, allow placement (shouldn't happen in island world)
                plugin.getLogger().warning("No island found at block placement location: " + blockLocation + 
                                          " for player " + player.getName());
                return;
            }
            
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerUuid);
            if (superiorPlayer == null) {
                plugin.getLogger().warning("Could not get SuperiorPlayer for " + player.getName());
                return;
            }
            
            // Check player's relationship to the island where they're placing the block
            boolean isMember = placementIsland.isMember(superiorPlayer);
            boolean isCoop = placementIsland.isCoop(superiorPlayer);
            
            if (!isMember && !isCoop) {
                // Player is neither member nor coop - SuperiorSkyblock should handle this,
                // but let's log it and allow SuperiorSkyblock to cancel if needed
                plugin.getLogger().fine("Player " + player.getName() + " is neither member nor coop of island at " + 
                                       blockLocation + " - letting SuperiorSkyblock handle permissions");
                return;
            }
            
                         // Get the cached highest level for this island
             int islandHighestLevel = getCachedIslandLevel(placementIsland);
            int requiredLevel = blockRequiredLevels.get(blockType);
            
            String playerRole = isMember ? "member" : "coop";
            plugin.getLogger().info("Block placement check for " + player.getName() + " (" + playerRole + 
                                   ") on island " + placementIsland.getUniqueId() + ": " + blockType.name() + 
                                   " (Required: " + requiredLevel + ", Island Highest Level: " + islandHighestLevel + ")");
            
            if (islandHighestLevel < requiredLevel) {
                event.setCancelled(true);
                
                String message = isMember ? 
                    "§cTo place this block, an island member needs to reach skyblock level " + requiredLevel + "!" :
                    "§cTo place this block, a member of this island needs to reach skyblock level " + requiredLevel + "!";
                    
                player.sendMessage(message + "§c Highest level on island: §6" + islandHighestLevel + "§c.");
                
                plugin.getLogger().info("Blocked " + player.getName() + " (" + playerRole + ") from placing " + 
                                       blockType.name() + " (insufficient island level: " + islandHighestLevel + 
                                       " < " + requiredLevel + ")");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking block placement for " + player.getName() + 
                                 " at " + blockLocation, e);
        }
    }
    
    /**
     * Note: We don't clean up island cache when players leave since islands persist
     * and other members may still be online. Cache cleanup happens during refreshAll.
     */
    
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
     * Get a player's level using hybrid approach (API + MySQL fallback)
     * @param uuid Player UUID
     * @return Player level or 0 if not found
     */
    private int getPlayerLevel(UUID uuid) {
        try {
            // First, check if player is loaded in AlonsoLevels API
            if (AlonsoLevelsAPI.isLoaded(uuid)) {
                int level = AlonsoLevelsAPI.getLevel(uuid);
                if (level != -1) {
                    plugin.getLogger().fine("Got level " + level + " for player " + uuid + " from AlonsoLevels API");
                    return level;
                }
            }
            
            // If player is not loaded or API returned -1, try MySQL fallback
            if (databaseManager.isEnabled()) {
                int level = databaseManager.getPlayerLevelFromDatabase(uuid);
                if (level != -1) {
                    plugin.getLogger().fine("Got level " + level + " for player " + uuid + " from MySQL database");
                    return level;
                } else {
                    plugin.getLogger().fine("Player " + uuid + " not found in database, defaulting to level 0");
                }
            }
            
            return 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get level for player " + uuid + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Update the cached level for a specific island
     * @param island The island to update cache for
     */
    public void updateIslandCache(Island island) {
        try {
            UUID islandUuid = island.getUniqueId();
            
            // Calculate the highest level among all island members
            List<SuperiorPlayer> members = island.getIslandMembers(true);
            
            if (members.isEmpty()) {
                plugin.getLogger().warning("Island " + islandUuid + " has no members");
                islandHighestLevel.put(islandUuid, 0);
                return;
            }
            
            int highest = members.stream()
                    .mapToInt(sp -> {
                        try {
                            return getPlayerLevel(sp.getUniqueId());
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to get level for island member " + sp.getName() + ": " + e.getMessage());
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            
            // Cache the result
            islandHighestLevel.put(islandUuid, highest);
            
            plugin.getLogger().fine("Updated island cache: " + islandUuid + " -> level " + highest + 
                                   " (among " + members.size() + " members)");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating cache for island " + island.getUniqueId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get the cached level for an island
     * @param island The island to get cached level for
     * @return Cached highest level, or calculates and caches if not present
     */
    public int getCachedIslandLevel(Island island) {
        UUID islandUuid = island.getUniqueId();
        
        // Check if we have this island cached
        Integer cachedLevel = islandHighestLevel.get(islandUuid);
        if (cachedLevel != null) {
            plugin.getLogger().fine("Retrieved cached level " + cachedLevel + " for island " + islandUuid);
            return cachedLevel;
        }
        
        // Not cached, calculate and cache it
        plugin.getLogger().fine("Island " + islandUuid + " not in cache, calculating...");
        updateIslandCache(island);
        
        return islandHighestLevel.getOrDefault(islandUuid, 0);
    }
    
    /**
     * Refresh cache for all online players' islands
     */
    public void refreshAllCache() {
        plugin.getLogger().info("Refreshing all island level caches...");
        
        Set<UUID> processedIslands = new HashSet<>();
        int refreshedCount = 0;
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            try {
                SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(onlinePlayer.getUniqueId());
                if (superiorPlayer == null) continue;
                
                Island island = superiorPlayer.getIsland();
                if (island == null) continue;
                
                UUID islandUuid = island.getUniqueId();
                if (processedIslands.contains(islandUuid)) continue;
                
                updateIslandCache(island);
                processedIslands.add(islandUuid);
                refreshedCount++;
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error refreshing cache for player " + onlinePlayer.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Refreshed " + refreshedCount + " island caches");
    }
    
    /**
     * Clear all cached island levels
     */
    public void clearCache() {
        int clearedCount = islandHighestLevel.size();
        islandHighestLevel.clear();
        plugin.getLogger().info("Cleared " + clearedCount + " cached island levels");
    }
    
    /**
     * Get cache statistics
     * @return Map with cache information
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_islands", islandHighestLevel.size());
        stats.put("online_players", Bukkit.getOnlinePlayers().size());
        return stats;
    }
} 