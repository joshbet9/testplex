package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class PlayerSnapshotCompassTargetTest {
    @Test
    void restoresSavedTargetAsAnIndependentLocation() {
        Location saved = new Location(null, 4.0D, 70.0D, -3.0D);
        Location fallback = new Location(null, 0.0D, 64.0D, 0.0D);

        Location restored = PlayerSnapshot.compassTargetForRestore(saved, fallback);

        assertNotSame(saved, restored);
        assertEquals(saved.getX(), restored.getX());
        assertEquals(saved.getY(), restored.getY());
        assertEquals(saved.getZ(), restored.getZ());
    }

    @Test
    void usesLobbySpawnWhenSnapshotHadNoCompassTarget() {
        Location lobbySpawn = new Location(null, 12.0D, 100.0D, 8.0D);

        Location restored = PlayerSnapshot.compassTargetForRestore(null, lobbySpawn);

        assertNotSame(lobbySpawn, restored);
        assertEquals(lobbySpawn.getX(), restored.getX());
        assertEquals(lobbySpawn.getY(), restored.getY());
        assertEquals(lobbySpawn.getZ(), restored.getZ());
    }
}
