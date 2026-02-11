package vn.saly.silentmobs.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib-based entity visibility manager.
 * Uses WHITELIST policy: entities are hidden from everyone except designated
 * viewers.
 */
public class EntityHider {

    private final Plugin plugin;
    private final ProtocolManager protocolManager;

    // entityId -> set of player UUIDs who CAN see this entity
    private final Map<Integer, Set<UUID>> visibleTo = new ConcurrentHashMap<>();

    // entityId -> Entity reference
    private final Map<Integer, Entity> trackedEntities = new ConcurrentHashMap<>();

    private static final PacketType[] ENTITY_PACKETS = {
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_LOOK,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_HEAD_ROTATION,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_EFFECT,
            PacketType.Play.Server.ENTITY_SOUND,
            PacketType.Play.Server.ENTITY_STATUS
    };

    public EntityHider(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerPacketListener();
    }

    private void registerPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ENTITY_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled())
                    return;

                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);

                // Only intercept tracked entities
                if (!visibleTo.containsKey(entityId))
                    return;

                Player receiver = event.getPlayer();
                Set<UUID> allowed = visibleTo.get(entityId);

                // If receiver is NOT in the allowed set, cancel the packet
                if (allowed != null && !allowed.contains(receiver.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        });

        // Also intercept DESTROY packets to avoid issues
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_DESTROY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // Let destroy packets through - they should reach everyone
            }
        });
    }

    /**
     * Register an entity to be visible ONLY to the specified viewer.
     */
    public void hideFromAll(Entity entity, Player viewer) {
        int entityId = entity.getEntityId();
        Set<UUID> allowed = ConcurrentHashMap.newKeySet();
        allowed.add(viewer.getUniqueId());
        visibleTo.put(entityId, allowed);
        trackedEntities.put(entityId, entity);

        // Send destroy packet to all online players except the viewer
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(viewer) && online.canSee(entity)) {
                sendDestroyPacket(online, entityId);
            }
        }
    }

    /**
     * Register an entity hidden from all, but visible to players with a specific
     * permission.
     */
    public void hideFromAllExceptPermission(Entity entity, String permission) {
        int entityId = entity.getEntityId();
        Set<UUID> allowed = ConcurrentHashMap.newKeySet();

        // Add all online players who have the permission
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                allowed.add(online.getUniqueId());
            }
        }

        visibleTo.put(entityId, allowed);
        trackedEntities.put(entityId, entity);

        // Send destroy to non-allowed players
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!allowed.contains(online.getUniqueId()) && online.canSee(entity)) {
                sendDestroyPacket(online, entityId);
            }
        }
    }

    /**
     * Dynamically add a viewer to an already-tracked entity.
     */
    public void addViewer(int entityId, UUID playerUUID) {
        Set<UUID> allowed = visibleTo.get(entityId);
        if (allowed == null)
            return;
        allowed.add(playerUUID);
    }

    /**
     * Dynamically remove a viewer from an already-tracked entity.
     */
    public void removeViewer(int entityId, UUID playerUUID) {
        Set<UUID> allowed = visibleTo.get(entityId);
        if (allowed == null)
            return;
        allowed.remove(playerUUID);
        // Send destroy to the removed viewer
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null) {
            sendDestroyPacket(player, entityId);
        }
    }

    /**
     * Hide an already-tracked entity from a specific player.
     * Used when a new player joins.
     */
    public void hideFromPlayer(Entity entity, Player player) {
        int entityId = entity.getEntityId();
        if (!visibleTo.containsKey(entityId))
            return;

        Set<UUID> allowed = visibleTo.get(entityId);
        if (allowed != null && !allowed.contains(player.getUniqueId())) {
            sendDestroyPacket(player, entityId);
        }
    }

    /**
     * Stop tracking an entity — makes it visible to everyone again.
     */
    public void untrack(Entity entity) {
        int entityId = entity.getEntityId();
        visibleTo.remove(entityId);
        trackedEntities.remove(entityId);
    }

    /**
     * Check if an entity is currently tracked (hidden).
     */
    public boolean isTracked(int entityId) {
        return visibleTo.containsKey(entityId);
    }

    /**
     * Check if a player can see a tracked entity.
     */
    public boolean canSee(Player player, int entityId) {
        Set<UUID> allowed = visibleTo.get(entityId);
        return allowed != null && allowed.contains(player.getUniqueId());
    }

    /**
     * Get the set of viewer UUIDs for a tracked entity.
     */
    public Set<UUID> getViewers(int entityId) {
        return visibleTo.getOrDefault(entityId, Collections.emptySet());
    }

    /**
     * Get all tracked entity IDs.
     */
    public Set<Integer> getTrackedEntityIds() {
        return Collections.unmodifiableSet(visibleTo.keySet());
    }

    /**
     * Get tracked entity by ID.
     */
    public Entity getTrackedEntity(int entityId) {
        return trackedEntities.get(entityId);
    }

    /**
     * Clear all tracked entities.
     */
    public void clearAll() {
        visibleTo.clear();
        trackedEntities.clear();
    }

    private void sendDestroyPacket(Player player, int entityId) {
        try {
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, List.of(entityId));
            protocolManager.sendServerPacket(player, destroy);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send destroy packet: " + e.getMessage());
        }
    }

    public void close() {
        protocolManager.removePacketListeners(plugin);
        clearAll();
    }
}
