package vn.saly.silentmobs.visibility;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MountedClientEntityTrackerTest {

    @Test
    void mapsDynamicPassengersOfAKnownClientEntity() {
        Map<Integer, Set<Integer>> entitiesByBase = new ConcurrentHashMap<>();
        entitiesByBase.put(10, ConcurrentHashMap.newKeySet());
        entitiesByBase.get(10).add(20);
        Map<Integer, Set<Integer>> mountedByBase = new ConcurrentHashMap<>();
        Map<Integer, Set<Integer>> childrenByVehicle = new ConcurrentHashMap<>();
        Map<Integer, Integer> parentByChild = new ConcurrentHashMap<>();
        Map<Integer, Integer> vehicleOwners = new ConcurrentHashMap<>();

        MountedClientEntityTracker.Update update = MountedClientEntityTracker.update(
                20, 10, Set.of(21, 22), entitiesByBase, mountedByBase,
                childrenByVehicle, parentByChild, vehicleOwners);

        assertEquals(Set.of(21, 22), update.added());
        assertEquals(Integer.valueOf(20), parentByChild.get(21));
        assertEquals(Set.of(21, 22), mountedByBase.get(10));
    }

    @Test
    void ignoresUnknownDirectPassengersOfTheBukkitBase() {
        Map<Integer, Set<Integer>> entitiesByBase = new ConcurrentHashMap<>();
        entitiesByBase.put(10, ConcurrentHashMap.newKeySet());
        entitiesByBase.get(10).add(20);
        Map<Integer, Set<Integer>> mountedByBase = new ConcurrentHashMap<>();
        Map<Integer, Set<Integer>> childrenByVehicle = new ConcurrentHashMap<>();
        Map<Integer, Integer> parentByChild = new ConcurrentHashMap<>();
        Map<Integer, Integer> vehicleOwners = new ConcurrentHashMap<>();

        MountedClientEntityTracker.Update update = MountedClientEntityTracker.update(
                10, 10, Set.of(21), entitiesByBase, mountedByBase,
                childrenByVehicle, parentByChild, vehicleOwners);

        assertEquals(Set.of(), update.added());
        assertFalse(parentByChild.containsKey(21));
    }

    @Test
    void releasesDynamicChildrenWhenTheMountIsUpdated() {
        Map<Integer, Set<Integer>> entitiesByBase = new ConcurrentHashMap<>();
        entitiesByBase.put(10, ConcurrentHashMap.newKeySet());
        entitiesByBase.get(10).add(20);
        Map<Integer, Set<Integer>> mountedByBase = new ConcurrentHashMap<>();
        Map<Integer, Set<Integer>> childrenByVehicle = new ConcurrentHashMap<>();
        Map<Integer, Integer> parentByChild = new ConcurrentHashMap<>();
        Map<Integer, Integer> vehicleOwners = new ConcurrentHashMap<>();

        MountedClientEntityTracker.update(20, 10, Set.of(21), entitiesByBase, mountedByBase,
                childrenByVehicle, parentByChild, vehicleOwners);
        MountedClientEntityTracker.Update update = MountedClientEntityTracker.update(
                20, 10, Set.of(), entitiesByBase, mountedByBase,
                childrenByVehicle, parentByChild, vehicleOwners);

        assertEquals(Set.of(21), update.removed());
        assertFalse(mountedByBase.containsKey(10));
        assertFalse(parentByChild.containsKey(21));
    }

    @Test
    void clearsAllMountStateWhenThePrivateBaseIsRemoved() {
        Map<Integer, Set<Integer>> entitiesByBase = new ConcurrentHashMap<>();
        entitiesByBase.put(10, ConcurrentHashMap.newKeySet());
        entitiesByBase.get(10).add(20);
        Map<Integer, Set<Integer>> mountedByBase = new ConcurrentHashMap<>();
        Map<Integer, Set<Integer>> childrenByVehicle = new ConcurrentHashMap<>();
        Map<Integer, Integer> parentByChild = new ConcurrentHashMap<>();
        Map<Integer, Integer> vehicleOwners = new ConcurrentHashMap<>();

        MountedClientEntityTracker.update(20, 10, Set.of(21), entitiesByBase, mountedByBase,
                childrenByVehicle, parentByChild, vehicleOwners);
        MountedClientEntityTracker.clearBase(10, mountedByBase, childrenByVehicle,
                parentByChild, vehicleOwners);

        assertFalse(mountedByBase.containsKey(10));
        assertFalse(childrenByVehicle.containsKey(20));
        assertFalse(parentByChild.containsKey(21));
        assertFalse(vehicleOwners.containsKey(20));
    }

}
