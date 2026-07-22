package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Optional bridge for plugins that render extra client-side entities.
 */
interface ModelVisibilityBridge extends AutoCloseable {

    boolean isAvailable();

    String getVersion();

    void hide(Entity entity, Player viewer);

    void show(Entity entity, Player viewer);

    void release(Entity entity, Player viewer);

    /**
     * Applies the authoritative viewer policy to the renderer itself. This
     * stops a renderer from producing client-only entities for unauthorized
     * players instead of relying only on post-render packet cancellation.
     */
    default void setViewers(Entity entity, Set<UUID> viewers) {
    }

    /**
     * Restores the renderer's normal audience policy when an entity is no
     * longer managed by SLSilentMobs.
     */
    default void clearViewers(Entity entity) {
    }

    /**
     * Drops renderer-specific state after the base entity is no longer
     * managed. Implementations must not retain dead entity wrappers.
     */
    default void forget(Entity entity) {
    }

    /**
     * Returns client-only entity IDs currently used by the model for this base
     * entity. These IDs must follow the same visibility policy as the base.
     */
    default Set<Integer> getClientEntityIds(Entity entity) {
        return Collections.emptySet();
    }

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
