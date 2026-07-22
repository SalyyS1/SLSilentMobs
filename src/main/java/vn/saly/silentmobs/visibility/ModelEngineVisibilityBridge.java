package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import vn.saly.silentmobs.SLSilentMobs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Reflection-based ModelEngine 4 bridge. Reflection is intentional because
 * ModelEngine changed viewer arguments from Player in 4.0 to UUID in 4.1.
 */
final class ModelEngineVisibilityBridge implements ModelVisibilityBridge, Listener {

    private static final String API_CLASS = "com.ticxo.modelengine.api.ModelEngineAPI";
    private static final String ENTITY_HANDLER_CLASS = "com.ticxo.modelengine.api.nms.entity.EntityHandler";
    private static final String TRACKED_ENTITY_CLASS = "com.ticxo.modelengine.api.nms.entity.wrapper.TrackedEntity";
    private static final String ADD_MODEL_EVENT_CLASS = "com.ticxo.modelengine.api.events.AddModelEvent";
    private static final String REMOVE_MODEL_EVENT_CLASS = "com.ticxo.modelengine.api.events.RemoveModelEvent";
    private static final String MODELED_ENTITY_CLASS = "com.ticxo.modelengine.api.model.ModeledEntity";
    private static final String BASE_ENTITY_CLASS = "com.ticxo.modelengine.api.entity.BaseEntity";

    private final SLSilentMobs plugin;
    private final Plugin modelEngine;
    private final Object entityHandler;
    private final Method wrapTrackedEntity;
    private final ModelEngineTrackedEntityCache trackedEntityCache;
    private final ModelEngineViewerMethods viewerMethods;
    private final ModelEngineAudienceMethods audienceMethods;
    private final ModelEngineEntityLifecycleMethods lifecycleMethods;
    private final ModelEngineNetworkDiagnostics networkDiagnostics;
    private final Method addModelEventGetTarget;
    private final Method removeModelEventGetTarget;
    private final Method modeledEntityGetBase;
    private final Method baseEntityGetOriginal;
    private final Consumer<Entity> resync;
    private final Consumer<Entity> modelRemoved;
    private final AtomicBoolean warned = new AtomicBoolean();
    private final Map<UUID, Set<UUID>> hiddenByThisPlugin = new ConcurrentHashMap<>();

    static ModelVisibilityBridge create(SLSilentMobs plugin, Consumer<Entity> resync,
            Consumer<Entity> modelRemoved) {
        if (!plugin.getConfigManager().getConfig().getBoolean("integrations.model-engine", true)) {
            return ModelVisibilityBridge.disabled();
        }

        Plugin modelEngine = plugin.getServer().getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null || !modelEngine.isEnabled()) {
            return ModelVisibilityBridge.disabled();
        }

