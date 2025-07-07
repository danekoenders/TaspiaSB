package com.taspia.taspiasb;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PersonalLightningManager {
    
    private final TaspiaSB plugin;
    private final ProtocolManager protocolManager;
    
    // Counter for unique entity IDs
    private final AtomicInteger entityIdCounter = new AtomicInteger(1000000);

    public PersonalLightningManager(TaspiaSB plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Spawns a lightning bolt at the specified location that only the specified player can see
     * 
     * @param player The player who should see the lightning bolt
     * @param location Where to spawn the lightning bolt
     * @return true if lightning was spawned successfully, false otherwise
     */
    public boolean spawnPersonalLightning(Player player, Location location) {
        // Generate unique entity ID
        int entityId = entityIdCounter.incrementAndGet();
        
        // Send the lightning bolt spawn packet
        return spawnLightningEntity(player, location, entityId);
    }

    private boolean spawnLightningEntity(Player player, Location location, int entityId) {
        try {
            // Create spawn entity packet for lightning bolt
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            
            // Set entity ID
            spawnPacket.getIntegers().write(0, entityId);
            
            // Set entity UUID (generate random UUID for the fake entity)
            spawnPacket.getUUIDs().write(0, UUID.randomUUID());
            
            // Set entity type to lightning bolt
            spawnPacket.getEntityTypeModifier().write(0, EntityType.LIGHTNING_BOLT);
            
            // Set position
            spawnPacket.getDoubles().write(0, location.getX()); // X
            spawnPacket.getDoubles().write(1, location.getY()); // Y
            spawnPacket.getDoubles().write(2, location.getZ()); // Z
            
            // Set rotation (lightning doesn't really rotate, but required for packet)
            spawnPacket.getIntegers().write(1, 0); // Pitch
            spawnPacket.getIntegers().write(2, 0); // Yaw
            spawnPacket.getIntegers().write(3, 0); // Head yaw
            
            // Lightning bolts don't need velocity data - skip velocity fields
            
            // Send the spawn packet
            protocolManager.sendServerPacket(player, spawnPacket);
            
            // Create and send entity metadata packet for proper lightning appearance
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            // Create data watcher for lightning bolt
            WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
            // Lightning bolts don't need special metadata, but we need to send an empty one
            metadataPacket.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
            
            protocolManager.sendServerPacket(player, metadataPacket);
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

} 