package vn.saly.silentmobs.visibility;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads ModelEngine's per-player Netty pipeline without depending on Netty or
 * a particular Minecraft mapping. This is diagnostic-only: it identifies
 * whether a ModelEngine unpacker can bypass ProtocolLib's outbound listener.
 */
final class ModelEngineNetworkDiagnostics {

    private final Method getNetworkHandler;
    private final Method getPipeline;

    ModelEngineNetworkDiagnostics(Method getNetworkHandler, Method getPipeline) {
        this.getNetworkHandler = getNetworkHandler;
        this.getPipeline = getPipeline;
    }

    static ModelEngineNetworkDiagnostics resolve(Class<?> apiType, ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> networkHandlerType = Class.forName("com.ticxo.modelengine.api.nms.network.NetworkHandler", true, loader);
        return new ModelEngineNetworkDiagnostics(
                apiType.getMethod("getNetworkHandler"),
                networkHandlerType.getMethod("getPipeline", UUID.class));
    }

    String describe(UUID playerId, String playerName) {
        try {
            Object handler = getNetworkHandler.invoke(null);
            if (handler == null) {
                return playerName + " pipeline=unavailable";
            }
            Object result = getPipeline.invoke(handler, playerId);
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return playerName + " pipeline=not-injected";
            }

            Object pipelineOrWrapper = optional.get();
            Object pipeline = unwrapPipeline(pipelineOrWrapper);
            Object names = invokeNoArg(pipeline, "names");
            if (!(names instanceof List<?> handlers)) {
                return playerName + " pipeline=unreadable";
            }
            return playerName + " pipeline=" + String.join(">", handlers.stream()
                    .map(String::valueOf)
                    .toList());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return playerName + " pipeline=error:" + exception.getClass().getSimpleName();
        }
    }

    private Object unwrapPipeline(Object pipelineOrWrapper) throws ReflectiveOperationException {
        Object channel;
        try {
            channel = invokeNoArg(pipelineOrWrapper, "getChannel");
        } catch (NoSuchMethodException ignored) {
            return pipelineOrWrapper;
        }
        return channel == null ? pipelineOrWrapper : invokeNoArg(channel, "pipeline");
    }

    private Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = target.getClass().getMethod(methodName);
        method.trySetAccessible();
        return method.invoke(target);
    }
}
