package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class TaspiaSBExpansion extends PlaceholderExpansion {
    private TaspiaSB plugin;
    private RewardsManager rewardsManager;
    private PlayerDataManager playerDataManager;
    private boolean isRed = true; // Static variable to keep track of color

    public TaspiaSBExpansion(TaspiaSB plugin, RewardsManager rewardsManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier(){
        return "taspiasb";
    }

    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("rewards_available")) {
            int availableRewards = countAvailableRewards(player);
            String message;
            ChatColor color; // Default color

            if (availableRewards == 1) {
                message = availableRewards + " reward available!";
                isRed = !isRed; // Toggle color only if rewards are available
                color = isRed ? ChatColor.RED : ChatColor.WHITE;
            } else if (availableRewards > 1) {
                message = availableRewards + " rewards available!";
                isRed = !isRed; // Toggle color only if rewards are available
                color = isRed ? ChatColor.RED : ChatColor.WHITE;
            } else {
                message = "No rewards available";
                color = ChatColor.RED; // Keep it red for no rewards
            }

            return color + message;
        }
        return null;
    }

    private int countAvailableRewards(Player player) {
        int count = 0;
        String levelPlaceholder = "%alonsolevels_level%";
        String levelString = PlaceholderAPI.setPlaceholders(player, levelPlaceholder);
        int playerLevel = 0;
        try {
            playerLevel = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            // Handle the exception, perhaps log it or return 0
            return 0;
        }

        for (Map.Entry<Integer, Map<String, Reward>> levelEntry : rewardsManager.getRewardsUpToLevel(playerLevel).entrySet()) {
            for (Map.Entry<String, Reward> rewardEntry : levelEntry.getValue().entrySet()) {
                Reward reward = rewardEntry.getValue();
                if (levelEntry.getKey() <= playerLevel && !playerDataManager.hasPlayerClaimedReward(player, reward)) {
                    count++;
                }
            }
        }
        return count;
    }


    private String alternatingColorMessage(String message, ChatColor color1, ChatColor color2) {
        StringBuilder coloredMessage = new StringBuilder();
        boolean useFirstColor = true;
        for (char c : message.toCharArray()) {
            coloredMessage.append(useFirstColor ? color1 : color2).append(c);
            if (c != ' ') {
                useFirstColor = !useFirstColor;
            }
        }
        return coloredMessage.toString();
    }
}


