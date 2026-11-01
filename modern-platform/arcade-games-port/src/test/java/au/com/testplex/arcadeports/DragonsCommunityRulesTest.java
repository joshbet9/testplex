package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DragonsCommunityRulesTest {
    @Test void allSixDragonsKitsHaveStableIdsAndAliases() {
        assertEquals("coward", ArcadePortRules.normalizeDragonsKit("Coward"));
        assertEquals("marksman", ArcadePortRules.normalizeDragonsKit("dragons-markman"));
        assertEquals("pyrotechnic", ArcadePortRules.normalizeDragonsKit("pyro"));
        assertEquals("frost", ArcadePortRules.normalizeDragonsKit("Ice Bridge"));
        assertEquals("glider", ArcadePortRules.normalizeDragonsKit("Glider"));
        assertEquals("tamer", ArcadePortRules.normalizeDragonsKit("Dragon Tamer"));
        assertNull(ArcadePortRules.normalizeDragonsKit("flare"));
    }

    @Test void requestedCooldownsAndDurationsAreExact() {
        assertEquals(25_000L, ArcadePortRules.DRAGONS_FROST_COOLDOWN_MILLIS);
        assertEquals(16, ArcadePortRules.DRAGONS_FROST_BRIDGE_LENGTH);
        assertEquals(6_000L, ArcadePortRules.DRAGONS_FROST_BRIDGE_LIFETIME_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_GLIDER_COOLDOWN_MILLIS);
        assertEquals(3_500L, ArcadePortRules.DRAGONS_GLIDER_DURATION_MILLIS);
        assertEquals(90_000L, ArcadePortRules.DRAGONS_TAMER_COOLDOWN_MILLIS);
        assertEquals(15_000L, ArcadePortRules.DRAGONS_TAMER_DURATION_MILLIS);
    }

    @Test void frostPathClampsSteepDirectionsAndOnlyUsesAirOrSnow() {
        assertEquals(0.5D, ArcadePortRules.dragonsFrostPathVerticalComponent(1.0D, 0.5D), 0.00001D);
        assertEquals(-0.5D, ArcadePortRules.dragonsFrostPathVerticalComponent(-1.0D, 0.5D), 0.00001D);
        assertEquals(0.3D, ArcadePortRules.dragonsFrostPathVerticalComponent(0.3D, 0.5D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonsFrostPathVerticalComponent(Double.NaN, 0.5D), 0.00001D);
        assertTrue(ArcadePortRules.isDragonFrostBridgeSurface(org.bukkit.Material.AIR));
        assertTrue(ArcadePortRules.isDragonFrostBridgeSurface(org.bukkit.Material.CAVE_AIR));
        assertTrue(ArcadePortRules.isDragonFrostBridgeSurface(org.bukkit.Material.SNOW));
        assertFalse(ArcadePortRules.isDragonFrostBridgeSurface(org.bukkit.Material.STONE));
        assertFalse(ArcadePortRules.isDragonFrostBridgeSurface(org.bukkit.Material.WATER));
    }

    @Test void survivalAchievementStartsAtExactlyFiveLiveMinutes() {
        assertFalse(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(299_999L));
        assertTrue(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(300_000L));
        assertTrue(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(300_001L));
    }

    @Test void threeWinStreakIncrementsAndAnyCompletedLossResets() {
        assertEquals(1, ArcadePortRules.nextDragonsWinStreak(0, true));
        assertEquals(2, ArcadePortRules.nextDragonsWinStreak(1, true));
        assertEquals(3, ArcadePortRules.nextDragonsWinStreak(2, true));
        assertEquals(0, ArcadePortRules.nextDragonsWinStreak(2, false));
        assertTrue(ArcadePortRules.shouldAwardDragonsWinStreak(3));
        assertFalse(ArcadePortRules.shouldAwardDragonsWinStreak(2));
    }

    @Test void noDamageAchievementRequiresWinningWithoutAnyDamage() {
        assertTrue(ArcadePortRules.shouldAwardDragonsNoDamageWin(true, false));
        assertFalse(ArcadePortRules.shouldAwardDragonsNoDamageWin(true, true));
        assertFalse(ArcadePortRules.shouldAwardDragonsNoDamageWin(false, false));
    }
}
