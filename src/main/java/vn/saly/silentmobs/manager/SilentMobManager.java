package vn.saly.silentmobs.manager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import vn.saly.silentmobs.model.SilentMob;
import vn.saly.silentmobs.visibility.EntityHider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central manager for all silent mobs on the server.
 */
public class SilentMobManager {

    private final Plugin plugin;
    private final EntityHider entityHider;

    // ownerUUID -> list of silent mobs
    private final Map<UUID, List<SilentMob>> silentMobs = new ConcurrentHashMap<>();

    // entityId -> SilentMob (reverse lookup)
    private final Map<Integer, SilentMob> entityIndex = new ConcurrentHashMap<>();

    public SilentMobManager(Plugin plugin, EntityHider entityHider) {
        this.plugin = plugin;
        this.entityHider = entityHider;
    }

    /**
     * Register a new silent mob after spawning.
     */
    public void addSilentMob(SilentMob mob) {
        UUID owner = mob.getOwnerUUID();
        silentMobs.computeIfAbsent(owner, k -> new ArrayList<>()).add(mob);

        if (mob.getEntity() != null) {
            entityIndex.put(mob.getEntityId(), mob);

            if (mob.getViewPermission() != null) {
                // Permission-based: hide from all, then reveal to players with permission
                entityHider.hideFromAllExceptPermission(mob.getEntity(), mob.getViewPermission());
            } else {
                // Owner-only visibility
                Player viewer = plugin.getServer().getPlayer(owner);
                if (viewer != null) {
                    entityHider.hideFromAll(mob.getEntity(), viewer);
                }
            }
        }
    }

    /**
     * Get a silent mob by entity ID.
     */
    public SilentMob getSilentMob(int entityId) {
        return entityIndex.get(entityId);
    }

    /**
     * Get a silent mob by entity reference.
     */
    public SilentMob getSilentMob(Entity entity) {
        return entityIndex.get(entity.getEntityId());
    }

    /**
     * Check if an entity is a tracked silent mob.
     */
    public boolean isSilentMob(Entity entity) {
        return entityIndex.containsKey(entity.getEntityId());
    }

