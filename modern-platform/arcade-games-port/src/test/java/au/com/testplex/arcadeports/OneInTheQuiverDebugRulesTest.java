package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OneInTheQuiverDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void kitDamageHealingAndProgressionThresholdsAreTunable() {
        assertEquals(7.0D, ArcadePortRules.quiverMeleeDamage("brawler", true));
        assertEquals(2.0D, ArcadePortRules.quiverMeleeDamage("brawler", false));
        assertEquals(5.5D, ArcadePortRules.quiverMeleeDamage("slam-shot", true));
        assertEquals(4.0D, ArcadePortRules.quiverMeleeDamage("ninja", true));
        assertEquals(10.0D, ArcadePortRules.quiverBrawlerHealthAfterKill(8.0D, 20.0D));
        assertTrue(ArcadePortRules.isQuiverPerfectRun(20));
        assertTrue(ArcadePortRules.isQuiverSpeedrunnerWin(89_999L));
        assertTrue(ArcadePortRules.shouldAwardQuiverFistPunchKills(3));

        GameDebugRuntimeOverrides.activate(Map.of(
                "QUIVER_BRAWLER_WEAPON_DAMAGE", 9.0D,
                "QUIVER_BRAWLER_PUNCH_DAMAGE", 4.0D,
                "QUIVER_SLAM_SHOT_WEAPON_DAMAGE", 8.0D,
                "QUIVER_NINJA_WEAPON_DAMAGE", 6.0D,
                "QUIVER_BRAWLER_HEALTH_PER_KILL", 5.0D,
                "QUIVER_PERFECT_RUN_KILL_STREAK", 12D,
                "QUIVER_SPEEDRUNNER_MAX_WIN_MILLIS", 30_000D,
                "QUIVER_FIST_PUNCH_ACHIEVEMENT_KILLS", 5D));
        assertEquals(9.0D, ArcadePortRules.quiverMeleeDamage("brawler", true));
        assertEquals(4.0D, ArcadePortRules.quiverMeleeDamage("brawler", false));
        assertEquals(8.0D, ArcadePortRules.quiverMeleeDamage("slam-shot", true));
        assertEquals(6.0D, ArcadePortRules.quiverMeleeDamage("ninja", true));
        assertEquals(13.0D, ArcadePortRules.quiverBrawlerHealthAfterKill(8.0D, 20.0D));
        assertTrue(ArcadePortRules.isQuiverPerfectRun(12));
        assertEquals("PERFECT RUN", ArcadePortRules.quiverKillstreakTitle(12));
        assertFalse(ArcadePortRules.isQuiverSpeedrunnerWin(30_000L));
        assertTrue(ArcadePortRules.shouldAwardQuiverFistPunchKills(5));

        GameDebugRuntimeOverrides.clear();
        assertEquals(7.0D, ArcadePortRules.quiverMeleeDamage("brawler", true));
        assertEquals(10.0D, ArcadePortRules.quiverBrawlerHealthAfterKill(8.0D, 20.0D));
        assertFalse(ArcadePortRules.isQuiverPerfectRun(12));
        assertTrue(ArcadePortRules.isQuiverSpeedrunnerWin(89_999L));
    }

    @Test
    void leaperGroundPoundAndReboundPhysicsRespondToProfileOverrides() {
        Vector direction = new Vector(1.0D, 0.0D, 0.0D);
        Vector defaultLeap = ArcadePortRules.quiverLeaperJumpVelocity(direction);
        assertEquals(0.9D, defaultLeap.getX(), 1.0e-9);
        assertEquals(0.55D, defaultLeap.getY(), 1.0e-9);
        Vector defaultPound = ArcadePortRules.quiverGroundPoundVelocity(direction, 0.0D);
        assertEquals(0.8D, defaultPound.getX(), 1.0e-9);
        assertEquals(1.8D, defaultPound.getY(), 1.0e-9);
        assertEquals(1.2D, ArcadePortRules.quiverReboundPower(30.0D), 1.0e-9);
        assertEquals(0.3D, ArcadePortRules.quiverReboundVerticalArc(30.0D), 1.0e-9);
        assertTrue(ArcadePortRules.isQuiverReboundImpactInRange(1.0D, 2.0D));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("QUIVER_LEAPER_JUMP_HORIZONTAL", 1.2D),
                Map.entry("QUIVER_LEAPER_JUMP_VERTICAL_ADD", 0.7D),
                Map.entry("QUIVER_LEAPER_JUMP_VERTICAL_MIN", 0.7D),
                Map.entry("QUIVER_LEAPER_JUMP_VERTICAL_MAX", 1.5D),
                Map.entry("QUIVER_GROUND_POUND_RADIUS_BLOCKS", 4.0D),
                Map.entry("QUIVER_GROUND_POUND_HORIZONTAL_STRENGTH", 0.4D),
                Map.entry("QUIVER_GROUND_POUND_VERTICAL_BASE", 0.3D),
                Map.entry("QUIVER_GROUND_POUND_VERTICAL_PROXIMITY", 0.5D),
                Map.entry("QUIVER_REBOUND_BASE_POWER", 0.5D),
                Map.entry("QUIVER_REBOUND_POWER_PER_BLOCK", 0.05D),
                Map.entry("QUIVER_REBOUND_MAX_POWER", 2.0D),
                Map.entry("QUIVER_REBOUND_VERTICAL_ARC_PER_BLOCK", 0.02D),
                Map.entry("QUIVER_REBOUND_MAX_IMPACT_DISTANCE_SQUARED", 4.0D)));
        Vector changedLeap = ArcadePortRules.quiverLeaperJumpVelocity(direction);
        assertEquals(1.2D, changedLeap.getX(), 1.0e-9);
        assertEquals(0.7D, changedLeap.getY(), 1.0e-9);
        assertEquals(4.0D, ArcadePortRules.quiverGroundPoundRadiusBlocks());
        Vector changedPound = ArcadePortRules.quiverGroundPoundVelocity(direction, 0.0D);
        assertEquals(0.4D, changedPound.getX(), 1.0e-9);
        assertEquals(0.8D, changedPound.getY(), 1.0e-9);
        assertEquals(2.0D, ArcadePortRules.quiverReboundPower(30.0D), 1.0e-9);
        assertEquals(0.6D, ArcadePortRules.quiverReboundVerticalArc(30.0D), 1.0e-9);
        assertFalse(ArcadePortRules.isQuiverReboundImpactInRange(5.0D, 6.0D));

        GameDebugRuntimeOverrides.clear();
        assertEquals(0.9D, ArcadePortRules.quiverLeaperJumpVelocity(direction).getX(), 1.0e-9);
        assertEquals(8.0D, ArcadePortRules.quiverGroundPoundRadiusBlocks());
        assertEquals(1.2D, ArcadePortRules.quiverReboundPower(30.0D), 1.0e-9);
        assertTrue(ArcadePortRules.isQuiverReboundImpactInRange(1.0D, 2.0D));
    }
}
