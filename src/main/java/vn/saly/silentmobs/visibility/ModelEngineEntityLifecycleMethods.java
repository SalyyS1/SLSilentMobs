package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Resolves ModelEngine's per-player model spawn and despawn API without
 * creating a mandatory runtime dependency on ModelEngine.
 */
final class ModelEngineEntityLifecycleMethods {

    private final Method getModeledEntity;
    private final Method getBase;
    private final Method forceSpawn;
    private final Method forceDespawn;

    private ModelEngineEntityLifecycleMethods(Method getModeledEntity, Method getBase,
            Method forceSpawn, Method forceDespawn) {
        this.getModeledEntity = getModeledEntity;
        this.getBase = getBase;
        this.forceSpawn = forceSpawn;
        this.forceDespawn = forceDespawn;
    }

    static ModelEngineEntityLifecycleMethods resolve(Class<?> apiType, Class<?> entityHandlerType,
            Class<?> modeledEntityType, Class<?> baseEntityType) throws NoSuchMethodException {
        Method modeledEntity = apiType.getMethod("getModeledEntity", Entity.class);
        Method base = modeledEntityType.getMethod("getBase");
        Method spawn = entityHandlerType.getMethod("forceSpawn", baseEntityType, Player.class);
        Method despawn = entityHandlerType.getMethod("forceDespawn", baseEntityType, Player.class);
        return new ModelEngineEntityLifecycleMethods(modeledEntity, base, spawn, despawn);
    }

    void spawn(Object entityHandler, Entity entity, Player viewer) throws ReflectiveOperationException {
        apply(forceSpawn, entityHandler, entity, viewer);
    }

    void despawn(Object entityHandler, Entity entity, Player viewer) throws ReflectiveOperationException {
        apply(forceDespawn, entityHandler, entity, viewer);
    }

    private void apply(Method action, Object entityHandler, Entity entity, Player viewer)
            throws ReflectiveOperationException {
        Object modeledEntity = getModeledEntity.invoke(null, entity);
        if (modeledEntity == null) {
            return;
        }

        Object baseEntity = getBase.invoke(modeledEntity);
        if (baseEntity != null) {
            action.invoke(entityHandler, baseEntity, viewer);
        }
    }
}
