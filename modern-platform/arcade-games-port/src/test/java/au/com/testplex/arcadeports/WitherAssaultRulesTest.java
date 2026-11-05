package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class WitherAssaultRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test
    void sourceHumanKitsAndTheAchievementKitAreSelectable() {
        assertEquals("archer", WitherAssaultRules.normalizeHumanKit("Archer"));
        assertEquals("medic", WitherAssaultRules.normalizeHumanKit("human-medic"));
        assertEquals("editor", WitherAssaultRules.normalizeHumanKit("Human Editor"));
        assertEquals("warden", WitherAssaultRules.normalizeHumanKit("Human Warden"));
        assertNull(WitherAssaultRules.normalizeHumanKit(null));
        assertNull(WitherAssaultRules.normalizeHumanKit(""));
        assertNull(WitherAssaultRules.normalizeHumanKit("miner"));
        assertNull(WitherAssaultRules.normalizeHumanKit("wither"));
    }

    @Test
    void lobbyCatalogueExposesTheSourceHumanKitsAndAchievementKit() {
        assertEquals(List.of("archer", "medic", "editor", "warden"),
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.WITHER_ASSAULT, List.of())
                        .stream().map(choice -> choice.id).toList());
    }

    @Test
    void smokeArrowsBelongOnlyToAliveHumanArchers() {
        assertTrue(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "archer", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "archer", false));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(1, "archer", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "medic", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, "editor", true));
        assertFalse(WitherAssaultRules.isHumanArcherSmokeArrowEligible(0, null, true));
    }

    @Test
    void smokeArrowsBlindEveryWitherStrictlyInsideTheSixBlockRadius() {
        assertTrue(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(0.0D, 6.0D));
        assertTrue(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(35.999D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(36.0D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(36.001D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(-1.0D, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(Double.NaN, 6.0D));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(1.0D, Double.NaN));
        assertFalse(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(1.0D, 0.0D));
    }

    @Test
    void editorCannotModifyBlocksBelowTheFourBlockFloorOrUnderAnotherPlayer() {
        assertTrue(WitherAssaultRules.canEditorModifyBlock(72, 76.0D, false));
        assertTrue(WitherAssaultRules.canEditorModifyBlock(80, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(71, 76.0D, false));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, 76.0D, true));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(76, Double.NaN, false));
    }

    @Test
    void witherSwordClicksMatchTheSourceAbilityLayout() {
        assertEquals(3, WitherAssaultRules.minionCount());
        assertTrue(WitherAssaultRules.shouldLaunchSkull("golden_sword", true));
        assertFalse(WitherAssaultRules.shouldLaunchSkull("golden_sword", false));
        assertTrue(WitherAssaultRules.shouldLaunchMinions("diamond_sword", true));
        assertFalse(WitherAssaultRules.shouldLaunchMinions("diamond_sword", false));
        assertTrue(WitherAssaultRules.shouldStartFireStorm("golden_sword", true));
        assertTrue(WitherAssaultRules.shouldStartFireStorm("diamond_sword", true));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("golden_sword", false));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("shield", true));
        assertFalse(WitherAssaultRules.shouldStartFireStorm("compass", true));
    }

    @Test
    void archerAndEditorShareFletcherAndSmokeArrowButMedicDoesNot() {
        assertTrue(WitherAssaultRules.hasFletcher("archer"));
        assertTrue(WitherAssaultRules.hasFletcher("editor"));
        assertFalse(WitherAssaultRules.hasFletcher("medic"));
        assertTrue(WitherAssaultRules.shouldTrackSmokeArrow(0, "archer", true, true));
        assertTrue(WitherAssaultRules.shouldTrackSmokeArrow(0, "editor", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "medic", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(1, "archer", true, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "editor", false, true));
        assertFalse(WitherAssaultRules.shouldTrackSmokeArrow(0, "archer", true, false));
    }

    @Test
    void perKitTimersAndSmokeArrowRulesAreProfileBackedAndRestoreDefaults() {
        assertEquals(10_000L, WitherAssaultRules.minionCooldownMillis());
        assertEquals(30_000L, WitherAssaultRules.scentCooldownMillis());
        assertEquals(6_000L, WitherAssaultRules.leapCooldownMillis());
        assertEquals(45_000L, WitherAssaultRules.medicRefillIntervalMillis());
        assertEquals(4_000L, WitherAssaultRules.fletcherArrowRefillMillis());
        assertEquals(2_000L, WitherAssaultRules.skullAttackCooldownMillis());
        assertEquals(6.0D, WitherAssaultRules.smokeArrowBlindRadiusBlocks());
        assertTrue(WitherAssaultRules.canEditorModifyBlock(72, 76.0D, false));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("WITHER_ASSAULT_MINION_COOLDOWN_MILLIS", 2_000L),
                Map.entry("WITHER_ASSAULT_SCENT_COOLDOWN_MILLIS", 5_000L),
                Map.entry("WITHER_ASSAULT_LEAP_COOLDOWN_MILLIS", 1_000L),
                Map.entry("WITHER_ASSAULT_MEDIC_REFILL_INTERVAL_MILLIS", 10_000L),
                Map.entry("WITHER_ASSAULT_FLETCHER_ARROW_REFILL_MILLIS", 500L),
                Map.entry("WITHER_ASSAULT_SKULL_ATTACK_COOLDOWN_MILLIS", 300L),
                Map.entry("WITHER_ASSAULT_MINION_LIFETIME_MILLIS", 4_000L),
                Map.entry("WITHER_ASSAULT_SMOKE_ARROW_MAX_LIFETIME_TICKS", 40),
                Map.entry("WITHER_ASSAULT_SMOKE_ARROW_BLINDNESS_TICKS", 20),
                Map.entry("WITHER_ASSAULT_SMOKE_ARROW_BLIND_RADIUS_BLOCKS", 10.0D),
                Map.entry("WITHER_ASSAULT_EDITOR_BUILD_DEPTH_BELOW_MARKER", 8)));
        assertEquals(2_000L, WitherAssaultRules.minionCooldownMillis());
        assertEquals(5_000L, WitherAssaultRules.scentCooldownMillis());
        assertEquals(1_000L, WitherAssaultRules.leapCooldownMillis());
        assertEquals(10_000L, WitherAssaultRules.medicRefillIntervalMillis());
        assertEquals(500L, WitherAssaultRules.fletcherArrowRefillMillis());
        assertEquals(300L, WitherAssaultRules.skullAttackCooldownMillis());
        assertEquals(4_000L, WitherAssaultRules.minionLifetimeMillis());
        assertEquals(40, WitherAssaultRules.smokeArrowMaxLifetimeTicks());
        assertEquals(20, WitherAssaultRules.smokeArrowBlindnessTicks());
        assertTrue(WitherAssaultRules.shouldSmokeArrowBlindAtDistanceSquared(81.0D,
                WitherAssaultRules.smokeArrowBlindRadiusBlocks()));
        assertFalse(WitherAssaultRules.canEditorModifyBlock(67, 76.0D, false));

        GameDebugRuntimeOverrides.clear();
        assertEquals(10_000L, WitherAssaultRules.minionCooldownMillis());
        assertEquals(6.0D, WitherAssaultRules.smokeArrowBlindRadiusBlocks());
        assertTrue(WitherAssaultRules.canEditorModifyBlock(72, 76.0D, false));
    }
}
