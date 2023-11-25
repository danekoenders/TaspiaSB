package com.taspia.taspiasb;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection rewardsSection = levelsSection.getConfigurationSection(levelKey);
                    Map<String, Reward> rewards = new HashMap<>();
                    for (String rewardKey : rewardsSection.getKeys(false)) {
                        String name = rewardsSection.getString(rewardKey + ".name");
                        Material material = Material.valueOf(rewardsSection.getString(rewardKey + ".material").toUpperCase());
                        String command = rewardsSection.getString(rewardKey + ".command");
                        rewards.put(rewardKey, new Reward(name, material, command));
                    }
                    rewardsByLevel.put(level, rewards);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid configuration in 'levels' section: " + levelKey);
                }
            }
        }
    }

    public Map<String, Reward> getRewardsForLevel(int level) {
        return rewardsByLevel.getOrDefault(level, new HashMap<>());
    }

    // Execute the command associated with a reward
    public void executeRewardCommand(Player player, String command) {
        String commandToExecute = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
    }
}

