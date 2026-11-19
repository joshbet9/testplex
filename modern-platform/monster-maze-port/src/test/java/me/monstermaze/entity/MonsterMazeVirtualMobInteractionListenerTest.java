package me.monstermaze.entity;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/** Ensures virtual mob clicks can only be accepted for that viewer's own IDs. */
class MonsterMazeVirtualMobInteractionListenerTest {
    @Test
    void onlyTheOwningViewerCanInteractWithTheirVirtualMob() {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Map<UUID, Set<Integer>> fakeIds = new ConcurrentHashMap<>();
        fakeIds.put(alice, ConcurrentHashMap.newKeySet());
        fakeIds.put(bob, ConcurrentHashMap.newKeySet());
        fakeIds.get(alice).add(1_900_000_000);
        fakeIds.get(bob).add(1_899_999_999);

        assertTrue(MonsterMazeVirtualMobInteractionListener.belongsToViewer(
                fakeIds, alice, 1_900_000_000));
        assertFalse(MonsterMazeVirtualMobInteractionListener.belongsToViewer(
                fakeIds, bob, 1_900_000_000));
        assertFalse(MonsterMazeVirtualMobInteractionListener.belongsToViewer(
                fakeIds, alice, 1_899_999_999));
        assertFalse(MonsterMazeVirtualMobInteractionListener.belongsToViewer(
                fakeIds, UUID.randomUUID(), 1_900_000_000));
        assertFalse(MonsterMazeVirtualMobInteractionListener.belongsToViewer(
                fakeIds, alice, 1234));
    }

    @Test
    void onceFakeMobIsDestroyedItsEntityIdCanNoLongerTriggerTheSelector() {
        UUID alice = UUID.randomUUID();
        Map<UUID, Set<Integer>> fakeIds = new ConcurrentHashMap<>();
        fakeIds.put(alice, ConcurrentHashMap.newKeySet());
        fakeIds.get(alice).add(1_900_000_000);
        assertTrue(MonsterMazeVirtualMobInteractionListener.belongsToViewer(fakeIds, alice, 1_900_000_000));
        fakeIds.get(alice).remove(1_900_000_000);
        assertFalse(MonsterMazeVirtualMobInteractionListener.belongsToViewer(fakeIds, alice, 1_900_000_000));
    }
}
