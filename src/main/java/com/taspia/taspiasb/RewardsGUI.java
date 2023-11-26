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

    public void openGUI(Player player, int level) {
        plugin.getLogger().info("Opening GUI for level: " + level); // Debug message
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.DARK_GRAY + "Skyblock Rewards"); // Size and title of the inventory

        Map<Integer, Map<String, Reward>> allRewards = rewardsManager.getRewardsUpToLevel(level);
        for (Map.Entry<Integer, Map<String, Reward>> levelEntry : allRewards.entrySet()) {
            int rewardLevel = levelEntry.getKey();
            for (Map.Entry<String, Reward> rewardEntry : levelEntry.getValue().entrySet()) {
                String rewardKey = rewardEntry.getKey();
                Reward reward = rewardEntry.getValue();
                if (!playerDataManager.hasPlayerClaimedReward(player, reward)) {
                    ItemStack item = createItem(reward, rewardLevel, rewardKey);
                    gui.addItem(item);
                }
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createItem(Reward reward, int level, String rewardKey) {
        ItemStack item = new ItemStack(reward.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getName()));
            List<String> lore = new ArrayList<>();
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
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != event.getWhoClicked()) {
            return; // Not our inventory
        }

        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Skyblock Rewards")) {
            event.setCancelled(true); // Prevent taking items from the inventory

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return; // No item clicked
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
    }
}


