package vn.saly.silentmobs.model;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a silent mob — an entity visible only to its owner/viewer.
 */
public class SilentMob {

    private UUID ownerUUID;
    private String ownerName;
    private final String mobId;
    private final Location spawnLocation;
    private final long spawnTime;
    private final int level;
    private final boolean isGlobal;
    private String viewPermission; // null = owner-only, non-null = permission-based
    private String regionName; // null = not region-managed
    private boolean regionAccessManaged;
    private boolean ownerVisible = true;
    private final Set<UUID> additionalViewers = new HashSet<>();
    private final Set<String> additionalViewPermissions = new HashSet<>();

    private Entity entity;

    public SilentMob(Player owner, String mobId, Location spawnLocation, int level, boolean isGlobal) {
        this.ownerUUID = owner.getUniqueId();
        this.ownerName = owner.getName();
        this.mobId = mobId;
        this.spawnLocation = spawnLocation;
        this.spawnTime = System.currentTimeMillis();
        this.level = level;
        this.isGlobal = isGlobal;
    }

    /**
     * Constructor for global silent mobs (auto-assigned from CreatureSpawnEvent).
     */
    public SilentMob(UUID ownerUUID, String ownerName, String mobId, Entity entity, boolean isGlobal) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.mobId = mobId;
        this.spawnLocation = entity.getLocation();
        this.spawnTime = System.currentTimeMillis();
        this.level = 1;
        this.isGlobal = isGlobal;
        this.entity = entity;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Reassign ownership without losing visibility, region, level, or age data.
     */
    public void reassignOwner(Player newOwner) {
        this.ownerUUID = newOwner.getUniqueId();
        this.ownerName = newOwner.getName();
    }

    public String getMobId() {
        return mobId;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public int getLevel() {
        return level;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String getViewPermission() {
        return viewPermission;
    }

    public void setViewPermission(String viewPermission) {
        this.viewPermission = viewPermission;
    }

    public boolean isOwnerVisible() {
        return ownerVisible;
    }

    public void setOwnerVisible(boolean ownerVisible) {
        this.ownerVisible = ownerVisible;
    }

    public void addViewer(UUID uuid) {
        additionalViewers.add(uuid);
    }

    public void addViewPermission(String permission) {
        additionalViewPermissions.add(permission);
    }

    public void replaceAdditionalViewers(Collection<UUID> viewers) {
        additionalViewers.clear();
        additionalViewers.addAll(viewers);
    }

    public void replaceAdditionalViewPermissions(Collection<String> permissions) {
        additionalViewPermissions.clear();
        additionalViewPermissions.addAll(permissions);
    }

    public Set<UUID> getAdditionalViewers() {
        return Collections.unmodifiableSet(additionalViewers);
    }

    public Set<String> getAdditionalViewPermissions() {
        return Collections.unmodifiableSet(additionalViewPermissions);
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public boolean isRegionAccessManaged() {
        return regionAccessManaged;
    }

    public void setRegionAccessManaged(boolean regionAccessManaged) {
        this.regionAccessManaged = regionAccessManaged;
    }

    /**
     * Check if a player can view this silent mob.
     * Returns true if player is the owner OR has the required permission.
     */
    public boolean canView(Player player) {
        if (ownerVisible && player.getUniqueId().equals(ownerUUID))
            return true;
        if (additionalViewers.contains(player.getUniqueId()))
            return true;
        if (viewPermission != null && player.hasPermission(viewPermission))
            return true;
        for (String permission : additionalViewPermissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Age of this mob in seconds.
     */
    public long getAge() {
        return (System.currentTimeMillis() - spawnTime) / 1000;
    }

    /**
     * Check if this mob has exceeded its timeout.
     */
    public boolean isExpired(int timeoutSeconds) {
        if (timeoutSeconds <= 0)
            return false;
        return getAge() >= timeoutSeconds;
    }

    /**
     * Check if the entity is still alive and valid.
     */
    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Remove the entity from the world.
     */
    public void despawn() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public int getEntityId() {
        return entity != null ? entity.getEntityId() : -1;
    }
}
