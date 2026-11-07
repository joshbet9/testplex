package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class HeroesOfGwenAchievementTrackerTest {
    private final UUID player = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final UUID otherTarget = UUID.randomUUID();
    private final HeroesOfGwenAchievementTracker tracker = new HeroesOfGwenAchievementTracker();

    @Test
    void hattoriMustLandAllThreeAbilitiesOnTheSameTargetWithinFifteenSeconds() {
        assertNull(tracker.recordAbilityHit(player, target, "hattori.shuriken", 1_000L));
        assertNull(tracker.recordAbilityHit(player, otherTarget, "hattori.ninja_dash", 2_000L));
        assertNull(tracker.recordAbilityHit(player, target, "hattori.ninja_dash", 3_000L));
        assertEquals("hattori-smoke-and-mirrors",
                tracker.recordAbilityHit(player, target, "hattori.ender_blade", 16_000L));
    }

    @Test
    void anathComboFitsInsideFlameWandsThreeSecondBurnWindow() {
        assertNull(tracker.recordAbilityHit(player, target, "anath.flame_wand", 0L));
        assertNull(tracker.recordAbilityHit(player, target, "anath.burn_beam", 1_500L));
        assertEquals("anath-from-ember-to-inferno",
                tracker.recordAbilityHit(player, target, "anath.meteor_shower", 3_000L));

        tracker.clear();
        tracker.recordAbilityHit(player, target, "anath.flame_wand", 0L);
        tracker.recordAbilityHit(player, target, "anath.burn_beam", 1_500L);
        assertNull(tracker.recordAbilityHit(player, target, "anath.meteor_shower", 3_001L));
    }

    @Test
    void biffAndIvyCombosRequireTheirFullTargetSpecificSequences() {
        tracker.recordAbilityHit(player, target, "biff.tether", 1_000L);
        assertEquals("biff-the-long-ride",
                tracker.recordAbilityHit(player, target, "biff.cavalry_charge", 6_000L));
        assertNull(tracker.recordAbilityHit(player, otherTarget, "ivy.vine_pull", 10_000L));
        tracker.recordAbilityHit(player, target, "ivy.vine_pull", 10_000L);
        tracker.recordAbilityHit(player, target, "ivy.thorn_wall", 11_000L);
        assertEquals("ivy-garden-variety-trouble",
                tracker.recordAbilityHit(player, target, "ivy.floral_leap", 16_000L));
    }

    @Test
    void larissaRequiresARealTeammateHealWithinTenSecondsOfAquaCannon() {
        tracker.recordLarissaAquaCannonHit(player, 1_000L);
        assertFalse(tracker.recordLarissaStormBlessingHeal(player, 11_001L));
        tracker.recordLarissaAquaCannonHit(player, 20_000L);
        assertTrue(tracker.recordLarissaStormBlessingHeal(player, 30_000L));
        assertFalse(tracker.recordLarissaStormBlessingHeal(player, 30_001L));
    }

    @Test
    void danaRequiresThreeDistinctHealedAlliesAndTwoDistinctDamagedEnemiesInOneRally() {
        UUID allyOne = UUID.randomUUID();
        UUID allyTwo = UUID.randomUUID();
        UUID allyThree = UUID.randomUUID();
        UUID enemyOne = UUID.randomUUID();
        UUID enemyTwo = UUID.randomUUID();
        tracker.startDanaRally(player);
        tracker.recordDanaRallyHeal(player, allyOne);
        tracker.recordDanaRallyHeal(player, allyOne);
        tracker.recordDanaRallyHeal(player, allyTwo);
        tracker.recordDanaRallyDamage(player, enemyOne);
        assertNull(tracker.recordDanaRallyDamage(player, enemyOne));
        assertNull(tracker.recordDanaRallyDamage(player, enemyTwo));
        assertEquals("dana-rally-the-vanguard", tracker.recordDanaRallyHeal(player, allyThree));
        tracker.endDanaRally(player);
        assertNull(tracker.recordDanaRallyHeal(player, allyThree));
    }

    @Test
    void bardolfRequiresFiveLivingWolvesAndAnActiveFullMoonForThePackKill() {
        assertFalse(tracker.recordBardolfPackKill(player, 4, true));
        assertFalse(tracker.recordBardolfPackKill(player, 5, false));
        assertTrue(tracker.recordBardolfPackKill(player, 5, true));
    }
}
