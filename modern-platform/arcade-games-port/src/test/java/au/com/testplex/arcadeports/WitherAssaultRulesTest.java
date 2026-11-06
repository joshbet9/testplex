package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.bukkit.util.Vector;

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

    @Test
    void minionAndFireStormProfilesChangeTheirLiveRuleOutputsAndRestoreDefaults() {
        Vector direction = new Vector(1.0D, 0.0D, 0.0D);
        Vector defaultLaunch = WitherAssaultRules.minionLaunchVelocity(direction, 0.0D, 0.0D, 0.0D);
        assertEquals(1.1D, defaultLaunch.length(), 1.0e-9);
        assertTrue(WitherAssaultRules.shouldMinionTargetHuman(4_000.0D,
                WitherAssaultRules.minionTargetRangeBlocks()));
        assertEquals(0.05F, WitherAssaultRules.fireStormExperienceAfterTick(0.0F, false), 0.000001F);
        assertEquals(2, WitherAssaultRules.fireStormArrowsPerTick());
        assertEquals(2.0D, WitherAssaultRules.fireStormArrowSpeed());
        assertEquals(WitherAssaultMovementRules.SPAWN_HOVER_SPEED,
                WitherAssaultMovementRules.targetFlySpeed(76.0D, 76.0D, 76.0D), 1.0e-9);

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("WITHER_ASSAULT_MINION_LAUNCH_SPEED", 2.0D),
                Map.entry("WITHER_ASSAULT_MINION_TARGET_RANGE_BLOCKS", 10.0D),
                Map.entry("WITHER_ASSAULT_MINION_SPEED_AMPLIFIER", 2),
                Map.entry("WITHER_ASSAULT_MINION_SPEED_DURATION_TICKS", 600),
                Map.entry("WITHER_ASSAULT_FIRE_STORM_CHARGE_PER_TICK", 0.1D),
                Map.entry("WITHER_ASSAULT_FIRE_STORM_DRAIN_PER_TICK", 0.2D),
                Map.entry("WITHER_ASSAULT_FIRE_STORM_ARROWS_PER_TICK", 4),
                Map.entry("WITHER_ASSAULT_FIRE_STORM_ARROW_SPEED", 3.0D),
                Map.entry("WITHER_ASSAULT_FIRE_STORM_SPREAD", 0.1D),
                Map.entry("WITHER_ASSAULT_FLIGHT_SPAWN_HOVER_SPEED", 0.03D),
                Map.entry("WITHER_ASSAULT_FLIGHT_SPEED_PER_Y_BLOCK", 0.01D),
                Map.entry("WITHER_ASSAULT_FLIGHT_MAX_SPEED_CHANGE_PER_TICK", 0.01D)));
        assertEquals(2.0D, WitherAssaultRules.minionLaunchVelocity(direction, 0.0D, 0.0D, 0.0D).length(), 1.0e-9);
        assertFalse(WitherAssaultRules.shouldMinionTargetHuman(121.0D,
                WitherAssaultRules.minionTargetRangeBlocks()));
        assertTrue(WitherAssaultRules.shouldMinionTargetHuman(100.0D,
                WitherAssaultRules.minionTargetRangeBlocks()));
        assertEquals(0.1F, WitherAssaultRules.fireStormExperienceAfterTick(0.0F, false), 0.000001F);
        assertEquals(0.3F, WitherAssaultRules.fireStormExperienceAfterTick(0.5F, true), 0.000001F);
        assertEquals(4, WitherAssaultRules.fireStormArrowsPerTick());
        assertEquals(3.0D, WitherAssaultRules.fireStormArrowSpeed());
        assertEquals(0.03D, WitherAssaultMovementRules.targetFlySpeed(76.0D, 76.0D, 76.0D), 1.0e-9);
        assertEquals(0.04D, WitherAssaultMovementRules.approachSpeed(0.03D, 0.8D), 1.0e-9);

        GameDebugRuntimeOverrides.clear();
        assertEquals(1.1D, WitherAssaultRules.minionLaunchVelocity(direction, 0.0D, 0.0D, 0.0D).length(), 1.0e-9);
        assertTrue(WitherAssaultRules.shouldMinionTargetHuman(4_000.0D,
                WitherAssaultRules.minionTargetRangeBlocks()));
        assertEquals(2, WitherAssaultRules.fireStormArrowsPerTick());
        assertEquals(WitherAssaultMovementRules.SPAWN_HOVER_SPEED,
                WitherAssaultMovementRules.targetFlySpeed(76.0D, 76.0D, 76.0D), 1.0e-9);
    }

    @Test
    void combatAndSkullObjectiveControlsChangeDamageAndRestoreSourceValues() {
        assertEquals(0.75D, WitherAssaultRules.humanArrowDamageMultiplier());
        assertEquals(12.0D, WitherAssaultRules.skullDamageAtDistance(0.0D, 1.0D), 1.0e-9);
        assertEquals(2.0D, WitherAssaultRules.skullDamageAtDistance(7.0D, 1.0D), 1.0e-9);
        assertEquals(1.25D, WitherAssaultRules.skullKnockbackPower());
        assertEquals(0.3D, WitherAssaultRules.skullKnockbackYMinimum());
        assertEquals(4, WitherAssaultRules.skullTerrainRadiusBlocks());
        assertEquals(120, WitherAssaultRules.skullBlockRestoreTicks());
        assertEquals(1.15D, WitherAssaultRules.skullLaunchSpeed());

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("WITHER_ASSAULT_HUMAN_ARROW_DAMAGE_MULTIPLIER", 1.25D),
                Map.entry("WITHER_ASSAULT_SKULL_BLAST_RADIUS_BLOCKS", 10.0D),
                Map.entry("WITHER_ASSAULT_SKULL_MIN_DAMAGE", 4.0D),
                Map.entry("WITHER_ASSAULT_SKULL_MAX_DAMAGE", 20.0D),
                Map.entry("WITHER_ASSAULT_SKULL_KNOCKBACK_POWER", 2.0D),
                Map.entry("WITHER_ASSAULT_SKULL_KNOCKBACK_Y_MINIMUM", 0.0D),
                Map.entry("WITHER_ASSAULT_SKULL_KNOCKBACK_Y_MULTIPLIER", 1.0D),
                Map.entry("WITHER_ASSAULT_SKULL_TERRAIN_RADIUS_BLOCKS", 2),
                Map.entry("WITHER_ASSAULT_SKULL_BLOCK_RESTORE_TICKS", 40),
                Map.entry("WITHER_ASSAULT_SKULL_LAUNCH_SPEED", 2.5D)));
        assertEquals(1.25D, WitherAssaultRules.humanArrowDamageMultiplier());
        assertEquals(20.0D, WitherAssaultRules.skullDamageAtDistance(0.0D, 1.0D), 1.0e-9);
        assertEquals(12.0D, WitherAssaultRules.skullDamageAtDistance(5.0D, 1.0D), 1.0e-9);
        assertEquals(2.0D, WitherAssaultRules.skullKnockbackPower());
        assertEquals(0.0D, WitherAssaultRules.skullKnockbackYMinimum());
        assertEquals(2, WitherAssaultRules.skullTerrainRadiusBlocks());
        assertEquals(40, WitherAssaultRules.skullBlockRestoreTicks());
        assertEquals(2.5D, WitherAssaultRules.skullLaunchSpeed());

        GameDebugRuntimeOverrides.clear();
        assertEquals(0.75D, WitherAssaultRules.humanArrowDamageMultiplier());
        assertEquals(12.0D, WitherAssaultRules.skullDamageAtDistance(0.0D, 1.0D), 1.0e-9);
        assertEquals(0.3D, WitherAssaultRules.skullKnockbackYMinimum());
        assertEquals(4, WitherAssaultRules.skullTerrainRadiusBlocks());
        assertEquals(120, WitherAssaultRules.skullBlockRestoreTicks());
        assertEquals(1.15D, WitherAssaultRules.skullLaunchSpeed());
    }

    @Test
    void humanLeapProfileControlsChangeRealVelocityComponentsAndRestoreKitDefaults() {
        assertEquals(1.2D, WitherAssaultRules.humanLeapHorizontalPower("archer"), 1.0e-9);
        assertEquals(1.0D, WitherAssaultRules.humanLeapHorizontalPower("medic"), 1.0e-9);
        assertEquals(0.65D, WitherAssaultRules.humanLeapVerticalVelocity(0.0D), 1.0e-9);
        assertEquals(1.0D, WitherAssaultRules.humanLeapVerticalVelocity(1.0D), 1.0e-9);

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("WITHER_ASSAULT_HUMAN_LEAP_HORIZONTAL_POWER", 1.5D),
                Map.entry("WITHER_ASSAULT_ARCHER_LEAP_HORIZONTAL_MULTIPLIER", 2.0D),
                Map.entry("WITHER_ASSAULT_HUMAN_LEAP_VERTICAL_MINIMUM", 0.8D),
                Map.entry("WITHER_ASSAULT_HUMAN_LEAP_VERTICAL_MAXIMUM", 1.8D),
                Map.entry("WITHER_ASSAULT_HUMAN_LEAP_VERTICAL_LOOK_ADD", 0.5D)));
        assertEquals(3.0D, WitherAssaultRules.humanLeapHorizontalPower("archer"), 1.0e-9);
        assertEquals(1.5D, WitherAssaultRules.humanLeapHorizontalPower("medic"), 1.0e-9);
        assertEquals(0.8D, WitherAssaultRules.humanLeapVerticalVelocity(0.0D), 1.0e-9);
        assertEquals(1.5D, WitherAssaultRules.humanLeapVerticalVelocity(1.0D), 1.0e-9);
        assertEquals(1.8D, WitherAssaultRules.humanLeapVerticalVelocity(2.0D), 1.0e-9);

        GameDebugRuntimeOverrides.clear();
        assertEquals(1.2D, WitherAssaultRules.humanLeapHorizontalPower("archer"), 1.0e-9);
        assertEquals(0.65D, WitherAssaultRules.humanLeapVerticalVelocity(0.0D), 1.0e-9);
    }
}
