package com.taspia.taspiasb;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TaspiaSB extends JavaPlugin {

    private RewardsManager rewardsManager;
    private PlayerDataManager playerDataManager;
    private TutorialManager tutorialManager;
    private CustomBossBarManager customBossBarManager;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        this.rewardsManager = new RewardsManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.tutorialManager = new TutorialManager(this);
        this.customBossBarManager = new CustomBossBarManager();

        // Initialize command handler
        this.commandHandler = new CommandHandler(this, rewardsManager, playerDataManager, customBossBarManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new RewardsGUI(this, rewardsManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(tutorialManager, this);
        getServer().getPluginManager().registerEvents(customBossBarManager, this);

        // Register command executors
        getCommand("collect").setExecutor(commandHandler);
        getCommand("taspiasb").setExecutor(commandHandler);

        // Check for PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // Register the PlaceholderAPI expansion
            new TaspiaSBExpansion(this, rewardsManager, playerDataManager).register();
            getLogger().info("Registered events and hooked into PlaceholderAPI.");
        } else {
            getLogger().warning("Could not find PlaceholderAPI!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Shutdown managers
        if (tutorialManager != null) {
            tutorialManager.shutdown();
        }
        if (customBossBarManager != null) {
            customBossBarManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
    }

    // Getters for managers (if needed by other classes)
    public RewardsManager getRewardsManager() {
        return rewardsManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TutorialManager getTutorialManager() {
        return tutorialManager;
    }

    public CustomBossBarManager getCustomBossBarManager() {
        return customBossBarManager;
    }
}