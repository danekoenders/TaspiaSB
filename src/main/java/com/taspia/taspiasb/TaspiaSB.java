package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaspiaSB extends JavaPlugin implements Listener {

    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final String finalMessage = ChatColor.translateAlternateColorCodes('&', "&eCongratulations, you received a &f&lSkeleton &espawner!");

    @Override
    public void onEnable() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getPluginManager().registerEvents(this, this);
            getLogger().info("Registered events and hooked into PlaceholderAPI.");
        } else {
            getLogger().warning("Could not find PlaceholderAPI!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            Bukkit.getScheduler().runTaskLater(this, () -> startTutorialCheck(player), 50L); // 5 seconds delay
        }
    }

    private void startTutorialCheck(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            BossBar bossBar = null;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                String[] tutorials = {
                        "tutorial1",
                        "tutorial2",
                        "tutorial3",
                        "tutorial4",
                        "tutorial5"
                };

                boolean allCompleted = true;
                for (int i = 0; i < tutorials.length; i++) {
                    String tutorial = tutorials[i];
                    String placeholder = "%quests_quest:" + tutorial + "_completed%";
                    String value = PlaceholderAPI.setPlaceholders(player, placeholder);

                    if ("false".equals(value)) {
                        if (bossBar == null || !bossBar.getTitle().equals(getTutorialMessage(tutorial))) {
                            if (bossBar != null) {
                                removeBossbar(bossBar);
                            }
                            bossBar = displayBossbar(player, getTutorialMessage(tutorial), (i + 1) / 6.0, BarColor.BLUE);
                        }
                        allCompleted = false;
                        break;
                    }
                }

                if (allCompleted) {
                    if (bossBar != null) {
                        removeBossbar(bossBar);
                    }
                    bossBar = displayBossbar(player, finalMessage, 1.0, BarColor.YELLOW);
                    Bukkit.getScheduler().runTaskLater(TaspiaSB.this, () -> removeBossbar(bossBar), 250L); // 15 seconds delay
                    this.cancel();
                }
            }
        };

        task.runTaskTimer(this, 0L, 20L); // Schedule to run every 20 ticks
        tasks.put(player.getUniqueId(), task);
    }

    private String getTutorialMessage(String tutorial) {
        switch (tutorial) {
            case "tutorial1":
                return ChatColor.translateAlternateColorCodes('&', "&e&lTutorial: &fCreate your first island using &e/is create&f. &b(1/5) &e(&8+&f\uD83D\uDC80&e)");
            case "tutorial2":
                return ChatColor.translateAlternateColorCodes('&', "&e&lTutorial: &fCreate a cobblestone generator to get your first materials. &b(2/5) &e(&8+&f\uD83D\uDC80&e)");
            case "tutorial3":
                return ChatColor.translateAlternateColorCodes('&', "&e&lTutorial: &fFarm some wheat at the wheat farm in &3Alibon &e(/spawn)&f. &b(3/5) &e(&8+&f\uD83D\uDC80&e)");
            case "tutorial4":
                return ChatColor.translateAlternateColorCodes('&', "&e&lTutorial: &fVisit the &5Quest Master &fat his house in &3Alibon&f. &b(4/5) &e(&8+&f\uD83D\uDC80&e)");
            case "tutorial5":
                return ChatColor.translateAlternateColorCodes('&', "&e&lTutorial: &fVisit your personal &6Unlocks & Rewards NPC &fat the well in &3Alibon &b(5/5) &e(&8+&f\uD83D\uDC80&e)");
            default:
                return ChatColor.translateAlternateColorCodes('&', "&fUnknown Tutorial");
        }
    }

    private BossBar displayBossbar(Player player, String message, double progress, BarColor color) {
        BossBar bossBar = Bukkit.createBossBar(
                message,
                color,
                BarStyle.SOLID
        );
        bossBar.setProgress(progress);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        return bossBar;
    }

    private void removeBossbar(BossBar bossBar) {
        bossBar.setVisible(false);
        bossBar.removeAll();
    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onPlayerRespawn(PlayerRespawnEvent event) {
//        Player player = event.getPlayer();
//        World world = player.getWorld();
//        Location respawnLocation;
//
//        if (world.getName().contains("shaldornkeep")) {
//            // Set respawn location to player's last checkpoint
//            Bukkit.getLogger().info("Respawning at last spawn location in Dungeon");
//            return;
//        } else if (world.getName().equals("Dryland-Zone")) {
//            // Set respawn location to the spawnpoints of these worlds
//            respawnLocation = world.getSpawnLocation();
//            Bukkit.getLogger().info("Respawning at a Zone: " + respawnLocation);
//        } else {
//            // Using SuperiorSkyblock API
//            com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer sPlayer = SuperiorSkyblockAPI.getPlayer(player);
//            if (sPlayer.hasIsland()) {
//                // Set respawn location to player's island
//                respawnLocation = sPlayer.getIsland().getTeleportLocation(World.Environment.NORMAL);
//                Bukkit.getLogger().info("Respawning at Island: " + respawnLocation);
//            } else {
//                // Set respawn location to server's spawnpoint
//                respawnLocation = Bukkit.getWorld("world").getSpawnLocation();
//                Bukkit.getLogger().info("Respawning at Spawn: " + respawnLocation);
//            }
//        }
//
//        event.setRespawnLocation(respawnLocation);
//    }

    @Override
    public void onDisable() {
        tasks.values().forEach(BukkitRunnable::cancel);
        tasks.clear();
    }
}