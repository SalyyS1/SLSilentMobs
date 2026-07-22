package vn.saly.silentmobs.visibility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains model children learned from MOUNT packets independently from IDs
 * exposed by a renderer API. This allows dynamically mounted parts to survive
 * the next API refresh and releases them when a mount relationship changes or
 * the private base entity is cleared.
 */
final class MountedClientEntityTracker {

    private MountedClientEntityTracker() {
    }

    static Update update(int vehicleId, int baseEntityId, Collection<Integer> passengerIds,
            Map<Integer, Set<Integer>> knownEntitiesByBase, Map<Integer, Set<Integer>> mountedEntitiesByBase,
            Map<Integer, Set<Integer>> passengersByVehicle, Map<Integer, Integer> parentByChild,
            Map<Integer, Integer> vehicleOwners) {
        boolean vehicleIsClientEntity = vehicleId != baseEntityId;
        Set<Integer> known = knownEntitiesByBase.get(baseEntityId);
        if (!vehicleIsClientEntity && (known == null || known.isEmpty())) {
            return Update.empty();
        }

        Set<Integer> accepted = new HashSet<>();
        for (int passengerId : passengerIds) {
            if (passengerId <= 0 || passengerId == baseEntityId || passengerId == vehicleId) {
                continue;
            }
            if (!vehicleIsClientEntity && !known.contains(passengerId)) {
                continue;
            }
            accepted.add(passengerId);
        }

        Set<Integer> previous = passengersByVehicle.put(vehicleId, Set.copyOf(accepted));
        vehicleOwners.put(vehicleId, baseEntityId);
        if (previous == null) {
            previous = Set.of();
        }

        Set<Integer> removed = new HashSet<>();
        for (int previousChild : previous) {
            if (!accepted.contains(previousChild) && parentByChild.remove(previousChild, vehicleId)) {
                removeMounted(baseEntityId, previousChild, mountedEntitiesByBase);
                removed.add(previousChild);
            }
        }

        Set<Integer> added = new HashSet<>();
        for (int child : accepted) {
            Integer previousParent = parentByChild.put(child, vehicleId);
            if (previousParent != null && previousParent != vehicleId) {
                Set<Integer> priorChildren = passengersByVehicle.get(previousParent);
                if (priorChildren != null) {
                    Set<Integer> replacement = new HashSet<>(priorChildren);
                    replacement.remove(child);
                    passengersByVehicle.put(previousParent, Set.copyOf(replacement));
                }
                Integer priorBase = vehicleOwners.get(previousParent);
                if (priorBase != null) {
                    removeMounted(priorBase, child, mountedEntitiesByBase);
                }
            }
            Set<Integer> mounted = mountedEntitiesByBase.computeIfAbsent(baseEntityId,
                    ignored -> ConcurrentHashMap.newKeySet());
            if (mounted.add(child)) {
                added.add(child);
            }
        }
        return new Update(Set.copyOf(added), Set.copyOf(removed));
    }

    static void clearBase(int baseEntityId, Map<Integer, Set<Integer>> mountedEntitiesByBase,
            Map<Integer, Set<Integer>> passengersByVehicle, Map<Integer, Integer> parentByChild,
            Map<Integer, Integer> vehicleOwners) {
        mountedEntitiesByBase.remove(baseEntityId);
        for (Map.Entry<Integer, Integer> entry : vehicleOwners.entrySet()) {
            if (entry.getValue() != baseEntityId || !vehicleOwners.remove(entry.getKey(), baseEntityId)) {
                continue;
            }
            Set<Integer> children = passengersByVehicle.remove(entry.getKey());
            if (children != null) {
                for (int child : children) {
                    parentByChild.remove(child, entry.getKey());
                }
            }
        }
    }

    private static void removeMounted(int baseEntityId, int childId,
            Map<Integer, Set<Integer>> mountedEntitiesByBase) {
        Set<Integer> mounted = mountedEntitiesByBase.get(baseEntityId);
        if (mounted != null) {
            mounted.remove(childId);
            if (mounted.isEmpty()) {
                mountedEntitiesByBase.remove(baseEntityId, mounted);
            }
        }
    }

    record Update(Set<Integer> added, Set<Integer> removed) {
        static Update empty() {
            return new Update(Set.of(), Set.of());
        }
    }
}
