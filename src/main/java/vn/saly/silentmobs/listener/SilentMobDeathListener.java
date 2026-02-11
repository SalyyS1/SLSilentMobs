package vn.saly.silentmobs.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.config.ConfigManager;
import vn.saly.silentmobs.model.SilentMob;

/**
 * Handles silent mob death — cleanup tracking and notify owner.
 * Now uses ConfigManager for messages and settings.
 */
public class SilentMobDeathListener implements Listener {

    private final SLSilentMobs plugin;

    public SilentMobDeathListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        SilentMob silentMob = plugin.getSilentMobManager().getSilentMob(entity);
        if (silentMob == null)
            return;

        // Notify owner if enabled
        if (plugin.getConfigManager().getConfig().getBoolean("settings.death-notification", true)) {
            Player owner = plugin.getServer().getPlayer(silentMob.getOwnerUUID());
            if (owner != null && owner.isOnline()) {
                ConfigManager cm = plugin.getConfigManager();
                String msg = cm.getMessage("mob-death").replace("{mob}", silentMob.getMobId());
                String prefix = cm.getMessage("prefix");
                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            }
        }

        // Cleanup from manager (untrack from EntityHider too)
        plugin.getSilentMobManager().removeSilentMob(silentMob);

        // Silent drops are handled by SilentDropListener
    }
}
