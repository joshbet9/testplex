package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class CastleAssaultDebugRulesTest {
    @AfterEach void resetOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void castleAssaultDefaultsKeepTeamScaledKingAndSourceTntBehaviour() {
        assertEquals(600, ArcadePortRules.castleAssaultKingHealthForOpponents(15));
        assertEquals(160, ArcadePortRules.castleAssaultKingHealthForOpponents(4));
        assertEquals(1, ArcadePortRules.castleAssaultKingHealthForOpponents(0));
        assertEquals(60_000L, ArcadePortRules.castleAssaultCarrierFuseMillis());
        assertEquals(4.0D, ArcadePortRules.castleAssaultTntRadius("fighter"));
        assertEquals(7.0D, ArcadePortRules.castleAssaultTntRadius("demolitionist"));
        assertEquals(10.0D, ArcadePortRules.castleAssaultTntDamage(0.0D, 4.0D));
        assertEquals(5.0D, ArcadePortRules.castleAssaultTntDamage(2.0D, 4.0D));
        assertEquals(0.0D, ArcadePortRules.castleAssaultTntDamage(4.0D, 4.0D));
        assertEquals(1.0D, ArcadePortRules.castleAssaultTntKnockbackMultiplier());
    }

    @Test void profileChangesKingScalingAndTntCombatOutputsThenResetRestoresDefaults() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "CASTLE_ASSAULT_KING_HEALTH_BASE", 300,
                "CASTLE_ASSAULT_KING_HEALTH_REFERENCE_PLAYERS", 10,
                "CASTLE_ASSAULT_CARRIER_FUSE_MILLIS", 30_000L,
                "CASTLE_ASSAULT_TNT_BASE_RADIUS", 6.0D,
                "CASTLE_ASSAULT_TNT_DEMOLITIONIST_RADIUS_BONUS", 2.0D,
                "CASTLE_ASSAULT_TNT_MAX_DAMAGE", 20.0D,
                "CASTLE_ASSAULT_TNT_KNOCKBACK_MULTIPLIER", 0.0D));

        assertEquals(120, ArcadePortRules.castleAssaultKingHealthForOpponents(4));
        assertEquals(30_000L, ArcadePortRules.castleAssaultCarrierFuseMillis());
        assertEquals(6.0D, ArcadePortRules.castleAssaultTntRadius("fighter"));
        assertEquals(8.0D, ArcadePortRules.castleAssaultTntRadius("demolitionist"));
        assertEquals(10.0D, ArcadePortRules.castleAssaultTntDamage(3.0D, 6.0D));
        assertEquals(0.0D, ArcadePortRules.castleAssaultTntKnockbackMultiplier());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(160, ArcadePortRules.castleAssaultKingHealthForOpponents(4));
        assertEquals(60_000L, ArcadePortRules.castleAssaultCarrierFuseMillis());
        assertEquals(4.0D, ArcadePortRules.castleAssaultTntRadius("fighter"));
        assertEquals(7.0D, ArcadePortRules.castleAssaultTntRadius("demolitionist"));
        assertEquals(5.0D, ArcadePortRules.castleAssaultTntDamage(2.0D, 4.0D));
        assertEquals(1.0D, ArcadePortRules.castleAssaultTntKnockbackMultiplier());
    }

    @Test void practicalCastleAssaultCombatRulesAppearInTheProfileEditor() {
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.CASTLE_ASSAULT,
                "CASTLE_ASSAULT_KING_HEALTH_BASE"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.CASTLE_ASSAULT,
                "CASTLE_ASSAULT_TNT_BASE_RADIUS"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.CASTLE_ASSAULT,
                "CASTLE_ASSAULT_TNT_MAX_DAMAGE"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.CASTLE_ASSAULT,
                "CASTLE_ASSAULT_TNT_FUSE_TICKS"));
        for (int tier = 1; tier <= 4; tier++) {
            GameDebugVariableCatalog.Variable threshold = GameDebugVariableCatalog.variable(
                    ArcadePortGame.CASTLE_ASSAULT, "CASTLE_ASSAULT_KILLSTREAK_TIER_" + tier + "_KILLS");
            assertNotNull(threshold);
            assertEquals(1.0D, threshold.minimum());
            assertEquals(1_000.0D, threshold.maximum());
        }
    }

    @Test void killStreakRewardThresholdsFollowProfilesAndResetToSourceSchedule() {
        assertEquals(1, ArcadePortRules.castleAssaultKillStreakRewardTier(2));
        assertEquals(2, ArcadePortRules.castleAssaultKillStreakRewardTier(4));
        assertEquals(3, ArcadePortRules.castleAssaultKillStreakRewardTier(6));
        assertEquals(4, ArcadePortRules.castleAssaultKillStreakRewardTier(8));
        assertEquals(0, ArcadePortRules.castleAssaultKillStreakRewardTier(5));

        GameDebugRuntimeOverrides.activate(Map.of(
                "CASTLE_ASSAULT_KILLSTREAK_TIER_1_KILLS", 1,
                "CASTLE_ASSAULT_KILLSTREAK_TIER_2_KILLS", 3,
                "CASTLE_ASSAULT_KILLSTREAK_TIER_3_KILLS", 5,
                "CASTLE_ASSAULT_KILLSTREAK_TIER_4_KILLS", 7));
        assertEquals(1, ArcadePortRules.castleAssaultKillStreakRewardTier(1));
        assertEquals(2, ArcadePortRules.castleAssaultKillStreakRewardTier(3));
        assertEquals(3, ArcadePortRules.castleAssaultKillStreakRewardTier(5));
        assertEquals(4, ArcadePortRules.castleAssaultKillStreakRewardTier(7));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(1, ArcadePortRules.castleAssaultKillStreakRewardTier(2));
        assertEquals(0, ArcadePortRules.castleAssaultKillStreakRewardTier(1));
    }
}
