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
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAW_MY_THING), "DRAW_MY_THING_MAX_GUESSES");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAW_MY_THING), "DRAW_MY_THING_FIRST_GUESS_POINTS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAW_MY_THING), "DRAW_MY_THING_LATER_GUESS_POINTS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_MINIONS_PER_WAVE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_OVERTIME_DELAY_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_DEVON_FLETCHER_MAX_ARROWS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HEROES_OF_GWEN), "HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DAMAGE_FACTOR");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPLEEF), "SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_CHASER_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_ALPHA_CHASER_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_RUNNER_BASHER_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_RUNNER_KILL_REWARD_GEMS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_IRON_SKIN_DAMAGE_REDUCTION");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DEATH_TAG), "DEATH_TAG_MELEE_KNOCKBACK_HORIZONTAL");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_MATCH_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.HOOPS_REMASTERED), "HOOPS_TARGET_SCORE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.GLADIATORS), "GLADIATOR_MELEE_NO_DAMAGE_TICKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.GLADIATORS), "GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.GLADIATORS), "GLADIATOR_TIER_3_ARROWS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.GLADIATORS), "GLADIATOR_POTION_HEALING");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_LINES_PER_KILL_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_COMBAT_BOUNDARY_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_BUILD_BOUNDARY_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_TERRITORY_KNOCKBACK_HORIZONTAL");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_TERRITORY_KNOCKBACK_VERTICAL");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_BARRAGE_ARROW_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.TURF_WARS), "TURF_WARS_BARRAGE_ARROW_SPREAD");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_MAX_INK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_NORMAL_INK_REGEN_PER_SECOND");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_SQUID_INK_REGEN_PER_SECOND");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_CHARGER_FULL_CHARGE_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_CHARGER_MAX_INK_COST");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_CHARGER_MIN_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_ROLLER_PRIMARY_INK_COST");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_CHARGER_PRIMARY_INTERVAL_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_LOBBER_SECONDARY_INK_COST");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_SECONDARY_COOLDOWN_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_ROLLER_STAMPEDE_HIT_REACH_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_CHARGER_PRISM_BEAM_STEPS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_LOBBER_PAINTSTORM_PULSES");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_DUALIES_AFTERIMAGE_DASHES");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_DUALIES_PRIMARY_PROJECTILES");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_ROLLER_PRIMARY_PAINT_STEPS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SUPER_SPLOOR), "SUPER_SPLOOR_LOBBER_PRIMARY_PROJECTILE_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_AUTONOMOUS_FLIGHT_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_AUTONOMOUS_MOVEMENT_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_SPAWNING_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_TAMER_FLIGHT_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_TURN_RATE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_SURVIVE_FIVE_MINUTES_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.DRAGONS), "DRAGONS_WIN_STREAK_ACHIEVEMENT");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_HAZARD_STEERING_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_MOVEMENT_GROWTH_CAP");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_ROULETTE_HIT_7_PLUS_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_SPEED_ABILITY_DURATION_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_TAIL_COLLISION_RADIUS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_HAZARD_CLEARANCE_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNAKE), "SNAKE_START_SPAWN_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_LEAPER_DOUBLE_JUMP_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_BRAWLER_WEAPON_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_GROUND_POUND_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_REBOUND_MAX_POWER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.ONE_IN_THE_QUIVER), "QUIVER_SCORE_TO_WIN");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MASTER_BUILDERS), "MASTER_BUILDERS_TOTAL_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MASTER_BUILDERS), "MASTER_BUILDERS_VOTE_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MASTER_BUILDERS), "MASTER_BUILDERS_REVIEW_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_BARRIER_DURATION_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_CRUMBLE_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_COW_BOMB_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_BODY_SLAM_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_ASSIST_CREDIT_WINDOW_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_GOT_THE_CHOPS_AIRBORNE_KILLS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_FARMER_DOUBLE_JUMP_HORIZONTAL_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_CHEAT_WINDOW_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_BORDER_PARTICLE_DISTANCE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_ANNIHILATION_KILLS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_COMBAT_LOG_EXPIRE_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_DASHER_JUMP_HORIZONTAL_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MICRO_BATTLES), "MICRO_BATTLES_DASHER_JUMP_VERTICAL_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPLEEF), "SPLEEF_HUNGER_DRAIN_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MILK_THE_COW), "MILK_THE_COW_SCORE_TO_WIN");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_DEFAULT_VIEW_SECONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_PERFECT_SCORE_PERCENT");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS), "SPEED_BUILDERS_TEMPLATE_MOBS_ENABLED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_BALL_GRAVITY_PER_TICK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_BALL_MAX_FLIGHT_TICKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_BALL_RADIUS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_BATTING_POWER_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_CURVE_ACCELERATION");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_CURVE_ARTIST_SIDE_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_SKYGUARD_LEAP_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_SLUGGER_DIVE_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.PLEXBALL), "PLEXBALL_SPRINTER_BURST_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_ENDGAME_START_MINUTES");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_WAND_SLOT_COUNT");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_MANA_REGEN_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_SPELL_MANA_COST_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_SPELL_COOLDOWN_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_SPELL_DAMAGE_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_SPELL_HEAL_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WIZARDS), "WIZARDS_PROJECTILE_SPEED_MULTIPLIER");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WITHER_ASSAULT), "WITHER_ASSAULT_MINIONS_PER_WAVE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WITHER_ASSAULT), "WITHER_ASSAULT_SKULL_BLAST_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WITHER_ASSAULT), "WITHER_ASSAULT_FIRE_STORM_ARROWS_PER_TICK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.WITHER_ASSAULT), "WITHER_ASSAULT_FLIGHT_MAX_SPEED_CHANGE_PER_TICK");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_FIRST_POWERUP_DELAY_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_WOOD_SWORD_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_PROJECTILE_DAMAGE_PER_SPEED");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_MAX_POWERUP_PROGRESS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_GLOBAL_REVEAL_DURATION_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_NPC_ATTACK_DAMAGE");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_NPC_RECOVERY_HORIZONTAL");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_ASSASSIN_STARTING_ARROWS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.SNEAKY_ASSASSINS), "SNEAKY_ASSASSINS_REVEALER_STARTING_DIAMONDS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE), "MONSTER_MAZE_CRYO_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE_SPEED), "MONSTER_MAZE_SPEED_CRYO_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.MONSTER_MAZE_MODERN), "MONSTER_MAZE_MODERN_CRYO_RADIUS_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.BACON_BRAWL), "BACON_BRAWL_HITS_DAMAGE_HEALTH");
        assertFalse(GameDebugVariableCatalog.variables(ArcadePortGame.SPEED_BUILDERS).stream()
                .anyMatch(variable -> variable.key().equals("SPEED_BUILDERS_BUILD_WIDTH")));
    }

    @Test
    void turfWarsControlsHaveSafeMechanicSpecificRanges() {
        assertRange("TURF_WARS_LINES_PER_KILL_MULTIPLIER", 1.0D, 8.0D);
        assertRange("TURF_WARS_COMBAT_BOUNDARY_DAMAGE", 0.0D, 40.0D);
        assertRange("TURF_WARS_BUILD_BOUNDARY_DAMAGE", 0.0D, 500.0D);
        assertRange("TURF_WARS_TERRITORY_KNOCKBACK_HORIZONTAL", 0.0D, 8.0D);
        assertRange("TURF_WARS_TERRITORY_KNOCKBACK_VERTICAL", 0.0D, 5.0D);
        assertRange("TURF_WARS_BARRAGE_ARROW_SPEED", 0.0D, 8.0D);
        assertRange("TURF_WARS_BARRAGE_ARROW_SPREAD", 0.0D, 1.0D);
    }

    @Test
    void gladiatorsControlsHaveSafeRanges() {
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_MELEE_NO_DAMAGE_TICKS", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_MELEE_KNOCKBACK_HORIZONTAL", 0.0D, 3.0D);
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_TIER_3_ARROWS", 0.0D, 64.0D);
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_POTION_HEALING", 0.0D, 20.0D);
        assertGameRange(ArcadePortGame.GLADIATORS, "GLADIATOR_POTION_SPLASH_RADIUS", 0.1D, 16.0D);
    }

    @Test
    void sneakyAssassinsCombatAndNpcControlsHaveSafeRanges() {
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_WOOD_SWORD_DAMAGE", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_PROJECTILE_DAMAGE_PER_SPEED", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_MAX_POWERUP_PROGRESS", 1.0D, 32.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_POWERUP_PICKUP_RADIUS_BLOCKS", 0.1D, 16.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_GLOBAL_REVEAL_DURATION_MILLIS", 0.0D, 600_000.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_SOURCE_NPC_COUNT", 0.0D, 500.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_NPC_ATTACK_DAMAGE", 0.0D, 8.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_ASSASSIN_STARTING_ARROWS", 0.0D, 64.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_NPC_ATTACK_COOLDOWN_MILLIS", 0.0D, 600_000.0D);
        assertGameRange(ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_REVEALER_PROJECTILE_LIFETIME_TICKS", 1.0D, 1_200.0D);
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_WOOD_SWORD_DAMAGE")));
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_NPC_ATTACK_DAMAGE")));
        assertFalse(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_SOURCE_NPC_COUNT")));
        assertFalse(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SNEAKY_ASSASSINS, "SNEAKY_ASSASSINS_ASSASSIN_STARTING_ARROWS")));
    }

    @Test
    void milkTheCowAbilityAndProgressControlsHaveSafeRanges() {
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_BOMB_COOLDOWN_MILLIS", 0.0D, 600_000.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_BOMB_CONTACT_RADIUS_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_BOMB_DAMAGE", 0.0D, 16.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_BODY_SLAM_RADIUS_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COMEBACK_LEAD_THRESHOLD", 1.0D, 100.0D);
    }

    @Test
    void quiverScoreGoalHasASafeAdjustableRange() {
        assertGameRange(ArcadePortGame.ONE_IN_THE_QUIVER, "QUIVER_SCORE_TO_WIN", 1.0D, 200.0D);
    }

    @Test
    void heroesOfGwenControlsHaveMechanicSafeRanges() {
        assertGameRange(ArcadePortGame.HEROES_OF_GWEN, "HEROES_OF_GWEN_OVERTIME_DELAY_MILLIS", 1.0D, 3_600_000.0D);
        assertGameRange(ArcadePortGame.HEROES_OF_GWEN, "HEROES_OF_GWEN_OVERTIME_PATH_ADVANCE_DISTANCE_SQUARED", 0.0D, 256.0D);
        assertGameRange(ArcadePortGame.HEROES_OF_GWEN, "HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.HEROES_OF_GWEN, "HEROES_OF_GWEN_DEVON_TNT_BOW_BASE_DAMAGE", 0.0D, 100.0D);
        assertGameRange(ArcadePortGame.HEROES_OF_GWEN, "HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DAMAGE_FACTOR", 0.0D, 8.0D);
    }

    @Test
    void deathTagKillRewardSliderHasBoundedValues() {
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_RUNNER_KILL_REWARD_GEMS", 0.0D, 64.0D);
    }

    @Test
    void deathTagTurtleAndTricksterControlsHaveBoundedRanges() {
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_TURTLE_STACK_RESET_MILLIS", 0.0D, 600_000.0D);
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_TURTLE_MAX_STACKS", 1.0D, 10.0D);
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_TURTLE_EFFECT_DURATION_TICKS", 1.0D, 1_200.0D);
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_TURTLE_HITS_PER_WIND_CHARGE", 1.0D, 100.0D);
        assertGameRange(ArcadePortGame.DEATH_TAG, "DEATH_TAG_TRICKSTER_FIREWORKS_COOLDOWN_MILLIS", 0.0D, 600_000.0D);
    }

    @Test
    void baconCrispyAbilityControlsHaveBoundedRanges() {
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_BURN_DURATION_MILLIS", 0.0D, 60_000.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_DASH_HIT_RADIUS_SQUARED", 0.0D, 64.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_FIRE_CONTACT_RADIUS_SQUARED", 0.0D, 64.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_IMPACT_FIRE_BLOCKS", 1.0D, 9.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CHRIS_ABILITY_METER_COST", 0.0D, 100.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_SHOT_COUNT", 1.0D, 9.0D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_SHOT_SPREAD_RADIANS", 0.0D, 0.6D);
        assertGameRange(ArcadePortGame.BACON_BRAWL, "BACON_CRISPY_SHOT_VELOCITY", 0.1D, 3.0D);
    }

    @Test
    void milkTheCowChargeAndHerdControlsHaveBoundedRanges() {
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_CHARGE_HUNGER_INTERVAL_MILLIS", 1.0D, 120_000.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_CHARGE_TARGET_RADIUS_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_CHARGE_TARGET_HEIGHT_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_CHARGE_TARGET_COOLDOWN_MILLIS", 0.0D, 120_000.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_CHARGE_DAMAGE", 0.0D, 40.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_SPRINT_SPEED_DURATION_TICKS", 1.0D, 1_200.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_COW_SPRINT_SPEED_AMPLIFIER", 0.0D, 10.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_SCORE_PER_COW_KILL", 0.0D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_HERD_UPDATE_INTERVAL_MILLIS", 1.0D, 120_000.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_HERD_FOLLOW_MIN_DISTANCE_SQUARED", 0.0D, 4_096.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_HERD_SPAWN_RADIUS_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.MILK_THE_COW, "MILK_THE_COW_FARMER_HEAL_PER_MILK_HP", 0.0D, 40.0D);
    }

    @Test
    void snapPhotoMechanicControlsHaveBoundedRanges() {
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_PHOTOS_TO_ELIMINATE", 1.0D, 100.0D);
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_CLOSE_PHOTO_BLOCKS", 0.1D, 32.0D);
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_LONG_PHOTO_BLOCKS", 0.1D, 64.0D);
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_PHOTO_LENS_MIN_DOT", 0.5D, 0.999D);
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_SPOTLIGHT_INTERVAL_MILLIS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_SMOKE_ABILITY_COOLDOWN_MILLIS");
        assertTrue(CameraShyRules.debuggableRule("DECOY_ABILITY_COOLDOWN_MILLIS"));
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_PURSUIT_ESCAPE_DISTANCE_BLOCKS");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_DECOY_LOOK_MIN_DOT");
        assertContains(GameDebugVariableCatalog.variables(ArcadePortGame.CAMERA_SHY), "CAMERA_SHY_PICTURE_PERFECT_PHOTOS_REQUIRED");
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_SPOTLIGHT_INTERVAL_MILLIS", 0.0D, 600_000.0D);
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_DECOY_LOOK_MIN_DOT", 0.0D, 1.0D);
        assertGameRange(ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_FALSE_ALARM_TARGETS_REQUIRED", 1.0D, 1_000.0D);
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_SMOKE_ABILITY_COOLDOWN_MILLIS")));
        assertFalse(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.CAMERA_SHY, "CAMERA_SHY_MATCH_SECONDS")));
    }

    @Test
    void masterBuildersRatingScaleChangesAcceptedVotesAndRestoresSourceScale() {
        assertEquals(6, ArcadePortRules.masterBuildersMaxRating());
        assertEquals(6, ArcadePortRules.masterBuildersRatingScore(org.bukkit.Material.PURPLE_CONCRETE));
        assertGameRange(ArcadePortGame.MASTER_BUILDERS, "MASTER_BUILDERS_MAX_RATING", 1.0D, 6.0D);

        GameDebugRuntimeOverrides.activate(Map.of("MASTER_BUILDERS_MAX_RATING", 3));
        assertEquals(3, ArcadePortRules.masterBuildersMaxRating());
        assertEquals(3, ArcadePortRules.masterBuildersRatingScore(org.bukkit.Material.YELLOW_CONCRETE));
        assertEquals(0, ArcadePortRules.masterBuildersRatingScore(org.bukkit.Material.LIME_CONCRETE));
        assertEquals(0, ArcadePortRules.masterBuildersRatingScore(org.bukkit.Material.PURPLE_CONCRETE));

        GameDebugRuntimeOverrides.clear();
        assertEquals(6, ArcadePortRules.masterBuildersMaxRating());
        assertEquals(6, ArcadePortRules.masterBuildersRatingScore(org.bukkit.Material.PURPLE_CONCRETE));
    }

    private static void assertRange(String key, double minimum, double maximum) {
        assertGameRange(ArcadePortGame.TURF_WARS, key, minimum, maximum);
    }

    private static void assertGameRange(ArcadePortGame game, String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(game, key);
        assertNotNull(variable, key + " must be exposed to the Turf Wars debug menu");
        assertEquals(minimum, variable.minimum());
        assertEquals(maximum, variable.maximum());
    }

    @Test
    void deathTagDamageSlidersChangeNormalChaserDamageCalculations() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_CHASER_DAMAGE", 2.5D,
                "DEATH_TAG_ALPHA_CHASER_DAMAGE", 7.5D));
        assertEquals(2.5D, ArcadePortRules.deathTagChaserDamage(false));
        assertEquals(7.5D, ArcadePortRules.deathTagChaserDamage(true));
    }

    @Test
    void variableMenusOnlyExposeRulesWithRuntimeConsumers() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugVariableCatalog.Variable variable : GameDebugVariableCatalog.variables(game)) {
                assertTrue(GameDebugWiredRuleKeys.KEYS.contains(variable.key())
                                || game.isMonsterMaze() && variable.key().startsWith(GameDebugVariableCatalog.rulePrefix(game))
                                || game == ArcadePortGame.SNEAKY_ASSASSINS
                                        && variable.key().startsWith(GameDebugVariableCatalog.rulePrefix(game))
                                || game == ArcadePortGame.CAMERA_SHY && variable.key().startsWith("CAMERA_SHY_")
                                || game == ArcadePortGame.WIZARDS && (variable.key().startsWith("WIZARDS_SPELL_")
                                        || variable.key().startsWith("WIZARDS_KIT_")),
                        game.id() + " exposes a rule without a runtime consumer: " + variable.key());
            }
        }
        assertTrue(GameDebugVariableCatalog.variables(ArcadePortGame.BACON_BRAWL).stream()
                .anyMatch(variable -> variable.key().equals("BACON_FLASH_FRIED_MAX_WIN_MILLIS")));
        assertTrue(GameDebugVariableCatalog.appliesImmediately(
                GameDebugVariableCatalog.variable(ArcadePortGame.BACON_BRAWL, "BACON_BRAWL_HITS_DAMAGE_HEALTH")));
    }

    @Test
    void superSploorMechanicControlsHaveSafeRangesAndPracticalSteps() {
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_MAX_PAINT_MARKS", 1.0D, 10.0D);
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED", 1.0D, 100.0D);
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_CHARGER_MIN_SPEED", 0.0D, 10.0D);
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_CHARGER_MAX_INK_COST", 0.0D, 500.0D);
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_ROLLER_STAMPEDE_HIT_REACH_BLOCKS", 0.05D, 64.0D);
        assertGameRange(ArcadePortGame.SUPER_SPLOOR, "SUPER_SPLOOR_LOBBER_PAINTSTORM_PULSES", 1.0D, 1_000.0D);
        assertEquals(100.0D, GameDebugVariableCatalog.variable(ArcadePortGame.SUPER_SPLOOR,
                "SUPER_SPLOOR_ROLLER_PRIMARY_INTERVAL_MILLIS").step());
    }

    @Test
    void monsterMazeModesExposeTheirTunableCombatMovementSpawningAndSafePadRules() {
        for (ArcadePortGame game : List.of(ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN)) {
            var variables = GameDebugVariableCatalog.variables(game);
            for (String field : List.of("STAGE_TIMER_INITIAL_SECONDS", "INITIAL_MONSTER_COUNT",
                    "ADDITIONAL_MONSTERS_PER_SAFE_PAD", "MONSTER_HEALTH", "BUMP_DAMAGE",
                    "BUMP_RADIUS_BLOCKS", "PLAYER_BUMP_COOLDOWN_MILLIS", "BODY_RUSH_STRENGTH",
                    "SAFE_PAD_DECAY_STAGES", "SAFE_PAD_CAPTURE_RADIUS_BLOCKS")) {
                assertTrue(variables.stream().anyMatch(variable -> variable.key().equals(
                        GameDebugVariableCatalog.rulePrefix(game) + field)),
                        game.id() + " debug menu is missing " + field);
            }
            var radius = GameDebugVariableCatalog.variable(game,
                    GameDebugVariableCatalog.rulePrefix(game) + "SAFE_PAD_CAPTURE_RADIUS_BLOCKS");
            assertEquals(0.5D, radius.minimum());
            assertEquals(2.0D, radius.maximum());
            assertFalse(GameDebugVariableCatalog.appliesImmediately(radius),
                    "safe pad shape/capture settings apply when the next round builds its pads");
        }
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
                    String key = matcher.group(1);
                    if (key.endsWith("_")) continue; // Explicit dynamic Sneaky Assassins key prefix.
                    assertTrue(GameDebugWiredRuleKeys.KEYS.contains(key),
                            file.getFileName() + " uses an unregistered debug rule: " + key);
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
        Path snakeSources = Path.of("..", "snake-port", "src", "main", "java");
        if (!Files.isDirectory(snakeSources)) {
            snakeSources = Path.of("modern-platform", "snake-port", "src", "main", "java");
        }
        if (Files.isDirectory(snakeSources)) {
            try (var files = Files.walk(snakeSources)) {
                productionSources += "\n" + files.filter(path -> path.toString().endsWith(".java"))
                        .map(path -> {
                            try {
                                return Files.readString(path);
                            } catch (IOException exception) {
                                throw new java.io.UncheckedIOException(exception);
                            }
                        }).collect(Collectors.joining("\n"));
            }
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
                boolean dynamicCameraConsumer = game == ArcadePortGame.CAMERA_SHY
                        && key.startsWith("CAMERA_SHY_")
                        && CameraShyRules.debuggableRule(key.substring("CAMERA_SHY_".length()));
                boolean dynamicSpleefProjectileSpeedConsumer = game == ArcadePortGame.SPLEEF
                        && key.endsWith("_SPEED_MULTIPLIER")
                        && productionSources.contains("spleefProjectileSpeedMultiplier(kit)")
                        && productionSources.contains("\"" + key + "\"");
                boolean dynamicTurfWarsConsumer = game == ArcadePortGame.TURF_WARS
                        && (key.endsWith("_ARROW_INTERVAL_MILLIS") || key.endsWith("_SLOW_INTERVAL_MILLIS")
                                || key.endsWith("_MAX_ARROWS")
                                || key.endsWith("_MAX_WOOL"))
                        && productionSources.contains("GameDebugRuntimeOverrides.longValue(key, fallback)")
                        || game == ArcadePortGame.TURF_WARS
                        && (key.endsWith("_MAX_ARROWS") || key.endsWith("_MAX_WOOL"))
                        && productionSources.contains("GameDebugRuntimeOverrides.intValue(key, fallback)")
                        || game == ArcadePortGame.TURF_WARS
                        && (key.equals("TURF_WARS_BUILD_BOUNDARY_DAMAGE")
                                || key.equals("TURF_WARS_COMBAT_BOUNDARY_DAMAGE"))
                        && productionSources.contains("turfWarsSpawnBoundaryDamage(session.turfCombat)");
                boolean dynamicSkyWarsConsumer = game == ArcadePortGame.SKY_WARS
                        && key.startsWith("SKY_WARS_")
                        && (productionSources.contains("GameDebugRuntimeOverrides.longValue(key, fallback)")
                                || productionSources.contains("GameDebugRuntimeOverrides.intValue(key, fallback)"));
                boolean dynamicEvolutionConsumer = game == ArcadePortGame.EVOLUTION
                        && key.startsWith("EVOLUTION_")
                        && productionSources.contains("GameDebugRuntimeOverrides.doubleValue(key, fallback)");
                boolean dynamicGladiatorConsumer = game == ArcadePortGame.GLADIATORS
                        && ((key.startsWith("GLADIATOR_TIER_") && key.endsWith("_ARROWS")
                                && productionSources.contains("ArcadePortRules.gladiatorsArrowCount(tier)"))
                            || (key.equals("GLADIATOR_MELEE_NO_DAMAGE_TICKS")
                                || key.equals("GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS"))
                                && productionSources.contains("ArcadePortRules.gladiatorsNoDamageTicks(projectile)")
                            || (key.endsWith("KNOCKBACK_HORIZONTAL") || key.endsWith("KNOCKBACK_VERTICAL"))
                                && productionSources.contains("ArcadePortRules.gladiatorsKnockbackHorizontal(projectile)")
                                && productionSources.contains("ArcadePortRules.gladiatorsKnockbackVertical(projectile)")
                            || key.equals("GLADIATOR_POTION_HEALING")
                                && productionSources.contains("ArcadePortRules.gladiatorsPotionHealing()")
                            || key.equals("GLADIATOR_POTION_SPLASH_RADIUS")
                                && productionSources.contains("ArcadePortRules.gladiatorsPotionSplashRadius()"));
                boolean dynamicWizardsConsumer = game == ArcadePortGame.WIZARDS
                        && key.startsWith("WIZARDS_") && key.endsWith("_MULTIPLIER")
                        && productionSources.contains(key)
                        && productionSources.contains("boundedMultiplier(");
                boolean dynamicWizardSpellConsumer = game == ArcadePortGame.WIZARDS
                        && key.startsWith("WIZARDS_SPELL_")
                        && (key.endsWith("_MANA_COST_MULTIPLIER") || key.endsWith("_COOLDOWN_MULTIPLIER"))
                        && productionSources.contains("spellDebugKey(spell")
                        && productionSources.contains("GameDebugRuntimeOverrides.doubleValue(key, intended)");
                boolean dynamicWizardKitConsumer = game == ArcadePortGame.WIZARDS
                        && key.startsWith("WIZARDS_KIT_")
                        && (key.endsWith("_MAX_MANA") || key.endsWith("_MANA_PER_SECOND")
                                || key.endsWith("_STARTING_WANDS"))
                        && productionSources.contains("\"WIZARDS_KIT_\" + kit.name()")
                        && productionSources.contains("GameDebugRuntimeOverrides.floatValue(key, intended)");
                boolean dynamicDeathTagRewardConsumer = game == ArcadePortGame.DEATH_TAG
                        && Set.of("DEATH_TAG_FIRST_PLACE_REWARD_GEMS", "DEATH_TAG_SECOND_PLACE_REWARD_GEMS",
                                "DEATH_TAG_THIRD_PLACE_REWARD_GEMS")
                                .contains(key)
                        && productionSources.contains("ArcadePortRules.deathTagPlacementRewardGems(");
                boolean dynamicMilkCowConsumer = game == ArcadePortGame.MILK_THE_COW
                        && key.startsWith("MILK_THE_COW_")
                        && productionSources.contains("private static String key(String suffix)")
                        && productionSources.contains("return \"MILK_THE_COW_\" + suffix")
                        && productionSources.contains("GameDebugRuntimeOverrides.longValue(key(suffix), fallback)");
                boolean dynamicSnakeConsumer = game == ArcadePortGame.SNAKE
                        && productionSources.contains("\"SNAKE_\" + key")
                        && productionSources.contains("GameDebugRuntimeOverrides.");
                boolean dynamicSuperSploorConsumer = game == ArcadePortGame.SUPER_SPLOOR
                        && key.startsWith("SUPER_SPLOOR_")
                        && productionSources.contains("GameDebugRuntimeOverrides.intValue(\"SUPER_SPLOOR_\" + suffix, fallback)")
                        && productionSources.contains("GameDebugRuntimeOverrides.longValue(\"SUPER_SPLOOR_\" + suffix, fallback)")
                        && productionSources.contains("GameDebugRuntimeOverrides.doubleValue(\"SUPER_SPLOOR_\" + suffix, fallback)")
                        && (productionSources.contains("debugInt(\"") || productionSources.contains("debugLong(\"")
                                || productionSources.contains("debugDouble(\""));
                assertTrue(literalConsumers.contains(key) || dynamicMazeConsumer || dynamicSneakyConsumer
                                || dynamicCameraConsumer || dynamicSpleefProjectileSpeedConsumer
                                || dynamicMilkCowConsumer || dynamicTurfWarsConsumer || dynamicSkyWarsConsumer || dynamicEvolutionConsumer
                                || dynamicGladiatorConsumer || dynamicWizardsConsumer || dynamicWizardSpellConsumer
                                || dynamicWizardKitConsumer
                                || dynamicDeathTagRewardConsumer
                                || dynamicSnakeConsumer || dynamicSuperSploorConsumer,
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
    void documentedLiveSwitchesUpdateTheActiveBooleanRuleWithoutReactivation() {
        GameDebugRuntimeOverrides.activate(Map.of("MICRO_BATTLES_CRUMBLE_ENABLED", true));
        assertTrue(GameDebugRuntimeOverrides.booleanValue("MICRO_BATTLES_CRUMBLE_ENABLED", false));
        GameDebugRuntimeOverrides.setValue("MICRO_BATTLES_CRUMBLE_ENABLED", false);
        assertFalse(GameDebugRuntimeOverrides.booleanValue("MICRO_BATTLES_CRUMBLE_ENABLED", true));
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.MICRO_BATTLES, "MICRO_BATTLES_CRUMBLE_ENABLED")));
        assertFalse(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.MICRO_BATTLES, "MICRO_BATTLES_BARRIER_DURATION_MILLIS")));
    }

    @Test
    void removingAnImmediateOverrideRestoresItsConsumerFallback() {
        GameDebugRuntimeOverrides.activate(Map.of("ability-crispy", false));
        assertFalse(GameDebugRuntimeOverrides.booleanValue("ability-crispy", true));

        GameDebugRuntimeOverrides.removeValue("ability-crispy");

        assertTrue(GameDebugRuntimeOverrides.booleanValue("ability-crispy", true));
        assertFalse(GameDebugRuntimeOverrides.booleanValue("ability-crispy", false));
    }

    @Test
    void monsterMazeKitAbilitySwitchesAreLiveAndDefaultToTheSourceBehavior() {
        java.util.UUID owner = java.util.UUID.randomUUID();
        java.util.UUID otherPlayer = java.util.UUID.randomUUID();
        GameDebugRuntimeOverrides.activate(Map.of(
                "kit-jumper", false,
                "kit-slowball", false,
                "kit-body-builder", false,
                "kit-repulsor", false,
                "kit-maverick", false), owner);
        assertFalse(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "jumper"));
        assertFalse(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "slowball"));
        assertFalse(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "body-builder"));
        assertFalse(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "repulsor"));
        assertFalse(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "maverick"));
        assertTrue(me.monstermaze.game.MazeDebugRules.kitEnabled(otherPlayer, "jumper"),
                "a tester profile must not alter another participant's kit");

        GameDebugRuntimeOverrides.setValue("kit-jumper", true);
        assertTrue(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "jumper"));

        GameDebugRuntimeOverrides.clear();
        assertTrue(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "jumper"));
        assertTrue(me.monstermaze.game.MazeDebugRules.kitEnabled(owner, "maverick"));
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
        GameDebugVariableCatalog.Variable perfect = GameDebugVariableCatalog.variable(
                ArcadePortGame.SPEED_BUILDERS, "SPEED_BUILDERS_PERFECT_SCORE_PERCENT");
        assertNotNull(perfect);
        assertEquals(1.0D, perfect.minimum());
        assertEquals(100.0D, perfect.maximum());
        assertGameRange(ArcadePortGame.SPEED_BUILDERS,
                "SPEED_BUILDERS_BUILD_TIME_DECREASE_PER_ROUND_SECONDS", 0.0D, 30.0D);
        assertGameRange(ArcadePortGame.SPEED_BUILDERS, "SPEED_BUILDERS_FINAL_RESULTS_SECONDS", 0.0D, 60.0D);
        assertGameRange(ArcadePortGame.SPEED_BUILDERS,
                "SPEED_BUILDERS_DOUBLE_JUMP_VERTICAL_VELOCITY", 0.1D, 2.5D);
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.SPEED_BUILDERS,
                "SPEED_BUILDERS_PERFECT_BUILD_TIME_TIEBREAK_ENABLED"));
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SPEED_BUILDERS, "SPEED_BUILDERS_DOUBLE_JUMP_ENABLED")));
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SPEED_BUILDERS, "SPEED_BUILDERS_PERFECT_BUILD_TIME_TIEBREAK_ENABLED")));
        assertFalse(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SPEED_BUILDERS, "SPEED_BUILDERS_BUILD_TIME_DECREASE_PER_ROUND_SECONDS")));
    }

    @Test
    void masterBuildersDebugProfileChangesBuildAndVoteTimers() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "MASTER_BUILDERS_TOTAL_MILLIS", 180_000L,
                "MASTER_BUILDERS_REVIEW_MILLIS", 2_000L,
                "MASTER_BUILDERS_VOTE_MILLIS", 1_200L));

        long buildMillis = ArcadeProgressRules.masterBuildersBuildMillis(
                GameDebugRuntimeOverrides.longValue("MASTER_BUILDERS_TOTAL_MILLIS",
                        ArcadePortRules.MASTER_BUILDERS_TOTAL_MILLIS), 6,
                GameDebugRuntimeOverrides.longValue("MASTER_BUILDERS_REVIEW_MILLIS",
                        ArcadePortRules.MASTER_BUILDERS_REVIEW_MILLIS));

        assertEquals(168_000L, buildMillis);
        assertEquals(1_200L, GameDebugRuntimeOverrides.longValue("MASTER_BUILDERS_VOTE_MILLIS",
                ArcadePortRules.MASTER_BUILDERS_VOTE_MILLIS));
    }

    @Test
    void plexballDebugValuesChangePlayerValidationAndFieldRules() {
        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("PLEXBALL_MIN_PLAYERS", 4),
                Map.entry("PLEXBALL_MAX_PLAYERS", 8),
                Map.entry("PLEXBALL_FAIR_ANGLE_DEGREES", 10.0D),
                Map.entry("PLEXBALL_INTRO_TICKS", 5),
                Map.entry("PLEXBALL_TURN_BREAK_TICKS", 3),
                Map.entry("PLEXBALL_TIMING_CYCLE_MIN_TICKS", 8),
                Map.entry("PLEXBALL_TIMING_CYCLE_MAX_TICKS", 12),
                Map.entry("PLEXBALL_TECHNICIAN_TIMING_BONUS_TICKS", 4),
                Map.entry("PLEXBALL_SLUGGER_BATTING_POWER", 1.5D),
                Map.entry("PLEXBALL_BATTING_BASE_HORIZONTAL_SPEED", 4.0D),
                Map.entry("PLEXBALL_TIMING_POWER_BASE", 0.5D),
                Map.entry("PLEXBALL_TIMING_PERFECT_MAX_TICKS", 0),
                Map.entry("PLEXBALL_TIMING_POOR_POWER", 0.1D)));

        assertFalse(PlexballRules.hasValidPlayerCount(2));
        assertTrue(PlexballRules.hasValidPlayerCount(6));
        assertFalse(PlexballRules.hasValidPlayerCount(9));
        assertFalse(PlexballRules.isFairYaw(160.0D));
        assertEquals(5, PlexballRules.introTicks());
        assertEquals(3, PlexballRules.turnBreakTicks());
        assertEquals(8, PlexballRules.timingCycleMinTicks());
        assertEquals(12, PlexballRules.timingCycleMaxTicks());
        assertEquals(4, PlexballRules.technicianTimingBonusTicks());
        assertEquals(1.5D, PlexballRules.kitBattingPower("slugger", PlexballRules.SLUGGER_BATTING_POWER));
        assertEquals(4.0D, PlexballRules.battingBaseHorizontalSpeed());
        assertEquals(0.5D, PlexballRules.timingPowerBase());
        assertEquals(0.1D, PlexballRules.timingMultiplier(100));
        assertEquals(1.0D, PlexballRules.timingMultiplier(0));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(PlexballRules.INTRO_TICKS, PlexballRules.introTicks());
        assertEquals(PlexballRules.TURN_BREAK_TICKS, PlexballRules.turnBreakTicks());
        assertEquals(PlexballRules.TIMING_CYCLE_MIN_TICKS, PlexballRules.timingCycleMinTicks());
        assertEquals(PlexballRules.SLUGGER_BATTING_POWER,
                PlexballRules.kitBattingPower("slugger", PlexballRules.SLUGGER_BATTING_POWER));
        assertEquals(PlexballRules.TIMING_PERFECT_POWER, PlexballRules.timingMultiplier(0));
        assertEquals(PlexballRules.TIMING_POOR_POWER, PlexballRules.timingMultiplier(100));
    }

    @Test
    void plexballDebugVariablesExposeBoundedPhysicsAndPhaseControls() {
        for (String key : List.of("PLEXBALL_INTRO_TICKS", "PLEXBALL_TURN_BREAK_TICKS",
                "PLEXBALL_TIMING_CYCLE_MIN_TICKS", "PLEXBALL_TIMING_CYCLE_MAX_TICKS",
                "PLEXBALL_TECHNICIAN_TIMING_BONUS_TICKS", "PLEXBALL_SLUGGER_BATTING_POWER",
                "PLEXBALL_TECHNICIAN_BATTING_POWER", "PLEXBALL_CURVE_ARTIST_BATTING_POWER",
                "PLEXBALL_SPRINTER_BATTING_POWER", "PLEXBALL_SKYGUARD_BATTING_POWER",
                "PLEXBALL_BATTING_BASE_HORIZONTAL_SPEED", "PLEXBALL_BATTING_CONTACT_HORIZONTAL_SCALE",
                "PLEXBALL_BATTING_BASE_VERTICAL_SPEED", "PLEXBALL_BATTING_CONTACT_VERTICAL_SCALE",
                "PLEXBALL_TIMING_POWER_BASE", "PLEXBALL_TIMING_POWER_SCALE",
                "PLEXBALL_TIMING_PERFECT_MAX_TICKS", "PLEXBALL_TIMING_GREAT_MAX_TICKS",
                "PLEXBALL_TIMING_GOOD_MAX_TICKS", "PLEXBALL_TIMING_FAIR_MAX_TICKS",
                "PLEXBALL_TIMING_WEAK_MAX_TICKS", "PLEXBALL_TIMING_PERFECT_POWER",
                "PLEXBALL_TIMING_GREAT_POWER", "PLEXBALL_TIMING_GOOD_POWER",
                "PLEXBALL_TIMING_FAIR_POWER", "PLEXBALL_TIMING_WEAK_POWER", "PLEXBALL_TIMING_POOR_POWER")) {
            GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.PLEXBALL, key);
            assertNotNull(variable, key + " appears in the game's sorted debug controls");
            assertTrue(variable.maximum() > variable.minimum(), key + " has a useful bounded range");
        }
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
    void wizardSpellDebugMultipliersHaveBoundedRangesAndUsefulSteps() {
        for (String key : List.of("WIZARDS_MANA_REGEN_MULTIPLIER", "WIZARDS_SPELL_MANA_COST_MULTIPLIER",
                "WIZARDS_SPELL_COOLDOWN_MULTIPLIER", "WIZARDS_SPELL_DAMAGE_MULTIPLIER",
                "WIZARDS_SPELL_HEAL_MULTIPLIER", "WIZARDS_PROJECTILE_SPEED_MULTIPLIER")) {
            GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.WIZARDS, key);
            assertNotNull(variable, key);
            assertEquals(0.0D, variable.minimum(), key);
            assertEquals(5.0D, variable.maximum(), key);
            assertEquals(0.1D, variable.step(), key);
        }
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.WIZARDS, "WIZARDS_KIT_MAGE_MAX_MANA"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.WIZARDS, "WIZARDS_KIT_MAGE_MANA_PER_SECOND"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.WIZARDS, "WIZARDS_KIT_MAGE_STARTING_WANDS"));
        for (String key : List.of("WIZARDS_SPELL_FIREBALL_MANA_COST_MULTIPLIER",
                "WIZARDS_SPELL_FIREBALL_COOLDOWN_MULTIPLIER")) {
            GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.WIZARDS, key);
            assertNotNull(variable, key + " is an individually tunable spell control");
            assertEquals(0.0D, variable.minimum(), key);
            assertEquals(5.0D, variable.maximum(), key);
            assertEquals(0.1D, variable.step(), key);
            assertTrue(GameDebugVariableCatalog.appliesImmediately(variable), key + " applies to the next cast");
        }
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

    @Test
    void monsterMazeKitVariablesShowModeCorrectDefaultsAndValidatedRanges() {
        GameDebugVariableCatalog.Variable originalJumps = GameDebugVariableCatalog.variable(
                ArcadePortGame.MONSTER_MAZE, "MONSTER_MAZE_JUMPER_MAX_JUMPS");
        GameDebugVariableCatalog.Variable speedJumps = GameDebugVariableCatalog.variable(
                ArcadePortGame.MONSTER_MAZE_SPEED, "MONSTER_MAZE_SPEED_JUMPER_MAX_JUMPS");
        GameDebugVariableCatalog.Variable modernJumps = GameDebugVariableCatalog.variable(
                ArcadePortGame.MONSTER_MAZE_MODERN, "MONSTER_MAZE_MODERN_JUMPER_MAX_JUMPS");
        GameDebugVariableCatalog.Variable modernSlow = GameDebugVariableCatalog.variable(
                ArcadePortGame.MONSTER_MAZE_MODERN, "MONSTER_MAZE_MODERN_SLOWBALL_SLOW_TICKS");
        GameDebugVariableCatalog.Variable modernRepulsor = GameDebugVariableCatalog.variable(
                ArcadePortGame.MONSTER_MAZE_MODERN, "MONSTER_MAZE_MODERN_REPULSOR_RADIUS_BLOCKS");
        assertNotNull(originalJumps);
        assertNotNull(speedJumps);
        assertNotNull(modernJumps);
        assertNotNull(modernSlow);
        assertNotNull(modernRepulsor);
        assertEquals(5.0D, originalJumps.defaultValue());
        assertEquals(3.0D, speedJumps.defaultValue());
        assertEquals(0.0D, modernJumps.minimum());
        assertEquals(1_200.0D, modernSlow.maximum());
        assertEquals(0.1D, modernRepulsor.minimum());
        assertEquals(64.0D, modernRepulsor.maximum());
    }

}
