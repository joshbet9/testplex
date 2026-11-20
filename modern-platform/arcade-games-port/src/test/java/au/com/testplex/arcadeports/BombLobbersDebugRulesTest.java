package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BombLobbersDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void pitcherIsARecognisedAchievementKitAndInheritsTeamArmor() {
        assertEquals("pitcher", ArcadePortRules.normalizeBombLobbersKit("Pitcher"));
        assertTrue(ArcadePortRules.bombLobbersKitUsesTeamArmor("pitcher"));
    }

    @Test
    void scoreboardMatchesTheSourceRosterCutoffWithoutPowerupRows() {
        assertTrue(ArcadePortRules.bombLobbersScoreboardShowsPlayerNames(12));
        assertFalse(ArcadePortRules.bombLobbersScoreboardShowsPlayerNames(13));
        assertTrue(ArcadePortRules.bombLobbersScoreboardShowsPlayerNames(0));

        assertEquals(List.of(ChatColor.RED + "RedPlayer", ChatColor.DARK_GRAY.toString(),
                        ChatColor.AQUA + "BluePlayer"),
                ArcadePortRules.bombLobbersScoreboardRoster(List.of("RedPlayer"), List.of("BluePlayer")));
        List<String> largeRoster = ArcadePortRules.bombLobbersScoreboardRoster(
                List.of("R1", "R2", "R3", "R4", "R5", "R6"),
                List.of("B1", "B2", "B3", "B4", "B5", "B6", "B7"));
        assertEquals(List.of(ChatColor.RED + "Red Team", ChatColor.RED + "6 Alive",
                ChatColor.AQUA + "Blue Team", ChatColor.AQUA + "7 Alive"), largeRoster);
        assertTrue(largeRoster.stream().noneMatch(line -> line.toLowerCase(java.util.Locale.ROOT).contains("power-up")));
    }

    @Test
    void powerupActivationChatUsesTeamAndPowerupColours() {
        assertEquals(ChatColor.RED + "RedPlayer" + ChatColor.GRAY + " has activated Powerup "
                        + ChatColor.GOLD + "Fiery TNT",
                ArcadePortRules.bombLobbersPowerupActivationMessage("RedPlayer", 0, "fiery-tnt"));
        assertEquals(ChatColor.AQUA + "BluePlayer" + ChatColor.GRAY + " has activated Powerup "
                        + ChatColor.DARK_RED + "Super Explosive TNT",
                ArcadePortRules.bombLobbersPowerupActivationMessage("BluePlayer", 1, "super-tnt"));
        assertEquals(ChatColor.GREEN, ArcadePortRules.bombLobbersPowerupChatColor("speed-boost"));
        assertEquals(ChatColor.AQUA, ArcadePortRules.bombLobbersTeamChatColor(1));
        assertEquals(ChatColor.AQUA, ArcadePortRules.arcadeTeamChatColor(1));
        assertEquals(ChatColor.GREEN, ArcadePortRules.arcadeTeamChatColor(2));
        assertEquals(ChatColor.YELLOW, ArcadePortRules.arcadeTeamChatColor(3));
    }

    @Test
    void approvedPowerupsHaveDistinctEffectsAndTimedProtectionExpires() {
        assertEquals("Jump Boost", ArcadePortRules.bombLobbersPowerupName("jump-boost"));
        assertEquals("TNT Multiplier", ArcadePortRules.bombLobbersPowerupName("tnt-multiplier"));
        assertEquals("TNT Throw Strength", ArcadePortRules.bombLobbersPowerupName("throw-strength"));
        assertEquals("Regeneration", ArcadePortRules.bombLobbersPowerupName("regeneration"));
        assertEquals("Double Jump Boost", ArcadePortRules.bombLobbersPowerupName("double-jump-boost"));
        assertEquals("Direct-Hit Protection", ArcadePortRules.bombLobbersPowerupName("direct-hit-protection"));

        assertEquals(1.5D, ArcadePortRules.bombLobbersPowerupTntDamageMultiplier("tnt-multiplier"));
        assertEquals(1.0D, ArcadePortRules.bombLobbersPowerupTntDamageMultiplier("normal"));
        assertEquals(1.4D, ArcadePortRules.bombLobbersPowerupThrowVelocityMultiplier("throw-strength"));
        assertEquals(1.0D, ArcadePortRules.bombLobbersPowerupThrowVelocityMultiplier("normal"));
        assertEquals(1.35D, ArcadePortRules.bombLobbersPowerupDoubleJumpMultiplier(true));
        assertEquals(1.0D, ArcadePortRules.bombLobbersPowerupDoubleJumpMultiplier(false));

        assertEquals(5.0D, ArcadePortRules.bombLobbersDirectHitDamageAfterProtection(10.0D, true));
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamageAfterProtection(10.0D, false));
        assertTrue(ArcadePortRules.bombLobbersTimedPowerupActive(1_000L, 1_001L));
        assertFalse(ArcadePortRules.bombLobbersTimedPowerupActive(1_001L, 1_001L));
        assertTrue(ArcadePortRules.shouldIgniteBombLobbersBlock(true, true, false, false));
        assertFalse(ArcadePortRules.shouldIgniteBombLobbersBlock(false, true, false, false));
        assertFalse(ArcadePortRules.shouldIgniteBombLobbersBlock(true, false, false, false));
        assertFalse(ArcadePortRules.shouldIgniteBombLobbersBlock(true, true, true, false));
        assertEquals(160, ArcadePortRules.bombLobbersPowerupEffectDurationTicks());
        assertEquals(160, ArcadePortRules.bombLobbersPowerupDoubleJumpDurationTicks());
        assertEquals(160, ArcadePortRules.bombLobbersPowerupDirectHitProtectionTicks());
        assertEquals(500L, ArcadePortRules.bombLobbersPowerupAuraIntervalMillis());
    }

    @Test
    void powerupToggleIsProfileBackedAndConsumedByTheRuntimeRule() {
        assertTrue(ArcadePortRules.bombLobbersPowerupsEnabled());
        assertTrue(GameDebugVariableCatalog.variables(ArcadePortGame.BOMB_LOBBERS).stream()
                .anyMatch(variable -> variable.key().equals("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED")
                        && variable.kind() == GameDebugVariableCatalog.Kind.BOOLEAN));

        GameDebugRuntimeOverrides.activate(Map.of("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED", false));
        assertFalse(ArcadePortRules.bombLobbersPowerupsEnabled());

        GameDebugRuntimeOverrides.clear();
        assertTrue(ArcadePortRules.bombLobbersPowerupsEnabled());
    }

    @Test
    void combatAndAbilitySlidersChangeProductionRuleResultsAndReset() {
        assertEquals(8.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(4_000L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertEquals(50.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "BOMB_LOBBERS_EXPLOSION_RADIUS", 4.0D,
                "BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE", 30.0D,
                "BOMB_LOBBERS_DIRECT_HIT_DAMAGE", 6.0D,
                "BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS", 500L,
                "BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS", 200L,
                "BOMB_LOBBERS_ASSIST_WINDOW_MILLIS", 2_000L,
                "BOMB_LOBBERS_ULTIMATE_KILLER_KILLS", 3));
        assertEquals(4.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(30.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));
        assertEquals(6.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(499L, true));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(500L, true));
        assertEquals(4_150L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(3));

        GameDebugRuntimeOverrides.clear();
        assertEquals(8.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(50.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertEquals(4_000L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(3));
    }

    @Test
    void fieryTntAloneEmitsTheFireTrail() {
        assertTrue(ArcadePortRules.bombLobbersFieryTrailEnabled("fiery-tnt"));
        assertFalse(ArcadePortRules.bombLobbersFieryTrailEnabled("normal"));
        assertFalse(ArcadePortRules.bombLobbersFieryTrailEnabled("super-tnt"));
        assertFalse(ArcadePortRules.bombLobbersFieryTrailEnabled(null));
    }

    @Test
    void powerupWaveScheduleUsesConfiguredInclusiveBoundsAndResets() {
        assertEquals(20_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(0L));
        assertEquals(100_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(80_000L));
        assertEquals(100_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(-1L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "BOMB_LOBBERS_POWERUP_SPAWN_MIN_DELAY_MILLIS", 1_000L,
                "BOMB_LOBBERS_POWERUP_SPAWN_MAX_DELAY_MILLIS", 2_000L));
        assertEquals(1_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(0L));
        assertEquals(2_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(1_000L));
        assertEquals(2_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(-1L));

        GameDebugRuntimeOverrides.clear();
        assertEquals(20_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(0L));
        assertEquals(100_000L, ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(80_000L));
    }

    @Test
    void directHitCollisionBoxGrowthChangesTheActualSweptHitResultAndResets() {
        var box = new org.bukkit.util.BoundingBox(-0.3D, 0.0D, -0.3D, 0.3D, 1.8D, 0.3D);
        var start = new org.bukkit.Location(null, -3.0D, 2.5D, 1.2D);
        var end = new org.bukkit.Location(null, 3.0D, 2.5D, 1.2D);
        assertTrue(Double.isFinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(start, end, box)));

        GameDebugRuntimeOverrides.activate(Map.of("BOMB_LOBBERS_DIRECT_HIT_BOX_GROW", 0.0D));
        assertTrue(Double.isInfinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(start, end, box)));

        GameDebugRuntimeOverrides.activate(Map.of("BOMB_LOBBERS_DIRECT_HIT_BOX_GROW", 1.0D));
        assertTrue(Double.isFinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(start, end, box)));

        GameDebugRuntimeOverrides.clear();
        assertTrue(Double.isFinite(ArcadePortRules.bombLobbersDirectHitDistanceSquared(start, end, box)));
    }

    @Test
    void trailSpacingCarryCapAndSpeedBoostDurationChangeRealRuleResults() {
        assertEquals(6, ArcadePortRules.bombLobbersTrailSampleCount(1.0D));
        assertTrue(ArcadePortRules.bombLobbersCanCarryMoreTnt(2));
        assertFalse(ArcadePortRules.bombLobbersCanCarryMoreTnt(3));
        assertEquals(240, ArcadePortRules.bombLobbersPowerupSpeedDurationTicks());

        GameDebugRuntimeOverrides.activate(Map.of(
                "BOMB_LOBBERS_TRAIL_SAMPLE_SPACING", 0.1D,
                "BOMB_LOBBERS_MAX_CARRIED", 1,
                "BOMB_LOBBERS_POWERUP_SPEED_DURATION_TICKS", 100));
        assertEquals(11, ArcadePortRules.bombLobbersTrailSampleCount(1.0D));
        assertTrue(ArcadePortRules.bombLobbersCanCarryMoreTnt(0));
        assertFalse(ArcadePortRules.bombLobbersCanCarryMoreTnt(1));
        assertEquals(100, ArcadePortRules.bombLobbersPowerupSpeedDurationTicks());

        GameDebugRuntimeOverrides.clear();
        assertEquals(6, ArcadePortRules.bombLobbersTrailSampleCount(1.0D));
        assertTrue(ArcadePortRules.bombLobbersCanCarryMoreTnt(2));
        assertFalse(ArcadePortRules.bombLobbersCanCarryMoreTnt(3));
        assertEquals(240, ArcadePortRules.bombLobbersPowerupSpeedDurationTicks());
    }
}
