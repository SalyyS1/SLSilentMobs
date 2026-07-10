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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Listens for mob spawns inside silent regions and applies region-based
 * visibility rules.
 * Mobs spawning in a region are hidden from everyone except players with
 * allowed permissions or player UUIDs.
 */
public class RegionSilentListener implements Listener {

    private static final UUID REGION_SYSTEM_OWNER = new UUID(0L, 0L);

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

        // Hide provisionally while MythicMobs assigns its internal ID and
        // ModelEngine attaches the client-side model.
        plugin.getEntityHider().hideFromAllExcept(entity, Collections.emptySet());

        // Check on next tick (for MythicMobs compatibility)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid()) {
                plugin.getEntityHider().untrack(entity, false);
                return;
            }

            // Already tracked by manual or global system? Skip
            if (plugin.getSilentMobManager().isSilentMob(entity))
                return;

            String mobType = getMobType(entity);

            // Check if this mob should be silent in this region
            if (!region.isMobSilent(mobType)) {
                plugin.getEntityHider().untrack(entity, true);
                return;
            }

            // Restricted regions never use unauthorized players as owners.
            Player owner = findBestOwner(entity, region);
            if (owner == null && !region.hasAccessRules()) {
                plugin.getEntityHider().untrack(entity, true);
                return;
            }

            // Create silent mob with region tag
            boolean hasAccessRules = region.hasAccessRules();
            UUID ownerUuid = !hasAccessRules && owner != null ? owner.getUniqueId() : REGION_SYSTEM_OWNER;
            String ownerName = !hasAccessRules && owner != null ? owner.getName() : "region:" + region.getName();
            SilentMob silentMob = new SilentMob(ownerUuid, ownerName, mobType, entity, false);
            silentMob.setRegionName(region.getName());
            silentMob.setRegionAccessManaged(true);

            if (hasAccessRules) {
                silentMob.setOwnerVisible(false);
                for (UUID uuid : region.getAllowedPlayers()) {
                    silentMob.addViewer(uuid);
                }
                for (String permission : region.getAllowedPermissions()) {
                    silentMob.addViewPermission(permission);
                }
            }

            plugin.getSilentMobManager().addSilentMob(silentMob);
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
        boolean restricted = region.hasAccessRules();

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getWorld().equals(entity.getWorld()))
                continue;
            if (restricted && !region.canPlayerSee(online))
                continue;

            double dist = online.getLocation().distance(entity.getLocation());
            if (dist <= radius && dist < minDist) {
                minDist = dist;
                nearest = online;
            }
        }
        return nearest;
    }
}
