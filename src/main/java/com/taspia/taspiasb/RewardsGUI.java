package com.taspia.taspiasb;

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

public class RewardsGUI implements Listener {

    private RewardsManager rewardsManager;
    private PlayerDataManager playerDataManager;

    public RewardsGUI(RewardsManager rewardsManager, PlayerDataManager playerDataManager) {
        this.rewardsManager = rewardsManager;
        this.playerDataManager = playerDataManager;
    }

    public void openGUI(Player player, int level) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.DARK_GRAY + "Skyblock Rewards"); // Size and title of the inventory

        Map<String, Reward> rewards = rewardsManager.getRewardsForLevel(level);
        for (Reward reward : rewards.values()) {
            ItemStack item = createItem(reward);
            gui.addItem(item); // Add item to the inventory
        }

        player.openInventory(gui);
    }

    private ItemStack createItem(Reward reward) {
        ItemStack item = new ItemStack(reward.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Click to claim!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle inventory clicks, claiming rewards, etc.
    }
}


