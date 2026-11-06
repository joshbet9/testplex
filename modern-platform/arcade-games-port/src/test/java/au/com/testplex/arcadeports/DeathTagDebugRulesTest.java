package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeathTagDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void turtleAndTricksterTuningChangesAbilitiesAndResetsToSourceDefaults() {
        assertEquals(5_000L, ArcadePortRules.deathTagTurtleStackResetMillis());
        assertEquals(3, ArcadePortRules.deathTagTurtleMaxStacks());
        assertEquals(60L, ArcadePortRules.deathTagTurtleEffectDurationTicks());
        assertEquals(3, ArcadePortRules.deathTagTurtleHitsPerWindCharge());
        assertEquals(30_000L, ArcadePortRules.deathTagTricksterFireworksCooldownMillis());

        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_TURTLE_STACK_RESET_MILLIS", 8_000L,
                "DEATH_TAG_TURTLE_MAX_STACKS", 5,
                "DEATH_TAG_TURTLE_EFFECT_DURATION_TICKS", 100L,
                "DEATH_TAG_TURTLE_HITS_PER_WIND_CHARGE", 2,
                "DEATH_TAG_TRICKSTER_FIREWORKS_COOLDOWN_MILLIS", 12_000L));
        assertEquals(8_000L, ArcadePortRules.deathTagTurtleStackResetMillis());
        assertEquals(5, ArcadePortRules.deathTagTurtleMaxStacks());
        assertEquals(100L, ArcadePortRules.deathTagTurtleEffectDurationTicks());
        assertEquals(2, ArcadePortRules.deathTagTurtleHitsPerWindCharge());
        assertEquals(12_000L, ArcadePortRules.deathTagTricksterFireworksCooldownMillis());

        GameDebugRuntimeOverrides.clear();
        assertEquals(5_000L, ArcadePortRules.deathTagTurtleStackResetMillis());
        assertEquals(3, ArcadePortRules.deathTagTurtleMaxStacks());
        assertEquals(60L, ArcadePortRules.deathTagTurtleEffectDurationTicks());
        assertEquals(3, ArcadePortRules.deathTagTurtleHitsPerWindCharge());
        assertEquals(30_000L, ArcadePortRules.deathTagTricksterFireworksCooldownMillis());
    }

    @Test
    void runnerDamageAndIronSkinReductionAreProfileBacked() {
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("basher", false));
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("traitor", false));
        assertEquals(4.0D, ArcadePortRules.deathTagRunnerDamage("archer", true));
        assertEquals(4.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, false));
        assertEquals(2.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, true));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_RUNNER_BASHER_DAMAGE", 8.0D,
                "DEATH_TAG_RUNNER_TRAITOR_DAMAGE", 5.0D,
                "DEATH_TAG_RUNNER_ARCHER_PROJECTILE_DAMAGE", 7.0D,
                "DEATH_TAG_IRON_SKIN_DAMAGE_REDUCTION", 1.0D,
                "DEATH_TAG_ALPHA_IRON_SKIN_DAMAGE_REDUCTION", 3.0D));
        assertEquals(8.0D, ArcadePortRules.deathTagRunnerDamage("basher", false));
        assertEquals(5.0D, ArcadePortRules.deathTagRunnerDamage("traitor", false));
        assertEquals(7.0D, ArcadePortRules.deathTagRunnerDamage("archer", true));
        assertEquals(5.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, false));
        assertEquals(3.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, true));

        GameDebugRuntimeOverrides.clear();
        assertEquals(6.0D, ArcadePortRules.deathTagRunnerDamage("basher", false));
        assertEquals(4.0D, ArcadePortRules.deathTagDamageAfterIronSkin(6.0D, false));
    }

    @Test
    void meleeKnockbackAndSoloHitCadenceChangeAndRestore() {
        Vector velocity = new Vector(0.0D, 0.0D, 0.0D);
        Vector direction = new Vector(1.0D, 0.0D, 0.0D);
        Vector defaults = ArcadePortRules.deathTagDefaultMeleeKnockback(velocity, direction, false);
        assertEquals(0.4D, defaults.getX(), 1.0e-9);
        assertEquals(0.36D, defaults.getY(), 1.0e-9);
        assertEquals(1_500L, ArcadePortRules.deathTagSoloChaserHitCooldownMillis());

        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_MELEE_KNOCKBACK_DAMPING", 0.0D,
                "DEATH_TAG_MELEE_KNOCKBACK_HORIZONTAL", 0.9D,
                "DEATH_TAG_MELEE_KNOCKBACK_VERTICAL", 0.5D,
                "DEATH_TAG_CHASER_HIT_COOLDOWN_MILLIS", 500L));
        Vector changed = ArcadePortRules.deathTagDefaultMeleeKnockback(velocity, direction, false);
        assertEquals(0.9D, changed.getX(), 1.0e-9);
        assertEquals(0.5D, changed.getY(), 1.0e-9);
        assertEquals(500L, ArcadePortRules.deathTagSoloChaserHitCooldownMillis());

        GameDebugRuntimeOverrides.clear();
        Vector restored = ArcadePortRules.deathTagDefaultMeleeKnockback(velocity, direction, false);
        assertEquals(0.4D, restored.getX(), 1.0e-9);
        assertEquals(0.36D, restored.getY(), 1.0e-9);
        assertEquals(1_500L, ArcadePortRules.deathTagSoloChaserHitCooldownMillis());
    }

    @Test
    void runnerKillGemRewardIsAdjustableButChaserKillsRemainUnrewarded() {
        assertEquals(4, ArcadePortRules.deathTagKillRewardGems(true));
        assertEquals(0, ArcadePortRules.deathTagKillRewardGems(false));

        GameDebugRuntimeOverrides.activate(Map.of("DEATH_TAG_RUNNER_KILL_REWARD_GEMS", 12));
        assertEquals(12, ArcadePortRules.deathTagKillRewardGems(true));
        assertEquals(0, ArcadePortRules.deathTagKillRewardGems(false));

        GameDebugRuntimeOverrides.clear();
        assertEquals(4, ArcadePortRules.deathTagKillRewardGems(true));
    }

    @Test
    void chaserPromotionArcherReplenishAndRespawnGraceFollowProfileAndReset() {
        assertEquals(30_000L, ArcadePortRules.deathTagChaserPromotionIntervalMillis());
        assertEquals(1, ArcadePortRules.requiredDeathTagChasers(0L));
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(1_200_000L, ArcadePortRules.deathTagGameTimeoutMillis());
        assertEquals(2_500L, ArcadePortRules.deathTagRespawnGraceMillis());
        assertEquals(2_000L, ArcadePortRules.deathTagArcherArrowIntervalMillis());
        assertEquals(2, ArcadePortRules.deathTagArcherMaxArrows());
        assertTrue(ArcadePortRules.deathTagCanRefillArcherArrows(1));
        assertFalse(ArcadePortRules.deathTagCanRefillArcherArrows(2));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_CHASER_PROMOTION_INTERVAL_MILLIS", 10_000L,
                "DEATH_TAG_GAME_TIMEOUT_MILLIS", 300_000L,
                "DEATH_TAG_RESPAWN_GRACE_MILLIS", 1_000L,
                "DEATH_TAG_ARCHER_ARROW_INTERVAL_MILLIS", 500L,
                "DEATH_TAG_ARCHER_MAX_ARROWS", 4));
        assertEquals(10_000L, ArcadePortRules.deathTagChaserPromotionIntervalMillis());
        assertEquals(7, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(300_000L, ArcadePortRules.deathTagGameTimeoutMillis());
        assertEquals(1_000L, ArcadePortRules.deathTagRespawnGraceMillis());
        assertEquals(500L, ArcadePortRules.deathTagArcherArrowIntervalMillis());
        assertEquals(4, ArcadePortRules.deathTagArcherMaxArrows());
        assertTrue(ArcadePortRules.deathTagCanRefillArcherArrows(3));
        assertFalse(ArcadePortRules.deathTagCanRefillArcherArrows(4));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(30_000L, ArcadePortRules.deathTagChaserPromotionIntervalMillis());
        assertEquals(3, ArcadePortRules.requiredDeathTagChasers(60_000L));
        assertEquals(1_200_000L, ArcadePortRules.deathTagGameTimeoutMillis());
        assertEquals(2_500L, ArcadePortRules.deathTagRespawnGraceMillis());
        assertTrue(ArcadePortRules.deathTagCanRefillArcherArrows(1));
        assertFalse(ArcadePortRules.deathTagCanRefillArcherArrows(2));
    }

    @Test
    void placementAndParticipationRewardsAreTunableAndRestore() {
        assertEquals(10, ArcadePortRules.deathTagForcedChaserRewardGems());
        assertEquals(15, ArcadePortRules.deathTagPlacementRewardGems(1));
        assertEquals(10, ArcadePortRules.deathTagPlacementRewardGems(2));
        assertEquals(5, ArcadePortRules.deathTagPlacementRewardGems(3));
        assertEquals(0, ArcadePortRules.deathTagPlacementRewardGems(4));
        assertEquals(10, ArcadePortRules.deathTagParticipationRewardGems());

        GameDebugRuntimeOverrides.activate(Map.of(
                "DEATH_TAG_FORCED_CHASER_REWARD_GEMS", 2,
                "DEATH_TAG_FIRST_PLACE_REWARD_GEMS", 30,
                "DEATH_TAG_SECOND_PLACE_REWARD_GEMS", 20,
                "DEATH_TAG_THIRD_PLACE_REWARD_GEMS", 10,
                "DEATH_TAG_PARTICIPATION_REWARD_GEMS", 1));
        assertEquals(2, ArcadePortRules.deathTagForcedChaserRewardGems());
        assertEquals(30, ArcadePortRules.deathTagPlacementRewardGems(1));
        assertEquals(20, ArcadePortRules.deathTagPlacementRewardGems(2));
        assertEquals(10, ArcadePortRules.deathTagPlacementRewardGems(3));
        assertEquals(1, ArcadePortRules.deathTagParticipationRewardGems());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(10, ArcadePortRules.deathTagForcedChaserRewardGems());
        assertEquals(15, ArcadePortRules.deathTagPlacementRewardGems(1));
        assertEquals(10, ArcadePortRules.deathTagPlacementRewardGems(2));
        assertEquals(5, ArcadePortRules.deathTagPlacementRewardGems(3));
        assertEquals(10, ArcadePortRules.deathTagParticipationRewardGems());
    }

    @Test
    void achievementProgressionThresholdsCanBeShortenedForTestingAndReset() {
        assertEquals("-1:-1", ArcadePortRules.deathTagStatueAreaKey(-1, -1));
        assertEquals(5, ArcadePortRules.deathTagStatueAreaSizeBlocks());
        assertEquals(180_000L, ArcadePortRules.deathTagStatueRequiredTimeMillis());
        assertEquals(3, ArcadePortRules.deathTagGallowsAssists());
        assertEquals(8, ArcadePortRules.deathTagUltimateChaserKills());
        assertEquals(3, ArcadePortRules.deathTagHastyHunterKills());
        assertEquals(60_000L, ArcadePortRules.deathTagHastyHunterWindowMillis());
        assertEquals(250.0D, ArcadePortRules.deathTagResilientRunnerHearts());
        assertEquals(10_000, ArcadePortRules.deathTagCatchMeDistanceBlocks());
        assertEquals(3, ArcadePortRules.deathTagTurtleShellHits());
        assertEquals(50, ArcadePortRules.deathTagDeathProofWins());
        assertTrue(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(2));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("DEATH_TAG_STATUE_AREA_SIZE_BLOCKS", 10),
                Map.entry("DEATH_TAG_STATUE_REQUIRED_TIME_MILLIS", 2_000L),
                Map.entry("DEATH_TAG_GALLOWS_ASSISTS", 1),
                Map.entry("DEATH_TAG_ULTIMATE_CHASER_KILLS", 2),
                Map.entry("DEATH_TAG_HASTY_HUNTER_KILLS", 2),
                Map.entry("DEATH_TAG_HASTY_HUNTER_WINDOW_MILLIS", 5_000L),
                Map.entry("DEATH_TAG_RESILIENT_RUNNER_HEARTS", 10.0D),
                Map.entry("DEATH_TAG_CATCH_ME_DISTANCE_BLOCKS", 100),
                Map.entry("DEATH_TAG_TURTLE_SHELL_HITS", 1),
                Map.entry("DEATH_TAG_DEATH_PROOF_WINS", 2),
                Map.entry("DEATH_TAG_COME_AT_ME_BRO_KILLS", 1),
                Map.entry("DEATH_TAG_TRAITOR_ASSIST_WINDOW_MILLIS", 1_000L)));
        assertEquals("0:-1", ArcadePortRules.deathTagStatueAreaKey(9, -1));
        assertEquals(10, ArcadePortRules.deathTagStatueAreaSizeBlocks());
        assertEquals(2_000L, ArcadePortRules.deathTagStatueRequiredTimeMillis());
        assertEquals(1, ArcadePortRules.deathTagGallowsAssists());
        assertEquals(2, ArcadePortRules.deathTagUltimateChaserKills());
        assertEquals(2, ArcadePortRules.deathTagHastyHunterKills());
        assertEquals(5_000L, ArcadePortRules.deathTagHastyHunterWindowMillis());
        assertEquals(10.0D, ArcadePortRules.deathTagResilientRunnerHearts());
        assertEquals(100, ArcadePortRules.deathTagCatchMeDistanceBlocks());
        assertEquals(1, ArcadePortRules.deathTagTurtleShellHits());
        assertEquals(2, ArcadePortRules.deathTagDeathProofWins());
        assertTrue(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(1));
        assertEquals(1_000L, ArcadePortRules.deathTagTraitorAssistWindowMillis());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals("1:-1", ArcadePortRules.deathTagStatueAreaKey(9, -1));
        assertEquals(5, ArcadePortRules.deathTagStatueAreaSizeBlocks());
        assertEquals(180_000L, ArcadePortRules.deathTagStatueRequiredTimeMillis());
        assertEquals(3, ArcadePortRules.deathTagGallowsAssists());
        assertEquals(8, ArcadePortRules.deathTagUltimateChaserKills());
        assertEquals(3, ArcadePortRules.deathTagHastyHunterKills());
        assertEquals(60_000L, ArcadePortRules.deathTagHastyHunterWindowMillis());
        assertEquals(250.0D, ArcadePortRules.deathTagResilientRunnerHearts());
        assertEquals(10_000, ArcadePortRules.deathTagCatchMeDistanceBlocks());
        assertEquals(3, ArcadePortRules.deathTagTurtleShellHits());
        assertEquals(50, ArcadePortRules.deathTagDeathProofWins());
        assertFalse(ArcadePortRules.shouldAwardDeathTagComeAtMeBro(1));
        assertEquals(8_000L, ArcadePortRules.deathTagTraitorAssistWindowMillis());
    }
}
