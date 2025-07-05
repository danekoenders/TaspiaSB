package com.taspia.taspiasb;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TutorialManager implements Listener {
    
    private final TaspiaSB plugin;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final String finalMessage = ChatColor.translateAlternateColorCodes('&', "&eCongratulations, you received a &f&lSkeleton &espawner!");

    public TutorialManager(TaspiaSB plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Wait 2.5 seconds for PlaceholderAPI to properly set up placeholders
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Check if tutorial5 is completed
            String tutorial5Placeholder = "%quests_quest:tutorial5_completed%";
            String tutorial5Value = PlaceholderAPI.setPlaceholders(player, tutorial5Placeholder);
            
            if ("false".equals(tutorial5Value)) {
                startTutorialCheck(player);
            }
        }, 50L); // 2.5 seconds delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Cancel tutorial task if running
        BukkitRunnable task = tasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
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
                    Bukkit.getScheduler().runTaskLater(plugin, () -> removeBossbar(bossBar), 250L); // 15 seconds delay
                    this.cancel();
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 20L); // Schedule to run every 20 ticks
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

    public void shutdown() {
        tasks.values().forEach(BukkitRunnable::cancel);
        tasks.clear();
    }
} 