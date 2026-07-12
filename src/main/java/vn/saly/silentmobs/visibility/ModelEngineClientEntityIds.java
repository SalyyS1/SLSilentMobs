package vn.saly.silentmobs.visibility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads the client-only IDs exposed by ModelEngine render objects. The model
 * API changed its display-bone representation in 4.0, so this intentionally
 * uses the stable getter names shared by the public render contracts.
 */
final class ModelEngineClientEntityIds {

    private static final String[] ID_GETTERS = {
            "getId", "getPivotId", "getHitboxId", "getShadowId"
    };

    private ModelEngineClientEntityIds() {
    }

    static Set<Integer> collect(Object modeledEntity) throws ReflectiveOperationException {
        Set<Integer> ids = new LinkedHashSet<>();
        if (modeledEntity == null) {
            return ids;
        }

        for (Object activeModel : values(invoke(modeledEntity, "getModels"))) {
            collectRenderer(invoke(activeModel, "getModelRenderer"), ids);
            for (Object behaviorRenderer : values(invoke(activeModel, "getBehaviorRenderers"))) {
                collectRenderer(behaviorRenderer, ids);
            }
        }
        return ids;
    }

    private static void collectRenderer(Object renderer, Set<Integer> ids) throws ReflectiveOperationException {
        if (renderer == null) {
            return;
        }

        collectPart(renderer, ids);
        collectPart(invoke(renderer, "getPivot"), ids);
        collectPart(invoke(renderer, "getHitbox"), ids);
        for (Object queued : values(invoke(renderer, "getSpawnQueue"))) {
            collectPart(queued, ids);
        }
        for (Object rendered : values(invoke(renderer, "getRendered"))) {
            collectPart(rendered, ids);
        }
        for (Object queuedForDestroy : values(invoke(renderer, "getDestroyQueue"))) {
            collectPart(queuedForDestroy, ids);
        }
    }

    private static void collectPart(Object part, Set<Integer> ids) throws ReflectiveOperationException {
        if (part == null) {
            return;
        }

        for (String getter : ID_GETTERS) {
            Object value = invoke(part, getter);
            if (value instanceof Integer entityId && entityId > 0) {
                ids.add(entityId);
            }
        }
        for (Object model : values(invoke(part, "getModel"))) {
            collectPart(model, ids);
        }
        for (Object fireDisplay : values(invoke(part, "getFireDisplay"))) {
            collectPart(fireDisplay, ids);
        }
    }

    private static Collection<?> values(Object source) throws ReflectiveOperationException {
        if (source instanceof Map<?, ?> map) {
            return map.values();
        }
        if (source instanceof Collection<?> collection) {
            return collection;
        }
        Object all = invoke(source, "getAll");
        if (all instanceof Collection<?> collection) {
            return collection;
        }
        return Set.of();
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.trySetAccessible();
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException reflectiveException) {
                throw reflectiveException;
            }
            throw exception;
        }
    }
}
