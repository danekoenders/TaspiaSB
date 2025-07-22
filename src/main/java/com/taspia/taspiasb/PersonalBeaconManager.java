package com.taspia.taspiasb;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PersonalBeaconManager implements Listener {
    
    private final TaspiaSB plugin;
    private final ProtocolManager protocolManager;
    
    // Map structure: PlayerUUID -> (BeaconID -> PersonalBeacon)
    private final Map<UUID, Map<String, PersonalBeacon>> personalBeacons = new HashMap<>();
    
    // Track pending beacon resends for players - PlayerUUID -> Set of beacon IDs waiting for chunk load
    private final Map<UUID, Set<String>> pendingBeaconResends = new ConcurrentHashMap<>();
    
    // Active chunk monitoring tasks - PlayerUUID -> Task ID
    private final Map<UUID, Integer> activeMonitoringTasks = new ConcurrentHashMap<>();

    public PersonalBeaconManager(TaspiaSB plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        // Schedule periodic cleanup of offline players (every 30 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupOfflinePlayers, 36000L, 36000L);
    }

    public boolean addPersonalBeacon(Player player, String id, Location location, String color) {
        // Validate color
        BeaconColor beaconColor = parseBeaconColor(color);
        if (beaconColor == null) {
            return false; // Invalid color
        }
        
        // Get or create player's beacon map
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        // Limit to prevent abuse (max 20 beacons per player)
        if (playerBeacons.size() >= 20 && !playerBeacons.containsKey(id)) {
            return false; // Too many beacons
        }
        
        // Remove existing beacon with same ID if present
        removePersonalBeacon(player, id);
        
        // Create new personal beacon
        PersonalBeacon beacon = new PersonalBeacon(id, location, beaconColor);
        
        // Store in the map first
        playerBeacons.put(id, beacon);
        
        // Check if we should send the beacon immediately or let monitoring handle it
        if (shouldSendBeaconImmediately(player, location)) {
            // Calculate delay based on distance and ping
            long delayTicks = calculateChunkDelay(player.getLocation(), location, player);
            
            // Always send with delay now (minimum 1 tick for reliability)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Double check player is still online and in same world
                if (player.isOnline() && player.getWorld().equals(location.getWorld())) {
                    sendFakeBeaconBlock(player, location, beaconColor);
                }
            }, delayTicks);
        } else {
            // Start or restart monitoring to handle this new beacon
            startBeaconChunkMonitoring(player);
        }
        
        return true;
    }

    private boolean shouldSendBeaconImmediately(Player player, Location beaconLocation) {
        // Check if beacon is in same world as player
        if (beaconLocation.getWorld() == null || !beaconLocation.getWorld().equals(player.getWorld())) {
            return false; // Different world
        }
        
        // Check if beacon is within view distance
        int viewDistanceChunks = player.getWorld().getViewDistance();
        int viewDistanceBlocks = viewDistanceChunks * 16;
        double distance = player.getLocation().distance(beaconLocation);
        if (distance > viewDistanceBlocks) {
            return false; // Too far away
        }
        
        // Check if server chunk is loaded
        if (!beaconLocation.getChunk().isLoaded()) {
            return false; // Server chunk not loaded
        }
        
        return true; // All checks passed - safe to send immediately
    }

    public boolean removePersonalBeacon(Player player, String id) {
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.get(player.getUniqueId());
        if (playerBeacons == null) {
            return false;
        }
        
        PersonalBeacon existingBeacon = playerBeacons.remove(id);
        if (existingBeacon != null) {
            // Send original block back to player
            sendOriginalBlock(player, existingBeacon.getLocation(), existingBeacon.getColor());
            
            // Clean up empty player map
            if (playerBeacons.isEmpty()) {
                personalBeacons.remove(player.getUniqueId());
            }
            return true;
        }
        return false;
    }

    public void removeAllPersonalBeacons(Player player) {
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.remove(player.getUniqueId());
        if (playerBeacons != null) {
            for (PersonalBeacon beacon : playerBeacons.values()) {
                sendOriginalBlock(player, beacon.getLocation(), beacon.getColor());
            }
        }
    }

    private void sendFakeBeaconBlock(Player player, Location location, BeaconColor color) {
        try {
            // Send the beacon block
            PacketContainer beaconPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            beaconPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            WrappedBlockData wrappedBeaconData = WrappedBlockData.createData(Material.BEACON.createBlockData());
            beaconPacket.getBlockData().write(0, wrappedBeaconData);
            protocolManager.sendServerPacket(player, beaconPacket);
            
            // Send the 3x3 iron block base underneath the beacon for the beam effect
            Location baseLocation = location.clone().subtract(0, 1, 0);
            WrappedBlockData wrappedIronData = WrappedBlockData.createData(Material.IRON_BLOCK.createBlockData());
            
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironBlockLoc = baseLocation.clone().add(x, 0, z);
                    PacketContainer ironPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                    ironPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                        ironBlockLoc.getBlockX(), ironBlockLoc.getBlockY(), ironBlockLoc.getBlockZ()));
                    ironPacket.getBlockData().write(0, wrappedIronData);
                    protocolManager.sendServerPacket(player, ironPacket);
                }
            }
            
            // Send stained glass block above the beacon for color
            if (color != BeaconColor.WHITE) { // White is default, no glass needed
                Material glassType = getStainedGlassMaterial(color);
                Location glassLocation = location.clone().add(0, 1, 0);
                PacketContainer glassPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                glassPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                    glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ()));
                WrappedBlockData wrappedGlassData = WrappedBlockData.createData(glassType.createBlockData());
                glassPacket.getBlockData().write(0, wrappedGlassData);
                protocolManager.sendServerPacket(player, glassPacket);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send fake beacon block: " + e.getMessage());
        }
    }

    private void sendOriginalBlock(Player player, Location location, BeaconColor color) {
        try {
            // Restore the beacon block
            PacketContainer beaconPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            beaconPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            WrappedBlockData wrappedOriginalData = WrappedBlockData.createData(location.getBlock().getBlockData());
            beaconPacket.getBlockData().write(0, wrappedOriginalData);
            protocolManager.sendServerPacket(player, beaconPacket);
            
            // Restore the 3x3 base underneath the beacon
            Location baseLocation = location.clone().subtract(0, 1, 0);
            
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironBlockLoc = baseLocation.clone().add(x, 0, z);
                    PacketContainer ironPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                    ironPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                        ironBlockLoc.getBlockX(), ironBlockLoc.getBlockY(), ironBlockLoc.getBlockZ()));
                    WrappedBlockData wrappedIronOriginalData = WrappedBlockData.createData(ironBlockLoc.getBlock().getBlockData());
                    ironPacket.getBlockData().write(0, wrappedIronOriginalData);
                    protocolManager.sendServerPacket(player, ironPacket);
                }
            }
            
            // Restore the glass block above the beacon if it was colored
            if (color != BeaconColor.WHITE) {
                Location glassLocation = location.clone().add(0, 1, 0);
                PacketContainer glassPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                glassPacket.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                    glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ()));
                WrappedBlockData wrappedGlassOriginalData = WrappedBlockData.createData(glassLocation.getBlock().getBlockData());
                glassPacket.getBlockData().write(0, wrappedGlassOriginalData);
                protocolManager.sendServerPacket(player, glassPacket);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send original block: " + e.getMessage());
        }
    }

    private BeaconColor parseBeaconColor(String color) {
        try {
            return BeaconColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getAvailableColors() {
        StringBuilder colors = new StringBuilder();
        for (BeaconColor color : BeaconColor.values()) {
            if (!colors.isEmpty()) {
                colors.append(", ");
            }
            colors.append(color.name().toLowerCase());
        }
        return colors.toString();
    }

    public Map<String, PersonalBeacon> getPlayerBeacons(Player player) {
        return personalBeacons.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Start monitoring for chunk loading and beacon resending
        startBeaconChunkMonitoring(player);
    }
    
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Start monitoring for chunk loading and beacon resending
        startBeaconChunkMonitoring(player);
    }
    

    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Stop any active monitoring tasks for this player
        stopBeaconChunkMonitoring(playerId);
        
        // Don't remove beacon data - keep it for when they rejoin
        // Just clean up any active visual effects
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.get(playerId);
        if (playerBeacons != null) {
            // Beacons will automatically disappear from client when they disconnect
            // No need to send restoration packets to offline player
        }
    }
    
    private void startBeaconChunkMonitoring(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Stop any existing monitoring task
        stopBeaconChunkMonitoring(playerId);
        
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.get(playerId);
        if (playerBeacons == null || playerBeacons.isEmpty()) {
            return; // No beacons to monitor
        }
        
        // Collect ALL beacon IDs in the player's current world
        // (Not just pending ones - client clears all fake blocks on world change)
        Set<String> beaconsToCheck = new HashSet<>();
        for (Map.Entry<String, PersonalBeacon> entry : playerBeacons.entrySet()) {
            PersonalBeacon beacon = entry.getValue();
            Location beaconLoc = beacon.getLocation();
            
            // Only monitor beacons in the player's current world
            if (beaconLoc.getWorld() != null && beaconLoc.getWorld().equals(player.getWorld())) {
                beaconsToCheck.add(entry.getKey());
            }
        }
        
        if (beaconsToCheck.isEmpty()) {
            return; // No beacons in current world
        }
        
        // ALWAYS store beacons to check - even if they were sent before
        // Because world changes clear all fake blocks from client
        pendingBeaconResends.put(playerId, beaconsToCheck);
        
        // Start the monitoring task (runs every second)
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            monitorBeaconChunks(player);
        }, 20L, 20L).getTaskId(); // Start after 1 second, repeat every 1 second
        
        activeMonitoringTasks.put(playerId, taskId);
    }
    
    private void monitorBeaconChunks(Player player) {
        if (!player.isOnline()) {
            stopBeaconChunkMonitoring(player.getUniqueId());
            return;
        }
        
        UUID playerId = player.getUniqueId();
        Set<String> pendingBeacons = pendingBeaconResends.get(playerId);
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.get(playerId);
        
        if (pendingBeacons == null || pendingBeacons.isEmpty() || playerBeacons == null) {
            stopBeaconChunkMonitoring(playerId);
            return;
        }
        
        // Get server view distance
        int viewDistanceChunks = 10;
        int viewDistanceBlocks = viewDistanceChunks * 16; // Convert chunks to blocks
        
        // Check each pending beacon
        Set<String> processedBeacons = new HashSet<>();
        
        for (String beaconId : pendingBeacons) {
            PersonalBeacon beacon = playerBeacons.get(beaconId);
            if (beacon == null) {
                processedBeacons.add(beaconId); // Beacon was removed
                continue;
            }
            
            Location beaconLoc = beacon.getLocation();
            
            // Check if beacon is still in the same world as player
            if (beaconLoc.getWorld() == null || !beaconLoc.getWorld().equals(player.getWorld())) {
                processedBeacons.add(beaconId); // Beacon not in current world
                continue;
            }
            
            // Check if beacon is within view distance (where chunks should be loaded)
            // NOTE: Minecraft loads chunks uniformly in all directions around player,
            // regardless of facing direction. View distance creates a circular area.
            double distance = player.getLocation().distance(beaconLoc);
            if (distance > viewDistanceBlocks) {
                // Beacon is too far away - client won't have this chunk loaded
                continue; // Keep checking until player gets closer
            }
            
            // Check if the server chunk is loaded
            if (beaconLoc.getChunk().isLoaded()) {
                // Beacon is within view distance AND server chunk is loaded
                // Client should have this chunk - safe to send packet with distance and ping-based delay
                long delayTicks = calculateChunkDelay(player.getLocation(), beaconLoc, player);
                
                // Always send with delay now (minimum 2 ticks for reliability)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Double check player is still online and in same world
                    if (player.isOnline() && player.getWorld().equals(beaconLoc.getWorld())) {
                        sendFakeBeaconBlock(player, beaconLoc, beacon.getColor());
                    }
                }, delayTicks);
                
                processedBeacons.add(beaconId);
            }
        }
        
        // Remove processed beacons from pending list
        pendingBeacons.removeAll(processedBeacons);
        
        // If all beacons are processed, stop monitoring
        if (pendingBeacons.isEmpty()) {
            stopBeaconChunkMonitoring(playerId);
        }
    }
    
    private void stopBeaconChunkMonitoring(UUID playerId) {
        // Cancel the monitoring task
        Integer taskId = activeMonitoringTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        // Clear pending beacons
        pendingBeaconResends.remove(playerId);
    }

    public void shutdown() {
        // Stop all monitoring tasks
        for (UUID playerId : activeMonitoringTasks.keySet()) {
            stopBeaconChunkMonitoring(playerId);
        }
        
        // Clean up all personal beacons
        for (UUID playerId : personalBeacons.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeAllPersonalBeacons(player);
            }
        }
        personalBeacons.clear();
        pendingBeaconResends.clear();
        activeMonitoringTasks.clear();
    }
    
    private void cleanupOfflinePlayers() {
        // Remove beacon data for players who haven't been online in the last 7 days
        long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7 days
        
        personalBeacons.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);
            
            // Keep data if player is online
            if (player != null && player.isOnline()) {
                return false;
            }
            
            // Check last seen time (this is a simple implementation)
            // In production, you might want to store last seen time in your PlayerDataManager
            return Bukkit.getOfflinePlayer(playerId).getLastSeen() < cutoffTime;
        });
        
        // Also clean up beacons in worlds that no longer exist
        personalBeacons.values().forEach(playerBeacons -> {
            playerBeacons.entrySet().removeIf(beaconEntry -> {
                Location beaconLoc = beaconEntry.getValue().getLocation();
                return beaconLoc.getWorld() == null;
            });
        });
    }

    private Material getStainedGlassMaterial(BeaconColor color) {
        switch (color) {
            case WHITE: return Material.WHITE_STAINED_GLASS;
            case ORANGE: return Material.ORANGE_STAINED_GLASS;
            case MAGENTA: return Material.MAGENTA_STAINED_GLASS;
            case LIGHT_BLUE: return Material.LIGHT_BLUE_STAINED_GLASS;
            case YELLOW: return Material.YELLOW_STAINED_GLASS;
            case LIME: return Material.LIME_STAINED_GLASS;
            case PINK: return Material.PINK_STAINED_GLASS;
            case GRAY: return Material.GRAY_STAINED_GLASS;
            case LIGHT_GRAY: return Material.LIGHT_GRAY_STAINED_GLASS;
            case CYAN: return Material.CYAN_STAINED_GLASS;
            case PURPLE: return Material.PURPLE_STAINED_GLASS;
            case BLUE: return Material.BLUE_STAINED_GLASS;
            case BROWN: return Material.BROWN_STAINED_GLASS;
            case GREEN: return Material.GREEN_STAINED_GLASS;
            case RED: return Material.RED_STAINED_GLASS;
            case BLACK: return Material.BLACK_STAINED_GLASS;
            default: return Material.WHITE_STAINED_GLASS;
        }
    }

    private long calculateChunkDelay(Location playerLoc, Location beaconLoc, Player player) {
        // Calculate chunk distance (not block distance)
        int playerChunkX = playerLoc.getBlockX() >> 4;
        int playerChunkZ = playerLoc.getBlockZ() >> 4;
        int beaconChunkX = beaconLoc.getBlockX() >> 4;
        int beaconChunkZ = beaconLoc.getBlockZ() >> 4;
        
        int chunkDistance = Math.max(Math.abs(playerChunkX - beaconChunkX), Math.abs(playerChunkZ - beaconChunkZ));
        
        long baseDelay;
        if (chunkDistance <= 1) {
            baseDelay = chunkDistance + 2L; // 1-2 ticks (100ms)
        } else if (chunkDistance <= 4) {
            baseDelay = chunkDistance * 4L; // 4 ticks per chunk (200ms each)
        } else if (chunkDistance <= 8) {
            baseDelay = chunkDistance * 6L; // 6 ticks per chunk (300ms each)
        } else {
            baseDelay = chunkDistance * 10L; // 10 ticks per chunk (500ms each)
        }
        
        // Add ping-based delay (higher ping = more time needed)
        // Convert ping from ms to ticks: ping/50 (since 1 tick = 50ms)
        // Scale it down a bit so ping doesn't dominate: ping/100 ticks
        int playerPing = player.getPing();
        long pingDelay = Math.max(0, (playerPing / 50) * 2); // At least 0, ping in ms / 100 = ticks
        
        // Total delay = base + ping component
        long totalDelay = baseDelay + pingDelay;
        
        // Cap at 100 ticks (5 seconds) for very distant beacons or high ping
        return Math.min(totalDelay, 100L);
    }

    // Inner classes
    public static class PersonalBeacon {
        private final String id;
        private final Location location;
        private final BeaconColor color;

        public PersonalBeacon(String id, Location location, BeaconColor color) {
            this.id = id;
            this.location = location;
            this.color = color;
        }

        public String getId() { return id; }
        public Location getLocation() { return location; }
        public BeaconColor getColor() { return color; }
    }

    public enum BeaconColor {
        WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, 
        LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK
    }
} 