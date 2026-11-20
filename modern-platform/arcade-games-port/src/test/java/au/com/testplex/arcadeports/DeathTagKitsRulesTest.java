package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeathTagKitsRulesTest {
    @Test void tricksterClutchRequiresActualRecentRightClick() {
        assertTrue(DeathTagTricksterMechanics.armedClutch(10_000L, 12_000L, 0L));
        assertFalse(DeathTagTricksterMechanics.armedClutch(12_000L, 12_000L, 0L));
        assertFalse(DeathTagTricksterMechanics.armedClutch(9_999L, 12_000L, 10_000L));
        assertTrue(DeathTagTricksterMechanics.armedClutch(10_000L, 12_000L, 10_000L));
    }

    @Test void waterAndWebOnlyOverwriteAir() {
        assertTrue(DeathTagTricksterMechanics.canPlaceUtilityBlock(Material.AIR));
        assertFalse(DeathTagTricksterMechanics.canPlaceUtilityBlock(Material.STONE));
        assertFalse(DeathTagTricksterMechanics.canPlaceUtilityBlock(Material.WATER));
        assertEquals(100L, DeathTagTricksterMechanics.WATER_DURATION_TICKS);
        assertEquals(160L, DeathTagTricksterMechanics.WEB_DURATION_TICKS);
        assertEquals(200L, DeathTagTricksterMechanics.DECOY_DURATION_TICKS);
    }

    @Test void turtleGuardStillAllowsLethalDamage() {
        long start = 100_000L;
        long end = start + DeathTagTurtleGuardRules.DURATION_MILLIS;
        assertTrue(DeathTagTurtleGuardRules.isActive(start, end));
        assertFalse(DeathTagTurtleGuardRules.isActive(end, end));
        assertEquals(3.0D, DeathTagTurtleGuardRules.damageAfterGuard(6.0D, true));
        assertEquals(6.0D, DeathTagTurtleGuardRules.damageAfterGuard(6.0D, false));
        assertEquals(0.0D, DeathTagTurtleGuardRules.damageAfterGuard(-1.0D, true));
    }

    @Test void turtleChargeCapsAtThreeAndShowsProgress() {
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(1, false),
                DeathTagTurtleChargeProgress.recordHit(0, 3, 0));
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(0, true),
                DeathTagTurtleChargeProgress.recordHit(2, 3, 2));
        assertEquals(new DeathTagTurtleChargeProgress.HitResult(0, false),
                DeathTagTurtleChargeProgress.recordHit(2, 3, 3));
        assertEquals(2.0F / 3.0F,
                DeathTagTurtleChargeProgress.experienceProgress(2, 3, 1), 0.0001F);
        assertEquals(0.0F,
                DeathTagTurtleChargeProgress.experienceProgress(2, 3, 3), 0.0001F);
    }

    @Test void tricksterSwapRefreshesAfterThirtySeconds() {
        long usedAt = 100_000L;
        long readyAt = usedAt + DeathTagTricksterSwapRules.COOLDOWN_MILLIS;
        assertFalse(DeathTagTricksterSwapRules.isReady(readyAt - 1L, readyAt));
        assertTrue(DeathTagTricksterSwapRules.isReady(readyAt, readyAt));
    }

    @Test void skeletonDisguisesDoNotBurnInSun() {
        assertFalse(DeathTagChaserRules.shouldBurnInDaylight());
    }
}
