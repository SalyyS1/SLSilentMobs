package vn.saly.silentmobs.loot;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.visibility.EntityHider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages silent item drops — items only visible to the killer.
 * Per-player instanced loot inspired by Wynncraft.
 * Now reads settings from drops.yml via ConfigManager.
 */
public class SilentDropManager {

    private final SLSilentMobs plugin;

    // Item entity ID -> owner UUID (who can see/pick up this item)
    private final Map<Integer, UUID> silentItems = new ConcurrentHashMap<>();

    public SilentDropManager(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn silent drops from a mob death.
     * The original drops are cancelled; new item entities are spawned and hidden.
     */
    public void createSilentDrops(Entity deadEntity, Player killer, List<ItemStack> drops, int droppedExp) {
        EntityHider hider = plugin.getSilentMobManager().getEntityHider();

        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir())
                continue;

            // Spawn item entity at the death location
            Item itemEntity = deadEntity.getWorld().dropItemNaturally(
                    deadEntity.getLocation(), drop);

            // Track this item as silent
            silentItems.put(itemEntity.getEntityId(), killer.getUniqueId());

            // Hide from all except killer
            hider.hideFromAll(itemEntity, killer);

            // Auto-remove after timeout
            int timeout = plugin.getConfigManager().getDropsConfig().getInt("silent-drops.drop-timeout", 60);
            if (timeout > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (itemEntity.isValid()) {
                        hider.untrack(itemEntity);
                        itemEntity.remove();
                        silentItems.remove(itemEntity.getEntityId());
                    }
                }, timeout * 20L);
            }
        }

        // Give exp directly to killer instead of dropping orbs
        if (droppedExp > 0) {
            killer.giveExp(droppedExp);
        }
    }

    /**
     * Check if an item entity is a silent drop.
     */
    public boolean isSilentDrop(int entityId) {
        return silentItems.containsKey(entityId);
    }

    /**
     * Get the owner of a silent drop item.
     */
    public UUID getDropOwner(int entityId) {
        return silentItems.get(entityId);
    }

    /**
     * Remove tracking for a picked-up item.
     */
    public void removeTracking(int entityId) {
        silentItems.remove(entityId);
        Entity entity = plugin.getSilentMobManager().getEntityHider().getTrackedEntity(entityId);
        if (entity != null) {
            plugin.getSilentMobManager().getEntityHider().untrack(entity);
        }
    }

    /**
     * Clean up all tracked silent items.
     */
    public void clearAll() {
        silentItems.clear();
    }
}
