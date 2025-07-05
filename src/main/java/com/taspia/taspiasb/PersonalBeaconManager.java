package com.taspia.taspiasb;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersonalBeaconManager implements Listener {
    
    private final TaspiaSB plugin;
    private final ProtocolManager protocolManager;
    
    // Map structure: PlayerUUID -> (BeaconID -> PersonalBeacon)
    private final Map<UUID, Map<String, PersonalBeacon>> personalBeacons = new HashMap<>();

    public PersonalBeaconManager(TaspiaSB plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public boolean addPersonalBeacon(Player player, String id, Location location, String color) {
        // Validate color
        BeaconColor beaconColor = parseBeaconColor(color);
        if (beaconColor == null) {
            return false; // Invalid color
        }
        
        // Get or create player's beacon map
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        // Remove existing beacon with same ID if present
        removePersonalBeacon(player, id);
        
        // Create new personal beacon
        PersonalBeacon beacon = new PersonalBeacon(id, location, beaconColor);
        
        // Send fake beacon block to player
        sendFakeBeaconBlock(player, location);
        
        // Store in the map
        playerBeacons.put(id, beacon);
        return true;
    }

    public boolean removePersonalBeacon(Player player, String id) {
        Map<String, PersonalBeacon> playerBeacons = personalBeacons.get(player.getUniqueId());
        if (playerBeacons == null) {
            return false;
        }
        
        PersonalBeacon existingBeacon = playerBeacons.remove(id);
        if (existingBeacon != null) {
            // Send original block back to player
            sendOriginalBlock(player, existingBeacon.getLocation());
            
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
                sendOriginalBlock(player, beacon.getLocation());
            }
        }
    }

    private void sendFakeBeaconBlock(Player player, Location location) {
        try {
            // Create block change packet
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            
            // Set the location
            packet.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            
            // Set the block data (beacon) - wrap with ProtocolLib's WrappedBlockData
            WrappedBlockData wrappedBeaconData = WrappedBlockData.createData(Material.BEACON.createBlockData());
            packet.getBlockData().write(0, wrappedBeaconData);
            
            // Send packet to player
            protocolManager.sendServerPacket(player, packet);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send fake beacon block: " + e.getMessage());
        }
    }

    private void sendOriginalBlock(Player player, Location location) {
        try {
            // Create block change packet with original block
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            
            // Set the location
            packet.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            
            // Set the original block data - wrap with ProtocolLib's WrappedBlockData
            WrappedBlockData wrappedOriginalData = WrappedBlockData.createData(location.getBlock().getBlockData());
            packet.getBlockData().write(0, wrappedOriginalData);
            
            // Send packet to player
            protocolManager.sendServerPacket(player, packet);
            
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
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up all personal beacons when player quits
        removeAllPersonalBeacons(player);
    }

    public void shutdown() {
        // Clean up all personal beacons
        for (UUID playerId : personalBeacons.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeAllPersonalBeacons(player);
            }
        }
        personalBeacons.clear();
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