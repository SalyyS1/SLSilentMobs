package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelEngineTrackedEntityCacheTest {

    @Test
    void retainsOneWrapperUntilTheEntityIsForgotten() throws ReflectiveOperationException {
        TestHandler handler = new TestHandler();
        Method wrap = TestHandler.class.getMethod("wrapTrackedEntity", Entity.class);
        ModelEngineTrackedEntityCache cache = new ModelEngineTrackedEntityCache(handler, wrap);
        Entity entity = entity(UUID.randomUUID());

        Object first = cache.get(entity);
        Object second = cache.get(entity);

        assertSame(first, second);
        assertEquals(1, handler.wrapCalls);

        cache.forget(entity);
        cache.get(entity);

        assertEquals(2, handler.wrapCalls);
    }

    private static Entity entity(UUID id) {
        return (Entity) Proxy.newProxyInstance(
                ModelEngineTrackedEntityCacheTest.class.getClassLoader(),
                new Class<?>[] { Entity.class },
                (proxy, method, args) -> method.getName().equals("getUniqueId") ? id : null);
    }

    public static final class TestHandler {
        private int wrapCalls;

        public Object wrapTrackedEntity(Entity entity) {
            wrapCalls++;
            return new Object();
        }
    }
}
