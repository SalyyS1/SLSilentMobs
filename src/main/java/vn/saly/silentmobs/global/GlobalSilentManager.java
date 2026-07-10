package vn.saly.silentmobs.global;

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

/**
 * Global Silent Mode — automatically makes ALL spawned mobs silent.
 * Whitelisted mobs (bosses) are excluded and remain visible to all.
 * Inspired by Wynncraft's per-player mob system.
 */
public class GlobalSilentManager implements Listener {

    private final SLSilentMobs plugin;
    private boolean enabled;

    public GlobalSilentManager(SLSilentMobs plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("global-silent.enabled", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfigManager().getConfig().set("global-silent.enabled", enabled);
        plugin.saveConfig();
    }

    public void reload() {
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("global-silent.enabled", false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!enabled)
            return;

        LivingEntity entity = event.getEntity();

        // Ignore players
        if (entity instanceof Player)
            return;

        // Check whitelist — vanilla EntityType
        if (plugin.getGlobalSilentConfig().isWhitelistedVanilla(entity.getType())) {
            return; // Boss/special mob — visible to all
        }

        // Close the one-tick MythicMobs initialization gap before model packets can
        // be paired to unauthorized viewers.
        plugin.getEntityHider().hideFromAllExcept(entity, Collections.emptySet());

        // Check whitelist — MythicMobs (check on next tick since MythicMob may not be
        // initialized yet)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid()) {
                plugin.getEntityHider().untrack(entity, false);
                return;
            }

            // Check if it's a MythicMob and whitelisted
            if (isMythicMobWhitelisted(entity)) {
                plugin.getEntityHider().untrack(entity, true);
                return;
            }

            // Already tracked? Skip
            if (plugin.getSilentMobManager().isSilentMob(entity))
                return;

            // Find nearest player to assign this mob to
            Player nearest = findNearestPlayer(entity);
            if (nearest == null) {
                // No player nearby
                if (plugin.getConfigManager().getConfig().getBoolean("global-silent.despawn-if-no-player", true)) {
                    plugin.getEntityHider().untrack(entity, false);
                    entity.remove();
                } else {
                    plugin.getEntityHider().untrack(entity, true);
                }
                return;
            }

            // Create and register as a global silent mob
            String mobId = entity.getType().name();
            SilentMob silentMob = new SilentMob(
                    nearest.getUniqueId(),
                    nearest.getName(),
                    mobId,
                    entity,
                    true // isGlobal
            );

            plugin.getSilentMobManager().addSilentMob(silentMob);
        }, 1L);
    }

    private boolean isMythicMobWhitelisted(Entity entity) {
        try {
            var mythic = io.lumine.mythic.bukkit.MythicBukkit.inst();
            if (mythic == null)
                return false;

            var activeMob = mythic.getMobManager().getActiveMob(entity.getUniqueId());
            if (activeMob.isPresent()) {
                String mythicId = activeMob.get().getMobType();
                return plugin.getGlobalSilentConfig().isWhitelistedMythic(mythicId);
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // MythicMobs not installed
        }
        return false;
    }

    private Player findNearestPlayer(Entity entity) {
        double radius = plugin.getConfigManager().getConfig().getDouble("global-silent.assign-radius", 32);
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getWorld().equals(entity.getWorld()))
                continue;

            double dist = online.getLocation().distanceSquared(entity.getLocation());
            if (dist <= radius * radius && dist < minDist) {
                minDist = dist;
                nearest = online;
            }
        }
        return nearest;
    }
}
