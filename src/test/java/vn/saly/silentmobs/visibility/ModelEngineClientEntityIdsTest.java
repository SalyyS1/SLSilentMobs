package vn.saly.silentmobs.visibility;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelEngineClientEntityIdsTest {

    @Test
    void collectsDisplayAndBehaviorClientEntityIds() throws ReflectiveOperationException {
        Set<Integer> ids = ModelEngineClientEntityIds.collect(new FakeModeledEntity(), new FakeVfx());

        assertEquals(Set.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22), ids);
    }

    @Test
    void collectsVfxClientEntityIdsWithoutAnActiveModel() throws ReflectiveOperationException {
        Set<Integer> ids = ModelEngineClientEntityIds.collect(null, new FakeVfx());

        assertEquals(Set.of(21, 22), ids);
    }

    public static final class FakeModeledEntity {
        public Map<String, FakeActiveModel> getModels() {
            return Map.of("main", new FakeActiveModel());
        }
    }

    public static final class FakeActiveModel {
        public FakeDisplayRenderer getModelRenderer() {
            return new FakeDisplayRenderer();
        }

        public Map<String, FakeBehaviorRenderer> getBehaviorRenderers() {
            return Map.of("name", new FakeBehaviorRenderer());
        }
    }

    public static final class FakeDisplayRenderer {
        public FakePivot getPivot() {
            return new FakePivot(11);
        }

        public FakeHitbox getHitbox() {
            return new FakeHitbox();
        }

        public Map<String, FakeDisplayBone> getRendered() {
            return Map.of("body", new FakeDisplayBone());
        }

        public Map<String, FakePart> getSpawnQueue() {
            return Map.of("new-body", new FakePart(19));
        }

        public Map<String, FakePart> getDestroyQueue() {
            return Map.of("old-body", new FakePart(20));
        }
    }

    public static final class FakeBehaviorRenderer {
        public Map<String, FakePart> getRendered() {
            return Map.of("label", new FakePart(18));
        }
    }

    public static final class FakeVfx {
        public FakeVfxRenderer getRenderer() {
            return new FakeVfxRenderer();
        }
    }

    public static final class FakeVfxRenderer {
        public FakeVfxModel getVFXModel() {
            return new FakeVfxModel();
        }
    }

    public static final class FakeVfxModel {
        public int getPivotId() {
            return 21;
        }

        public int getModelId() {
            return 22;
        }
    }

    public static final class FakePivot {
        private final int id;

        private FakePivot(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static final class FakeHitbox {
        public int getPivotId() {
            return 12;
        }

        public int getHitboxId() {
            return 13;
        }

        public int getShadowId() {
            return 14;
        }

        public FakePool getFireDisplay() {
            return new FakePool();
        }
    }

    public static final class FakePool {
        public List<FakePart> getAll() {
            return List.of(new FakePart(15));
        }
    }

    public static final class FakeDisplayBone {
        public Map<Integer, FakePart> getModel() {
            Map<Integer, FakePart> models = new LinkedHashMap<>();
            models.put(16, new FakePart(16));
            models.put(17, new FakePart(17));
            return models;
        }
    }

    public static final class FakePart {
        private final int id;

        private FakePart(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
