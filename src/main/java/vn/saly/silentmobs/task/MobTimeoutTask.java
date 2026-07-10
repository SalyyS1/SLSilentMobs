package vn.saly.silentmobs.task;

import org.bukkit.scheduler.BukkitRunnable;
import vn.saly.silentmobs.SLSilentMobs;

/**
 * Periodic maintenance for cleanup, viewer refresh, and orphan reassignment.
 */
public class MobTimeoutTask extends BukkitRunnable {

    private final SLSilentMobs plugin;

    public MobTimeoutTask(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int timeout = plugin.getConfig().getInt("settings.default-timeout", 300);
        int cleaned = plugin.getSilentMobManager().runMaintenance(timeout);
        if (cleaned > 0) {
            plugin.getLogger().info("Cleaned up " + cleaned + " expired silent mob(s)");
        }
    }
}
