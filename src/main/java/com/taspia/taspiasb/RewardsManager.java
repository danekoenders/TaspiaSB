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
                        rewards.put(rewardKey, new Reward(level, rewardKey, name, material, command));
                    }
                    rewardsByLevel.put(level, rewards);
                    plugin.getLogger().info("Loaded rewards for level " + levelKey + ": " + rewards.toString());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid configuration in 'levels' section: " + levelKey);
                }
            }
        } else {
            plugin.getLogger().warning("No 'levels' section found in config.yml");
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
        loadRewards(); // Assuming loadRewards is your method to load rewards from the config
    }
}

