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
    @Test void microBattlesBorderWaitsThirtySecondsThenShrinksEveryTwentySeconds() {
        assertFalse(ArcadePortRules.shouldShrinkMicroBattlesBorder(599));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(600));
        assertFalse(ArcadePortRules.shouldShrinkMicroBattlesBorder(999));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(1000));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(1400));
    }

    @Test void microBattlesBorderStopsAtItsMinimumSize() {
        assertEquals(36.0D, ArcadePortRules.nextMicroBattlesBorderSize(42.0D));
        assertEquals(10.0D, ArcadePortRules.nextMicroBattlesBorderSize(10.0D));
        assertEquals(10.0D, ArcadePortRules.nextMicroBattlesBorderSize(8.0D));
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

}
