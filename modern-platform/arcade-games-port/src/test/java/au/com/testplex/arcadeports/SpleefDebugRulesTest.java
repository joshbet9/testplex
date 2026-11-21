package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpleefDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void brawlerSmasherRadiusPreservesTheSixFaceDefaultAndChangesItsReach() {
        assertEquals(Set.of("1,0,0", "-1,0,0", "0,1,0", "0,-1,0", "0,0,1", "0,0,-1"),
                offsetSet());

        GameDebugRuntimeOverrides.activate(Map.of("SPLEEF_SMASHER_RADIUS_BLOCKS", 2));
        Set<String> expanded = offsetSet();
        assertEquals(12, expanded.size());
        assertTrue(expanded.contains("2,0,0"));
        assertTrue(expanded.contains("0,-2,0"));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(6, offsetSet().size());
        assertEquals(Set.of("1,0,0", "-1,0,0", "0,1,0", "0,-1,0", "0,0,1", "0,0,-1"),
                offsetSet());
    }

    @Test
    void smasherReachIsAvailableAsABoundedSpleefProfileSetting() {
        GameDebugVariableCatalog.Variable radius = GameDebugVariableCatalog.variable(ArcadePortGame.SPLEEF,
                "SPLEEF_SMASHER_RADIUS_BLOCKS");
        assertNotNull(radius);
        assertEquals(0.0D, radius.minimum());
        assertEquals(8.0D, radius.maximum());
        assertEquals(1.0D, radius.defaultValue());
    }

    @Test
    void profileControlsTuneLeapKnockbackHungerProjectilesAndResetToSourceDefaults() {
        org.bukkit.util.Vector facing = new org.bukkit.util.Vector(1, 0, 0);
        assertEquals(1.2D, ArcadePortRules.spleefLeapVelocity(facing, false).getX(), 1.0E-6D);
        assertEquals(0.3D, ArcadePortRules.spleefKnockbackVelocity(facing, true, 0.3D).getY(), 1.0E-6D);
        assertEquals(11, ArcadePortRules.spleefHungerAfterHit(10));
        assertEquals(2, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(1.0D, ArcadePortRules.spleefProjectileSpeedMultiplier("archer"));
        assertEquals(1, ArcadePortRules.spleefProjectileImpactSearchRadius());

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SPLEEF_LEAP_FORWARD_POWER", 2.0D),
                Map.entry("SPLEEF_LEAP_VERTICAL_ADD", 0.6D),
                Map.entry("SPLEEF_LEAP_GROUND_BONUS", 0.4D),
                Map.entry("SPLEEF_KNOCKBACK_VERTICAL_BASE", 0.5D),
                Map.entry("SPLEEF_KNOCKBACK_GROUND_BONUS", 0.5D),
                Map.entry("SPLEEF_HUNGER_RESTORE_PER_HIT", 4),
                Map.entry("SPLEEF_ARROW_CLEAR_RADIUS_BASE_BLOCKS", 0.0D),
                Map.entry("SPLEEF_ARROW_CLEAR_RADIUS_SPEED_DIVISOR", 10.0D),
                Map.entry("SPLEEF_ARROW_CLEAR_RADIUS_CAP_BLOCKS", 1.0D),
                Map.entry("SPLEEF_PROJECTILE_CLEAR_RADIUS_MIN_BLOCKS", 0),
                Map.entry("SPLEEF_PROJECTILE_CLEAR_RADIUS_MAX_BLOCKS", 0),
                Map.entry("SPLEEF_PROJECTILE_IMPACT_SEARCH_RADIUS_BLOCKS", 2),
                Map.entry("SPLEEF_ARCHER_ARROW_SPEED_MULTIPLIER", 2.0D)));
        org.bukkit.util.Vector tunedLeap = ArcadePortRules.spleefLeapVelocity(facing, true);
        assertEquals(2.0D, tunedLeap.getX(), 1.0E-6D);
        assertEquals(1.0D, tunedLeap.getY(), 1.0E-6D);
        assertEquals(1.0D, ArcadePortRules.spleefKnockbackVelocity(facing, true, 0.3D).getY(), 1.0E-6D);
        assertEquals(14, ArcadePortRules.spleefHungerAfterHit(10));
        assertEquals(0, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(2.0D, ArcadePortRules.spleefProjectileSpeedMultiplier("archer"));
        assertEquals(2, ArcadePortRules.spleefProjectileImpactSearchRadius());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(1.2D, ArcadePortRules.spleefLeapVelocity(facing, false).getX(), 1.0E-6D);
        assertEquals(0.3D, ArcadePortRules.spleefKnockbackVelocity(facing, true, 0.3D).getY(), 1.0E-6D);
        assertEquals(11, ArcadePortRules.spleefHungerAfterHit(10));
        assertEquals(2, ArcadePortRules.spleefProjectileClearRadius(true, 1.6D));
        assertEquals(1.0D, ArcadePortRules.spleefProjectileSpeedMultiplier("archer"));
        assertEquals(1, ArcadePortRules.spleefProjectileImpactSearchRadius());
    }

    @Test
    void gustLaunchHasAnUpwardRecoveryImpulseAndArrowsOnlyRemoveTilesActuallyInTheirImpactRadius() {
        assertEquals(4.0D, ArcadePortRules.spleefGustRadiusBlocks());
        org.bukkit.util.Vector selfImpulse = ArcadePortRules.spleefGustVelocity(new org.bukkit.util.Vector());
        assertEquals(0.0D, selfImpulse.getX(), 1.0E-6D);
        assertEquals(ArcadePortRules.SPLEEF_GUST_POWER, selfImpulse.getY(), 1.0E-6D);
        org.bukkit.util.Vector nearbyImpulse = ArcadePortRules.spleefGustVelocity(
                new org.bukkit.util.Vector(2.0D, 0.0D, 0.0D));
        assertTrue(nearbyImpulse.getX() > 0.0D);
        assertTrue(nearbyImpulse.getY() > 0.0D);
        assertEquals(0.0D, ArcadePortRules.spleefGustVelocity(
                new org.bukkit.util.Vector(ArcadePortRules.SPLEEF_GUST_RADIUS_BLOCKS, 0.0D, 0.0D))
                .lengthSquared(), 1.0E-6D);

        assertTrue(ArcadePortRules.spleefArrowClearsOffset(0, 0, 0, 1.5D));
        assertTrue(ArcadePortRules.spleefArrowClearsOffset(1, 0, 0, 1.5D));
        assertFalse(ArcadePortRules.spleefArrowClearsOffset(2, 0, 0, 1.5D));
        assertTrue(ArcadePortRules.spleefArrowBreaksPlayerFloor(0, 63, 0, 1, 64, 0, 1.5D));
        assertFalse(ArcadePortRules.spleefArrowBreaksPlayerFloor(0, 63, 0, 2, 64, 0, 1.5D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "SPLEEF_GUST_RADIUS_BLOCKS", 6.0D,
                "SPLEEF_GUST_POWER", 2.0D,
                "SPLEEF_GUST_VERTICAL_BOOST", 1.0D));
        assertEquals(6.0D, ArcadePortRules.spleefGustRadiusBlocks());
        org.bukkit.util.Vector tuned = ArcadePortRules.spleefGustVelocity(
                new org.bukkit.util.Vector(3.0D, 0.0D, 0.0D));
        assertEquals(1.0D, tuned.getX(), 1.0E-6D);
        assertEquals(0.5D, tuned.getY(), 1.0E-6D);
        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(4.0D, ArcadePortRules.spleefGustRadiusBlocks());
        assertEquals(ArcadePortRules.SPLEEF_GUST_POWER,
                ArcadePortRules.spleefGustVelocity(new org.bukkit.util.Vector()).getY(), 1.0E-6D);
    }

    @Test
    void allAdditionalSpleefSlidersHaveBoundedRangesAndAccurateImmediateMetadata() {
        assertRange("SPLEEF_HUNGER_RESTORE_PER_HIT", 0.0D, 20.0D);
        assertRange("SPLEEF_LEAP_FORWARD_POWER", 0.0D, 5.0D);
        assertRange("SPLEEF_LEAP_VERTICAL_CAP", 0.0D, 5.0D);
        assertRange("SPLEEF_KNOCKBACK_VERTICAL_CAP", 0.0D, 12.0D);
        assertRange("SPLEEF_ARROW_CLEAR_RADIUS_SPEED_DIVISOR", 0.1D, 10.0D);
        assertRange("SPLEEF_PROJECTILE_IMPACT_SEARCH_RADIUS_BLOCKS", 0.0D, 4.0D);
        assertRange("SPLEEF_ARCHER_ARROW_SPEED_MULTIPLIER", 0.1D, 4.0D);
        assertRange("SPLEEF_GUST_RADIUS_BLOCKS", 1.0D, 8.0D);
        assertRange("SPLEEF_GUST_POWER", 0.0D, 5.0D);
        assertRange("SPLEEF_GUST_VERTICAL_BOOST", 0.0D, 4.0D);
        assertTrue(GameDebugVariableCatalog.appliesImmediately(GameDebugVariableCatalog.variable(
                ArcadePortGame.SPLEEF, "SPLEEF_LEAP_FORWARD_POWER")));
    }

    private static void assertRange(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.SPLEEF, key);
        assertNotNull(variable, key + " should be available in the Spleef profile");
        assertEquals(minimum, variable.minimum());
        assertEquals(maximum, variable.maximum());
    }

    private static Set<String> offsetSet() {
        return ArcadePortRules.spleefSmasherOffsets().stream()
                .map(offset -> offset.x() + "," + offset.y() + "," + offset.z())
                .collect(Collectors.toSet());
    }
}
