package vn.saly.silentmobs.util;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Shared vanilla/MythicMobs spawn helper.
 */
public final class MobSpawner {

    private MobSpawner() {
    }

    public static Entity spawn(String mobId, Location loc, int level) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }

        try {
            MythicBukkit mythic = MythicBukkit.inst();
            if (mythic != null && mythic.getMobManager().getMythicMob(mobId).isPresent()) {
                AbstractLocation mythicLoc = new AbstractLocation(
                        io.lumine.mythic.bukkit.BukkitAdapter.adapt(loc.getWorld()),
                        loc.getX(), loc.getY(), loc.getZ());
                ActiveMob activeMob = mythic.getMobManager().spawnMob(mobId, mythicLoc, level);
                return activeMob != null ? activeMob.getEntity().getBukkitEntity() : null;
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }

        try {
            EntityType type = EntityType.valueOf(mobId.toUpperCase());
            return loc.getWorld().spawnEntity(loc, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
