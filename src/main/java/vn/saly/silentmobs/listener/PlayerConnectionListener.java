package vn.saly.silentmobs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;

import java.util.List;
import java.util.UUID;

/**
 * Handles player join/quit for silent mob visibility and cleanup.
 */
public class PlayerConnectionListener implements Listener {

    private final SLSilentMobs plugin;

    public PlayerConnectionListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay by 1 tick to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Hide all existing silent mobs from this new player
            plugin.getSilentMobManager().hideAllFrom(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getConfigManager().getConfig().getBoolean("settings.despawn-on-quit", true)) {
            // Check if global silent mode wants reassignment
            boolean globalEnabled = plugin.getConfigManager().getConfig().getBoolean("global-silent.enabled", false);
            boolean reassign = plugin.getConfigManager().getConfig().getBoolean("global-silent.reassign-on-owner-quit",
                    true);

            if (globalEnabled && reassign) {
                // Reassign global mobs to nearest player instead of despawning
                List<SilentMob> mobs = plugin.getSilentMobManager().getActiveMobs(uuid);
                for (SilentMob mob : mobs) {
                    if (mob.isGlobal() && mob.isAlive()) {
                        Player nearest = findNearestPlayer(mob, uuid);
                        if (nearest != null) {
                            plugin.getSilentMobManager().reassignOwner(mob, nearest);
                            continue;
                        }
                        // No player nearby — check if should despawn
                        if (plugin.getConfigManager().getConfig().getBoolean("global-silent.despawn-if-no-player",
                                true)) {
                            plugin.getSilentMobManager().removeSilentMob(mob);
                        }
                    }
                }
                // Despawn remaining non-global mobs
                List<SilentMob> remaining = plugin.getSilentMobManager().getActiveMobs(uuid);
                for (SilentMob mob : remaining) {
                    if (!mob.isGlobal()) {
                        plugin.getSilentMobManager().removeSilentMob(mob);
                    }
                }
            } else {
                // Simply despawn all
                plugin.getSilentMobManager().despawnByPlayer(uuid);
            }
        }
    }

    private Player findNearestPlayer(SilentMob mob, UUID excludeUUID) {
        if (mob.getEntity() == null || !mob.isAlive())
            return null;

        double radius = plugin.getConfigManager().getConfig().getDouble("global-silent.assign-radius", 32);
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.getUniqueId().equals(excludeUUID))
                continue;
            if (!online.getWorld().equals(mob.getEntity().getWorld()))
                continue;

            double dist = online.getLocation().distance(mob.getEntity().getLocation());
            if (dist <= radius && dist < minDist) {
                minDist = dist;
                nearest = online;
            }
        }
        return nearest;
    }
}
