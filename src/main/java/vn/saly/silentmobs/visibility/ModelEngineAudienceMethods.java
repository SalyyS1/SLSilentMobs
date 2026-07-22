package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Resolves ModelEngine's native per-viewer renderer predicate, which is
 * present in ModelEngine R4.0.7 through R4.1.x.
 */
final class ModelEngineAudienceMethods {

    private final Method setPlayerPredicate;
    private final Predicate<Player> defaultPredicate;

    private ModelEngineAudienceMethods(Method setPlayerPredicate, Predicate<Player> defaultPredicate) {
        this.setPlayerPredicate = setPlayerPredicate;
        this.defaultPredicate = defaultPredicate;
    }

    @SuppressWarnings("unchecked")
    static ModelEngineAudienceMethods resolve(Class<?> trackedEntityType) throws ReflectiveOperationException {
        Method setPredicate = trackedEntityType.getMethod("setPlayerPredicate", Predicate.class);
        Field defaultField = trackedEntityType.getField("DEFAULT_PREDICATE");
        Object defaultValue = defaultField.get(null);
        if (!(defaultValue instanceof Predicate<?> predicate)) {
            throw new IllegalStateException("ModelEngine DEFAULT_PREDICATE is not a player predicate");
        }
        return new ModelEngineAudienceMethods(setPredicate, (Predicate<Player>) predicate);
    }

    void set(Object trackedEntity, Set<UUID> viewers) throws ReflectiveOperationException {
        Set<UUID> allowed = Set.copyOf(viewers);
        setPlayerPredicate.invoke(trackedEntity,
                (Predicate<Player>) player -> player != null && allowed.contains(player.getUniqueId()));
    }

    void clear(Object trackedEntity) throws ReflectiveOperationException {
        setPlayerPredicate.invoke(trackedEntity, defaultPredicate);
    }
}
