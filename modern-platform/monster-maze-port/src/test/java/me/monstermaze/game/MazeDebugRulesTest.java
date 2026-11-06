package me.monstermaze.game;

import static org.junit.jupiter.api.Assertions.*;

import au.com.testplex.arcadeports.GameDebugRuntimeOverrides;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MazeDebugRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void eachMazeModeRetainsItsStageAndMonsterDefaultsAndCanBeTunedIndependently() {
        assertEquals(60, MazeMode.ORIGINAL.stageTimerSeconds(1));
        assertEquals(58, MazeMode.SPEED.stageTimerSeconds(2));
        assertEquals(33, MazeMode.MODERN.stageTimerSeconds(2));
        assertEquals(15, MazeMode.MODERN.stageTimerSeconds(10));
        assertEquals(150, MazeMode.ORIGINAL.initialMonsterCount());
        assertEquals(225, MazeMode.MODERN.initialMonsterCount());
        assertEquals(15, MazeMode.SPEED.additionalMonstersPerSafePad());
        assertEquals(30, MazeMode.MODERN.additionalMonstersPerSafePad());

        GameDebugRuntimeOverrides.activate(Map.of(
                "MONSTER_MAZE_MODERN_STAGE_TIMER_INITIAL_SECONDS", 50,
                "MONSTER_MAZE_MODERN_STAGE_TIMER_DECREASE_PER_STAGE_SECONDS", 5.0D,
                "MONSTER_MAZE_MODERN_STAGE_TIMER_MIN_SECONDS", 20,
                "MONSTER_MAZE_MODERN_INITIAL_MONSTER_COUNT", 90,
                "MONSTER_MAZE_MODERN_ADDITIONAL_MONSTERS_PER_SAFE_PAD", 6));
        assertEquals(50, MazeMode.MODERN.stageTimerSeconds(1));
        assertEquals(45, MazeMode.MODERN.stageTimerSeconds(2));
        assertEquals(20, MazeMode.MODERN.stageTimerSeconds(20));
        assertEquals(90, MazeMode.MODERN.initialMonsterCount());
        assertEquals(6, MazeMode.MODERN.additionalMonstersPerSafePad());
        assertEquals(60, MazeMode.ORIGINAL.stageTimerSeconds(1), "mode profile values must not leak");

        GameDebugRuntimeOverrides.clear();
        assertEquals(35, MazeMode.MODERN.stageTimerSeconds(1));
        assertEquals(225, MazeMode.MODERN.initialMonsterCount());
    }

    @Test void bumpRangeDamageAndCooldownSettingsChangeProductionPredicates() {
        assertTrue(MazeDebugRules.isBumpHitWithinRange(MazeMode.ORIGINAL, 0.9D, 0.9D));
        assertFalse(MazeDebugRules.isBumpHitWithinRange(MazeMode.ORIGINAL, 1.0D, 0.5D));
        assertEquals(4.0D, MazeDebugRules.bumpDamage(MazeMode.ORIGINAL));
        assertFalse(MazeDebugRules.isBumpCooldownReady(MazeMode.ORIGINAL, 1_999L, 1_000L));
        assertTrue(MazeDebugRules.isBumpCooldownReady(MazeMode.ORIGINAL, 2_000L, 1_000L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "MONSTER_MAZE_BUMP_RADIUS_BLOCKS", 1.5D,
                "MONSTER_MAZE_BUMP_DAMAGE", 7.0D,
                "MONSTER_MAZE_PLAYER_BUMP_COOLDOWN_MILLIS", 200L));
        assertTrue(MazeDebugRules.isBumpHitWithinRange(MazeMode.ORIGINAL, 1.5D, 1.8D));
        assertEquals(7.0D, MazeDebugRules.bumpDamage(MazeMode.ORIGINAL));
        assertTrue(MazeDebugRules.isBumpCooldownReady(MazeMode.ORIGINAL, 1_200L, 1_000L));
        assertEquals(4.0D, MazeDebugRules.bumpDamage(MazeMode.SPEED), "mode-specific values must remain isolated");

        GameDebugRuntimeOverrides.clear();
        assertFalse(MazeDebugRules.isBumpHitWithinRange(MazeMode.ORIGINAL, 1.5D, 1.8D));
        assertEquals(4.0D, MazeDebugRules.bumpDamage(MazeMode.ORIGINAL));
        assertFalse(MazeDebugRules.isBumpCooldownReady(MazeMode.ORIGINAL, 1_200L, 1_000L));
    }

    @Test void mazeModeProfilesExposeTheNewGameplayControls() {
        assertEquals("MONSTER_MAZE_SAFE_PAD_DECAY_STAGES", MazeDebugRules.key(MazeMode.ORIGINAL, "SAFE_PAD_DECAY_STAGES"));
        assertEquals("MONSTER_MAZE_SPEED_BUMP_DAMAGE", MazeDebugRules.key(MazeMode.SPEED, "BUMP_DAMAGE"));
        assertEquals("MONSTER_MAZE_MODERN_MONSTER_HEALTH", MazeDebugRules.key(MazeMode.MODERN, "MONSTER_HEALTH"));
        assertEquals(11, MazeDebugRules.SAFE_PAD_DECAY_STAGES);
        assertEquals(2.0D, MazeDebugRules.SAFE_PAD_CAPTURE_RADIUS_BLOCKS);
    }

    @Test void kitMechanicsHaveModeScopedTuningAndRestoreSourceDefaults() {
        assertEquals(5, MazeDebugRules.jumperMaxJumps(MazeMode.ORIGINAL));
        assertEquals(3, MazeDebugRules.jumperMaxJumps(MazeMode.SPEED));
        assertEquals(750L, MazeDebugRules.jumperCooldownMillis(MazeMode.ORIGINAL));
        assertEquals(16, MazeDebugRules.slowballMaxSnowballs(MazeMode.ORIGINAL));
        assertEquals(2_000L, MazeDebugRules.slowballRegenIntervalMillis(MazeMode.ORIGINAL));
        assertEquals(40, MazeDebugRules.slowballSlowTicks(MazeMode.ORIGINAL));
        assertEquals(1, MazeDebugRules.slowballSlowAmplifier(MazeMode.ORIGINAL));
        assertEquals(22.0D, MazeDebugRules.bodyBuilderMaxHealthAfterPad(MazeMode.MODERN, 20.0D));
        assertEquals(30.0D, MazeDebugRules.bodyBuilderMaxHealthAfterPad(MazeMode.MODERN, 29.0D));
        assertEquals(10_000L, MazeDebugRules.bodyRushUntilAfterContact(MazeMode.MODERN, 12_000L));
        assertFalse(MazeDebugRules.shouldAwardBodyRushAchievement(MazeMode.MODERN, 9));
        assertTrue(MazeDebugRules.shouldAwardBodyRushAchievement(MazeMode.MODERN, 10));
        assertEquals(6.0D, MazeDebugRules.repulsorRadiusBlocks(MazeMode.MODERN));
        assertEquals(1.0D, MazeDebugRules.repulsorLaunchStrength(MazeMode.MODERN));
        assertEquals(0.8D, MazeDebugRules.repulsorLaunchYAdd(MazeMode.MODERN));
        assertEquals(2.0D, MazeDebugRules.repulsorLaunchYMax(MazeMode.MODERN));
        assertEquals(500L, MazeDebugRules.repulsorGroundCleanupDelayMillis(MazeMode.MODERN));
        assertEquals(1_500L, MazeDebugRules.repulsorMaxLifetimeMillis(MazeMode.MODERN));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("MONSTER_MAZE_MODERN_JUMPER_MAX_JUMPS", 7),
                Map.entry("MONSTER_MAZE_MODERN_JUMPER_JUMP_COOLDOWN_MILLIS", 100L),
                Map.entry("MONSTER_MAZE_MODERN_SLOWBALL_MAX_SNOWBALLS", 8),
                Map.entry("MONSTER_MAZE_MODERN_SLOWBALL_REGEN_INTERVAL_MILLIS", 500L),
                Map.entry("MONSTER_MAZE_MODERN_SLOWBALL_SLOW_TICKS", 80),
                Map.entry("MONSTER_MAZE_MODERN_SLOWBALL_SLOW_AMPLIFIER", 2),
                Map.entry("MONSTER_MAZE_MODERN_BODY_BUILDER_HEALTH_PER_PAD", 4.0D),
                Map.entry("MONSTER_MAZE_MODERN_BODY_BUILDER_MAX_HEALTH", 40.0D),
                Map.entry("MONSTER_MAZE_MODERN_BODY_RUSH_DURATION_REDUCTION_MILLIS", 500L),
                Map.entry("MONSTER_MAZE_MODERN_BODY_RUSH_ACHIEVEMENT_CONTACTS", 3),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_RADIUS_BLOCKS", 10.0D),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_LAUNCH_STRENGTH", 2.0D),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_LAUNCH_Y_ADD", 1.5D),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_LAUNCH_Y_MAX", 3.0D),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_GROUND_CLEANUP_DELAY_MILLIS", 100L),
                Map.entry("MONSTER_MAZE_MODERN_REPULSOR_MAX_LIFETIME_MILLIS", 3_000L)));

        assertEquals(7, MazeDebugRules.jumperMaxJumps(MazeMode.MODERN));
        assertEquals(5, MazeDebugRules.jumperMaxJumps(MazeMode.ORIGINAL), "mode-specific profiles do not leak");
        assertEquals(8, MazeDebugRules.slowballMaxSnowballs(MazeMode.MODERN));
        assertEquals(80, MazeDebugRules.slowballSlowTicks(MazeMode.MODERN));
        assertEquals(2, MazeDebugRules.slowballSlowAmplifier(MazeMode.MODERN));
        assertEquals(24.0D, MazeDebugRules.bodyBuilderMaxHealthAfterPad(MazeMode.MODERN, 20.0D));
        assertEquals(40.0D, MazeDebugRules.bodyBuilderMaxHealthAfterPad(MazeMode.MODERN, 39.0D));
        assertEquals(11_500L, MazeDebugRules.bodyRushUntilAfterContact(MazeMode.MODERN, 12_000L));
        assertTrue(MazeDebugRules.shouldAwardBodyRushAchievement(MazeMode.MODERN, 3));
        assertEquals(10.0D, MazeDebugRules.repulsorRadiusBlocks(MazeMode.MODERN));
        assertEquals(2.0D, MazeDebugRules.repulsorLaunchStrength(MazeMode.MODERN));

        GameDebugRuntimeOverrides.clear();
        assertEquals(3, MazeDebugRules.jumperMaxJumps(MazeMode.MODERN));
        assertEquals(16, MazeDebugRules.slowballMaxSnowballs(MazeMode.MODERN));
        assertEquals(22.0D, MazeDebugRules.bodyBuilderMaxHealthAfterPad(MazeMode.MODERN, 20.0D));
        assertEquals(6.0D, MazeDebugRules.repulsorRadiusBlocks(MazeMode.MODERN));
    }
}
