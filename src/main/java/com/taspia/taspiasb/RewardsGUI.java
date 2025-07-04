package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.tr7zw.nbtapi.NBTItem;

public class RewardsGUI implements Listener {

    private RewardsManager rewardsManager;
    private PlayerDataManager playerDataManager;
    private TaspiaSB plugin;

    public RewardsGUI(TaspiaSB plugin, RewardsManager rewardsManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
    }

    // Custom InventoryHolder for Rewards GUI
    public static class RewardsGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // Not used
        }
    }

    public void openGUI(Player player, int level) {
        Map<Integer, Map<String, Reward>> allRewards = rewardsManager.getRewardsUpToLevel(level);
        boolean hasRewards = false;

        for (Map.Entry<Integer, Map<String, Reward>> levelEntry : allRewards.entrySet()) {
            for (Map.Entry<String, Reward> rewardEntry : levelEntry.getValue().entrySet()) {
                Reward reward = rewardEntry.getValue();
                if (levelEntry.getKey() <= level && !playerDataManager.hasPlayerClaimedReward(player, reward)) {
                    hasRewards = true;
                    break;
                }
            }
            if (hasRewards) break;
        }

        Inventory gui;
        if (hasRewards) {
            gui = Bukkit.createInventory(new RewardsGUIHolder(), 54, ChatColor.GOLD + "REWARDS");
            fillWithGlassPanes(gui);
            addClaimAllButton(gui);
        } else {
            gui = Bukkit.createInventory(new RewardsGUIHolder(), 27, ChatColor.GOLD + "REWARDS");
            addNoRewardsItem(gui);
        }

        if (hasRewards) {
            for (Map.Entry<Integer, Map<String, Reward>> levelEntry : allRewards.entrySet()) {
                int rewardLevel = levelEntry.getKey();
                for (Map.Entry<String, Reward> rewardEntry : levelEntry.getValue().entrySet()) {
                    String rewardKey = rewardEntry.getKey();
                    Reward reward = rewardEntry.getValue();
                    if (rewardLevel <= level && !playerDataManager.hasPlayerClaimedReward(player, reward)) {
                        ItemStack item = createItem(reward, rewardLevel, rewardKey);
                        gui.addItem(item);
                    }
                }
            }
        }

        player.openInventory(gui);
    }

    private void fillWithGlassPanes(Inventory gui) {
        ItemStack orangeGlassPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
        ItemMeta meta = orangeGlassPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            orangeGlassPane.setItemMeta(meta);
        }

        for (int i = 36; i < 54; i++) {
            gui.setItem(i, orangeGlassPane);
        }
    }

    private void addClaimAllButton(Inventory gui) {
        ItemStack emeraldBlock = new ItemStack(Material.EMERALD_BLOCK, 1);
        ItemMeta meta = emeraldBlock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Claim All Rewards");
            emeraldBlock.setItemMeta(meta);
        }

        NBTItem nbtItem = new NBTItem(emeraldBlock);
        nbtItem.setBoolean("ClaimAllRewards", true);
        gui.setItem(49, nbtItem.getItem()); // Center of the last row
    }

    private void addNoRewardsItem(Inventory gui) {
        ItemStack redGlassPane = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        ItemMeta meta = redGlassPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "No rewards found!");
            redGlassPane.setItemMeta(meta);
        }
        gui.setItem(13, redGlassPane); // Center of the second row
    }

    private ItemStack createItem(Reward reward, int level, String rewardKey) {
        ItemStack item = new ItemStack(reward.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "You received this reward at level: " + ChatColor.WHITE + level);
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Click to claim!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger("RewardLevel", level);
        nbtItem.setString("RewardKey", rewardKey);

        return nbtItem.getItem();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof RewardsGUIHolder)) {
            return; // Not our inventory
        }

        event.setCancelled(true); // Prevent taking items from the inventory

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return; // No item clicked
        }

        NBTItem nbtItemAllRewards = new NBTItem(clickedItem);
        if (nbtItemAllRewards.hasKey("ClaimAllRewards") && nbtItemAllRewards.getBoolean("ClaimAllRewards")) {
            // Handle "Claim All" button
            claimAllRewards(player);
            return;
        }

        NBTItem nbtItem = new NBTItem(clickedItem);
        if (nbtItem.hasKey("RewardLevel") && nbtItem.hasKey("RewardKey")) {
            int level = nbtItem.getInteger("RewardLevel");
            String rewardKey = nbtItem.getString("RewardKey");

            Reward reward = rewardsManager.getReward(level, rewardKey);
            if (reward != null && !playerDataManager.hasPlayerClaimedReward(player, reward)) {
                // Process reward claiming
                rewardsManager.executeRewardCommand(player, reward.getCommand());
                playerDataManager.markRewardAsClaimed(player, reward);

                player.sendMessage(ChatColor.GREEN + "You have claimed your reward!");

                // Retrieve the player's level again
                String levelPlaceholder = "%alonsolevels_level%";
                String levelString = PlaceholderAPI.setPlaceholders(player, levelPlaceholder);
                int playerLevel = 0; // Default value in case of parsing failure
                try {
                    playerLevel = Integer.parseInt(levelString);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Could not determine your level.");
                    return; // Exit the method if level can't be determined
                }

                // Reopen the GUI to reflect the claimed reward
                openGUI(player, playerLevel);
            }
        }
    }

    private void claimAllRewards(Player player) {
        String levelPlaceholder = "%alonsolevels_level%";
        String levelString = PlaceholderAPI.setPlaceholders(player, levelPlaceholder);
        int playerLevel = 0;
        try {
            playerLevel = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Could not determine your level.");
            return;
        }

        Map<Integer, Map<String, Reward>> allRewards = rewardsManager.getRewardsUpToLevel(playerLevel);
        for (Map.Entry<Integer, Map<String, Reward>> levelEntry : allRewards.entrySet()) {
            for (Map.Entry<String, Reward> rewardEntry : levelEntry.getValue().entrySet()) {
                Reward reward = rewardEntry.getValue();
                if (!playerDataManager.hasPlayerClaimedReward(player, reward)) {
                    rewardsManager.executeRewardCommand(player, reward.getCommand());
                    playerDataManager.markRewardAsClaimed(player, reward);
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "All available rewards have been claimed!");
        openGUI(player, playerLevel); // Reopen the GUI
    }
}


