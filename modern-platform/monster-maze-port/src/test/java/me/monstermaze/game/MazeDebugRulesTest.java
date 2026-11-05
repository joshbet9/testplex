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
}
