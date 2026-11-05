package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

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
            String gamePrefix = GameDebugVariableCatalog.rulePrefix(game);
            assertTrue(variables.stream().anyMatch(variable -> variable.key().startsWith(gamePrefix)),
                    game.id() + " has at least one game-specific runtime variable");
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
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_LEAPER_DOUBLE_JUMP_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPLEEF), "SPLEEF_HUNGER_DRAIN_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_SCORE_TO_WIN");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_DEFAULT_VIEW_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_TEMPLATE_MOBS_ENABLED");
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
    void wiredKeyRegistryStaysInSyncWithProductionGameplayConsumers() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java");
        }
        Pattern call = Pattern.compile("GameDebugRuntimeOverrides\\.(?:intValue|longValue|doubleValue|floatValue|booleanValue)\\(\\s*\"([A-Z0-9_]+)\"");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                var matcher = call.matcher(source);
                while (matcher.find()) {
                    assertTrue(GameDebugWiredRuleKeys.KEYS.contains(matcher.group(1)),
                            file.getFileName() + " uses an unregistered debug rule: " + matcher.group(1));
                }
            }
        }
    }

    @Test
    void everyCataloguedVariableHasALiteralOrExplicitDynamicGameplayConsumer() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java");
        }
        String productionSources;
        try (var files = Files.walk(sourceRoot)) {
            productionSources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    }).collect(Collectors.joining("\n"));
        }
        Pattern consumer = Pattern.compile(
                "GameDebugRuntimeOverrides\\.(?:intValue|longValue|doubleValue|floatValue|booleanValue)"
                        + "\\(\\s*\"([A-Z0-9_]+)\"");
        Set<String> literalConsumers = consumer.matcher(productionSources).results()
                .map(match -> match.group(1)).collect(Collectors.toSet());

        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugVariableCatalog.Variable variable : GameDebugVariableCatalog.variables(game)) {
                String key = variable.key();
                boolean dynamicMazeConsumer = game.isMonsterMaze()
                        && key.startsWith(GameDebugVariableCatalog.rulePrefix(game))
                        && (me.monstermaze.game.MazeDebugRules.key(me.monstermaze.game.MazeMode.ORIGINAL,
                                    key.substring(GameDebugVariableCatalog.rulePrefix(game).length()))
                                .equals(key)
                            || me.monstermaze.game.MazeDebugRules.key(me.monstermaze.game.MazeMode.SPEED,
                                    key.substring(GameDebugVariableCatalog.rulePrefix(game).length()))
                                .equals(key)
                            || me.monstermaze.game.MazeDebugRules.key(me.monstermaze.game.MazeMode.MODERN,
                                    key.substring(GameDebugVariableCatalog.rulePrefix(game).length()))
                                .equals(key));
                boolean dynamicSneakyConsumer = game == ArcadePortGame.SNEAKY_ASSASSINS
                        && key.startsWith(GameDebugVariableCatalog.rulePrefix(game))
                        && productionSources.contains("GameDebugRuntimeOverrides.longValue(\"SNEAKY_ASSASSINS_\" + name");
                assertTrue(literalConsumers.contains(key) || dynamicMazeConsumer || dynamicSneakyConsumer,
                        game.id() + " exposes " + key + " without a gameplay consumer");
            }
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
    void everyVisibleProfileVariableCanBeChangedAtItsGameplayOverrideBoundary() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugVariableCatalog.Variable variable : GameDebugVariableCatalog.variables(game)) {
                double configured = variable.defaultValue();
                Object supplied;
                Object fallback;
                Object actual;
                if (variable.kind() == GameDebugVariableCatalog.Kind.BOOLEAN) {
                    supplied = configured == 0.0D;
                    fallback = configured != 0.0D;
                    GameDebugRuntimeOverrides.activate(Map.of(variable.key(), supplied));
                    actual = GameDebugRuntimeOverrides.booleanValue(variable.key(), (Boolean) fallback);
                } else {
                    double changed = configured + variable.step();
                    if (changed > variable.maximum()) changed = configured - variable.step();
                    if (changed < variable.minimum()) changed = variable.minimum();
                    if (changed == configured) continue;
                    supplied = changed;
                    fallback = configured;
                    GameDebugRuntimeOverrides.activate(Map.of(variable.key(), supplied));
                    actual = switch (variable.kind()) {
                        case INTEGER -> GameDebugRuntimeOverrides.intValue(variable.key(), (int) configured);
                        case LONG -> GameDebugRuntimeOverrides.longValue(variable.key(), (long) configured);
                        case FLOAT -> GameDebugRuntimeOverrides.floatValue(variable.key(), (float) configured);
                        case DOUBLE -> GameDebugRuntimeOverrides.doubleValue(variable.key(), configured);
                        case BOOLEAN -> throw new AssertionError("boolean handled above");
                    };
                }
                if (supplied instanceof Number suppliedNumber) {
                    assertEquals(suppliedNumber.doubleValue(), ((Number) actual).doubleValue(), 1.0e-5D,
                            game.id() + " / " + variable.key());
                } else {
                    assertEquals(supplied, actual, game.id() + " / " + variable.key());
                }
                GameDebugRuntimeOverrides.clear();
                Object restored = switch (variable.kind()) {
                    case INTEGER -> GameDebugRuntimeOverrides.intValue(variable.key(), (int) variable.defaultValue());
                    case LONG -> GameDebugRuntimeOverrides.longValue(variable.key(), (long) variable.defaultValue());
                    case FLOAT -> GameDebugRuntimeOverrides.floatValue(variable.key(), (float) variable.defaultValue());
                    case DOUBLE -> GameDebugRuntimeOverrides.doubleValue(variable.key(), variable.defaultValue());
                    case BOOLEAN -> GameDebugRuntimeOverrides.booleanValue(variable.key(), variable.defaultValue() != 0.0D);
                };
                if (fallback instanceof Number fallbackNumber) {
                    assertEquals(fallbackNumber.doubleValue(), ((Number) restored).doubleValue(), 1.0e-5D,
                            game.id() + " reset / " + variable.key());
                } else {
                    assertEquals(fallback, restored, game.id() + " reset / " + variable.key());
                }
            }
        }
    }

    @Test
    void malformedOversizedIntegerOverrideCannotCrashGameplayConversion() {
        GameDebugRuntimeOverrides.activate(Map.of("UNTRUSTED_TEST_VALUE", Double.MAX_VALUE));

        assertEquals(Integer.MAX_VALUE,
                GameDebugRuntimeOverrides.intValue("UNTRUSTED_TEST_VALUE", 7));
    }

    @Test
    void configuredDebugRoundsSuppressProgressionEvenAfterResetToDefaults() {
        GameDebugRuntimeOverrides.activate(Map.of("BACON_1_8_HIT_DELAY_MILLIS", 250L));
        assertTrue(GameDebugRuntimeOverrides.isDebugSession());

        GameDebugRuntimeOverrides.resetActiveValues();

        assertTrue(GameDebugRuntimeOverrides.isDebugSession());
        assertEquals(400L, GameDebugRuntimeOverrides.longValue("BACON_1_8_HIT_DELAY_MILLIS", 400L));
        GameDebugRuntimeOverrides.clear();
        assertFalse(GameDebugRuntimeOverrides.isDebugSession());
    }

    @Test
    void liveDebugActionsMarkTheRoundForProgressionIsolation() {
        GameDebugRuntimeOverrides.activate(Map.of());
        assertFalse(GameDebugRuntimeOverrides.isDebugSession());

        GameDebugRuntimeOverrides.markDebugSession();
        GameDebugRuntimeOverrides.resetActiveValues();

        assertTrue(GameDebugRuntimeOverrides.isDebugSession());
        GameDebugRuntimeOverrides.clear();
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
                Map.entry("TURF_WARS_MELEE_DAMAGE", 3.0D),
                Map.entry("RUNNER_BLOCK_DECAY_MILLIS", 100L),
                Map.entry("DRAGON_ESCAPE_FINISH_X", 10.0D),
                Map.entry("DRAGON_ESCAPE_FINISH_Y", 20.0D),
                Map.entry("DRAGON_ESCAPE_FINISH_Z", 30.0D),
                Map.entry("DRAGON_ESCAPE_FINISH_RADIUS", 2.0D),
                Map.entry("DRAGON_ESCAPE_DISRUPTOR_ARM_TICKS", 5),
                Map.entry("DRAGON_ESCAPE_DISRUPTOR_TRIGGER_RADIUS", 4.0D),
                Map.entry("TURF_WARS_MARKSMAN_ARROW_INTERVAL_MILLIS", 500L),
                Map.entry("TURF_WARS_MARKSMAN_MAX_WOOL", 12),
                Map.entry("TURF_WARS_MARKSMAN_MAX_ARROWS", 4)));

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
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(100L));
        assertTrue(ArcadePortRules.isDragonEscapeFinishReached(10.0D, 20.0D, 30.0D));
        assertFalse(ArcadePortRules.isDragonEscapeFinishReached(12.1D, 20.0D, 30.0D));
        assertTrue(ArcadePortRules.shouldTriggerDragonEscapeDisruptor(5, 4.0D, false));
        assertEquals(500L, ArcadePortRules.turfWarsArrowIntervalMillis("marksman"));
        assertEquals(12, ArcadePortRules.turfWarsConstructorMax("marksman"));
        assertEquals(4, ArcadePortRules.turfWarsArrowMax("marksman"));
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
