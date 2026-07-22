package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEngineAudienceMethodsTest {

    @Test
    void nativeAudiencePredicateAllowsOnlyConfiguredViewers() throws ReflectiveOperationException {
        ModelEngineAudienceMethods methods = ModelEngineAudienceMethods.resolve(TestTrackedEntity.class);
        TestTrackedEntity tracked = new TestTrackedEntity();
        Player allowed = player(UUID.randomUUID());
        Player denied = player(UUID.randomUUID());

        methods.set(tracked, Set.of(allowed.getUniqueId()));

        assertTrue(tracked.playerPredicate.test(allowed));
        assertFalse(tracked.playerPredicate.test(denied));

        methods.set(tracked, Set.of(allowed.getUniqueId(), denied.getUniqueId()));

        assertTrue(tracked.playerPredicate.test(denied));

        methods.set(tracked, Set.of());

        assertFalse(tracked.playerPredicate.test(allowed));

        methods.clear(tracked);

        assertTrue(tracked.playerPredicate.test(denied));
    }

    private Player player(UUID playerId) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] { Player.class },
                (proxy, method, args) -> method.getName().equals("getUniqueId") ? playerId : null);
    }

    public static final class TestTrackedEntity {
        public static final Predicate<Player> DEFAULT_PREDICATE = player -> true;
        private Predicate<Player> playerPredicate = DEFAULT_PREDICATE;

        public void setPlayerPredicate(Predicate<Player> playerPredicate) {
            this.playerPredicate = playerPredicate;
        }
    }
}
