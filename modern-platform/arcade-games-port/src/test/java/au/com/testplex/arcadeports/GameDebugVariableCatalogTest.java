package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugVariableCatalogTest {
    @AfterEach
    void clearSessionOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void everyGameGetsItsOwnSortedRuleCatalog() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            List<GameDebugVariableCatalog.Variable> variables = GameDebugVariableCatalog.variables(game);
            assertFalse(variables.isEmpty(), game.id() + " has a debug variable page");
            assertEquals(variables.stream().map(GameDebugVariableCatalog.Variable::key).sorted().toList(),
                    variables.stream().map(GameDebugVariableCatalog.Variable::key).toList(),
                    game.id() + " variables are stable for menu paging");
            assertTrue(variables.stream().anyMatch(variable -> variable.key().equals("LEGACY_ATTACK_SPEED")),
                    game.id() + " includes the shared 1.8 combat rule");
        }
    }

    @Test
    void gameSpecificCatalogsIncludeTheirOwnRulePrefixes() {
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.BACON_BRAWL), "BACON_1_8_HIT_DELAY_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGON_ESCAPE), "DRAGON_ESCAPE_CHASE_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_MINIONS_PER_WAVE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPLEEF), "SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_MATCH_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HOOPS_REMASTERED), "HOOPS_TARGET_SCORE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_MAX_INK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_HAZARD_STEERING_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_SCORE_TO_WIN");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_DEFAULT_VIEW_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_BALL_GRAVITY_PER_TICK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_ENDGAME_START_MINUTES");
        assertFalse(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS).stream()
                .anyMatch(variable -> variable.key().equals("WIZARDS_WAND_SLOT_COUNT")));
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WITHER_ASSAULT), "WITHER_ASSAULT_MINIONS_PER_WAVE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_FIRST_POWERUP_DELAY_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE), "MONSTER_MAZE_CRYO_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE_SPEED), "MONSTER_MAZE_SPEED_CRYO_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE_MODERN), "MONSTER_MAZE_MODERN_CRYO_RADIUS_BLOCKS");
        assertFalse(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS).stream()
                .anyMatch(variable -> variable.key().equals("SPEED_BUILDERS_BUILD_WIDTH")));
    }

    @Test
    void variableMenusOnlyExposeRulesWithRuntimeConsumers() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugVariableCatalog.Variable variable : GameDebugVariableCatalog.variables(game)) {
                assertTrue(GameDebugWiredRuleKeys.KEYS.contains(variable.key())
                                || game.isMonsterMaze() && variable.key().startsWith(GameDebugVariableCatalog.rulePrefix(game))
                                || game == ArcadePortGame.SNEAKY_ASSASSINS
                                        && variable.key().startsWith(GameDebugVariableCatalog.rulePrefix(game)),
                        game.id() + " exposes a rule without a runtime consumer: " + variable.key());
            }
        }
        assertFalse(GameDebugVariableCatalog.variables(ArcadePortGame.BACON_BRAWL).stream()
                .anyMatch(variable -> variable.key().equals("BACON_FLASH_FRIED_MAX_WIN_MILLIS")));
    }

    @Test
    void everyLiteralRuntimeOverrideHasAProfileMenuEntry() {
        var catalogKeys = java.util.Arrays.stream(ArcadePortGame.values())
                .flatMap(game -> GameDebugVariableCatalog.variables(game).stream())
                .map(GameDebugVariableCatalog.Variable::key)
                .collect(java.util.stream.Collectors.toSet());
        for (String key : GameDebugWiredRuleKeys.KEYS) {
            assertTrue(catalogKeys.contains(key), "Runtime override is unavailable in every game's profile menu: " + key);
        }
    }

    @Test
    void numericAndBooleanRuleOverridesUseProfileValuesAndPreserveDefaults() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "BACON_1_8_HIT_DELAY_MILLIS", 250.0D,
                "DRAGON_ESCAPE_CHASE_SPEED", 0.42D,
                "BACON_BRAWL_HAS_ROUND_TIMER", true));

        assertEquals(250L, GameDebugRuntimeOverrides.longValue("BACON_1_8_HIT_DELAY_MILLIS", 400L));
        assertEquals(0.42D, GameDebugRuntimeOverrides.doubleValue("DRAGON_ESCAPE_CHASE_SPEED", 0.2D), 1.0e-9);
        assertTrue(GameDebugRuntimeOverrides.booleanValue("BACON_BRAWL_HAS_ROUND_TIMER", false));
        assertEquals(7, GameDebugRuntimeOverrides.intValue("UNCONFIGURED_RULE", 7));

        GameDebugRuntimeOverrides.clear();
        assertEquals(400L, GameDebugRuntimeOverrides.longValue("BACON_1_8_HIT_DELAY_MILLIS", 400L));
        assertFalse(GameDebugRuntimeOverrides.booleanValue("BACON_BRAWL_HAS_ROUND_TIMER", false));
    }

    @Test
    void malformedOversizedIntegerOverrideCannotCrashGameplayConversion() {
        GameDebugRuntimeOverrides.activate(Map.of("UNTRUSTED_TEST_VALUE", Double.MAX_VALUE));

        assertEquals(Integer.MAX_VALUE,
                GameDebugRuntimeOverrides.intValue("UNTRUSTED_TEST_VALUE", 7));
    }

    @Test
    void timingCombatAndAbilityVariablesChangeTheirDeterministicGameRules() {
        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("CASTLE_ASSAULT_OBJECTIVE_TNT_RESPAWN_MILLIS", 10_000L),
                Map.entry("DRAW_MY_THING_GAME_MILLIS", 80_000L),
                Map.entry("DRAW_MY_THING_BASE_ROUND_MILLIS", 20_000L),
                Map.entry("DEATH_TAG_CHASER_PROMOTION_INTERVAL_MILLIS", 10_000L),
                Map.entry("DEATH_TAG_ARCHER_ARROW_KNOCKBACK_POWER", 5.0D),
                Map.entry("DEATH_TAG_TRAITOR_KNOCKBACK_COOLDOWN_MILLIS", 1_000L),
                Map.entry("DRAGON_ESCAPE_DIGGER_NEARBY_PLAYER_RADIUS", 2.5D),
                Map.entry("DRAGON_ESCAPE_WARPER_GRACE_MILLIS", 2_000L),
                Map.entry("DRAGONS_MAX_SPARKLERS", 1),
                Map.entry("DRAGONS_SPARKLER_LIFETIME_TICKS", 5),
                Map.entry("EVOLUTION_WIN_LEVEL", 4),
                Map.entry("EVOLUTION_OVERKILL_KILLS_PER_EVOLUTION", 3),
                Map.entry("EVOLUTION_SULPHUR_BOMB_RADIUS", 4.0D),
                Map.entry("EVOLUTION_SULPHUR_BOMB_COOLDOWN_MILLIS", 1_000L),
                Map.entry("PAINTBALL_MACHINE_GUN_MAX_HEAT", 0.8D),
                Map.entry("PAINTBALL_MACHINE_GUN_SHOTS_PER_HEAT_CYCLE", 2),
                Map.entry("PAINTBALL_MACHINE_GUN_COOL_PER_TICK", 0.1D),
                Map.entry("TURF_WARS_INITIAL_BUILD_PHASE_MILLIS", 5_000L),
                Map.entry("TURF_WARS_MELEE_DAMAGE", 3.0D)));

        assertEquals(10_000L, ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(false));
        assertEquals(20_000L, ArcadePortRules.drawMyThingRoundMillis(2));
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(20_000L));
        assertEquals(5.0D, ArcadePortRules.deathTagArcherArrowKnockbackMultiplier(false));
        assertFalse(ArcadePortRules.deathTagTraitorKnockbackReady(999L, 1L));
        assertTrue(ArcadePortRules.shouldPreventDragonEscapeDigger(2.4D, 20.0D));
        assertFalse(ArcadePortRules.isDragonEscapeWarpGraceActive(3_000L, 1_000L));
        assertFalse(ArcadePortRules.shouldReplenishDragonSparkler(10L, 10L, 1));
        assertTrue(ArcadePortRules.shouldExpireDragonSparkler(6, 64.0D, false));
        assertTrue(ArcadePortRules.isEvolutionWinnerLevel(4));
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(2));
        assertTrue(ArcadePortRules.isEvolutionSulphurBombInRange(4.0D));
        assertEquals(1_500L, ArcadePortRules.nextEvolutionSulphurBombReadyAt(500L));
        assertEquals(0.4D, ArcadePortRules.paintballMachineGunHeatPerShot(), 1.0e-9);
        assertEquals(0.7D, ArcadePortRules.coolPaintballMachineGunHeat(0.8D), 1.0e-9);
        assertEquals(5_000L, ArcadePortRules.turfWarsInitialBuildPhaseMillis(false));
        assertEquals(3.0D, ArcadePortRules.turfWarsHitDamage("marksman", false, false));
    }

    @Test
    void standaloneRuntimeRuleKeysChangeTheValuesConsumedByTheirGameLoops() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "CAMERA_SHY_MATCH_SECONDS", 90,
                "HOOPS_TARGET_SCORE", 15,
                "SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS", 75L,
                "SNAKE_MAX_HAZARDS", 2,
                "MILK_THE_COW_SCORE_TO_WIN", 12));

        assertEquals(90, GameDebugRuntimeOverrides.intValue("CAMERA_SHY_MATCH_SECONDS", 180));
        assertEquals(15, GameDebugRuntimeOverrides.intValue("HOOPS_TARGET_SCORE", 21));
        assertEquals(75L, GameDebugRuntimeOverrides.longValue("SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS", 200L));
        assertEquals(2, GameDebugRuntimeOverrides.intValue("SNAKE_MAX_HAZARDS", 4));
        assertEquals(12, GameDebugRuntimeOverrides.intValue("MILK_THE_COW_SCORE_TO_WIN", 15));
    }

    @Test
    void speedBuildersDebugTimingValuesChangeTheRulesUsedToStartBuildPhases() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "SPEED_BUILDERS_MIN_BUILD_SECONDS", 12,
                "SPEED_BUILDERS_MAX_BUILD_SECONDS", 12));

        assertEquals(12, SpeedBuildersRules.buildSeconds(40, 0, 0, 1.0D, null));
    }

    @Test
    void plexballDebugValuesChangePlayerValidationAndFieldRules() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "PLEXBALL_MIN_PLAYERS", 4,
                "PLEXBALL_MAX_PLAYERS", 8,
                "PLEXBALL_FAIR_ANGLE_DEGREES", 10.0D));

        assertFalse(PlexballRules.hasValidPlayerCount(2));
        assertTrue(PlexballRules.hasValidPlayerCount(6));
        assertFalse(PlexballRules.hasValidPlayerCount(9));
        assertFalse(PlexballRules.isFairYaw(160.0D));
    }

    @Test
    void wizardsDebugValuesChangeTheEndgameStrikeRules() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "WIZARDS_ENDGAME_MIN_STRIKE_MILLIS", 200,
                "WIZARDS_ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS", 10));

        assertEquals(990, au.com.testplex.wizards.WizardsRules.nextEndgameStrikeInterval(1_000));
        assertEquals(200, au.com.testplex.wizards.WizardsRules.nextEndgameStrikeInterval(100));
    }

    @Test
    void witherAndSneakyDebugValuesChangeTheirRealRuleConsumers() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "WITHER_ASSAULT_MINIONS_PER_WAVE", 5,
                "SNEAKY_ASSASSINS_FIRST_POWERUP_DELAY_MILLIS", 5_000L,
                "SNEAKY_ASSASSINS_REVEAL_RADIUS_BLOCKS", 20));

        assertEquals(5, WitherAssaultRules.minionCount());
        assertEquals(5_000L, SneakyAssassinsRules.firstPowerUpDelayMillis());
        assertEquals(400.0D, SneakyAssassinsRules.revealRadiusSquared());
    }

    @Test
    void monsterMazeProfilesKeepEachModeSeparateAndChangeKitAndMobRules() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "MONSTER_MAZE_CRYO_RADIUS_BLOCKS", 3,
                "MONSTER_MAZE_MODERN_CRYO_RADIUS_BLOCKS", 9,
                "MONSTER_MAZE_SPEED_MOB_MOVE_SPEED", 1.2F));

        assertEquals(3, me.monstermaze.game.MazeDebugRules.intValue(
                me.monstermaze.game.MazeMode.ORIGINAL, "CRYO_RADIUS_BLOCKS", 6));
        assertEquals(9, me.monstermaze.game.MazeDebugRules.intValue(
                me.monstermaze.game.MazeMode.MODERN, "CRYO_RADIUS_BLOCKS", 6));
        assertEquals(0.8F, me.monstermaze.game.MazeDebugRules.floatValue(
                me.monstermaze.game.MazeMode.ORIGINAL, "MOB_MOVE_SPEED", 0.8F));
        assertEquals(1.2F, me.monstermaze.game.MazeDebugRules.floatValue(
                me.monstermaze.game.MazeMode.SPEED, "MOB_MOVE_SPEED", 0.8F));
    }

    private static void assertContains(List<GameDebugVariableCatalog.Variable> variables, String key) {
        assertTrue(variables.stream().anyMatch(variable -> variable.key().equals(key)), key);
    }
}
