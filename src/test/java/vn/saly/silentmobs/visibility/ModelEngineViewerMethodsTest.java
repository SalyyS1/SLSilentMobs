package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEngineViewerMethodsTest {

    @Test
    void supportsModelEngine40PlayerViewerMethods() throws ReflectiveOperationException {
        ModelEngineViewerMethods methods = ModelEngineViewerMethods.resolve(LegacyTrackedEntity.class);
        LegacyTrackedEntity tracked = new LegacyTrackedEntity();
        Player player = player(UUID.randomUUID());

        methods.hide(tracked, player);
        methods.show(tracked, player, true, true);

        assertFalse(methods.usesUuid());
        assertSame(player, tracked.hidden);
        assertSame(player, tracked.shown);
        assertSame(player, tracked.paired);
    }

    @Test
    void supportsModelEngine41UuidViewerMethods() throws ReflectiveOperationException {
        ModelEngineViewerMethods methods = ModelEngineViewerMethods.resolve(ModernTrackedEntity.class);
        ModernTrackedEntity tracked = new ModernTrackedEntity();
        UUID playerId = UUID.randomUUID();
        Player player = player(playerId);

        methods.hide(tracked, player);
        methods.show(tracked, player, true, true);

        assertTrue(methods.usesUuid());
        assertEquals(playerId, tracked.hidden);
        assertEquals(playerId, tracked.shown);
        assertSame(player, tracked.paired);
    }

    @Test
    void pairingDoesNotRemoveHiddenStateOwnedByAnotherPlugin() throws ReflectiveOperationException {
        ModelEngineViewerMethods methods = ModelEngineViewerMethods.resolve(LegacyTrackedEntity.class);
        LegacyTrackedEntity tracked = new LegacyTrackedEntity();
        Player player = player(UUID.randomUUID());

        methods.show(tracked, player, false, true);

        assertNull(tracked.shown);
        assertSame(player, tracked.paired);
    }

    private Player player(UUID playerId) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] { Player.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) {
                        return playerId;
                    }
                    if (method.getName().equals("toString")) {
                        return "TestPlayer[" + playerId + "]";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive())
            return null;
        if (type == boolean.class)
            return false;
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0F;
        if (type == double.class)
            return 0D;
        if (type == char.class)
            return '\0';
        return null;
    }

    public static final class LegacyTrackedEntity {
        private Player hidden;
        private Player shown;
        private Player paired;

        public void addForcedHidden(Player player) {
            hidden = player;
        }

        public void removeForcedHidden(Player player) {
            shown = player;
        }

        public void sendPairingData(Player player) {
            paired = player;
        }
    }

    public static final class ModernTrackedEntity {
        private UUID hidden;
        private UUID shown;
        private Player paired;

        public void addForcedHidden(UUID playerId) {
            hidden = playerId;
        }

        public void removeForcedHidden(UUID playerId) {
            shown = playerId;
        }

        public void sendPairingData(Player player) {
            paired = player;
        }
    }
}
