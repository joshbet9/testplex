package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadePortGameTest {
    @Test void exposesTheFourteenGamesNotCoveredByDedicatedMechanicsModules() {
        assertEquals(14, ArcadePortGame.values().length);
        Set<String> ids = new HashSet<>();
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertTrue(ids.add(game.id()), "duplicate game id: " + game.id());
            assertNotNull(ArcadePortGame.fromId(game.id()));
            assertNotNull(game.floor());
        }
        assertNull(ArcadePortGame.fromId("monster-maze"));
        assertNull(ArcadePortGame.fromId("snake"));
    }

    @Test void normalizesSpacesAndUnderscoresInTestCommands() {
        assertEquals(ArcadePortGame.ONE_IN_THE_QUIVER, ArcadePortGame.fromId("one_in_the_quiver"));
        assertEquals(ArcadePortGame.BOMB_LOBBERS, ArcadePortGame.fromId("Bomb Lobbers"));
        assertEquals(ArcadePortGame.MICRO_BATTLES, ArcadePortGame.fromId("micro-battles"));
    }
    @Test void microBattlesGlassBarrierDropsAfterTenSeconds() {
        assertFalse(ArcadePortRules.shouldRemoveMicroBarrier(9_999L));
        assertTrue(ArcadePortRules.shouldRemoveMicroBarrier(10_000L));
        assertTrue(ArcadePortRules.shouldRemoveMicroBarrier(12_000L));
    }

    @Test void microBattlesOpeningRestrictionOnlyPunishesLeavingForAnotherIsland() {
        assertTrue(ArcadePortRules.shouldPunishMicroIslandExit(3_999L, 100.0D, 25.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(4_000L, 100.0D, 25.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(3_999L, 25.0D, 100.0D));
        assertFalse(ArcadePortRules.shouldPunishMicroIslandExit(-1L, 100.0D, 25.0D));
    }

    @Test void worldDragonHazardIsNotConfusedWithPlayerFireball() {
        assertTrue(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, true));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, false, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.BOMB_LOBBERS, true, false));
    }

    @Test void oneInTheQuiverNeedsTwentyKillsToWin() {
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(0));
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(19));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(20));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(21));
    }

    @Test void turfWarsAlternatesBuildAndCombatPhases() {
        assertEquals(20_000L, ArcadePortRules.TURF_WARS_BUILD_PHASE_MILLIS);
        assertEquals(90_000L, ArcadePortRules.TURF_WARS_COMBAT_PHASE_MILLIS);
    }

    @Test void turfWarsKillsMoveFrontlineAndWinAtTheTerritoryEdge() {
        assertEquals(0, ArcadePortRules.advanceTurfFrontline(-1, 0));
        assertEquals(-2, ArcadePortRules.advanceTurfFrontline(-1, 1));
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 3));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(18, 0, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(19, 0, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(-19, 1, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(-20, 1, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(20, 2, 20));
    }

    @Test void respawningPlayersPreventPrematureRoundEnd() {
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 1));
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(1, 0));
        assertTrue(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 0));
    }

    @Test void deathTagAddsAChaserEveryThirtySeconds() {
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(0L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(29_999L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(30_000L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(59_999L));
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(-1L));
    }

    @Test void baconBrawlEscalatesKnockbackWithShorterIntervals() {
        assertEquals(50_000L, ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS);
        assertEquals(45_000L, ArcadePortRules.nextBaconKnockbackInterval(50_000L));
        assertEquals(5_000L, ArcadePortRules.nextBaconKnockbackInterval(5_000L));
        assertEquals(5_000L, ArcadePortRules.nextBaconKnockbackInterval(1_000L));
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
    }

    @Test void runnerFloorBlocksFallAfterSixHundredMilliseconds() {
        assertEquals(600L, ArcadePortRules.RUNNER_BLOCK_DECAY_MILLIS);
        assertEquals(1_200L, ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        assertFalse(ArcadePortRules.shouldDropRunnerBlock(599L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(600L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(1_200L));
    }

}
