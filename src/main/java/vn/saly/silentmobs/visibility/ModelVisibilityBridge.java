package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Optional bridge for plugins that render extra client-side entities.
 */
interface ModelVisibilityBridge extends AutoCloseable {

    boolean isAvailable();

    String getVersion();

    void hide(Entity entity, Player viewer);

    void show(Entity entity, Player viewer);

    void release(Entity entity, Player viewer);

    @Override
    void close();

    static ModelVisibilityBridge disabled() {
        return DisabledBridge.INSTANCE;
    }

    enum DisabledBridge implements ModelVisibilityBridge {
        INSTANCE;

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String getVersion() {
            return "not installed";
        }

        @Override
        public void hide(Entity entity, Player viewer) {
        }

        @Override
        public void show(Entity entity, Player viewer) {
        }

        @Override
        public void release(Entity entity, Player viewer) {
        }

        @Override
        public void close() {
        }
    }
}
