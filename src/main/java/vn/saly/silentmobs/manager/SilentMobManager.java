package vn.saly.silentmobs.manager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;
import vn.saly.silentmobs.region.SilentRegion;
import vn.saly.silentmobs.visibility.EntityHider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central manager for all silent mobs on the server.
 */
public class SilentMobManager {

    private static final UUID REGION_SYSTEM_OWNER = new UUID(0L, 0L);

    private final SLSilentMobs plugin;
    private final EntityHider entityHider;

    // ownerUUID -> list of silent mobs
    private final Map<UUID, List<SilentMob>> silentMobs = new ConcurrentHashMap<>();

    // entityId -> SilentMob (reverse lookup)
    private final Map<Integer, SilentMob> entityIndex = new ConcurrentHashMap<>();

    public SilentMobManager(SLSilentMobs plugin, EntityHider entityHider) {
        this.plugin = plugin;
        this.entityHider = entityHider;
    }

    /**
     * Register a new silent mob after spawning.
     */
    public void addSilentMob(SilentMob mob) {
        Entity entity = mob.getEntity();
        if (entity == null || !entity.isValid()) {
            plugin.getLogger().warning("Ignored silent mob registration without a valid entity: " + mob.getMobId());
            return;
        }

        SilentMob existing = entityIndex.get(entity.getEntityId());
        if (existing != null) {
            if (existing.getEntity() != null
                    && existing.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return;
            }
            detachEntity(existing, false);
            removeFromOwnerList(existing);
        }

        UUID owner = mob.getOwnerUUID();
        silentMobs.computeIfAbsent(owner, k -> new ArrayList<>()).add(mob);
        entityIndex.put(mob.getEntityId(), mob);
        refreshVisibility(mob);
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
        SilentMob mob = entityIndex.get(entity.getEntityId());
        if (mob == null || mob.getEntity() == null
                || !mob.getEntity().getUniqueId().equals(entity.getUniqueId())) {
            return null;
        }
        return mob;
    }

