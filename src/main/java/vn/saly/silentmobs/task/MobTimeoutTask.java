package vn.saly.silentmobs.task;

import org.bukkit.scheduler.BukkitRunnable;
import vn.saly.silentmobs.SLSilentMobs;

/**
 * Periodic task to clean up expired silent mobs.
 */
public class MobTimeoutTask extends BukkitRunnable {

    private final SLSilentMobs plugin;

    public MobTimeoutTask(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int timeout = plugin.getConfig().getInt("settings.default-timeout", 300);
        if (timeout <= 0)
            return;

        int cleaned = plugin.getSilentMobManager().cleanupExpired(timeout);
        if (cleaned > 0) {
            plugin.getLogger().info("Cleaned up " + cleaned + " expired silent mob(s)");
        }
    }
}
