package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathTagDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
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
