package vn.saly.silentmobs.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib-based entity visibility manager.
 * Uses WHITELIST policy: entities are hidden from everyone except designated
 * viewers.
 */
public class EntityHider {

    private final SLSilentMobs plugin;
    private final ProtocolManager protocolManager;
    private ModelVisibilityBridge modelBridge;

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

    public EntityHider(SLSilentMobs plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.modelBridge = ModelEngineVisibilityBridge.create(plugin, this::syncModelVisibility);
        registerPacketListener();
    }

    private void registerPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ENTITY_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled())
                    return;

                PacketContainer packet = event.getPacket();
                if (packet.getIntegers().size() == 0)
                    return;
                int entityId = packet.getIntegers().read(0);

                // Only intercept tracked entities
                if (!visibleTo.containsKey(entityId))
                    return;

                Entity tracked = trackedEntities.get(entityId);
                if (event.getPacketType().equals(PacketType.Play.Server.SPAWN_ENTITY)
                        && tracked != null
                        && packet.getUUIDs().size() > 0
                        && !tracked.getUniqueId().equals(packet.getUUIDs().read(0))) {
                    visibleTo.remove(entityId);
                    trackedEntities.remove(entityId, tracked);
                    plugin.getServer().getScheduler().runTask(plugin, () -> releaseModelState(tracked));
                    return;
                }

                Player receiver = event.getPlayer();
                Set<UUID> allowed = visibleTo.get(entityId);

                // If receiver is NOT in the allowed set, cancel the packet
                if (allowed != null && !allowed.contains(receiver.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    /**
     * Register an entity to be visible ONLY to the specified viewer.
     */
    public void hideFromAll(Entity entity, Player viewer) {
        hideFromAllExcept(entity, Set.of(viewer.getUniqueId()));
    }

    /**
     * Register an entity or replace its allowed viewer set.
     */
    public void hideFromAllExcept(Entity entity, Collection<UUID> viewers) {
        setViewers(entity, viewers);
    }

    /**
     * Atomically replace the viewers of a tracked entity and apply only the
     * visibility changes.
     */
    public void setViewers(Entity entity, Collection<UUID> viewers) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(viewers, "viewers");

        int entityId = entity.getEntityId();
        Set<UUID> allowed = ConcurrentHashMap.newKeySet();
        allowed.addAll(viewers);
        Set<UUID> previous = visibleTo.put(entityId, allowed);
        Entity previousEntity = trackedEntities.put(entityId, entity);

        if (previousEntity != null && !previousEntity.getUniqueId().equals(entity.getUniqueId())) {
            releaseModelState(previousEntity);
            previous = null;
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            boolean canSeeNow = allowed.contains(online.getUniqueId());
            if (previous == null) {
                if (canSeeNow) {
                    modelBridge.release(entity, online);
                } else {
                    hideEntity(entity, online);
                }
                continue;
            }

            boolean couldSeeBefore = previous.contains(online.getUniqueId());
            if (couldSeeBefore == canSeeNow) {
                continue;
            }
            if (canSeeNow) {
                showEntity(entity, online);
            } else {
                hideEntity(entity, online);
            }
        }
    }

    /**
     * Register an entity hidden from all, but visible to players with a specific
     * permission.
     */
    public void hideFromAllExceptPermission(Entity entity, String permission) {
        Set<UUID> allowed = new HashSet<>();

        // Add all online players who have the permission
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                allowed.add(online.getUniqueId());
            }
        }

        hideFromAllExcept(entity, allowed);
    }

    /**
     * Dynamically add a viewer to an already-tracked entity.
     */
    public void addViewer(int entityId, UUID playerUUID) {
        Set<UUID> allowed = visibleTo.get(entityId);
        if (allowed == null)
            return;
        allowed.add(playerUUID);

        Player player = plugin.getServer().getPlayer(playerUUID);
        Entity entity = trackedEntities.get(entityId);
        if (player != null && entity != null) {
            showEntity(entity, player);
        }
    }

    /**
     * Dynamically remove a viewer from an already-tracked entity.
     */
    public void removeViewer(int entityId, UUID playerUUID) {
        Set<UUID> allowed = visibleTo.get(entityId);
        if (allowed == null)
            return;
        allowed.remove(playerUUID);

        Player player = plugin.getServer().getPlayer(playerUUID);
        Entity entity = trackedEntities.get(entityId);
        if (player != null && entity != null) {
            hideEntity(entity, player);
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
        if (allowed != null && allowed.contains(player.getUniqueId())) {
            showEntity(entity, player);
        } else {
            hideEntity(entity, player);
        }
    }

    /**
     * Stop tracking an entity — makes it visible to everyone again.
     */
    public void untrack(Entity entity) {
        untrack(entity, true);
    }

    /**
     * Stop tracking an entity. Restore visibility only when the entity will stay
     * alive after this operation.
     */
    public void untrack(Entity entity, boolean restoreVisibility) {
        int entityId = entity.getEntityId();
        Set<UUID> allowed = visibleTo.remove(entityId);
        Entity tracked = trackedEntities.remove(entityId);
        if (allowed == null)
            return;

        Entity target = tracked != null ? tracked : entity;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (restoreVisibility && !allowed.contains(online.getUniqueId())) {
                showEntity(target, online);
            } else {
                modelBridge.release(target, online);
            }
        }
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
        Set<UUID> viewers = visibleTo.get(entityId);
        return viewers == null ? Collections.emptySet() : Set.copyOf(viewers);
    }

    /**
     * Get all tracked entity IDs.
     */
    public Set<Integer> getTrackedEntityIds() {
        return Set.copyOf(visibleTo.keySet());
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
        for (Entity entity : List.copyOf(trackedEntities.values())) {
            untrack(entity, false);
        }
        visibleTo.clear();
        trackedEntities.clear();
    }

    public boolean isModelEngineAvailable() {
        return modelBridge.isAvailable();
    }

    public String getModelEngineVersion() {
        return modelBridge.getVersion();
    }

    public void reloadIntegrations() {
        ModelVisibilityBridge previous = modelBridge;
        for (Entity entity : List.copyOf(trackedEntities.values())) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                previous.release(entity, online);
            }
        }
        previous.close();

        modelBridge = ModelEngineVisibilityBridge.create(plugin, this::syncModelVisibility);
        for (Entity entity : List.copyOf(trackedEntities.values())) {
            syncModelVisibility(entity);
        }
    }

    /**
     * Drop per-viewer state when a player disconnects to avoid stale ModelEngine
     * pairing references.
     */
    public void releasePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        for (Map.Entry<Integer, Entity> entry : trackedEntities.entrySet()) {
            Set<UUID> allowed = visibleTo.get(entry.getKey());
            if (allowed != null) {
                allowed.remove(playerId);
            }
            modelBridge.release(entry.getValue(), player);
        }
    }

    private void syncModelVisibility(Entity entity) {
        Set<UUID> allowed = visibleTo.get(entity.getEntityId());
        Entity tracked = trackedEntities.get(entity.getEntityId());
        if (allowed == null || tracked == null || !tracked.getUniqueId().equals(entity.getUniqueId())) {
            return;
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (allowed.contains(online.getUniqueId())) {
                modelBridge.show(entity, online);
            } else {
                modelBridge.hide(entity, online);
            }
        }
    }

    private void hideEntity(Entity entity, Player player) {
        sendDestroyPacket(player, entity.getEntityId());
        modelBridge.hide(entity, player);
    }

    private void showEntity(Entity entity, Player player) {
        try {
            if (entity.isValid() && player.isOnline()) {
                protocolManager.updateEntity(entity, List.of(player));
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to refresh entity for " + player.getName() + ": "
                    + exception.getMessage());
        }
        modelBridge.show(entity, player);
    }

    private void releaseModelState(Entity entity) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            modelBridge.release(entity, online);
        }
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
        clearAll();
        modelBridge.close();
        protocolManager.removePacketListeners(plugin);
    }
}
