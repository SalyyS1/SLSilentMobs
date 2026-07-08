package vn.saly.silentmobs.listener;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;

/**
 * Prevents non-owner players from damaging silent mobs.
 * Handles all damage sources: direct hit, projectile, AOE skills, and
 * AreaEffectCloud.
 * This is critical for plugins like MythicMobs/MMOItems whose AOE skills
 * use server-side entity positions — even invisible mobs get hit without this.
 */
public class SilentMobDamageListener implements Listener {

    private final SLSilentMobs plugin;

    public SilentMobDamageListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();

        // Check if the damaged entity is a tracked silent mob
        SilentMob silentMob = plugin.getSilentMobManager().getSilentMob(damaged);
        if (silentMob == null)
            return;

        // Extract the actual player who caused the damage
        Player damager = extractPlayerDamager(event.getDamager());

        if (damager == null) {
            // Damage from environment, other mobs, etc. — allow by default
            return;
        }

        // Only players who can see the silent mob can damage it.
        if (!silentMob.canView(damager)) {
            event.setCancelled(true);
        }
    }

    /**
     * Extracts the Player responsible for damage from various sources.
     *
     * Handles:
     * - Direct player damage (melee, AOE sweep)
     * - Projectile damage (arrows, fireballs, tridents)
     * - AreaEffectCloud (lingering potions, some MythicMobs AOE skills)
     * - EvokerFangs and similar indirect player-initiated entities
     *
     * @param damager The entity that directly caused the damage
     * @return The Player responsible, or null if not player-caused
     */
    private Player extractPlayerDamager(Entity damager) {
        // Case 1: Direct player damage (melee, AOE sweep attacks)
        if (damager instanceof Player) {
            return (Player) damager;
        }

        // Case 2: Projectile damage (Arrow, Fireball, Trident, Snowball, etc.)
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
            return null;
        }

        // Case 3: AreaEffectCloud (lingering potions, some MythicMobs AOE)
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
            return null;
        }

        // Case 4: EvokerFangs — check owner
        if (damager instanceof EvokerFangs fangs) {
            LivingEntity owner = fangs.getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
            return null;
        }

        // Case 5: TNT — check igniter
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
            return null;
        }

        // Case 6: Firework (crossbow etc.)
        if (damager instanceof Firework firework) {
            ProjectileSource shooter = firework.getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
            return null;
        }

        // Not player-caused damage
        return null;
    }
}
