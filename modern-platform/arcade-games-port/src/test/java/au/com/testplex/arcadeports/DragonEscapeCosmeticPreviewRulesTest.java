package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonEscapeCosmeticPreviewRulesTest {
    @Test
    void finishBeaconPreviewUsesTheRealStainedGlassColourIncludingSilver() {
        assertEquals(Material.WHITE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("white"));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("light-blue"));
        assertEquals(Material.LIGHT_GRAY_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("silver"));
        assertEquals(Material.RED_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("red"));
        assertEquals(Material.WHITE_STAINED_GLASS, ArcadePortRules.dragonEscapeBeaconGlassMaterial("unknown"));
    }

    @Test
    void winnerBeaconFollowsEachNewRaceLeaderWithoutRepeatingForTheSameLeader() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertFalse(ArcadePortRules.shouldUpdateDragonEscapeBeaconLeader(first, first));
        assertTrue(ArcadePortRules.shouldUpdateDragonEscapeBeaconLeader(first, second));
        assertTrue(ArcadePortRules.shouldUpdateDragonEscapeBeaconLeader(null, first));
        assertFalse(ArcadePortRules.shouldUpdateDragonEscapeBeaconLeader(first, null));
    }
}
