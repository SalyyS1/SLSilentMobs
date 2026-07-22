package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelEngineEntityLifecycleMethodsTest {

    @Test
    void despawnsAndRespawnsTheModeledEntityForOneViewer() throws ReflectiveOperationException {
        ModelEngineEntityLifecycleMethods methods = ModelEngineEntityLifecycleMethods.resolve(
                TestApi.class, TestEntityHandler.class, TestModeledEntity.class, TestBaseEntity.class);
        TestEntityHandler handler = new TestEntityHandler();

        methods.despawn(handler, null, null);
        methods.spawn(handler, null, null);

        assertEquals(1, handler.despawnCount);
        assertEquals(1, handler.spawnCount);
    }

    @Test
    void ignoresEntitiesWithoutAModel() throws ReflectiveOperationException {
        ModelEngineEntityLifecycleMethods methods = ModelEngineEntityLifecycleMethods.resolve(
                EmptyApi.class, TestEntityHandler.class, TestModeledEntity.class, TestBaseEntity.class);
        TestEntityHandler handler = new TestEntityHandler();

        methods.despawn(handler, null, null);
        methods.spawn(handler, null, null);

        assertEquals(0, handler.despawnCount);
        assertEquals(0, handler.spawnCount);
    }

    public static final class TestApi {
        public static TestModeledEntity getModeledEntity(Entity entity) {
            return new TestModeledEntity();
        }

        public static Object getVFX(Entity entity) {
            return null;
        }
    }

    public static final class EmptyApi {
        public static TestModeledEntity getModeledEntity(Entity entity) {
            return null;
        }

        public static Object getVFX(Entity entity) {
            return null;
        }
    }

    public static final class TestModeledEntity {
        public TestBaseEntity getBase() {
            return new TestBaseEntity();
        }
    }

    public static final class TestBaseEntity {
    }

    public static final class TestEntityHandler {
        private int spawnCount;
        private int despawnCount;

        public void forceSpawn(TestBaseEntity entity, Player viewer) {
            spawnCount++;
        }

        public void forceDespawn(TestBaseEntity entity, Player viewer) {
            despawnCount++;
        }
    }
}
