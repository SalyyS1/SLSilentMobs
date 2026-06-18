package vn.saly.silentmobs.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;

/**
 * Prevents silent mobs from targeting players who aren't their owner.
 */
public class SilentMobTargetListener implements Listener {

    private final SLSilentMobs plugin;

    public SilentMobTargetListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.getConfig().getBoolean("settings.mob-target-owner-only", true))
            return;

        Entity entity = event.getEntity();
        SilentMob silentMob = plugin.getSilentMobManager().getSilentMob(entity);
        if (silentMob == null)
            return;

        LivingEntity target = event.getTarget();

        // If target is null (lost target), allow
        if (target == null)
            return;

        // Allow targeting the owner
        if (target.getUniqueId().equals(silentMob.getOwnerUUID()))
            return;

        // Allow targeting the owner's party members (MMOCore)
        if (target instanceof org.bukkit.entity.Player targetPlayer
                && plugin.getPartyHook().isInSameParty(silentMob.getOwnerUUID(), targetPlayer))
            return;

        event.setCancelled(true);
    }
}
