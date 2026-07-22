package vn.saly.silentmobs.visibility;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DirectMountedClientPartTrackerTest {

    @Test
    void onlyConfirmsPassengersWhoseSpawnWasObserved() {
        Map<Integer, Set<Integer>> candidatesByBase = new ConcurrentHashMap<>();
        Map<Integer, Integer> candidateOwners = new ConcurrentHashMap<>();

        Set<Integer> confirmed = DirectMountedClientPartTracker.updateMount(10, Set.of(20, 21), Set.of(20),
                candidatesByBase, candidateOwners);

        assertEquals(Set.of(20), confirmed);
        assertEquals(10, DirectMountedClientPartTracker.confirmSpawn(21, candidateOwners));
        assertEquals(Set.of(20, 21), candidatesByBase.get(10));
    }

    @Test
    void releasesCandidatesWhenTheBaseMountChangesOrIsCleared() {
        Map<Integer, Set<Integer>> candidatesByBase = new ConcurrentHashMap<>();
        Map<Integer, Integer> candidateOwners = new ConcurrentHashMap<>();

        DirectMountedClientPartTracker.updateMount(10, Set.of(20), Set.of(20), candidatesByBase, candidateOwners);
        DirectMountedClientPartTracker.updateMount(10, Set.of(), Set.of(), candidatesByBase, candidateOwners);

        assertEquals(-1, DirectMountedClientPartTracker.confirmSpawn(20, candidateOwners));
        DirectMountedClientPartTracker.updateMount(10, Set.of(21), Set.of(), candidatesByBase, candidateOwners);
        DirectMountedClientPartTracker.clearBase(10, candidatesByBase, candidateOwners);
        assertFalse(candidatesByBase.containsKey(10));
        assertEquals(-1, DirectMountedClientPartTracker.confirmSpawn(21, candidateOwners));
    }
}
