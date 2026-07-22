package vn.saly.silentmobs.visibility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Correlates a direct passenger of a private Bukkit entity with its later
 * spawn packet. The mount alone is insufficient evidence because players can
 * ride mobs; only a non-player spawn confirms a render part.
 */
final class DirectMountedClientPartTracker {

    private DirectMountedClientPartTracker() {
    }

    static Set<Integer> updateMount(int baseEntityId, Collection<Integer> passengerIds,
            Set<Integer> recentlySpawnedIds, Map<Integer, Set<Integer>> candidatesByBase,
            Map<Integer, Integer> candidateOwners) {
        Set<Integer> accepted = new HashSet<>();
        for (int passengerId : passengerIds) {
            if (passengerId > 0 && passengerId != baseEntityId) {
                accepted.add(passengerId);
            }
        }

        Set<Integer> previous = candidatesByBase.put(baseEntityId, Set.copyOf(accepted));
        if (previous != null) {
            for (int childId : previous) {
                if (!accepted.contains(childId)) {
                    candidateOwners.remove(childId, baseEntityId);
                }
            }
        }
        for (int childId : accepted) {
            candidateOwners.put(childId, baseEntityId);
        }

        Set<Integer> confirmed = new HashSet<>(accepted);
        confirmed.retainAll(recentlySpawnedIds);
        return Set.copyOf(confirmed);
    }

    static int confirmSpawn(int entityId, Map<Integer, Integer> candidateOwners) {
        return candidateOwners.getOrDefault(entityId, -1);
    }

    static void clearBase(int baseEntityId, Map<Integer, Set<Integer>> candidatesByBase,
            Map<Integer, Integer> candidateOwners) {
        Set<Integer> candidates = candidatesByBase.remove(baseEntityId);
        if (candidates != null) {
            for (int candidate : candidates) {
                candidateOwners.remove(candidate, baseEntityId);
            }
        }
    }
}
