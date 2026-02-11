package vn.saly.silentmobs.model;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a silent mob — an entity visible only to its owner/viewer.
 */
public class SilentMob {

    private final UUID ownerUUID;
    private final String ownerName;
    private final String mobId;
    private final Location spawnLocation;
    private final long spawnTime;
    private final int level;
    private final boolean isGlobal;
    private String viewPermission; // null = owner-only, non-null = permission-based
    private String regionName; // null = not region-managed

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

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    /**
     * Check if a player can view this silent mob.
     * Returns true if player is the owner OR has the required permission.
     */
    public boolean canView(Player player) {
        if (player.getUniqueId().equals(ownerUUID))
            return true;
        if (viewPermission != null && player.hasPermission(viewPermission))
            return true;
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
