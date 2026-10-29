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

    @Test void microBattlesBarrierHeightTracksSourceIslandY() {
        assertEquals(65, ArcadePortRules.microBattlesBarrierTopY(64));
        assertEquals(73, ArcadePortRules.microBattlesBarrierTopY(72));
        assertEquals(41, ArcadePortRules.microBattlesBarrierTopY(40));
    }

    @Test void microBattlesBorderChecksRespectItsConfiguredCentre() {
        assertFalse(ArcadePortRules.isOutsideWorldBorder(110.0D, -40.0D, 100.0D, -40.0D, 40.0D));
        assertFalse(ArcadePortRules.isOutsideWorldBorder(120.0D, -60.0D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(120.01D, -40.0D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(100.0D, -60.01D, 100.0D, -40.0D, 40.0D));
        assertTrue(ArcadePortRules.isOutsideWorldBorder(Double.NaN, 0.0D, 0.0D, 0.0D, 100.0D));
    }

    @Test void microBattlesVerticalMapScanUsesExclusiveMaximum() {
        assertFalse(ArcadePortRules.isMicroCrumbleScanComplete(95, 96));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(96, 96));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(97, 96));
        assertTrue(ArcadePortRules.isMicroCrumbleScanComplete(0, 0));
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

    @Test void quiverLeaperOnlyRearmsOnLanding() {
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(false, true, 0.0D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(true, true, 0.4D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, true, 0.0D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, true, -0.1D));
        assertTrue(ArcadePortRules.shouldRearmQuiverLeap(true, false, 0.4D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(false, false, 0.0D));
        assertFalse(ArcadePortRules.shouldRearmQuiverLeap(true, true, Double.NaN));
    }

    @Test void quiverOnlyRegrantsArrowAfterElimination() {
        assertFalse(ArcadePortRules.quiverRegrantsArrowAfterHit(false));
        assertTrue(ArcadePortRules.quiverRegrantsArrowAfterHit(true));
    }

    @Test void quiverEnchanterChainsTwiceAndFallsBackWhenNoEnemyIsNearby() {
        assertEquals(2, ArcadePortRules.QUIVER_MAX_ARROW_BOUNCES);
        assertTrue(ArcadePortRules.shouldQuiverArrowBounce(2));
        assertTrue(ArcadePortRules.shouldQuiverArrowBounce(1));
        assertFalse(ArcadePortRules.shouldQuiverArrowBounce(0));
        assertEquals(1, ArcadePortRules.quiverArrowBouncesAfterBounce(2));
        assertEquals(0, ArcadePortRules.quiverArrowBouncesAfterBounce(1));
        assertEquals(0, ArcadePortRules.quiverArrowBouncesAfterBounce(0));

        assertTrue(ArcadePortRules.isQuiverReboundTargetInRange(0.0D));
        assertTrue(ArcadePortRules.isQuiverReboundTargetInRange(64.0D));
        assertFalse(ArcadePortRules.isQuiverReboundTargetInRange(64.01D));
        assertFalse(ArcadePortRules.isQuiverReboundTargetInRange(Double.NaN));
        assertEquals(64.0D, ArcadePortRules.QUIVER_REBOUND_MAX_DISTANCE_SQUARED, 0.00001D);
    }

    @Test void quiverGroundPoundUsesSourceRadialImpulse() {
        assertEquals(1.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(0.0D), 0.00001D);
        assertEquals(0.5D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(4.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(8.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(10.0D), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.quiverGroundPoundHorizontalMultiplier(-1.0D), 0.00001D);
    }

    @Test void oneInTheQuiverNeedsTwentyKillsToWin() {
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(0));
        assertFalse(ArcadePortRules.isQuiverWinnerScoreReached(19));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(20));
        assertTrue(ArcadePortRules.isQuiverWinnerScoreReached(21));
        assertEquals(20L, ArcadePortRules.QUIVER_RESPAWN_DELAY_TICKS);
    }

    @Test void turfWarsUsesFortySecondOpeningBuildThenTwentySecondBuildAndNinetySecondCombat() {
        assertEquals(40_000L, ArcadePortRules.TURF_WARS_INITIAL_BUILD_PHASE_MILLIS);
        assertEquals(20_000L, ArcadePortRules.TURF_WARS_BUILD_PHASE_MILLIS);
        assertEquals(90_000L, ArcadePortRules.TURF_WARS_COMBAT_PHASE_MILLIS);
    }

    @Test void turfWarsKillsMoveFrontlineAndWinAtTheTerritoryEdge() {
        assertEquals(0, ArcadePortRules.advanceTurfFrontline(-1, 0));
        assertEquals(-2, ArcadePortRules.advanceTurfFrontline(-1, 1));
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 3));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(18, 0, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(19, 0, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(-20, 1, 20));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(-21, 1, 20));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(20, 2, 20));
    }

    @Test void paintballEndsAtTenEliminationPoints() {
        assertFalse(ArcadePortRules.isPaintballWinnerScoreReached(0));
        assertFalse(ArcadePortRules.isPaintballWinnerScoreReached(9));
        assertTrue(ArcadePortRules.isPaintballWinnerScoreReached(10));
        assertTrue(ArcadePortRules.isPaintballWinnerScoreReached(11));
        assertEquals(10, ArcadePortRules.PAINTBALL_WINNER_SCORE);
    }

    @Test void paintballKitHitsPaintTheCorrectNumberOfArmourPieces() {
        assertEquals(3, ArcadePortRules.paintballPaintAmount("rifle", false, 0.0D));
        assertEquals(3, ArcadePortRules.paintballPaintAmount("rifle", true, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("shotgun", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("machine-gun", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("sniper", false, 0.999D));
        assertEquals(1, ArcadePortRules.paintballPaintAmount("sniper", true, 0.0D));
        assertEquals(2, ArcadePortRules.paintballPaintAmount("sniper", true, 0.3D));
        assertEquals(3, ArcadePortRules.paintballPaintAmount("sniper", true, 0.6D));
        assertEquals(4, ArcadePortRules.paintballPaintAmount("sniper", true, 0.999D));
        assertEquals(1, ArcadePortRules.paintballSniperPaintDamage(-1.0D));
    }

    @Test void respawningPlayersPreventPrematureRoundEnd() {
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 1));
        assertFalse(ArcadePortRules.shouldFinishWhenNobodyIsAlive(1, 0));
        assertTrue(ArcadePortRules.shouldFinishWhenNobodyIsAlive(0, 0));
    }

    @Test void deathTagAwardsRoundToChasersOnlyWhenNoRunnersRemain() {
        assertTrue(ArcadePortRules.deathTagChasersWinRound(0));
        assertTrue(ArcadePortRules.deathTagChasersWinRound(-1));
        assertFalse(ArcadePortRules.deathTagChasersWinRound(1));
        assertFalse(ArcadePortRules.deathTagChasersWinRound(3));
    }

    @Test void deathTagTimeoutPrefersAnySurvivingRunner() {
        assertTrue(ArcadePortRules.shouldDeathTagRunnerWinTimeout(1));
        assertTrue(ArcadePortRules.shouldDeathTagRunnerWinTimeout(4));
        assertFalse(ArcadePortRules.shouldDeathTagRunnerWinTimeout(0));
        assertFalse(ArcadePortRules.shouldDeathTagRunnerWinTimeout(-1));
    }

    @Test void deathTagAddsAChaserEveryThirtySeconds() {
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(0L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(29_999L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(30_000L));
        assertEquals(2, ArcadePortRules.requiredDeathTagChasers(59_999L));
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(-1L));
    }

    @Test void sourceWorldResetOnlyAcceptsReservedWorldNames() {
        assertTrue(SourceMapCatalog.isReservedSourceWorldName("arcadeport_source_bacon_brawl"));
        assertTrue(SourceMapCatalog.isReservedSourceWorldName("arcadeport_source_micro_battles"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("world"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("../world"));
        assertFalse(SourceMapCatalog.isReservedSourceWorldName("arcadeport_proto_evolution"));
    }

    @Test void dragonEscapeMapMarkerQueriesAreNormalised() {
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGON_ESCAPE));
        assertEquals("maps/dragon-escape.zip", SourceMapCatalog.resourceFor(ArcadePortGame.DRAGON_ESCAPE));
    }

    @Test void oneInTheQuiverArchiveIsNotAccidentallyTheDeathTagMap() throws Exception {
        try (var quiver = getClass().getClassLoader().getResourceAsStream("maps/one-in-the-quiver.zip");
             var deathTag = getClass().getClassLoader().getResourceAsStream("maps/death-tag.zip")) {
            assertNotNull(quiver, "One in the Quiver needs its own source map archive");
            assertNotNull(deathTag, "Death Tag source map archive should be present");
            assertFalse(java.util.Arrays.equals(quiver.readAllBytes(), deathTag.readAllBytes()),
                    "One in the Quiver must not silently reuse the Death Tag village archive");
        }
    }

    @Test void microBattlesGeneratedBarrierFallbackIsLimitedToPrototypeMaps() {
        assertTrue(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(false, 0));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(true, 0));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(true, 12));
        assertFalse(ArcadePortRules.shouldRemoveGeneratedMicroBarrier(false, 4));
    }

    @Test void dragonsFlightTargetsStayInsideSourceMapBounds() {
        assertEquals(101.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(50.0D, 100, 140), 0.00001D);
        assertEquals(138.999D, ArcadePortRules.clampDragonSkyTargetCoordinate(200.0D, 100, 140), 0.00001D);
        assertEquals(120.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(Double.NaN, 100, 140), 0.00001D);
        assertEquals(100.5D, ArcadePortRules.clampDragonSkyTargetCoordinate(500.0D, 100, 101), 0.00001D);
        assertEquals(-19.0D, ArcadePortRules.clampDragonSkyTargetCoordinate(-500.0D, -20, 20), 0.00001D);
    }

    @Test void sourceMapSpawnsAreInterleavedAcrossConfiguredTeams() {
        assertEquals(java.util.List.of(0, 1, 2, 0, 1, 0),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(3, 2, 1)));
        assertEquals(java.util.List.of(0, 1, 2, 3, 0, 1, 2, 3),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(2, 2, 2, 2)));
        assertEquals(java.util.List.of(),
                SourceMapCatalog.MapData.roundRobinGroupOrder(java.util.List.of(0, -1, 0)));
        assertEquals(java.util.List.of(),
                SourceMapCatalog.MapData.roundRobinGroupOrder(null));
        assertEquals(0, SourceMapCatalog.MapData.teamIndexForGroup("Red", 0, 4));
        assertEquals(1, SourceMapCatalog.MapData.teamIndexForGroup("Blue", 1, 4));
        assertEquals(1, SourceMapCatalog.MapData.teamIndexForGroup("Aqua", 1, 4));
        assertEquals(2, SourceMapCatalog.MapData.teamIndexForGroup("Green", 2, 4));
        assertEquals(3, SourceMapCatalog.MapData.teamIndexForGroup("Yellow", 3, 4));
        assertEquals(3, SourceMapCatalog.MapData.teamIndexForGroup("team-four", 3, 4));
        assertEquals(0, SourceMapCatalog.MapData.teamIndexForGroup("Green", 2, 2));
    }

    @Test void dragonEscapeSourceRouteKeepsAuthoredOrderThroughSwitchbacks() {
        org.bukkit.Location start = new org.bukkit.Location(null, 0.0D, 64.0D, 0.0D);
        java.util.List<org.bukkit.Location> source = java.util.List.of(
                new org.bukkit.Location(null, 0.0D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 0.1D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 64.0D, 64.0D, 5.0D),
                new org.bukkit.Location(null, 64.0D, 64.0D, 1.0D));
        java.util.List<org.bukkit.Location> ordered =
                SourceMapCatalog.MapData.orderedRouteMarkers(start, source, 0.75D);
        assertEquals(4, ordered.size());
        assertEquals(0.0D, ordered.get(0).getZ(), 0.00001D); // RED start is waypoint zero.
        assertEquals(5.0D, ordered.get(1).getZ(), 0.00001D);
        assertEquals(64.0D, ordered.get(2).getX(), 0.00001D);
        assertEquals(1.0D, ordered.get(3).getZ(), 0.00001D);
        assertEquals(1, SourceMapCatalog.MapData.orderedRouteMarkers(start, null, 0.75D).size());
    }

    @Test void originalMapCatalogEmbedsThirteenSourceWorldsAndLeavesEvolutionExplicitlyUnmapped() {
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.BACON_BRAWL));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.BOMB_LOBBERS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DEATH_TAG));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGON_ESCAPE));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAGONS));
        assertFalse(SourceMapCatalog.hasMap(ArcadePortGame.EVOLUTION));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.GLADIATORS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MICRO_BATTLES));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.ONE_IN_THE_QUIVER));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.PAINTBALL));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.RUNNER));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SHEEP_QUEST));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SPLEEF));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.TURF_WARS));
        assertNull(SourceMapCatalog.resourceFor(ArcadePortGame.EVOLUTION));
    }

    @Test void oneInTheQuiverSourceKitsAndMeleeDamageAreExplicit() {
        assertEquals("leaper", ArcadePortRules.normalizeQuiverKit("Leaper"));
        assertEquals("brawler", ArcadePortRules.normalizeQuiverKit("quiver-brawler"));
        assertEquals("enchanter", ArcadePortRules.normalizeQuiverKit("Enchanter"));
        assertEquals("slam-shot", ArcadePortRules.normalizeQuiverKit("Slam Shot"));
        assertEquals("ninja", ArcadePortRules.normalizeQuiverKit("Ninja"));
        assertNull(ArcadePortRules.normalizeQuiverKit("unknown"));
        assertEquals(5.0D, ArcadePortRules.quiverMeleeDamage("leaper"), 0.00001D);
        assertEquals(7.0D, ArcadePortRules.quiverMeleeDamage("brawler"), 0.00001D);
        assertEquals(5.0D, ArcadePortRules.quiverMeleeDamage("enchanter"), 0.00001D);
        assertEquals(5.5D, ArcadePortRules.quiverMeleeDamage("slam-shot"), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.quiverMeleeDamage("ninja"), 0.00001D);
        assertTrue(ArcadePortRules.shouldRemoveQuiverInvisibilityOnHit(true, true));
        assertFalse(ArcadePortRules.shouldRemoveQuiverInvisibilityOnHit(false, true));
        assertEquals(10_000L, ArcadePortRules.QUIVER_GROUND_POUND_COOLDOWN_MILLIS);
        assertEquals(1_200L, ArcadePortRules.QUIVER_NINJA_VANISH_MILLIS);
    }

    @Test void dragonsSourceKitsSpawnLoopSparklerAndFallRulesAreExplicit() {
        assertEquals("coward", ArcadePortRules.normalizeDragonsKit("Coward"));
        assertEquals("marksman", ArcadePortRules.normalizeDragonsKit("Marksman"));
        assertEquals("pyrotechnic", ArcadePortRules.normalizeDragonsKit("Pyro"));
        assertNull(ArcadePortRules.normalizeDragonsKit("unknown"));
        assertTrue(ArcadePortRules.shouldSpawnTestDragon(0, 1_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(5, 1_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(2, 999L, 1_000L));
        assertTrue(ArcadePortRules.shouldReplenishDragonSparkler(25_000L, 25_000L, 1));
        assertFalse(ArcadePortRules.shouldReplenishDragonSparkler(25_000L, 25_000L, 2));
        assertTrue(ArcadePortRules.shouldExpireDragonSparkler(101, 64.0D, false));
        assertTrue(ArcadePortRules.shouldExpireDragonSparkler(20, 3.9D, false));
        assertTrue(ArcadePortRules.shouldExpireDragonSparkler(20, 64.0D, true));
        assertEquals(3.0D, ArcadePortRules.dragonFallDamageAfterReduction(4.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonFallDamageAfterReduction(0.5D), 0.00001D);
        assertEquals(500L, ArcadePortRules.DRAGONS_SPAWN_INTERVAL_MILLIS);
        assertEquals(6, ArcadePortRules.DRAGONS_BARRAGE_MAX_ARROWS);
    }

    @Test void microBattlesSourceKitsFletcherAndIronSkinAreExplicit() {
        assertEquals("archer", ArcadePortRules.normalizeMicroBattlesKit("Archer"));
        assertEquals("worker", ArcadePortRules.normalizeMicroBattlesKit("Worker"));
        assertEquals("fighter", ArcadePortRules.normalizeMicroBattlesKit("Fighter"));
        assertNull(ArcadePortRules.normalizeMicroBattlesKit("unknown"));
        assertEquals(20_000L, ArcadePortRules.MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS);
        assertEquals(3, ArcadePortRules.MICRO_BATTLES_FLETCHER_MAX_ARROWS);
        assertEquals(3, ArcadePortRules.MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK);
        assertEquals(2, ArcadePortRules.MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS);
        assertTrue(ArcadePortRules.shouldReplenishMicroArrow(20_000L, 20_000L, 2, 3));
        assertFalse(ArcadePortRules.shouldReplenishMicroArrow(19_999L, 20_000L, 2, 3));
        assertFalse(ArcadePortRules.shouldReplenishMicroArrow(20_000L, 20_000L, 3, 3));
        assertEquals(2.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(4.0D, true), 0.00001D);
        assertEquals(3.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(6.0D, true), 0.00001D);
        assertEquals(1.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(1.0D, true), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.microBattlesDamageAfterIronSkin(4.0D, false), 0.00001D);
    }

    @Test void spleefSourceKitSelectionHungerAndPerkTimingAreExplicit() {
        assertEquals("snowballer", ArcadePortRules.normalizeSpleefKit("Snowballer"));
        assertEquals("brawler", ArcadePortRules.normalizeSpleefKit("brawler"));
        assertEquals("archer", ArcadePortRules.normalizeSpleefKit("Archer"));
        assertNull(ArcadePortRules.normalizeSpleefKit("unknown"));
        assertEquals(4_000L, ArcadePortRules.SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.SPLEEF_HUNGER_DRAIN_PER_INTERVAL);
        assertEquals(50L, ArcadePortRules.SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS);
        assertEquals(2_000L, ArcadePortRules.SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.SPLEEF_ARCHER_MAX_ARROWS);
        assertEquals(16, ArcadePortRules.SPLEEF_SNOWBALLER_MAX_SNOWBALLS);
        assertEquals(6_000L, ArcadePortRules.SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS);
        assertEquals(250L, ArcadePortRules.SPLEEF_SMASHER_COOLDOWN_MILLIS);
        assertEquals(4, ArcadePortRules.nextSpleefSlowDamageData(5));
        assertEquals(4, ArcadePortRules.nextSpleefSlowDamageData(13));
        assertEquals(14, ArcadePortRules.nextSpleefSlowDamageData(4));
        assertEquals(-1, ArcadePortRules.nextSpleefSlowDamageData(14));
    }

    @Test void spleefSlowDamageMatchesLegacyColourDataTransitions() {
        assertEquals(org.bukkit.Material.YELLOW_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.LIME_WOOL));
        assertEquals(org.bukkit.Material.YELLOW_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.GREEN_WOOL));
        assertEquals(org.bukkit.Material.RED_WOOL,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.YELLOW_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.RED_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.ORANGE_WOOL));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.BLUE_WOOL));
        assertEquals(org.bukkit.Material.YELLOW_TERRACOTTA,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.LIME_TERRACOTTA));
        assertEquals(org.bukkit.Material.RED_TERRACOTTA,
                ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.YELLOW_TERRACOTTA));
        assertNull(ArcadePortRules.nextSpleefSlowDamageMaterial(org.bukkit.Material.ORANGE_TERRACOTTA));
    }

    @Test void spleefOnlyAcceptsSourceFloorMaterialsAsBreakable() {
        assertTrue(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.SNOW_BLOCK));
        assertTrue(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.LIGHT_BLUE_WOOL));
        assertTrue(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.RED_TERRACOTTA));
        assertTrue(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.STONE));
        assertTrue(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.CRACKED_STONE_BRICKS));
        assertFalse(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.AIR));
        assertFalse(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.BEDROCK));
        assertFalse(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.GLASS));
        assertFalse(ArcadePortRules.isSpleefFloorMaterial(org.bukkit.Material.OBSIDIAN));
        assertFalse(ArcadePortRules.isSpleefFloorMaterial(null));
    }

    @Test void bombLobbersKitsPitchLevelsCraftmanAndBombPrimingMatchSourceTiming() {
        assertEquals("jumper", ArcadePortRules.normalizeBombLobbersKit("Jumper"));
        assertEquals("armorer", ArcadePortRules.normalizeBombLobbersKit("armourer"));
        assertEquals("pitcher", ArcadePortRules.normalizeBombLobbersKit("Pitcher"));
        assertEquals("waller", ArcadePortRules.normalizeBombLobbersKit("Waller"));
        assertNull(ArcadePortRules.normalizeBombLobbersKit("unknown"));
        assertEquals(1.75D, ArcadePortRules.bombLobbersPitchVelocity(1), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.bombLobbersPitchVelocity(2), 0.00001D);
        assertEquals(2.25D, ArcadePortRules.bombLobbersPitchVelocity(3), 0.00001D);
        assertEquals(8_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(0L));
        assertEquals(8_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(20_000L));
        assertEquals(7_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(20_001L));
        assertEquals(6_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(40_001L));
        assertEquals(5_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(60_001L));
        assertEquals(4_000L, ArcadePortRules.bombLobbersCraftmanIntervalMillis(620_001L));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(2_999L, true));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(7_999L, false));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(8_000L, false));
    }

    @Test void baconBrawlSourceArchiveContainsTheRequiredGreenSpawnMarkers() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("maps/bacon-brawl.zip")) {
            assertNotNull(input, "Bacon Brawl must use its embedded Reborn source map");
            String worldConfig = null;
            try (var zip = new java.util.zip.ZipInputStream(input)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().endsWith("WorldConfig.dat")) {
                        worldConfig = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                    zip.closeEntry();
                }
            }
            assertNotNull(worldConfig, "Source archive must include WorldConfig.dat");
            String teamName = "";
            int greenSpawnCount = 0;
            int mapMinY = Integer.MIN_VALUE;
            boolean greenStartAtY13 = false;
            for (String line : worldConfig.split("\\R")) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if ("MIN_Y".equalsIgnoreCase(key)) {
                    mapMinY = Integer.parseInt(value);
                } else if ("TEAM_NAME".equalsIgnoreCase(key)) {
                    teamName = value;
                } else if ("TEAM_SPAWNS".equalsIgnoreCase(key) && "Green".equalsIgnoreCase(teamName)
                        && !value.isBlank()) {
                    for (String coordinate : value.split(":")) {
                        if (coordinate.isBlank()) continue;
                        greenSpawnCount++;
                        String[] xyz = coordinate.split(",");
                        if (xyz.length >= 2 && Double.parseDouble(xyz[1].trim()) == 13.0D) {
                            greenStartAtY13 = true;
                        }
                    }
                }
            }
            assertTrue(greenSpawnCount >= 16,
                    "Source Bacon Brawl WorldConfig.dat requires at least 16 Green TEAM_SPAWNS; got "
                            + greenSpawnCount);
            assertEquals(4, mapMinY, "Mighty Lava Arena's source volume starts at Y=4");
            assertTrue(greenStartAtY13, "Mighty Lava Arena includes valid Green starts at Y=13");
            assertEquals(3.0D, ArcadePortRules.sourceMapVoidThresholdY(true, mapMinY), 0.00001D,
                    "the arena fall guard must not eliminate players at the source spawn height");
        }
    }

    @Test void baconBrawlExposesTheThreeSourceKitsAndDoubleHungerRules() {
        assertEquals("pig", ArcadePortRules.normalizeBaconBrawlKit("Pig"));
        assertEquals("pig", ArcadePortRules.normalizeBaconBrawlKit("sheep-pig"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("mama"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("Mama Pig"));
        assertEquals("mama-piggles", ArcadePortRules.normalizeBaconBrawlKit("Mama Piggles"));
        assertEquals("el-muchacho-pigo", ArcadePortRules.normalizeBaconBrawlKit("El Muchacho Pigo"));
        assertEquals("el-muchacho-pigo", ArcadePortRules.normalizeBaconBrawlKit("body slam"));
        assertNull(ArcadePortRules.normalizeBaconBrawlKit("chris-p-bacon"));
        assertNull(ArcadePortRules.normalizeBaconBrawlKit("unknown"));
        assertEquals(4_000L, ArcadePortRules.BACON_SLOW_UPDATE_INTERVAL_MILLIS);
        assertEquals(120_000L, ArcadePortRules.BACON_BRAWL_ROUND_MILLIS);
        assertEquals(20L, ArcadePortRules.BACON_BRAWL_RESPAWN_DELAY_TICKS);
        assertEquals(4_000L, ArcadePortRules.BACON_HUNGER_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.BACON_HUNGER_DRAIN_PER_INTERVAL,
                "the selected balance change doubles Bacon Brawl hunger drain");
        assertEquals(5.0F, ArcadePortRules.BACON_SATURATION_ON_FOOD_CHANGE, 0.00001F);
        assertEquals(4.0D, ArcadePortRules.BACON_STARVATION_DAMAGE, 0.00001D);
        assertFalse(ArcadePortRules.BACON_BRAWL_HITS_DAMAGE_HEALTH,
                "source hits heal back the damage and rely on knockback for arena knock-outs");
        assertEquals(2.0D, ArcadePortRules.BACON_BODY_SLAM_EXTRA_KNOCKBACK, 0.00001D);
        assertEquals(8_000L, ArcadePortRules.BACON_BODY_SLAM_COOLDOWN_MILLIS);
        assertEquals(3_000L, ArcadePortRules.BACON_BLAST_COOLDOWN_MILLIS);
        assertEquals(10_000L, ArcadePortRules.BACON_CLOAK_COOLDOWN_MILLIS);
        assertEquals(5_000L, ArcadePortRules.BACON_CLOAK_DURATION_MILLIS);
        assertEquals(1.8D, ArcadePortRules.initialBaconKnockback(2), 0.00001D,
                "two-player rounds start with stronger knockback");
        assertEquals(1.5D, ArcadePortRules.initialBaconKnockback(3), 0.00001D);
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
    }

    @Test void baconBrawlAbilitiesRequireTheSourceIronAxeAndRightClick() {
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "el-muchacho-pigo"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "mama-piggles"));
        assertTrue(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "pig"),
                "Pig cloak is the source perk that also fires on an axe right-click to a usable block");
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(false,
                org.bukkit.Material.IRON_AXE, false, "pig"));
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.STICK, false, "pig"));
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, true, "el-muchacho-pigo"));
        assertFalse(ArcadePortRules.shouldActivateBaconBrawlAbility(true,
                org.bukkit.Material.IRON_AXE, false, "chris-p-bacon"));
    }

    @Test void baconPigBackstabAddsTwoHundredAndFiftyPercentKnockback() {
        assertEquals(1.0D, ArcadePortRules.baconBrawlKnockbackMultiplier(false), 0.00001D);
        assertEquals(3.5D, ArcadePortRules.BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER, 0.00001D);
        assertEquals(5.25D, 1.5D * ArcadePortRules.baconBrawlKnockbackMultiplier(true), 0.00001D);
    }

    @Test void sheepPigBackstabBonusOnlyAppliesFromBehind() {
        assertTrue(ArcadePortRules.isBaconBrawlBackstab(0.0D, 1.0D, 0.0D, -1.0D));
        assertTrue(ArcadePortRules.isBaconBrawlBackstab(1.0D, 0.0D, -1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(0.0D, 1.0D, 0.0D, 1.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(1.0D, 0.0D, 1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(0.0D, 0.0D, 1.0D, 0.0D));
        assertFalse(ArcadePortRules.isBaconBrawlBackstab(Double.NaN, 1.0D, 0.0D, -1.0D));
    }

    @Test void baconBrawlAchievementThresholdsUseSelectedValues() {
        assertEquals(15, ArcadePortRules.BACON_BRAWL_KING_OF_BACON_WINS);
        assertFalse(ArcadePortRules.shouldAwardBaconKingOfBacon(14));
        assertTrue(ArcadePortRules.shouldAwardBaconKingOfBacon(15));
        assertTrue(ArcadePortRules.shouldAwardBaconKingOfBacon(30));
        assertEquals(6, ArcadePortRules.BACON_BRAWL_PIGS_IN_BLANKETS_KILLS);
        assertFalse(ArcadePortRules.shouldAwardBaconPigsInBlankets(5));
        assertTrue(ArcadePortRules.shouldAwardBaconPigsInBlankets(6));
        assertTrue(ArcadePortRules.shouldAwardBaconPigsInBlankets(7));
    }

    @Test void baconBlastNeverBreaksSourceArenaBlocks() {
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.STONE, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.OAK_PLANKS, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.BEDROCK, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.WATER, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(null, 0.0D));
        assertFalse(ArcadePortRules.baconBlastShouldBreakBlock(org.bukkit.Material.STONE, Double.NaN));
    }

    @Test void baconBrawlRecognisesSourceArenaHazardKillReasons() {
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("fell from the arena"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("was lost to the water"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("LAVA"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("hot_floor"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("Magma Block"));
        assertTrue(ArcadePortRules.isBaconArenaKnockoutReason("void"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason("starved"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason("melee"));
        assertFalse(ArcadePortRules.isBaconArenaKnockoutReason(null));
    }

    @Test void baconBrawlCreditsRecentKnockbackForFallAndWaterKillsOnly() {
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L, true));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 10_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_001L, 5_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 9_000L, false),
                "starvation and Body Slam recoil do not receive opponent kill credit");
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(9_000L, 10_000L, true));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 0L, true));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("lava")));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("hot_floor")));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 5_001L,
                ArcadePortRules.isBaconArenaKnockoutReason("starved")));
    }

    @Test void baconBrawlAwardsTheUniqueHighestKillCountAtTimeout() {
        assertEquals(1, ArcadePortRules.baconBrawlUniqueLeaderIndex(java.util.List.of(2, 5, 3)));
        assertEquals(0, ArcadePortRules.baconBrawlUniqueLeaderIndex(java.util.List.of(7, 2, 1)));
        assertEquals(-1, ArcadePortRules.baconBrawlUniqueLeaderIndex(java.util.List.of(4, 4, 1)));
        assertEquals(-1, ArcadePortRules.baconBrawlUniqueLeaderIndex(java.util.List.of(0, 0)));
        assertEquals(-1, ArcadePortRules.baconBrawlUniqueLeaderIndex(java.util.List.of()));
        assertEquals(-1, ArcadePortRules.baconBrawlUniqueLeaderIndex(null));
    }

    @Test void baconBrawlEscalatesKnockbackTwiceAndSpeedsUpInATwoPlayerEndgame() {
        assertEquals(50_000L, ArcadePortRules.BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS);
        assertEquals(50_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 8));
        assertEquals(50_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 3));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 2));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 1));
        assertEquals(0L, ArcadePortRules.nextBaconKnockbackInterval(2, 8));
        assertEquals(2, ArcadePortRules.BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS);
        assertEquals(1.8D, ArcadePortRules.nextBaconKnockback(1.5D), 0.00001D);
    }

    @Test void runnerSourceKitsHaveDistinctCooldownsAndFrostBallRules() {
        assertEquals("leaper", ArcadePortRules.normalizeRunnerKit("Leaper"));
        assertEquals("archer", ArcadePortRules.normalizeRunnerKit("runner-archer"));
        assertEquals("frosty", ArcadePortRules.normalizeRunnerKit("Frosty"));
        assertNull(ArcadePortRules.normalizeRunnerKit("unknown"));
        assertEquals(8_000L, ArcadePortRules.RUNNER_LEAP_COOLDOWN_MILLIS);
        assertEquals(6_000L, ArcadePortRules.RUNNER_QUICKSHOT_COOLDOWN_MILLIS);
        assertEquals(500L, ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
        assertEquals(16, ArcadePortRules.RUNNER_MAX_FROSTBALLS);
        assertEquals(40, ArcadePortRules.RUNNER_FROSTBALL_SLOW_TICKS);
        assertEquals(2, ArcadePortRules.RUNNER_FROSTBALL_SLOW_AMPLIFIER);
    }

    @Test void runnerFloorBlocksFallAfterSixHundredMilliseconds() {
        assertEquals(600L, ArcadePortRules.RUNNER_BLOCK_DECAY_MILLIS);
        assertEquals(1_200L, ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        assertFalse(ArcadePortRules.shouldDropRunnerBlock(599L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(600L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(1_200L));
    }

    @Test void runnerOnlyDestabilisesExposedSolidNonLiquidBlocksOnce() {
        assertTrue(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(false, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, true, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, true, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, false, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, true));
    }

    @Test void runnerFallingBlocksCannotHitPlayersStandingAboveThem() {
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(64.0D, 64.0D));
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(64.5D, 64.0D));
        assertFalse(ArcadePortRules.shouldRunnerFallingBlockHit(64.5001D, 64.0D));
        assertFalse(ArcadePortRules.shouldRunnerFallingBlockHit(70.0D, 64.0D));
    }

    @Test void sourceWaterDamageMatchesTheOriginalArcadeGameSettings() {
        assertEquals(5, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BOMB_LOBBERS, "Mighty Lava Arena"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGONS, "Dragons"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.RUNNER, "Runner"));
        assertEquals(4, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.SPLEEF, "Spleef"));
        assertEquals(2, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Pirate"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.DRAGON_ESCAPE, "Hell"));
        assertEquals(0, ArcadePortRules.sourceWorldWaterDamage(ArcadePortGame.BACON_BRAWL, "Arena"));
    }

    @Test void dragonEscapeHellMapStartsAtNight() {
        assertEquals(16_000L, ArcadePortRules.dragonEscapeWorldTime("Dragon Escape Hell"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime("Dragon Escape Pirate"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime("Standard"));
        assertEquals(12_000L, ArcadePortRules.dragonEscapeWorldTime(null));
    }

    @Test void sourceWorldTimeHonoursPerGameOverridesAndBaseDefault() {
        assertEquals(6_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BOMB_LOBBERS, "Mighty Lava Arena"));
        assertEquals(18_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BOMB_LOBBERS, "Intergalactic"));
        assertEquals(2_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.SHEEP_QUEST, "Sheep Quest"));
        assertEquals(16_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, "Hell"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, "Pirate"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.BACON_BRAWL, "Bacon Brawl"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.SPLEEF, "Spleef"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(ArcadePortGame.TURF_WARS, "Turf Wars"));
        assertEquals(12_000L, ArcadePortRules.sourceWorldTime(null, "Unknown"));
    }

    @Test void dragonEscapeKitsExposeSourceKitChoicesAndLeapCharges() {
        assertEquals("jumper", ArcadePortRules.normalizeDragonEscapeKit("Jumper"));
        assertEquals("disruptor", ArcadePortRules.normalizeDragonEscapeKit("DISRUPTOR"));
        assertEquals("warper", ArcadePortRules.normalizeDragonEscapeKit("Warper"));
        assertEquals("digger", ArcadePortRules.normalizeDragonEscapeKit("Digger"));
        assertNull(ArcadePortRules.normalizeDragonEscapeKit("speedster"));
        assertEquals(4, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(3, ArcadePortRules.dragonEscapeLeapUses("disruptor"));
        assertEquals(2, ArcadePortRules.dragonEscapeLeapUses("warper"));
        assertEquals(0, ArcadePortRules.dragonEscapeLeapUses("digger"));
        assertEquals(6, ArcadePortRules.DRAGON_ESCAPE_DIGGER_MAX_USES);
        assertEquals(2, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_MAX_TNT);
        assertEquals(10_000L, ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS);
    }

    @Test void sourceMapFallThresholdUsesVerticalBoundsInsteadOfFixedY58() {
        assertEquals(3.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 4), 0.00001D);
        assertEquals(13.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 14), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.sourceMapVoidThresholdY(true, 0), 0.00001D);
        assertEquals(58.0D, ArcadePortRules.sourceMapVoidThresholdY(false, 4), 0.00001D);
        assertTrue(13.0D > ArcadePortRules.sourceMapVoidThresholdY(true, 4),
                "Mighty Lava Arena Bacon Brawl starts around Y=13 and must not be eliminated on spawn");
    }

    @Test void dragonEscapeVoidThresholdFollowsLowAltitudeSourceMaps() {
        assertEquals(58.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Skylands"), 0.00001D);
        assertEquals(24.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Through Hell"), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Pirate Bay"), 0.00001D);
        assertEquals(58.0D, ArcadePortRules.dragonEscapeVoidThresholdY(null), 0.00001D);
    }

    @Test void dragonEscapeDestructionThrottleChecksNewAreaAfterMovement() {
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.0D, false));
        assertFalse(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.0D, true));
        assertFalse(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.24D, true));
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(0.25D, true));
        assertTrue(ArcadePortRules.shouldProcessDragonEscapeDestruction(Double.NaN, true));
    }

    @Test void dragonEscapeFallbackCrumblesOnlyItsGeneratedCourse() {
        assertTrue(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 0));
        assertTrue(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 82));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, -19));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(false, 83));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(true, 0));
        assertFalse(ArcadePortRules.shouldCrumbleDragonEscapeFallbackTrack(true, 82));
    }

    @Test void dragonEscapeRouteProgressNeverMovesBackAtCourseBends() {
        assertEquals(0, ArcadePortRules.monotonicDragonEscapeProgress(0, 0, 4));
        assertEquals(2, ArcadePortRules.monotonicDragonEscapeProgress(2, 1, 4));
        assertEquals(3, ArcadePortRules.monotonicDragonEscapeProgress(2, 99, 4));
        assertEquals(0, ArcadePortRules.monotonicDragonEscapeProgress(-1, -99, 4));
        assertEquals(-1, ArcadePortRules.monotonicDragonEscapeProgress(0, 1, 0));
    }

    @Test void dragonEscapeDisruptorRechargeStartsWhenTntIsThrown() {
        assertEquals(10_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));
        assertEquals(25_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(15_000L));
        assertEquals(ArcadePortRules.DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS,
                ArcadePortRules.dragonEscapeNextTntChargeAt(-1_000L) - (-1_000L));
    }

    @Test void dragonEscapeRouteOnlyDeduplicatesCoincidentWaypoints() {
        assertTrue(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.0D));
        assertTrue(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.5D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(0.75D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(2.0D));
        assertFalse(ArcadePortRules.isDuplicateDragonEscapeWaypoint(Double.NaN));
    }

    @Test void dragonEscapeUsesARealChaseProgressAndCourseFinishProximity() {
        assertEquals(0.2D, ArcadePortRules.DRAGON_ESCAPE_CHASE_SPEED, 0.00001D);
        assertEquals(10.0D, ArcadePortRules.DRAGON_ESCAPE_BLOCK_RADIUS, 0.00001D);
        assertTrue(ArcadePortRules.shouldDragonEscapePlayerBeCaught(25.01D, 25.0D));
        assertFalse(ArcadePortRules.shouldDragonEscapePlayerBeCaught(24.99D, 25.0D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(10.0D, 64.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 70.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 74.4D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 75.0D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 77.5D));
    }

    @Test void spleefProjectileRadiusScalesWithArrowSpeed() {
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(false, 8.0D));
        assertEquals(1, ArcadePortRules.spleefProjectileClearRadius(true, 0.0D));
        assertEquals(2, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 4.0D));
        assertEquals(3, ArcadePortRules.spleefProjectileClearRadius(true, 100.0D));
    }

    @Test void evolutionStagesAdvanceOnlyAfterACompletedCrouchEvolutionAndPersistOnDeath() {
        assertEquals(1, ArcadePortRules.nextEvolutionLevel(0, true));
        assertEquals(6, ArcadePortRules.nextEvolutionLevel(5, true));
        assertEquals(6, ArcadePortRules.nextEvolutionLevel(6, true));
        assertEquals(2, ArcadePortRules.nextEvolutionLevel(2, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(0, false));
        assertEquals(0, ArcadePortRules.nextEvolutionLevel(-1, false));
        assertFalse(ArcadePortRules.isEvolutionWinnerLevel(5));
        assertTrue(ArcadePortRules.isEvolutionWinnerLevel(6));
        assertEquals(80L, ArcadePortRules.EVOLUTION_RESPAWN_TICKS);
        assertEquals(44L, ArcadePortRules.EVOLUTION_FORM_CHANGE_TICKS);
        assertEquals(140L, ArcadePortRules.EVOLUTION_TOTAL_ANIMATION_TICKS);
    }

    @Test void evolutionUsesSourceFormHealthAndMeleeDamage() {
        assertEquals(40.0D, ArcadePortRules.evolutionMaxHealth(0));
        assertEquals(36.0D, ArcadePortRules.evolutionMaxHealth(1));
        assertEquals(32.0D, ArcadePortRules.evolutionMaxHealth(2));
        assertEquals(28.0D, ArcadePortRules.evolutionMaxHealth(3));
        assertEquals(22.0D, ArcadePortRules.evolutionMaxHealth(4));
        assertEquals(16.0D, ArcadePortRules.evolutionMaxHealth(5));
        assertEquals(6.0D, ArcadePortRules.evolutionMeleeDamage(0));
        assertEquals(5.0D, ArcadePortRules.evolutionMeleeDamage(1));
        assertEquals(5.0D, ArcadePortRules.evolutionMeleeDamage(2));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(3));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(4));
        assertEquals(3.0D, ArcadePortRules.evolutionMeleeDamage(5));
    }

    @Test void evolutionChargeAcceleratesWithKillsAndCapsAtTheSourceRate() {
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(0), 0.00001D);
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(1), 0.00001D);
        assertEquals(0.015D, ArcadePortRules.evolutionChargePerTick(2), 0.00001D);
        assertEquals(0.021D, ArcadePortRules.evolutionChargePerTick(4), 0.00001D);
        assertEquals(0.022D, ArcadePortRules.evolutionChargePerTick(5), 0.00001D);
        assertEquals(0.022D, ArcadePortRules.evolutionChargePerTick(50), 0.00001D);
    }

    @Test void sheepQuestLeaderRequiresMorePenSheep() {
        assertEquals(0, ArcadePortRules.sheepQuestLeadingTeam(3, 2));
        assertEquals(1, ArcadePortRules.sheepQuestLeadingTeam(1, 4));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(2, 2));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(0, 0));
    }

    @Test void paintballWaterBombRangeAndCooldownMatchSourceReviveFlow() {
        assertEquals(250L, ArcadePortRules.PAINTBALL_WATER_BOMB_COOLDOWN_MILLIS);
        assertEquals(3.0D, ArcadePortRules.PAINTBALL_SAVE_ME_RADIUS, 0.00001D);
        assertTrue(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(0.0D, true));
        assertTrue(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(9.0D, true));
        assertFalse(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(9.01D, true));
        assertFalse(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(1.0D, false));
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

    @Test void gladiatorsWaitFiveSecondsAndKeepCombatInsideTheCurrentDuel() {
        assertEquals(5_000L, ArcadePortRules.GLADIATOR_ROUND_COUNTDOWN_MILLIS);
        assertFalse(ArcadePortRules.canGladiatorsFight(4_999L, 5_000L, 0, 0, false));
        assertTrue(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 0, false));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 1, false));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, 0, 0, true));
        assertFalse(ArcadePortRules.canGladiatorsFight(5_000L, 5_000L, -1, 0, false));
    }

    @Test void gladiatorsPoisonOnlyUnresolvedDuelsAfterSixtySecondsAtOneDamagePerSecond() {
        assertEquals(60_000L, ArcadePortRules.GLADIATOR_POISON_DELAY_MILLIS);
        assertEquals(1_000L, ArcadePortRules.GLADIATOR_POISON_INTERVAL_MILLIS);
        assertEquals(1.0D, ArcadePortRules.GLADIATOR_POISON_DAMAGE, 0.0001D);
        assertFalse(ArcadePortRules.shouldGladiatorPoison(59_999L, 0L, false, 2));
        assertTrue(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 2));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, true, 2));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 1));
        assertFalse(ArcadePortRules.shouldGladiatorPoison(60_000L, 0L, false, 0));
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

    @Test void dragonEscapeHasAThreeDimensionalFinishWaypoint() {
        assertEquals(77.5D, ArcadePortRules.DRAGON_ESCAPE_FINISH_Z);
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(4.0D, 64.0D, 77.5D));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 77.5D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(0.5D, 64.0D, 80.51D));
    }

    @Test void deathTagChasersConvertRunnersOnlyAfterLethalDamageAndHaveDistinctKits() {
        assertFalse(ArcadePortRules.deathTagHitIsLethal(20.0D, 4.0D));
        assertFalse(ArcadePortRules.deathTagHitIsLethal(4.01D, 4.0D));
        assertTrue(ArcadePortRules.deathTagHitIsLethal(4.0D, 4.0D));
        assertTrue(ArcadePortRules.deathTagHitIsLethal(1.0D, 6.0D));
        assertFalse(ArcadePortRules.deathTagHitIsLethal(1.0D, 0.0D));
        assertEquals(4.0D, ArcadePortRules.deathTagChaserDamage(false));
        assertEquals(6.0D, ArcadePortRules.deathTagChaserDamage(true));
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("basher", false));
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("traitor", false));
        assertEquals(4.0D, ArcadePortRules.deathTagRunnerDamage("archer", true));
        assertEquals(0.0D, ArcadePortRules.deathTagRunnerDamage("null", false));
        assertEquals(0.0D, ArcadePortRules.deathTagRunnerDamage("basher", true));
        assertEquals(4.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, false));
        assertEquals(2.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, true));
        assertEquals(0.0D, ArcadePortRules.deathTagDamageAfterIronSkin(4.0D, true));
        assertEquals(1.0D, ArcadePortRules.deathTagDamageAfterIronSkin(1.0D, true));
        assertEquals(0.5D, ArcadePortRules.deathTagKnockbackMultiplier(true));
        assertEquals(1.0D, ArcadePortRules.deathTagKnockbackMultiplier(false));
    }

    @Test void deathTagOnlyChasersSpreadTheTagToRunners() {
        assertTrue(ArcadePortRules.shouldSpreadDeathTag(true, false));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, true));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(false, false));
        assertFalse(ArcadePortRules.shouldSpreadDeathTag(true, true));
    }

    @Test void deathTagRunnersGainSpeedOnlyWhenChasersExceedOneQuarterOfPlayers() {
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(1, 4));
        assertTrue(ArcadePortRules.deathTagRunnersReceiveSpeed(2, 6));
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(0, 4));
        assertFalse(ArcadePortRules.deathTagRunnersReceiveSpeed(0, 0));
    }

    @Test void deathTagEndsWhenOneRunnerRemainsExceptInSmallTestLobbies() {
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(1, 8));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 8));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(2, 8));
        assertFalse(ArcadePortRules.shouldFinishDeathTagRound(1, 2));
        assertTrue(ArcadePortRules.shouldFinishDeathTagRound(0, 2));
    }

    @Test void bombLobbersTntOnlyDamagesAssignedEnemyTeams() {
        assertFalse(ArcadePortRules.areBombLobbersEnemies(0, 0));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(1, 1));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(0, 1));
        assertTrue(ArcadePortRules.areBombLobbersEnemies(1, 0));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(-1, 1));
        assertFalse(ArcadePortRules.areBombLobbersEnemies(0, -1));
    }

    @Test void bombLobbersDamageRespectsLegacyDamageDependentArmorReduction() {
        assertEquals(10.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 0.0D), 0.00001D);
        assertEquals(9.2D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 7.0D), 0.00001D);
        assertEquals(7.2D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 12.0D), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 15.0D), 0.00001D);
        assertEquals(4.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, 20.0D), 0.00001D);
        assertEquals(0.42D, ArcadePortRules.bombLobbersDamageAfterArmor(1.0D, 15.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.bombLobbersDamageAfterArmor(-1.0D, 15.0D), 0.00001D);
        assertEquals(10.0D, ArcadePortRules.bombLobbersDamageAfterArmor(10.0D, -5.0D), 0.00001D);
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
        assertEquals(300_000L, ArcadePortRules.SHEEP_QUEST_ROUND_MILLIS);
        assertEquals(3, ArcadePortRules.SHEEP_QUEST_MAX_CARRY);
        assertEquals(20_000L, ArcadePortRules.SHEEP_QUEST_SPAWN_INTERVAL_MILLIS);
        assertTrue(ArcadePortRules.canCarryAnotherSheep(0));
        assertTrue(ArcadePortRules.canCarryAnotherSheep(2));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(3));
        assertFalse(ArcadePortRules.canCarryAnotherSheep(-1));
    }

    @Test void sheepQuestSlowsMoreAsPlayersCarryAdditionalSheep() {
        assertEquals(-1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(0));
        assertEquals(0, ArcadePortRules.sheepQuestCarrySlownessAmplifier(1));
        assertEquals(1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(2));
        assertEquals(2, ArcadePortRules.sheepQuestCarrySlownessAmplifier(3));
        assertEquals(2, ArcadePortRules.sheepQuestCarrySlownessAmplifier(4));
        assertEquals(-1, ArcadePortRules.sheepQuestCarrySlownessAmplifier(-1));
    }

    @Test void sheepQuestSupportsFourTeamsAndDetectsTies() {
        assertEquals(0, ArcadePortRules.sheepQuestLeadingTeam(new int[]{5, 3, 2, 1}));
        assertEquals(1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 4, 2, 3}));
        assertEquals(2, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 2, 6, 3}));
        assertEquals(3, ArcadePortRules.sheepQuestLeadingTeam(new int[]{1, 2, 3, 7}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{3, 3, 1, 0}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(new int[]{2, 2, 2, 2}));
        assertEquals(-1, ArcadePortRules.sheepQuestLeadingTeam(null));
    }

    @Test void sheepQuestFourTeamMarkersFollowSourceMapTeamIndexOrder() {
        assertEquals("RED", ArcadePortRules.sheepQuestTeamPenMarker(0));
        assertEquals("BLUE", ArcadePortRules.sheepQuestTeamPenMarker(1));
        assertEquals("GREEN", ArcadePortRules.sheepQuestTeamPenMarker(2));
        assertEquals("YELLOW", ArcadePortRules.sheepQuestTeamPenMarker(3));
        assertEquals("UNASSIGNED", ArcadePortRules.sheepQuestTeamPenMarker(4));
    }

    @Test void sheepQuestPenMarkersMatchTheFloorOneBlockBelowSheepFeet() {
        assertTrue(ArcadePortRules.sheepQuestInsidePenMarker(10, 64, -4, 10, 63, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(10, 63, -4, 10, 63, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(11, 64, -4, 10, 63, -4));
        assertFalse(ArcadePortRules.sheepQuestInsidePenMarker(10, 64, -3, 10, 63, -4));
    }

    @Test void sheepQuestPlayerHitsDropSheepAndDealDamage() {
        assertEquals(4.0D, ArcadePortRules.SHEEP_QUEST_PLAYER_HIT_DAMAGE, 0.00001D);
    }

    @Test void turfWarsTerritoryUsesTheSourceSpawnFloorHeight() {
        assertEquals(63, ArcadePortRules.turfWarsFloorYFromSpawn(64.0D));
        assertEquals(69, ArcadePortRules.turfWarsFloorYFromSpawn(70.9D));
        assertEquals(40, ArcadePortRules.turfWarsFloorYFromSpawn(41.0D));
        assertEquals(63, ArcadePortRules.turfWarsFloorYFromSpawn(Double.NaN));
    }

    @Test void turfWarsBuildPlacementStaysInsideSourceBoundsAndTerritory() {
        assertTrue(ArcadePortRules.isTurfWarsBuildWithinBounds(100, -10, 100, 140, -20, 20));
        assertTrue(ArcadePortRules.isTurfWarsBuildWithinBounds(139, 19, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(99, 0, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(140, 0, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(120, -21, 100, 140, -20, 20));
        assertFalse(ArcadePortRules.isTurfWarsBuildWithinBounds(120, 20, 100, 140, -20, 20));

        assertTrue(ArcadePortRules.canTurfWarsBuildOnTerritory(0, 119, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(0, 120, 119));
        assertTrue(ArcadePortRules.canTurfWarsBuildOnTerritory(1, 120, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(1, 119, 119));
        assertFalse(ArcadePortRules.canTurfWarsBuildOnTerritory(2, 120, 119));
    }

    @Test void turfWarsFrontlineAndVictoryUseTheSourceMapBounds() {
        assertEquals(-1, ArcadePortRules.turfWarsInitialFrontline(-20, 20));
        assertEquals(119, ArcadePortRules.turfWarsInitialFrontline(100, 140));
        assertEquals(120, ArcadePortRules.turfWarsInitialFrontline(100, 141));

        assertFalse(ArcadePortRules.isTurfWarsWinningLine(138, 0, 100, 140));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(139, 0, 100, 140));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(100, 1, 100, 140));
        assertTrue(ArcadePortRules.isTurfWarsWinningLine(99, 1, 100, 140));
        assertFalse(ArcadePortRules.isTurfWarsWinningLine(120, 2, 100, 140));

        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(120, 119, 0, 50));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(118, 119, 50, 0));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 3));
        assertEquals(-1, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 4));
    }

    @Test void turfWarsUsesDifferentArrowAndMeleeDamage() {
        assertEquals(9.0D, ArcadePortRules.turfWarsHitDamage(true, false), 0.00001D);
        assertEquals(9.0D, ArcadePortRules.turfWarsHitDamage(true, true), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.turfWarsHitDamage(false, false), 0.00001D);
        assertEquals(12.0D, ArcadePortRules.turfWarsHitDamage(false, true), 0.00001D);
    }

    @Test void paintballHealthTracksRemainingArmourPieces() {
        assertEquals(21.0D, ArcadePortRules.paintballHealthAfterHit(4), 0.00001D);
        assertEquals(16.0D, ArcadePortRules.paintballHealthAfterHit(3), 0.00001D);
        assertEquals(11.0D, ArcadePortRules.paintballHealthAfterHit(2), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.paintballHealthAfterHit(1), 0.00001D);
        assertEquals(2.0D, ArcadePortRules.paintballHealthAfterHit(0), 0.00001D);
    }

    @Test void microBattlesUsesNonLethalWeaponHitsAndMinimumHunger() {
        assertEquals(4.0D, ArcadePortRules.microBattlesHitDamage(true), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.microBattlesHitDamage(false), 0.00001D);
        assertEquals(2, ArcadePortRules.microBattlesMinimumFoodLevel(0));
        assertEquals(2, ArcadePortRules.microBattlesMinimumFoodLevel(2));
        assertEquals(7, ArcadePortRules.microBattlesMinimumFoodLevel(7));
        assertEquals(20, ArcadePortRules.microBattlesMinimumFoodLevel(25));
    }


    @Test void timedTeamGamesPreferSurvivorsThenAggregateKills() {
        assertEquals(0, ArcadePortRules.teamWithBestTimeoutStanding(3, 2, 1, 99));
        assertEquals(1, ArcadePortRules.teamWithBestTimeoutStanding(1, 2, 99, 0));
        assertEquals(0, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 7, 6));
        assertEquals(1, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 3, 4));
        assertEquals(-1, ArcadePortRules.teamWithBestTimeoutStanding(2, 2, 4, 4));
    }

    @Test void deathTagLightsAlternateBetweenGoldAndWhiteWool() {
        assertEquals(org.bukkit.Material.WHITE_WOOL,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.GOLD_BLOCK));
        assertEquals(org.bukkit.Material.GOLD_BLOCK,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.WHITE_WOOL));
        assertEquals(org.bukkit.Material.GOLD_BLOCK,
                ArcadePortRules.deathTagNextLightMaterial(org.bukkit.Material.GLOWSTONE));
    }

    @Test void gladiatorsStartingTierScalesWithLobbySize() {
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(1));
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(2));
        assertEquals(2, ArcadePortRules.gladiatorsStartingTierForPlayers(3));
        assertEquals(2, ArcadePortRules.gladiatorsStartingTierForPlayers(4));
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(5));
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(8));
        assertEquals(0, ArcadePortRules.gladiatorsStartingTierForPlayers(9));
        assertEquals(0, ArcadePortRules.gladiatorsStartingTierForPlayers(16));
    }

    @Test void gladiatorsRoundsMoveFromOuterGreenArenasTowardRedFinals() {
        assertEquals("GREEN", ArcadePortRules.gladiatorsArenaMarkerGroup(0));
        assertEquals("YELLOW", ArcadePortRules.gladiatorsArenaMarkerGroup(1));
        assertEquals("ORANGE", ArcadePortRules.gladiatorsArenaMarkerGroup(2));
        assertEquals("RED", ArcadePortRules.gladiatorsArenaMarkerGroup(3));
        assertEquals("GREEN", ArcadePortRules.gladiatorsArenaMarkerGroup(-1));
        assertEquals("RED", ArcadePortRules.gladiatorsArenaMarkerGroup(4));
    }

    @Test void turfWarsTimeoutUsesTerritoryBeforeKills() {
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(0, 0, 99));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(8, 0, 99));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-2, 99, 0));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-8, 99, 0));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(-1, 5, 4));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(-1, 4, 5));
        assertEquals(-1, ArcadePortRules.turfWarsTimeoutLeader(-1, 5, 5));
    }
}
