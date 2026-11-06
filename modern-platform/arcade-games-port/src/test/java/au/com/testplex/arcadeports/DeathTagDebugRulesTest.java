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
}