    /**
     * Check if an entity is a tracked silent mob.
     */
    public boolean isSilentMob(Entity entity) {
        return getSilentMob(entity) != null;
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
        detachEntity(mob, true);
        removeFromOwnerList(mob);
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
            detachEntity(mob, true);
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
                detachEntity(mob, true);
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
                detachEntity(mob, true);
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
        int count = 0;
        for (Map.Entry<UUID, List<SilentMob>> entry : silentMobs.entrySet()) {
            Iterator<SilentMob> it = entry.getValue().iterator();
            while (it.hasNext()) {
                SilentMob mob = it.next();
                boolean expired = timeoutSeconds > 0 && mob.isExpired(timeoutSeconds);
                if (expired || !mob.isAlive()) {
                    detachEntity(mob, true);
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
        if (mob == null || newOwner == null || !mob.isAlive())
            return;

        removeFromOwnerList(mob);
        mob.reassignOwner(newOwner);
        silentMobs.computeIfAbsent(newOwner.getUniqueId(), key -> new ArrayList<>()).add(mob);
        refreshVisibility(mob);
    }

    /**
     * Get all owner UUIDs with active mobs.
     */
    public Set<UUID> getAllOwners() {
        return Set.copyOf(silentMobs.keySet());
    }

    /**
     * Hide all currently tracked entities from a newly joined player.
     */
    public void hideAllFrom(Player player) {
        for (SilentMob mob : List.copyOf(entityIndex.values())) {
            if (!mob.isAlive())
                continue;
            if (mob.canView(player)) {
                entityHider.addViewer(mob.getEntityId(), player.getUniqueId());
            } else {
                entityHider.removeViewer(mob.getEntityId(), player.getUniqueId());
            }
        }
    }

    /**
     * Recompute one mob's online viewer set from its current access policy.
     */
    public void refreshVisibility(SilentMob mob) {
        if (mob == null || !mob.isAlive())
            return;

        Set<UUID> allowed = new HashSet<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (mob.canView(online)) {
                allowed.add(online.getUniqueId());
            }
        }
        entityHider.setViewers(mob.getEntity(), allowed);
    }

    public void refreshAllViewers() {
        for (SilentMob mob : List.copyOf(entityIndex.values())) {
            refreshVisibility(mob);
        }
    }

    /**
     * Apply changed region access rules to already spawned region-managed mobs.
     */
    public void refreshRegion(SilentRegion region) {
        if (region == null)
            return;

        for (SilentMob mob : List.copyOf(entityIndex.values())) {
            if (mob.isRegionAccessManaged()
                    && region.getName().equalsIgnoreCase(mob.getRegionName())) {
                applyRegionPolicy(mob, region);
                refreshVisibility(mob);
            }
        }
    }

    public void refreshAllRegionPolicies() {
        for (SilentMob mob : List.copyOf(entityIndex.values())) {
            if (!mob.isRegionAccessManaged() || mob.getRegionName() == null)
                continue;
            SilentRegion region = plugin.getRegionManager().getRegion(mob.getRegionName());
            if (region != null) {
                applyRegionPolicy(mob, region);
            }
        }
    }

    /**
     * Shared maintenance pass used by the existing five-second task.
     */
    public int runMaintenance(int timeoutSeconds) {
        int removed = cleanupExpired(timeoutSeconds);
        refreshAllRegionPolicies();
        reassignOrphanedGlobalMobs();
        refreshAllViewers();
        return removed;
    }

    private void applyRegionPolicy(SilentMob mob, SilentRegion region) {
        mob.replaceAdditionalViewers(region.getAllowedPlayers());
        mob.replaceAdditionalViewPermissions(region.getAllowedPermissions());
        mob.setOwnerVisible(!region.hasAccessRules());

        if (!region.hasAccessRules()
                && REGION_SYSTEM_OWNER.equals(mob.getOwnerUUID())
                && mob.isAlive()) {
            Player nearest = findNearestPlayer(mob.getEntity(), 64.0);
            if (nearest != null) {
                reassignOwner(mob, nearest);
            }
        }
    }

    private void reassignOrphanedGlobalMobs() {
        boolean despawnWithoutPlayer = plugin.getConfigManager().getConfig()
                .getBoolean("global-silent.despawn-if-no-player", true);
        double radius = plugin.getConfigManager().getConfig().getDouble("global-silent.assign-radius", 32.0);

        for (SilentMob mob : List.copyOf(entityIndex.values())) {
            if (!mob.isGlobal() || mob.isRegionAccessManaged() || !mob.isAlive())
                continue;
            if (plugin.getServer().getPlayer(mob.getOwnerUUID()) != null)
                continue;

            Player nearest = findNearestPlayer(mob.getEntity(), radius);
            if (nearest != null) {
                reassignOwner(mob, nearest);
            } else if (despawnWithoutPlayer) {
                removeSilentMob(mob);
            }
        }
    }

    private Player findNearestPlayer(Entity entity, double radius) {
        if (entity == null)
            return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getWorld().equals(entity.getWorld()))
                continue;
            double distance = online.getLocation().distanceSquared(entity.getLocation());
            if (distance <= radius * radius && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = online;
            }
        }
        return nearest;
    }

    private void detachEntity(SilentMob mob, boolean despawn) {
        Entity entity = mob.getEntity();
        if (entity != null) {
            entityHider.untrack(entity, false);
            entityIndex.remove(mob.getEntityId(), mob);
        }
        if (despawn) {
            mob.despawn();
        }
    }

    private void removeFromOwnerList(SilentMob mob) {
        UUID owner = mob.getOwnerUUID();
        List<SilentMob> list = silentMobs.get(owner);
        if (list == null)
            return;
        list.remove(mob);
        if (list.isEmpty()) {
            silentMobs.remove(owner, list);
        }
    }

    public EntityHider getEntityHider() {
        return entityHider;
    }
}