    /**
     * Get all active silent mobs for a player.
     */
    public List<SilentMob> getActiveMobs(UUID playerUUID) {
        return silentMobs.getOrDefault(playerUUID, Collections.emptyList())
                .stream()
                .filter(SilentMob::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * Count active silent mobs for a player.
     */
    public int getActiveCount(UUID playerUUID) {
        return getActiveMobs(playerUUID).size();
    }

    /**
     * Total count of all silent mobs on server.
     */
    public int getTotalCount() {
        return entityIndex.size();
    }

    /**
     * Remove a specific silent mob.
     */
    public void removeSilentMob(SilentMob mob) {
        if (mob.getEntity() != null) {
            entityHider.untrack(mob.getEntity());
            entityIndex.remove(mob.getEntityId());
        }
        mob.despawn();

        List<SilentMob> list = silentMobs.get(mob.getOwnerUUID());
        if (list != null) {
            list.remove(mob);
            if (list.isEmpty()) {
                silentMobs.remove(mob.getOwnerUUID());
            }
        }
    }

    /**
     * Despawn all silent mobs for a specific player.
     */
    public int despawnByPlayer(UUID playerUUID) {
        List<SilentMob> list = silentMobs.remove(playerUUID);
        if (list == null)
            return 0;

        int count = 0;
        for (SilentMob mob : list) {
            if (mob.getEntity() != null) {
                entityHider.untrack(mob.getEntity());
                entityIndex.remove(mob.getEntityId());
            }
            mob.despawn();
            count++;
        }
        return count;
    }

    /**
     * Despawn all silent mobs of a specific type for a player.
     */
    public int despawnByType(UUID playerUUID, String mobId) {
        List<SilentMob> list = silentMobs.get(playerUUID);
        if (list == null)
            return 0;

        int count = 0;
        Iterator<SilentMob> it = list.iterator();
        while (it.hasNext()) {
            SilentMob mob = it.next();
            if (mob.getMobId().equalsIgnoreCase(mobId)) {
                if (mob.getEntity() != null) {
                    entityHider.untrack(mob.getEntity());
                    entityIndex.remove(mob.getEntityId());
                }
                mob.despawn();
                it.remove();
                count++;
            }
        }

        if (list.isEmpty()) {
            silentMobs.remove(playerUUID);
        }
        return count;
    }

    /**
     * Despawn all silent mobs on the server.
     */
    public int despawnAll() {
        int count = 0;
        for (List<SilentMob> list : silentMobs.values()) {
            for (SilentMob mob : list) {
                if (mob.getEntity() != null) {
                    entityHider.untrack(mob.getEntity());
                }
                mob.despawn();
                count++;
            }
        }
        silentMobs.clear();
        entityIndex.clear();
        return count;
    }

    /**
     * Cleanup expired mobs based on timeout.
     */
    public int cleanupExpired(int timeoutSeconds) {
        if (timeoutSeconds <= 0)
            return 0;

        int count = 0;
        for (Map.Entry<UUID, List<SilentMob>> entry : silentMobs.entrySet()) {
            Iterator<SilentMob> it = entry.getValue().iterator();
            while (it.hasNext()) {
                SilentMob mob = it.next();
                if (mob.isExpired(timeoutSeconds) || !mob.isAlive()) {
                    if (mob.getEntity() != null) {
                        entityHider.untrack(mob.getEntity());
                        entityIndex.remove(mob.getEntityId());
                    }
                    mob.despawn();
                    it.remove();
                    count++;
                }
            }
            if (entry.getValue().isEmpty()) {
                silentMobs.remove(entry.getKey());
            }
        }
        return count;
    }

    /**
     * Reassign a mob's owner (used when original owner quits).
     */
    public void reassignOwner(SilentMob mob, Player newOwner) {
        // Remove from old owner's list
        List<SilentMob> oldList = silentMobs.get(mob.getOwnerUUID());
        if (oldList != null) {
            oldList.remove(mob);
            if (oldList.isEmpty()) {
                silentMobs.remove(mob.getOwnerUUID());
            }
        }

        // Create new SilentMob with new owner (since fields are final, we track via
        // index)
        SilentMob newMob = new SilentMob(
                newOwner.getUniqueId(),
                newOwner.getName(),
                mob.getMobId(),
                mob.getEntity(),
                mob.isGlobal());

        // Untrack old, track new
        if (mob.getEntity() != null) {
            entityHider.untrack(mob.getEntity());
            entityIndex.remove(mob.getEntityId());
        }

        silentMobs.computeIfAbsent(newOwner.getUniqueId(), k -> new ArrayList<>()).add(newMob);
        if (newMob.getEntity() != null) {
            entityIndex.put(newMob.getEntityId(), newMob);
            entityHider.hideFromAll(newMob.getEntity(), newOwner);
        }
    }

    /**
     * Get all owner UUIDs with active mobs.
     */
    public Set<UUID> getAllOwners() {
        return Collections.unmodifiableSet(silentMobs.keySet());
    }

    /**
     * Hide all currently tracked entities from a newly joined player.
     */
    public void hideAllFrom(Player player) {
        for (Map.Entry<Integer, SilentMob> entry : entityIndex.entrySet()) {
            SilentMob mob = entry.getValue();
            if (mob.isAlive() && !mob.canView(player)) {
                entityHider.hideFromPlayer(mob.getEntity(), player);
            }
            // If player CAN view (owner or permission), ensure they are a viewer
            if (mob.isAlive() && mob.canView(player)) {
                entityHider.addViewer(mob.getEntityId(), player.getUniqueId());
            }
        }
    }

    public EntityHider getEntityHider() {
        return entityHider;
    }
}
