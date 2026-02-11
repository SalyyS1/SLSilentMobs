package vn.saly.silentmobs.loot;

import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import vn.saly.silentmobs.SLSilentMobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener for silent item drops.
 * Intercepts mob death drops and item pickups for instanced loot.
 * Now reads settings from drops.yml via ConfigManager.
 */
public class SilentDropListener implements Listener {

    private final SLSilentMobs plugin;

    public SilentDropListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().getDropsConfig().getBoolean("silent-drops.enabled", true))
            return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null)
            return;

        boolean applyGlobally = plugin.getConfigManager().getDropsConfig().getBoolean("silent-drops.apply-globally",
                false);
        boolean applyToSilentOnly = plugin.getConfigManager().getDropsConfig()
                .getBoolean("silent-drops.apply-to-silent-mobs-only", true);

        boolean shouldApply = false;

        if (applyGlobally) {
            // Apply to ALL mobs (except players)
            shouldApply = !(entity instanceof Player);
        } else if (applyToSilentOnly) {
            // Only apply to tracked silent mobs
            shouldApply = plugin.getSilentMobManager().isSilentMob(entity);
        }

        if (!shouldApply)
            return;

        // Capture and clear original drops
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        int exp = event.getDroppedExp();

        event.getDrops().clear();
        event.setDroppedExp(0);

        // Create silent drops (only visible to killer)
        plugin.getSilentDropManager().createSilentDrops(entity, killer, drops, exp);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        Item item = event.getItem();
        int entityId = item.getEntityId();

        if (!plugin.getSilentDropManager().isSilentDrop(entityId))
            return;

        UUID owner = plugin.getSilentDropManager().getDropOwner(entityId);

        // Only the owner can pick up this item
        if (!player.getUniqueId().equals(owner)) {
            event.setCancelled(true);
            return;
        }

        // Owner is picking up — remove tracking
        plugin.getSilentDropManager().removeTracking(entityId);
    }
}
