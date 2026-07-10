package vn.saly.silentmobs.visibility;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * Resolves ModelEngine's viewer API across the 4.0 Player and 4.1 UUID
 * signatures.
 */
final class ModelEngineViewerMethods {

    private final Method addForcedHidden;
    private final Method removeForcedHidden;
    private final Method sendPairingData;
    private final boolean uuidArgument;

    private ModelEngineViewerMethods(Method addForcedHidden, Method removeForcedHidden,
            Method sendPairingData, boolean uuidArgument) {
        this.addForcedHidden = addForcedHidden;
        this.removeForcedHidden = removeForcedHidden;
        this.sendPairingData = sendPairingData;
        this.uuidArgument = uuidArgument;
    }

    static ModelEngineViewerMethods resolve(Class<?> trackedEntityType) throws NoSuchMethodException {
        Method add = findViewerMethod(trackedEntityType, "addForcedHidden");
        Method remove = findViewerMethod(trackedEntityType, "removeForcedHidden");
        if (!add.getParameterTypes()[0].equals(remove.getParameterTypes()[0])) {
            throw new NoSuchMethodException("ModelEngine forced-hidden methods use different viewer types");
        }

        Class<?> viewerType = add.getParameterTypes()[0];
        boolean usesUuid = UUID.class.equals(viewerType);
        if (!usesUuid && !Player.class.isAssignableFrom(viewerType)) {
            throw new NoSuchMethodException("Unsupported ModelEngine viewer type: " + viewerType.getName());
        }

        Method pair = trackedEntityType.getMethod("sendPairingData", Player.class);
        return new ModelEngineViewerMethods(add, remove, pair, usesUuid);
    }

    void hide(Object trackedEntity, Player viewer) throws ReflectiveOperationException {
        addForcedHidden.invoke(trackedEntity, viewerArgument(viewer));
    }

    void show(Object trackedEntity, Player viewer, boolean removeHidden, boolean sendPairing)
            throws ReflectiveOperationException {
        if (removeHidden) {
            removeForcedHidden.invoke(trackedEntity, viewerArgument(viewer));
        }
        if (sendPairing) {
            sendPairingData.invoke(trackedEntity, viewer);
        }
    }

    boolean usesUuid() {
        return uuidArgument;
    }

    private Object viewerArgument(Player viewer) {
        return uuidArgument ? viewer.getUniqueId() : viewer;
    }

    private static Method findViewerMethod(Class<?> type, String name) throws NoSuchMethodException {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name) && method.getParameterCount() == 1)
                .filter(method -> {
                    Class<?> parameter = method.getParameterTypes()[0];
                    return UUID.class.equals(parameter) || Player.class.isAssignableFrom(parameter);
                })
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(type.getName() + "#" + name));
    }
}
