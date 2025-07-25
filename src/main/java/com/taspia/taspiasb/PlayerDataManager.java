package com.taspia.taspiasb;

import org.bukkit.configuration.ConfigurationSection;
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

    private ConfigurationSection getPlayerSection(UUID playerId) {
        return playerDataConfig.getConfigurationSection(playerId.toString());
    }

    public boolean hasPlayerClaimedReward(Player player, Reward reward) {
        String key = generateRewardKey(reward);
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) return false;
        
        List<String> claimedRewards = playerSection.getStringList("claimed-rewards");
        return claimedRewards != null && claimedRewards.contains(key);
    }

    public void markRewardAsClaimed(Player player, Reward reward) {
        String key = generateRewardKey(reward);
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) {
            playerSection = playerDataConfig.createSection(player.getUniqueId().toString());
        }
        
        List<String> claimedRewards = playerSection.getStringList("claimed-rewards");
        if (!claimedRewards.contains(key)) {
            claimedRewards.add(key);
            playerSection.set("claimed-rewards", claimedRewards);
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

    public void reloadPlayerData() {
        // First save any current changes to avoid losing data
        saveAllData();
        
        // Reload the configuration from disk
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        plugin.getLogger().info("Player data reloaded from playerdata.yml");
    }

    private void savePlayerData() {
        saveAllData();
    }

    private void savePlayerData(UUID playerId) {
        saveAllData();
    }

    // Zone management methods
    public boolean isZoneUnlocked(Player player, String zoneId) {
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) return false;
        
        ConfigurationSection zonesSection = playerSection.getConfigurationSection("unlocked-zones");
        if (zonesSection == null) return false;
        
        return zonesSection.getBoolean(zoneId, false);
    }

    public void setZoneUnlocked(Player player, String zoneId, boolean unlocked) {
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) {
            playerSection = playerDataConfig.createSection(player.getUniqueId().toString());
        }
        
        ConfigurationSection zonesSection = playerSection.getConfigurationSection("unlocked-zones");
        if (zonesSection == null) {
            zonesSection = playerSection.createSection("unlocked-zones");
        }
        
        zonesSection.set(zoneId, unlocked);
        savePlayerData(player.getUniqueId());
    }

    // Cutscene management methods
    public boolean isCutsceneCompleted(Player player, String cutsceneId) {
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) return false;
        
        ConfigurationSection cutscenesSection = playerSection.getConfigurationSection("completed-cutscenes");
        if (cutscenesSection == null) return false;
        
        return cutscenesSection.getBoolean(cutsceneId, false);
    }

    public void setCutsceneCompleted(Player player, String cutsceneId, boolean completed) {
        ConfigurationSection playerSection = getPlayerSection(player.getUniqueId());
        if (playerSection == null) {
            playerSection = playerDataConfig.createSection(player.getUniqueId().toString());
        }
        
        ConfigurationSection cutscenesSection = playerSection.getConfigurationSection("completed-cutscenes");
        if (cutscenesSection == null) {
            cutscenesSection = playerSection.createSection("completed-cutscenes");
        }
        
        cutscenesSection.set(cutsceneId, completed);
        savePlayerData(player.getUniqueId());
    }
}

