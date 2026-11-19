package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonEscapeSpectatorRulesTest {
    @Test
    void finishersReceiveSpectatorToolsWithoutLeavingPlacementRoster() {
        assertTrue(DragonEscapeSpectatorRules.shouldReceiveSpectatorTools(true, true));
        assertFalse(DragonEscapeSpectatorRules.shouldReceiveSpectatorTools(true, false));
        assertTrue(DragonEscapeSpectatorRules.shouldReceiveSpectatorTools(false, false));
    }

    @Test
    void onlyOnlineUnfinishedRacersCanBeCompassTargets() {
        assertTrue(DragonEscapeSpectatorRules.isCompassTarget(true, true, false, false));
        assertFalse(DragonEscapeSpectatorRules.isCompassTarget(false, true, false, false));
        assertFalse(DragonEscapeSpectatorRules.isCompassTarget(true, false, false, false));
        assertFalse(DragonEscapeSpectatorRules.isCompassTarget(true, true, true, false));
        assertFalse(DragonEscapeSpectatorRules.isCompassTarget(true, true, false, true));
    }
}
