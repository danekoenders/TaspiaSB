package com.taspia.taspiasb;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerDataManager {
    private TaspiaSB plugin;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    public PlayerDataManager(TaspiaSB plugin) {
        this.plugin = plugin;
        setupPlayerDataFile();
    }

    private void setupPlayerDataFile() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public boolean hasPlayerClaimedReward(Player player, Reward reward) {
        String key = generateRewardKey(reward);
        List<String> claimedRewards = playerDataConfig.getStringList(player.getUniqueId().toString());
        return claimedRewards != null && claimedRewards.contains(key);
    }

    public void markRewardAsClaimed(Player player, Reward reward) {
        String key = generateRewardKey(reward);
        List<String> claimedRewards = playerDataConfig.getStringList(player.getUniqueId().toString());
        if (!claimedRewards.contains(key)) {
            claimedRewards.add(key);
            playerDataConfig.set(player.getUniqueId().toString(), claimedRewards);
            savePlayerData();
        }
    }

    private String generateRewardKey(Reward reward) {
        return reward.getLevel() + ":" + reward.getIdentifier();
    }

    public void saveAllData() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
        }
    }

    private void savePlayerData() {
        saveAllData();
    }
}

