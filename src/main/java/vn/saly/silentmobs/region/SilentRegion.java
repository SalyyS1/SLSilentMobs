package vn.saly.silentmobs.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a named rectangular region where mob visibility can be controlled.
 * Each region has its own lists of silent/exempt mobs and allowed
 * players/permissions.
 */
public class SilentRegion {

    private final String name;
    private final String worldName;
    private final int x1, y1, z1, x2, y2, z2;

    // Mobs that are silent inside this region
    private final Set<String> silentMobs = new HashSet<>();
    // Mobs exempt from being silent (whitelist)
    private final Set<String> exemptMobs = new HashSet<>();
    // Players who can see silent mobs in this region (VIP)
    private final Set<UUID> allowedPlayers = new HashSet<>();
    // Permissions that grant visibility to silent mobs
    private final Set<String> allowedPermissions = new HashSet<>();

    public SilentRegion(String name, String worldName, Location pos1, Location pos2) {
        this.name = name;
        this.worldName = worldName;
        this.x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.y2 = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public SilentRegion(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.worldName = worldName;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    /**
     * Check if a location is inside this region.
     */
    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;
        if (!loc.getWorld().getName().equals(worldName))
            return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    /**
     * Check if a mob type should be silent in this region.
     */
    public boolean isMobSilent(String mobType) {
        String upper = mobType.toUpperCase();
        if (exemptMobs.contains(upper))
            return false;
        // If silentMobs is empty, ALL mobs in region are silent
        // If silentMobs has entries, only listed mobs are silent
        return silentMobs.isEmpty() || silentMobs.contains(upper);
    }

    /**
     * Check if a player can see silent mobs in this region.
     */
    public boolean canPlayerSee(Player player) {
        // Check specific player UUID
        if (allowedPlayers.contains(player.getUniqueId()))
            return true;
        // Check permissions
        for (String perm : allowedPermissions) {
            if (player.hasPermission(perm))
                return true;
        }
        return false;
    }

    public boolean hasAccessRules() {
        return !allowedPlayers.isEmpty() || !allowedPermissions.isEmpty();
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getZ2() {
        return z2;
    }

    // --- Silent Mobs ---
    public void addSilentMob(String mob) {
        silentMobs.add(mob.toUpperCase());
    }

    public void removeSilentMob(String mob) {
        silentMobs.remove(mob.toUpperCase());
    }

    public Set<String> getSilentMobs() {
        return Collections.unmodifiableSet(silentMobs);
    }

    // --- Exempt Mobs ---
    public void addExemptMob(String mob) {
        exemptMobs.add(mob.toUpperCase());
    }

    public void removeExemptMob(String mob) {
        exemptMobs.remove(mob.toUpperCase());
    }

    public Set<String> getExemptMobs() {
        return Collections.unmodifiableSet(exemptMobs);
    }

    // --- Allowed Players ---
    public void addAllowedPlayer(UUID uuid) {
        allowedPlayers.add(uuid);
    }

    public void removeAllowedPlayer(UUID uuid) {
        allowedPlayers.remove(uuid);
    }

    public Set<UUID> getAllowedPlayers() {
        return Collections.unmodifiableSet(allowedPlayers);
    }

    // --- Allowed Permissions ---
    public void addAllowedPermission(String perm) {
        allowedPermissions.add(perm);
    }

    public void removeAllowedPermission(String perm) {
        allowedPermissions.remove(perm);
    }

    public Set<String> getAllowedPermissions() {
        return Collections.unmodifiableSet(allowedPermissions);
    }
}
