package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps ModelEngine tracked-entity wrappers alive for a base entity's lifetime.
 * Some ModelEngine handlers expose wrapper-local viewer state, so recreating a
 * wrapper for every visibility update would discard the applied policy.
 */
final class ModelEngineTrackedEntityCache {

    private final Object entityHandler;
    private final Method wrapTrackedEntity;
    private final Map<UUID, Object> wrappers = new ConcurrentHashMap<>();

    ModelEngineTrackedEntityCache(Object entityHandler, Method wrapTrackedEntity) {
        this.entityHandler = entityHandler;
        this.wrapTrackedEntity = wrapTrackedEntity;
    }

    Object get(Entity entity) throws ReflectiveOperationException {
        UUID entityId = entity.getUniqueId();
        Object existing = wrappers.get(entityId);
        if (existing != null) {
            return existing;
        }

        Object created = wrapTrackedEntity.invoke(entityHandler, entity);
        if (created == null) {
            return null;
        }
        Object raced = wrappers.putIfAbsent(entityId, created);
        return raced != null ? raced : created;
    }

    void forget(Entity entity) {
        wrappers.remove(entity.getUniqueId());
    }

    void clear() {
        wrappers.clear();
    }
}
