package vn.saly.silentmobs.region;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;

import java.util.List;

/**
 * Listens for mob spawns inside silent regions and applies region-based
 * visibility rules.
 * Mobs spawning in a region are hidden from everyone except players with
 * allowed permissions or player UUIDs.
 */
public class RegionSilentListener implements Listener {

    private final SLSilentMobs plugin;

    public RegionSilentListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player)
            return;

        // Check if mob spawned inside any region
        List<SilentRegion> regions = plugin.getRegionManager().getRegionsAt(entity.getLocation());
        if (regions.isEmpty())
            return;

        // Use first matching region
        SilentRegion region = regions.get(0);

        // Check on next tick (for MythicMobs compatibility)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid())
                return;

            // Already tracked by manual or global system? Skip
            if (plugin.getSilentMobManager().isSilentMob(entity))
                return;

            String mobType = getMobType(entity);

            // Check if this mob should be silent in this region
            if (!region.isMobSilent(mobType))
                return;

            // Find a player to "own" this mob — use nearest allowed player, or just nearest
            Player owner = findBestOwner(entity, region);
            if (owner == null)
                return;

            // Create silent mob with region tag
            SilentMob silentMob = new SilentMob(
                    owner.getUniqueId(),
                    owner.getName(),
                    mobType,
                    entity,
                    true // isGlobal (region-managed)
            );
            silentMob.setRegionName(region.getName());

            plugin.getSilentMobManager().addSilentMob(silentMob);

            // Additionally reveal to all allowed players in the region
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (online.equals(owner))
                    continue;
                if (region.canPlayerSee(online)) {
                    plugin.getEntityHider().addViewer(entity.getEntityId(), online.getUniqueId());
                }
            }
        }, 1L);
    }

    private String getMobType(Entity entity) {
        try {
            var mythic = io.lumine.mythic.bukkit.MythicBukkit.inst();
            if (mythic != null) {
                var activeMob = mythic.getMobManager().getActiveMob(entity.getUniqueId());
                if (activeMob.isPresent()) {
                    return activeMob.get().getMobType();
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return entity.getType().name();
    }

    private Player findBestOwner(Entity entity, SilentRegion region) {
        double radius = 64;
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getWorld().equals(entity.getWorld()))
                continue;
            double dist = online.getLocation().distance(entity.getLocation());
            if (dist <= radius && dist < minDist) {
                // Prefer allowed players
                if (region.canPlayerSee(online)) {
                    return online;
                }
                if (nearest == null || dist < minDist) {
                    minDist = dist;
                    nearest = online;
                }
            }
        }
        return nearest;
    }
}
