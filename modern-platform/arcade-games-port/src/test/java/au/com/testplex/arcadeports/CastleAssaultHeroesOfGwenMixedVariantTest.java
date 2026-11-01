package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CastleAssaultHeroesOfGwenMixedVariantTest {
    @Test
    void allFiveCastleAssaultKitsAndAliasesNormalizeToCanonicalIds() {
        assertEquals("alchemist", ArcadePortRules.normalizeCastleAssaultKit("Alchemist"));
        assertEquals("archer", ArcadePortRules.normalizeCastleAssaultKit("Archer"));
        assertEquals("demolitionist", ArcadePortRules.normalizeCastleAssaultKit("Demolitionist"));
        assertEquals("demolitionist", ArcadePortRules.normalizeCastleAssaultKit("demo"));
        assertEquals("demolitionist", ArcadePortRules.normalizeCastleAssaultKit("demolition"));
        assertEquals("fighter", ArcadePortRules.normalizeCastleAssaultKit("Fighter"));
        assertEquals("tank", ArcadePortRules.normalizeCastleAssaultKit("Tank"));
        assertNull(ArcadePortRules.normalizeCastleAssaultKit("mage"));
        assertNull(ArcadePortRules.normalizeCastleAssaultKit(null));
    }

    @Test
    void castleAssaultMixedArcadeHalvesObjectiveTntRespawnDelay() {
        assertEquals(60_000L, ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(false));
        assertEquals(30_000L, ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(true));
    }

    @Test
    void heroesOfGwenMixedArcadeHalvesStoreCostWithoutChangingStandardPrices() {
        assertEquals(800, ArcadePortRules.heroesOfGwenStoreItemCost(800, false));
        assertEquals(400, ArcadePortRules.heroesOfGwenStoreItemCost(800, true));
        assertEquals(375, ArcadePortRules.heroesOfGwenStoreItemCost(750, true));
        assertEquals(0, ArcadePortRules.heroesOfGwenStoreItemCost(0, true));
        assertThrows(IllegalArgumentException.class,
                () -> ArcadePortRules.heroesOfGwenStoreItemCost(-1, true));
    }

    @Test
    void heroesOfGwenMixedArcadeSkipsOnlyTheMiddleTowerMarkers() {
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(false, true));
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(false, false));
        assertFalse(ArcadePortRules.heroesOfGwenShouldSpawnTower(true, true));
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(true, false));
    }

    @Test
    void heroesOfGwenMiddleTowerFilterRecognisesOnlyExplicitMiddleMarkers() {
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED MIDDLE"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER_BLUE_MID"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER CENTRE LANE"));
        // In the archived Heros Valley map, marker 1 is the central tower beside the beacons;
        // marker 2 is the base-side tower and remains in the Mixed Arcade variant.
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED 1"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER BLUE 1"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER BLUE 2"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED 2"));
    }

    @Test
    void sourceMapDataExposesAuthoredTowerMarkerGroupsWithoutSharingMutableLists() {
        SourceMapCatalog.MapData data = SourceMapCatalog.parseMapConfig(null, List.of(
                "MAP_NAME: Heroes of GWEN",
                "CUSTOM_NAME: TOWER RED 1",
                "CUSTOM_LOCS: 10,64,20",
                "CUSTOM_NAME: TOWER RED MIDDLE",
                "CUSTOM_LOCS: 12,64,24"
        ));
        assertNotNull(data);
        assertEquals(1, data.customLocationGroups().get("TOWER RED 1").size());
        assertEquals(1, data.customLocationGroups().get("TOWER RED MIDDLE").size());
        data.customLocationGroups().get("TOWER RED 1").clear();
        assertEquals(1, data.customLocations("TOWER RED 1").size());
    }


    @Test
    void heroesOfGwenMinionWavesAndGoldMatchTheOriginalCadence() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_MINION_WAVE_INTERVAL_MILLIS);
        assertEquals(6, ArcadePortRules.HEROES_OF_GWEN_MINIONS_PER_WAVE);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_MINION_TARGET_UPDATE_MILLIS);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_GOLD_INTERVAL_MILLIS);
        assertEquals(5, ArcadePortRules.HEROES_OF_GWEN_BASE_GOLD_PER_INTERVAL);
        assertEquals(5, ArcadePortRules.HEROES_OF_GWEN_CAPTURE_POINT_GOLD_PER_INTERVAL);
        assertEquals(25, ArcadePortRules.HEROES_OF_GWEN_CAPTURE_POINT_REWARD_GOLD);
    }

    @Test
    void heroesOfGwenTowersOnlyPreferAnEarlierPlayerEntryOverMinions() {
        assertTrue(ArcadePortRules.heroesOfGwenTowerTargetsPlayer(100L, 200L));
        assertFalse(ArcadePortRules.heroesOfGwenTowerTargetsPlayer(200L, 100L));
        assertFalse(ArcadePortRules.heroesOfGwenTowerTargetsPlayer(100L, 100L));
        assertTrue(ArcadePortRules.heroesOfGwenTowerTargetsPlayer(100L, 0L));
        assertFalse(ArcadePortRules.heroesOfGwenTowerTargetsPlayer(0L, 100L));
    }

    @Test
    void anathBurnBeamMatchesArchivedRangeDamageAndTargetRecharge() {
        assertEquals(9_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_COOLDOWN_MILLIS);
        assertEquals(9.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_RANGE, 0.00001D);
        assertEquals(0.2D, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_STEP, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_HIT_RADIUS, 0.00001D);
        assertEquals(11, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_DAMAGE);
        assertEquals(2_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_TARGET_RECHARGE_MILLIS);
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_ANATH_BURN_BEAM_STEPS_PER_TICK);
    }

    @Test
    void anathFlameWandMatchesArchivedEmberDamageSpeedAndAmmoCadence() {
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_INTERVAL_MILLIS);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_RELOAD_MILLIS);
        assertEquals(4, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_AMMO_CAPACITY);
        assertEquals(5, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_DAMAGE);
        assertEquals(1.25D, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_PROJECTILE_SPEED, 0.00001D);
        assertEquals(60, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_WAND_FIRE_TICKS);
    }

    @Test
    void anathFlameDashMatchesArchivedVelocityDurationAndFireTrail() {
        assertEquals(12_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_COOLDOWN_MILLIS);
        assertEquals(600L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_DURATION_MILLIS);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_SPEED, 0.00001D);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_FIRE_DELAY_MILLIS);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_FLAME_DASH_FIRE_DURATION_MILLIS);
    }

    @Test
    void anathMeteorShowerMatchesItsDocumentedAreaDurationAndDamageCadence() {
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_COOLDOWN_MILLIS);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_DURATION_MILLIS);
        assertEquals(10.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_RADIUS, 0.00001D);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_BURN_INTERVAL_MILLIS);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_BURN_DAMAGE, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_PULSE_DAMAGE, 0.00001D);
        assertEquals(20.0D, ArcadePortRules.HEROES_OF_GWEN_ANATH_METEOR_FIREBALL_DAMAGE, 0.00001D);
        assertTrue(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(0.0D));
        assertTrue(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(10.0D));
        assertFalse(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(10.01D));
        assertFalse(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(Double.NaN));
    }

    @Test
    void heroesOfGwenFletcherUsesTheSourceHunterCapacitiesAndReloadTimes() {
        assertEquals(3, ArcadePortRules.heroesOfGwenFletcherMaxArrows("devon"));
        assertEquals(3_000L, ArcadePortRules.heroesOfGwenFletcherReloadMillis("devon"));
        assertEquals(2, ArcadePortRules.heroesOfGwenFletcherMaxArrows("rowena"));
        assertEquals(2_000L, ArcadePortRules.heroesOfGwenFletcherReloadMillis("rowena"));
        assertEquals(0, ArcadePortRules.heroesOfGwenFletcherMaxArrows("hattori"));
        assertEquals(0L, ArcadePortRules.heroesOfGwenFletcherReloadMillis("hattori"));
    }

    @Test
    void bothGamesResolveTheirOriginalSourceMapFoldersAndIds() {
        assertEquals(ArcadePortGame.CASTLE_ASSAULT, ArcadePortGame.fromId("castle-assault"));
        assertEquals(ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.fromId("Heroes of Gwen"));
        assertEquals(List.of("Castle Assault"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.CASTLE_ASSAULT));
        assertEquals(List.of("Heroes of GWEN"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.HEROES_OF_GWEN));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(ArcadePortGame.CASTLE_ASSAULT,
                "source-maps/Castle Assault/CastleAssault_Castle Siege.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(ArcadePortGame.HEROES_OF_GWEN,
                "source-maps/Heroes of GWEN/HOG_Heros_Valley.zip"));
    }

    @Test
    void larissaAquaCannonUsesSourceDamageRangeAndWaterStoneReload() {
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_DAMAGE);
        assertEquals(10.0D, ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_RANGE, 0.00001D);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_AQUA_CANNON_INTERVAL_MILLIS);
        assertEquals(3_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_RELOAD_MILLIS);
        assertEquals(64, ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_AMMO_CAPACITY);
    }

    @Test
    void larissaDancingFountainMatchesSourceAreaDurationHealingAndDamage() {
        assertEquals(14_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_COOLDOWN_MILLIS);
        assertEquals(6_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DURATION_MILLIS);
        assertEquals(3.0D, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_RADIUS, 0.00001D);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_PULSE_INTERVAL_MILLIS);
        assertEquals(4, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_HEAL_PER_SECOND);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_LARISSA_DANCING_FOUNTAIN_DAMAGE_PER_SECOND);
    }

    @Test
    void larissaWaterDashAndStormBlessingMatchSourceDurationsAndCooldowns() {
        assertEquals(600L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_DASH_DURATION_MILLIS);
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_WATER_DASH_COOLDOWN_MILLIS);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_DURATION_MILLIS);
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_COOLDOWN_MILLIS);
        assertEquals(66_500L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_REUSE_DELAY_MILLIS);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_HEAL_INTERVAL_MILLIS);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_LARISSA_STORM_BLESSING_HEAL_PER_SECOND);
    }

    @Test
    void bardolfSummonWolfMatchesTheArchivedPackLimitHealthAndCooldown() {
        assertEquals(5, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_MAX_WOLVES);
        assertEquals(8.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_HEALTH, 0.00001D);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_SUMMON_COOLDOWN_MILLIS);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_ATTACK_INTERVAL_MILLIS);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_WOLF_ATTACK_DAMAGE, 0.00001D);
        assertEquals(9.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_ATTACK_DISTANCE_SQUARED, 0.00001D);
        assertEquals(100.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_OWNER_TELEPORT_DISTANCE_SQUARED, 0.00001D);
    }

    @Test
    void bardolfWolfPounceMatchesTheCoordinatedLeapTimingAndVector() {
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_COOLDOWN_MILLIS);
        assertEquals(6_500L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_OVERRIDE_MILLIS);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_TRIGGER_DISTANCE_SQUARED, 0.00001D);
        assertEquals(1.5D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_VELOCITY_MULTIPLIER, 0.00001D);
        assertEquals(0.8D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_POUNCE_MINIMUM_VERTICAL_VELOCITY, 0.00001D);
    }

    @Test
    void bardolfFullMoonMatchesItsActiveDurationBuffsAndCooldownRestart() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_DURATION_MILLIS);
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_COOLDOWN_MILLIS);
        assertEquals(70_000L, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_REUSE_DELAY_MILLIS);
        assertEquals(20.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_WOLF_HEALTH, 0.00001D);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_DAMAGE_PER_WOLF, 0.00001D);
        assertEquals(0.05D, ArcadePortRules.HEROES_OF_GWEN_BARDOLF_FULL_MOON_SPEED_PER_WOLF, 0.00001D);
    }

    @Test
    void rowenaLightArrowsMatchesFivePiercingShotsAndDamageScaling() {
        assertEquals(5, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_COUNT);
        assertEquals(12_000L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROWS_COOLDOWN_MILLIS);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_TARGET_RECHARGE_MILLIS);
        assertEquals(40.0D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_RANGE, 0.00001D);
        assertEquals(0.4D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_STEP, 0.00001D);
        assertEquals(1.5D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_LIGHT_ARROW_HIT_RADIUS, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.heroesOfGwenRowenaLightArrowDamage(0), 0.00001D);
        assertEquals(5.25D, ArcadePortRules.heroesOfGwenRowenaLightArrowDamage(1), 0.00001D);
        assertEquals(6.5D, ArcadePortRules.heroesOfGwenRowenaLightArrowDamage(2), 0.00001D);
    }

    @Test
    void rowenaCombatSlideMatchesSourceVelocityDurationAndCooldown() {
        assertEquals(8_000L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_COOLDOWN_MILLIS);
        assertEquals(250L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_DURATION_MILLIS);
        assertEquals(1.5D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_COMBAT_SLIDE_SPEED, 0.00001D);
    }

    @Test
    void rowenaBombardmentMatchesSourceFlightShotsAndExplosiveDamage() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DURATION_MILLIS);
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_COOLDOWN_MILLIS);
        assertEquals(70_000L, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_REUSE_DELAY_MILLIS);
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_SHOTS);
        assertEquals(15.0D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_ASCENT_BLOCKS, 0.00001D);
        assertEquals(40.0D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_RANGE, 0.00001D);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_ROWENA_BOMBARDMENT_BLAST_RADIUS, 0.00001D);
        assertEquals(10.0D, ArcadePortRules.heroesOfGwenRowenaBombardmentDamage(0), 0.00001D);
        assertEquals(12.5D, ArcadePortRules.heroesOfGwenRowenaBombardmentDamage(1), 0.00001D);
    }

    @Test
    void danaPulseHealMatchesSourceRadiusCooldownAndHealingValues() {
        assertEquals(8_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_COOLDOWN_MILLIS);
        assertEquals(7.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_RADIUS, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_SELF, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_PULSE_HEAL_ALLY, 0.00001D);
    }

    @Test
    void danaKnockUpMatchesSourceDashAndCollisionValues() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_COOLDOWN_MILLIS);
        assertEquals(600L, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_DURATION_MILLIS);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_SPEED, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_COLLISION_RADIUS, 0.00001D);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_TARGET_RECHARGE_MILLIS);
        assertEquals(12.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_PLAYER_DAMAGE, 0.00001D);
        assertEquals(8.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_MINION_DAMAGE, 0.00001D);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_PLAYER_LIFT, 0.00001D);
        assertEquals(0.5D, ArcadePortRules.HEROES_OF_GWEN_DANA_KNOCK_UP_MINION_LIFT, 0.00001D);
    }

    @Test
    void danaRallyMatchesSourceLeapLandingAndBannerTimings() {
        assertEquals(40_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_COOLDOWN_MILLIS);
        assertEquals(47_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_REUSE_DELAY_MILLIS);
        assertEquals(1.5D, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_JUMP_VERTICAL_VELOCITY, 0.00001D);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_MINIMUM_FLIGHT_MILLIS);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_BANNER_DURATION_MILLIS);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_RADIUS, 0.00001D);
        assertEquals(3.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_HEAL_PER_SECOND, 0.00001D);
        assertEquals(3.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_LANDING_DAMAGE_RADIUS, 0.00001D);
        assertEquals(7.0D, ArcadePortRules.HEROES_OF_GWEN_DANA_RALLY_LANDING_DAMAGE, 0.00001D);
    }

    @Test
    void biffTetherMatchesSourceRangeSlowAndActiveCooldownWindow() {
        assertEquals(12_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_COOLDOWN_MILLIS);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_ACTIVE_MILLIS);
        assertEquals(17_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_REUSE_DELAY_MILLIS);
        assertEquals(8.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_RANGE, 0.00001D);
        assertEquals(100.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_BREAK_DISTANCE_SQUARED, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_PULL_STOP_DISTANCE_SQUARED, 0.00001D);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_SLOW_DURATION_MILLIS);
        assertEquals(1, ArcadePortRules.HEROES_OF_GWEN_BIFF_TETHER_SLOW_AMPLIFIER);
    }

    @Test
    void biffBattleLeapMatchesSourceLeapLandingDamageAndKnockUp() {
        assertEquals(11_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_COOLDOWN_MILLIS);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_VERTICAL_VELOCITY, 0.00001D);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_MINIMUM_FLIGHT_MILLIS);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_LANDING_RADIUS, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_DAMAGE, 0.00001D);
        assertEquals(0.6D, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_MINIMUM_KNOCK_UP, 0.00001D);
        assertEquals(0.5D, ArcadePortRules.HEROES_OF_GWEN_BIFF_BATTLE_LEAP_RANDOM_KNOCK_UP, 0.00001D);
    }

    @Test
    void biffCavalryChargeMatchesSourceMountDurationRootAndCooldown() {
        assertEquals(45_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_COOLDOWN_MILLIS);
        assertEquals(50_500L, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_REUSE_DELAY_MILLIS);
        assertEquals(6_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_DURATION_MILLIS);
        assertEquals(2_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_ROOT_RECHARGE_MILLIS);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_ROOT_DURATION_MILLIS);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_RADIUS, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_DAMAGE, 0.00001D);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_BIFF_CAVALRY_CHARGE_JUMP_STRENGTH, 0.00001D);
    }

    @Test
    void ivyVinePullMatchesSourceBeamRangeDamageAndSlowValues() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_COOLDOWN_MILLIS);
        assertEquals(9.0D, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_RANGE, 0.00001D);
        assertEquals(0.25D, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_BEAM_STEP, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_HIT_RADIUS, 0.00001D);
        assertEquals(4.0D, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_STOP_DISTANCE_SQUARED, 0.00001D);
        assertEquals(4, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_PLAYER_DAMAGE);
        assertEquals(6, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_MINION_DAMAGE);
        assertEquals(60, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_SLOW_TICKS);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_SLOW_AMPLIFIER);
        assertEquals(8_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_VINE_PULL_MAX_PULL_MILLIS);
    }

    @Test
    void ivyFloralLeapMatchesSourceLandingDamageFlowerBedAndCooldown() {
        assertEquals(11_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_COOLDOWN_MILLIS);
        assertEquals(1.2D, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_VERTICAL_VELOCITY, 0.00001D);
        assertEquals(500L, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_MINIMUM_FLIGHT_MILLIS);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_BED_DURATION_MILLIS);
        assertEquals(1_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE_INTERVAL_MILLIS);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE_RADIUS, 0.00001D);
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_FLOWER_RADIUS);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_DAMAGE);
        assertEquals(50, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_SLOW_TICKS);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_IVY_FLORAL_LEAP_SLOW_AMPLIFIER);
    }

    @Test
    void ivyThornWallMatchesSourceDurationAreaSupportAndEnemyEffects() {
        assertEquals(45_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_COOLDOWN_MILLIS);
        assertEquals(5_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_DURATION_MILLIS);
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_BLOCK_RADIUS);
        assertEquals(60L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ALLY_REGEN_TICKS);
        assertEquals(1, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ALLY_REGEN_AMPLIFIER);
        assertEquals(2_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_ROOT_MILLIS);
        assertEquals(2_000L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_ROOT_RECHARGE_MILLIS);
        assertEquals(900L, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_DAMAGE_RECHARGE_MILLIS);
        assertEquals(2, ArcadePortRules.HEROES_OF_GWEN_IVY_THORN_WALL_ENEMY_DAMAGE);
    }

    @Test
    void hattoriDoubleJumpMatchesSourcePowerHeightAndRecharge() {
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_HATTORI_DOUBLE_JUMP_POWER, 0.00001D);
        assertEquals(1.0D, ArcadePortRules.HEROES_OF_GWEN_HATTORI_DOUBLE_JUMP_HEIGHT_LIMIT, 0.00001D);
        assertEquals(3_000L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_DOUBLE_JUMP_COOLDOWN_MILLIS);
    }

    @Test
    void hattoriShurikenNinjaDashAndEnderBladeMatchSourceValues() {
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_COUNT);
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_DAMAGE);
        assertEquals(1_500L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_COOLDOWN_MILLIS);
        assertEquals(4L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_SHURIKEN_SPACING_TICKS);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_COOLDOWN_MILLIS);
        assertEquals(10.0D, ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_RANGE, 0.00001D);
        assertEquals(2.0D, ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_COLLISION_RADIUS, 0.00001D);
        assertEquals(8, ArcadePortRules.HEROES_OF_GWEN_HATTORI_NINJA_DASH_DAMAGE);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_ACTIVE_MILLIS);
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_COOLDOWN_MILLIS);
        assertEquals(67_000L, ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_REUSE_DELAY_MILLIS);
        assertEquals(9.0D, ArcadePortRules.HEROES_OF_GWEN_HATTORI_ENDER_BLADE_DAMAGE_BONUS, 0.00001D);
    }

    @Test
    void devonTntArrowsMatchSourceShotCountCooldownAndBowDamageScaling() {
        assertEquals(3, ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_ARROW_COUNT);
        assertEquals(17_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_ARROW_COOLDOWN_MILLIS);
        assertEquals(5.0D, ArcadePortRules.HEROES_OF_GWEN_DEVON_TNT_EXPLOSION_RADIUS, 0.00001D);
        assertEquals(10.0D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(0), 0.00001D);
        assertEquals(12.5D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(1), 0.00001D);
        assertEquals(15.0D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(2), 0.00001D);
    }

    @Test
    void devonHuntersBoostAndInfinityMatchSourceActiveAndCooldownWindows() {
        assertEquals(10_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_HUNTERS_BOOST_COOLDOWN_MILLIS);
        assertEquals(3_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_HUNTERS_BOOST_DURATION_MILLIS);
        assertEquals(7_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_ACTIVE_MILLIS);
        assertEquals(60_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_COOLDOWN_MILLIS);
        assertEquals(67_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_REUSE_DELAY_MILLIS);
        assertEquals(6.0D, ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_HOMING_RANGE, 0.00001D);
        assertEquals(3_000L, ArcadePortRules.HEROES_OF_GWEN_DEVON_INFINITY_WITHER_DURATION_MILLIS);
    }

    @Test
    void witherTargetsHostileLivingEntitiesIncludingNeutralMobsButNotAlliesOrInactivePlayers() {
        assertTrue(ArcadePortRules.heroesOfGwenWitherCanTarget(0, 1, true));
        assertTrue(ArcadePortRules.heroesOfGwenWitherCanTarget(0, -1, true));
        assertFalse(ArcadePortRules.heroesOfGwenWitherCanTarget(0, 0, true));
        assertFalse(ArcadePortRules.heroesOfGwenWitherCanTarget(0, 1, false));
        assertFalse(ArcadePortRules.heroesOfGwenWitherCanTarget(0, -1, false));
    }

    @Test
    void witherDirectMovementUsesSourceTickPerBlockSpeed() {
        assertEquals(0.2D, ArcadePortRules.heroesOfGwenWitherStepPerTick(
                ArcadePortRules.HEROES_OF_GWEN_WITHER_TARGET_SPEED), 0.00001D);
        assertEquals(0.3D, ArcadePortRules.heroesOfGwenWitherStepPerTick(
                ArcadePortRules.HEROES_OF_GWEN_WITHER_HOME_SPEED), 0.00001D);
    }
}
