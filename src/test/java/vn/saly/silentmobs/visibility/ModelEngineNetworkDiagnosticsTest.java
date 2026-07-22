package vn.saly.silentmobs.visibility;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelEngineNetworkDiagnosticsTest {

    @Test
    void reportsPipelineHandlerOrder() throws ReflectiveOperationException {
        ModelEngineNetworkDiagnostics diagnostics = diagnostics(new TestNetworkHandler(true));

        String result = diagnostics.describe(UUID.randomUUID(), "Viewer");

        assertEquals("Viewer pipeline=protocol_lib_encoder>modelengine_unpacker>packet_handler", result);
    }

    @Test
    void reportsMissingModelEngineChannel() throws ReflectiveOperationException {
        ModelEngineNetworkDiagnostics diagnostics = diagnostics(new TestNetworkHandler(false));

        assertEquals("Viewer pipeline=not-injected", diagnostics.describe(UUID.randomUUID(), "Viewer"));
    }

    private static ModelEngineNetworkDiagnostics diagnostics(TestNetworkHandler handler)
            throws ReflectiveOperationException {
        TestApi.handler = handler;
        return new ModelEngineNetworkDiagnostics(
                TestApi.class.getMethod("getNetworkHandler"),
                NetworkHandlerContract.class.getMethod("getPipeline", UUID.class));
    }

    public static final class TestApi {
        private static TestNetworkHandler handler;

        public static TestNetworkHandler getNetworkHandler() {
            return handler;
        }
    }

    public interface NetworkHandlerContract {
        Optional<?> getPipeline(UUID playerId);
    }

    public static final class TestNetworkHandler implements NetworkHandlerContract {
        private final boolean injected;

        private TestNetworkHandler(boolean injected) {
            this.injected = injected;
        }

        @Override
        public Optional<?> getPipeline(UUID playerId) {
            return injected ? Optional.of(new TestWrapper()) : Optional.empty();
        }
    }

    public static final class TestWrapper {
        public TestChannel getChannel() {
            return new TestChannel();
        }
    }

    public static final class TestChannel {
        public TestPipeline pipeline() {
            return new TestPipeline();
        }
    }

    public static final class TestPipeline {
        public List<String> names() {
            return List.of("protocol_lib_encoder", "modelengine_unpacker", "packet_handler");
        }
    }
}