        try {
            return new ModelEngineVisibilityBridge(plugin, modelEngine, resync, modelRemoved);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("ModelEngine integration disabled: " + exception.getMessage());
            return ModelVisibilityBridge.disabled();
        }
    }

    @SuppressWarnings("unchecked")
    private ModelEngineVisibilityBridge(SLSilentMobs plugin, Plugin modelEngine, Consumer<Entity> resync,
            Consumer<Entity> modelRemoved)
            throws ReflectiveOperationException {
        this.plugin = plugin;
        this.modelEngine = modelEngine;
        this.resync = resync;
        this.modelRemoved = modelRemoved;

        ClassLoader loader = modelEngine.getClass().getClassLoader();
        Class<?> apiType = Class.forName(API_CLASS, true, loader);
        Class<?> entityHandlerType = Class.forName(ENTITY_HANDLER_CLASS, true, loader);
        Class<?> trackedEntityType = Class.forName(TRACKED_ENTITY_CLASS, true, loader);
        Class<?> addModelEventType = Class.forName(ADD_MODEL_EVENT_CLASS, true, loader);
        Class<?> removeModelEventType = Class.forName(REMOVE_MODEL_EVENT_CLASS, true, loader);
        Class<?> modeledEntityType = Class.forName(MODELED_ENTITY_CLASS, true, loader);
        Class<?> baseEntityType = Class.forName(BASE_ENTITY_CLASS, true, loader);

        entityHandler = apiType.getMethod("getEntityHandler").invoke(null);
        if (entityHandler == null) {
            throw new IllegalStateException("ModelEngine EntityHandler is unavailable");
        }

        wrapTrackedEntity = entityHandlerType.getMethod("wrapTrackedEntity", Entity.class);
        trackedEntityCache = new ModelEngineTrackedEntityCache(entityHandler, wrapTrackedEntity);
        viewerMethods = ModelEngineViewerMethods.resolve(trackedEntityType);
        audienceMethods = ModelEngineAudienceMethods.resolve(trackedEntityType);
        lifecycleMethods = ModelEngineEntityLifecycleMethods.resolve(
                apiType, entityHandlerType, modeledEntityType, baseEntityType);
        networkDiagnostics = resolveNetworkDiagnostics(apiType, loader);
        addModelEventGetTarget = addModelEventType.getMethod("getTarget");
        removeModelEventGetTarget = removeModelEventType.getMethod("getTarget");
        modeledEntityGetBase = modeledEntityType.getMethod("getBase");
        baseEntityGetOriginal = baseEntityType.getMethod("getOriginal");

        registerModelEvent(addModelEventType, this::onModelAdded);
        registerModelEvent(removeModelEventType, this::onModelRemoved);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getVersion() {
        return modelEngine.getDescription().getVersion();
    }

    @Override
    public void hide(Entity entity, Player viewer) {
        hiddenByThisPlugin.computeIfAbsent(entity.getUniqueId(), key -> ConcurrentHashMap.newKeySet())
                .add(viewer.getUniqueId());
        apply(entity, viewer, true, false, false);
    }

    @Override
    public void show(Entity entity, Player viewer) {
        boolean removeHidden = unmarkHidden(entity, viewer);
        apply(entity, viewer, false, removeHidden, true);
    }

    @Override
    public void release(Entity entity, Player viewer) {
        if (unmarkHidden(entity, viewer)) {
            apply(entity, viewer, false, true, false);
        }
    }

    @Override
    public void setViewers(Entity entity, Set<UUID> viewers) {
        try {
            Object trackedEntity = trackedEntityCache.get(entity);
            if (trackedEntity != null) {
                audienceMethods.set(trackedEntity, viewers);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    @Override
    public void clearViewers(Entity entity) {
        try {
            Object trackedEntity = trackedEntityCache.get(entity);
            if (trackedEntity != null) {
                audienceMethods.clear(trackedEntity);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    @Override
    public Set<Integer> getClientEntityIds(Entity entity) {
        try {
            return ModelEngineClientEntityIds.collect(
                    lifecycleMethods.getModeledEntity(entity), lifecycleMethods.getVfx(entity));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
            return Set.of();
        }
    }

    @Override
    public List<String> getNetworkDiagnostics(Iterable<? extends Player> players) {
        if (networkDiagnostics == null) {
            return List.of("ModelEngine pipelines: unavailable");
        }
        List<String> lines = new ArrayList<>();
        for (Player player : players) {
            lines.add(networkDiagnostics.describe(player.getUniqueId(), player.getName()));
        }
        return lines;
    }

    @Override
    public void forget(Entity entity) {
        trackedEntityCache.forget(entity);
        hiddenByThisPlugin.remove(entity.getUniqueId());
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        hiddenByThisPlugin.clear();
        trackedEntityCache.clear();
    }

    private void apply(Entity entity, Player viewer, boolean hidden, boolean removeHidden, boolean sendPairing) {
        try {
            Object trackedEntity = trackedEntityCache.get(entity);
            if (trackedEntity == null) {
                return;
            }
            if (hidden) {
                viewerMethods.hide(trackedEntity, viewer);
                lifecycleMethods.despawn(entityHandler, entity, viewer);
            } else {
                viewerMethods.show(trackedEntity, viewer, removeHidden, sendPairing);
                if (sendPairing) {
                    lifecycleMethods.spawn(entityHandler, entity, viewer);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    private void onModelAdded(Event event) {
        try {
            Entity entity = getOriginalEntity(event, addModelEventGetTarget);
            if (entity == null) {
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (entity.isValid()) {
                    resync.accept(entity);
                    // Renderer initialization can finish after the add event.
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (entity.isValid()) {
                            resync.accept(entity);
                        }
                    }, 1L);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    private void onModelRemoved(Event event) {
        try {
            Entity entity = getOriginalEntity(event, removeModelEventGetTarget);
            if (entity != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> modelRemoved.accept(entity), 1L);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerModelEvent(Class<?> eventType, java.util.function.Consumer<Event> handler) {
        plugin.getServer().getPluginManager().registerEvent(
                (Class<? extends Event>) eventType.asSubclass(Event.class),
                this,
                EventPriority.MONITOR,
                (listener, event) -> handler.accept(event),
                plugin,
                true);
    }

    private Entity getOriginalEntity(Event event, Method getTarget) throws ReflectiveOperationException {
        Object modeledEntity = getTarget.invoke(event);
        Object baseEntity = modeledEntityGetBase.invoke(modeledEntity);
        Object original = baseEntityGetOriginal.invoke(baseEntity);
        return original instanceof Entity entity ? entity : null;
    }

    private void warnOnce(Throwable throwable) {
        if (warned.compareAndSet(false, true)) {
            plugin.getLogger().warning("ModelEngine visibility sync failed: " + throwable.getMessage());
        }
    }

    private ModelEngineNetworkDiagnostics resolveNetworkDiagnostics(Class<?> apiType, ClassLoader loader) {
        try {
            return ModelEngineNetworkDiagnostics.resolve(apiType, loader);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private boolean unmarkHidden(Entity entity, Player viewer) {
        Set<UUID> viewers = hiddenByThisPlugin.get(entity.getUniqueId());
        if (viewers == null || !viewers.remove(viewer.getUniqueId())) {
            return false;
        }
        if (viewers.isEmpty()) {
            hiddenByThisPlugin.remove(entity.getUniqueId(), viewers);
        }
        return true;
    }
}
