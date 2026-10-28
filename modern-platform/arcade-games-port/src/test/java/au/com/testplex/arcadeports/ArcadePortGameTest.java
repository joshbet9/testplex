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

    @Test void baconBrawlEscalatesKnockbackWithShorterFiniteIntervals() {
        assertEquals(50_000L, ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS);
        assertEquals(45_000L, ArcadePortRules.nextBaconKnockbackInterval(50_000L));
        assertEquals(5_000L, ArcadePortRules.nextBaconKnockbackInterval(10_000L));
        assertEquals(0L, ArcadePortRules.nextBaconKnockbackInterval(5_000L));
        assertEquals(0L, ArcadePortRules.nextBaconKnockbackInterval(0L));
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
    }

    @Test void runnerFloorBlocksFallAfterSixHundredMilliseconds() {
        assertEquals(600L, ArcadePortRules.RUNNER_BLOCK_DECAY_MILLIS);
        assertEquals(1_200L, ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        assertFalse(ArcadePortRules.shouldDropRunnerBlock(599L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(600L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(1_200L));
    }

    @Test void dragonEscapeLaunchesChaseHazardsOnSchedule() {
        assertEquals(6_000L, ArcadePortRules.DRAGON_ESCAPE_HAZARD_INTERVAL_MILLIS);
        assertFalse(ArcadePortRules.shouldLaunchDragonEscapeHazard(5_999L, 6_000L));
        assertTrue(ArcadePortRules.shouldLaunchDragonEscapeHazard(6_000L, 6_000L));
        assertTrue(ArcadePortRules.shouldLaunchDragonEscapeHazard(12_000L, 6_000L));
    }

    @Test void spleefProjectileRadiusScalesWithArrowSpeed() {
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(false, 8.0D));
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(true, 0.0D));
        assertEquals(2, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 4.0D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 100.0D));
    }

    @Test void evolutionStagesAdvanceOnKillsAndAreRetainedOnDeath() {
        assertEquals(1, ArcadePortRules.nextEvolutionLevel(0, true));
        assertEquals(6, ArcadePortRules.nextEvolutionLevel(5, true));
        assertEquals(6, ArcadePortRules.nextEvolutionLevel(6, true));
        assertEquals(2, ArcadePortRules.nextEvolutionLevel(2, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(0, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(-1, false));
        assertFalse(ArcadePortRules.isEvolutionWinnerLevel(5));
        assertTrue(ArcadePortRules.isEvolutionWinnerLevel(6));
    }

    @Test void sheepQuestLeaderRequiresMorePenSheep() {
        assertEquals(0, ArcadePortRules.sheepQuestLeadingTeam(3, 2));
        assertEquals(1, ArcadePortRules.sheepQuestLeadingTeam(1, 4));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(2, 2));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(0, 0));
    }

    @Test void paintballPlayerIsOutOnlyAfterAllFourArmourPiecesArePainted() {
        int remaining = 4;
        remaining = ArcadePortRules.paintballPiecesAfterHit(remaining, 1);
        assertEquals(3, remaining);
        assertFalse(ArcadePortRules.isPaintballPlayerOut(remaining));
        assertEquals(2, ArcadePortRules.paintballPiecesAfterHit(remaining, 1));
        assertEquals(1, ArcadePortRules.paintballPiecesAfterHit(2, 1));
        assertEquals(0, ArcadePortRules.paintballPiecesAfterHit(1, 1));
        assertTrue(ArcadePortRules.isPaintballPlayerOut(0));
        assertEquals(0, ArcadePortRules.paintballPiecesAfterHit(0, 3));
        assertEquals(4, ArcadePortRules.paintballPiecesAfterHit(4, 0));
    }

    @Test void gladiatorsProgressThroughFourArenaTiers() {
        assertEquals(0, ArcadePortRules.gladiatorsTierForRound(-1));
        assertEquals(0, ArcadePortRules.gladiatorsTierForRound(0));
        assertEquals(1, ArcadePortRules.gladiatorsTierForRound(1));
        assertEquals(2, ArcadePortRules.gladiatorsTierForRound(2));
        assertEquals(3, ArcadePortRules.gladiatorsTierForRound(3));
        assertEquals(3, ArcadePortRules.gladiatorsTierForRound(4));
    }

    @Test void gladiatorsPairPlayersAndOnlyAdvanceAfterEveryMatchResolves() {
        assertEquals(0, ArcadePortRules.gladiatorsPairCount(0));
        assertEquals(1, ArcadePortRules.gladiatorsPairCount(1));
        assertEquals(2, ArcadePortRules.gladiatorsPairCount(3));
        assertEquals(8, ArcadePortRules.gladiatorsPairCount(16));
        assertFalse(ArcadePortRules.allGladiatorPairsResolved(4, 3));
        assertTrue(ArcadePortRules.allGladiatorPairsResolved(4, 4));
        assertTrue(ArcadePortRules.allGladiatorPairsResolved(4, 5));
        assertFalse(ArcadePortRules.allGladiatorPairsResolved(0, 0));
    }

    @Test void dragonContactHitsRespectPerPlayerCooldown() {
        assertTrue(ArcadePortRules.shouldApplyDragonContactHit(10_000L, 0L));
        assertFalse(ArcadePortRules.shouldApplyDragonContactHit(10_000L, 11_500L));
        assertTrue(ArcadePortRules.shouldApplyDragonContactHit(11_500L, 11_500L));
    }

    @Test void dragonEscapeHasARealFinishLine() {
        assertEquals(77.0D, ArcadePortRules.DRAGON_ESCAPE_FINISH_Z);
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(76.99D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(77.0D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(80.0D));
    }

    @Test void deathTagChasersConvertRunnersWithoutTagTransfer() {
        assertTrue(ArcadePortRules.shouldSpreadDeathTag(true, false));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(true, true));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, true));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, false));
    }

    @Test void deathTagEndsWhenOneRunnerRemainsExceptInSmallTestLobbies() {
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(1, 8));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 8));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(2, 8));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(1, 2));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 2));
    }

    @Test void bombLobbersDirectHitUsesSweptTntCollisionRadius() {
        assertTrue(ArcadePortRules.isBombDirectHit(0.0D));
        assertTrue(ArcadePortRules.isBombDirectHit(0.75D));
        assertTrue(ArcadePortRules.isBombDirectHit(1.0D));
        assertFalse(ArcadePortRules.isBombDirectHit(1.01D));
        assertFalse(ArcadePortRules.isBombDirectHit(-0.01D));
    }

    @Test void baconBrawlStartsWithExtraKnockbackForTwoPlayers() {
        assertEquals(1.8D, ArcadePortRules.initialBaconKnockback(2), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(1), 0.00001D);
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(3), 0.00001D);
    }

    @Test void sheepQuestUsesFourInitialSheepAndThreeSheepStacks() {
        assertEquals(4, ArcadePortRules.SHEEP_QUEST_INITIAL_SHEEP);
        assertEquals(3, ArcadePortRules.SHEEP_QUEST_MAX_CARRY);
        assertEquals(20_000L, ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS);
        assertTrue(ArcadePortRules.canCarryAnotherSheep(0));
        assertTrue(ArcadePortRules.canCarryAnotherSheep(2));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(3));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(-1));
    }

}
