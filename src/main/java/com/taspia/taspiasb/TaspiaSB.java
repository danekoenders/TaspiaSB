package com.taspia.taspiasb;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TaspiaSB extends JavaPlugin {

    private RewardsManager rewardsManager;
    private PlayerDataManager playerDataManager;
    private CustomBossBarManager customBossBarManager;
    private PersonalBeaconManager personalBeaconManager;
    private PersonalLightningManager personalLightningManager;
    private IslandLevelManager islandLevelManager;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        this.rewardsManager = new RewardsManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.customBossBarManager = new CustomBossBarManager();
        this.personalBeaconManager = new PersonalBeaconManager(this);
        this.personalLightningManager = new PersonalLightningManager(this);
        this.islandLevelManager = new IslandLevelManager(this);

        // Initialize command handler
        this.commandHandler = new CommandHandler(this, rewardsManager, playerDataManager, customBossBarManager, personalBeaconManager, personalLightningManager, islandLevelManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new RewardsGUI(this, rewardsManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(customBossBarManager, this);
        getServer().getPluginManager().registerEvents(personalBeaconManager, this);
        getServer().getPluginManager().registerEvents(islandLevelManager, this);

        // Register command executors and tab completers
        getCommand("collect").setExecutor(commandHandler);
        getCommand("collect").setTabCompleter(commandHandler);
        
        getCommand("taspiasb").setExecutor(commandHandler);
        getCommand("taspiasb").setTabCompleter(commandHandler);

        // Check for required dependencies
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // Register the PlaceholderAPI expansion
            new TaspiaSBExpansion(this, rewardsManager, playerDataManager).register();
            getLogger().info("Registered events and hooked into PlaceholderAPI.");
        } else {
            getLogger().warning("Could not find PlaceholderAPI!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            getLogger().info("Hooked into ProtocolLib for personal beacons and lightning.");
        } else {
            getLogger().warning("Could not find ProtocolLib! Personal beacon and lightning features will be disabled.");
        }

        // Check for island level management dependencies
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            getLogger().info("Hooked into SuperiorSkyblock2 for island management.");
        } else {
            getLogger().warning("Could not find SuperiorSkyblock2! Island level features may not work properly.");
        }

        if (Bukkit.getPluginManager().getPlugin("AlonsoLevels") != null) {
            getLogger().info("Hooked into AlonsoLevels API for level management.");
        } else {
            getLogger().severe("AlonsoLevels plugin not found! Island level caching system requires AlonsoLevels.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        // Shutdown managers
        if (customBossBarManager != null) {
            customBossBarManager.shutdown();
        }
        if (personalBeaconManager != null) {
            personalBeaconManager.shutdown();
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

    public CustomBossBarManager getCustomBossBarManager() {
        return customBossBarManager;
    }

    public PersonalBeaconManager getPersonalBeaconManager() {
        return personalBeaconManager;
    }

    public PersonalLightningManager getPersonalLightningManager() {
        return personalLightningManager;
    }

    public IslandLevelManager getIslandLevelManager() {
        return islandLevelManager;
    }
}