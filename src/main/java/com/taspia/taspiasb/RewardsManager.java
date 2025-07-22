package com.taspia.taspiasb;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RewardsManager {
    private JavaPlugin plugin;
    private Map<Integer, Map<String, Reward>> rewardsByLevel = new HashMap<>();

    public RewardsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    private void loadRewards() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // Check if server config exists
        if (!configFile.exists()) {
            plugin.getLogger().info("No server config file found for rewards, using defaults only for initial creation");
            return;
        }
        
        // Load ONLY the server config file (not merged with default)
        FileConfiguration serverConfig = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Loading rewards from server config file: " + configFile.getAbsolutePath());
        
        ConfigurationSection levelsSection = serverConfig.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection rewardsSection = levelsSection.getConfigurationSection(levelKey);
                    
                    if (rewardsSection == null) {
                        plugin.getLogger().warning("Level '" + levelKey + "' is not a valid configuration section");
                        continue;
                    }
                    
                    Map<String, Reward> rewards = new HashMap<>();
                    for (String rewardKey : rewardsSection.getKeys(false)) {
                        // Skip non-reward sections like island-block-unlocks
                        if (rewardKey.equals("island-block-unlocks")) {
                            continue;
                        }
                        
                        // Check if this is a valid reward section with required fields
                        if (!rewardsSection.isConfigurationSection(rewardKey)) {
                            continue; // Skip if it's not a configuration section
                        }
                        
                        ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(rewardKey);
                        if (rewardSection == null) {
                            continue; // Skip if we can't get the section
                        }
                        
                        String name = rewardSection.getString("name");
                        String materialString = rewardSection.getString("material");
                        String command = rewardSection.getString("command");
                        
                        // Validate that all required fields are present
                        if (name == null || materialString == null || command == null) {
                            plugin.getLogger().warning("Invalid reward configuration in level " + level + 
                                ", reward '" + rewardKey + "': missing required fields (name, material, or command)");
                            continue;
                        }
                        
                        try {
                            Material material = Material.valueOf(materialString.toUpperCase());
                            rewards.put(rewardKey, new Reward(level, rewardKey, name, material, command));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material '" + materialString + "' in level " + level + 
                                ", reward '" + rewardKey + "'");
                        }
                    }
                    rewardsByLevel.put(level, rewards);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level number in 'levels' section: '" + levelKey + "' - must be a number");
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing level '" + levelKey + "': " + e.getMessage());
                }
            }
        } else {
            plugin.getLogger().warning("No 'levels' section found in server config.yml");
        }
    }

    public Map<Integer, Map<String, Reward>> getRewardsUpToLevel(int level) {
        Map<Integer, Map<String, Reward>> allRewards = new HashMap<>();
        for (int i = 1; i <= level; i++) {
            if (rewardsByLevel.containsKey(i)) {
                allRewards.put(i, new HashMap<>(rewardsByLevel.get(i)));
            }
        }
        return allRewards;
    }

    public Reward getReward(int level, String rewardKey) {
        Map<String, Reward> levelRewards = rewardsByLevel.getOrDefault(level, new HashMap<>());
        return levelRewards.get(rewardKey);
    }

    // Execute the command associated with a reward
    public void executeRewardCommand(Player player, String command) {
        String commandToExecute = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
    }

    public void reloadRewards() {
        rewardsByLevel.clear(); // Clear existing rewards before reloading
        loadRewards(); // Load rewards from server config only
        plugin.getLogger().info("Reloaded rewards from server config");
    }
}

