package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RowenaTowerAchievementTrackerTest {
    private static final int ATTACKING_TEAM = 0;
    private static final String OUTER = "outer";
    private static final String INNER = "inner";

    @Test
    void minionDamageMayFinishBothTowersAfterRowenaHitEachWithLightArrows() {
        RowenaTowerAchievementTracker tracker = new RowenaTowerAchievementTracker();
        UUID rowena = UUID.randomUUID();
        tracker.recordDamage(ATTACKING_TEAM, OUTER, rowena, true, false);
        tracker.recordDamage(ATTACKING_TEAM, INNER, rowena, true, false);
        tracker.recordDamage(ATTACKING_TEAM, OUTER, null, false, true);
        tracker.recordDamage(ATTACKING_TEAM, INNER, null, false, true);

        assertEquals(rowena, tracker.achievementOwner(ATTACKING_TEAM,
                Set.of(OUTER, INNER), Set.of(OUTER, INNER)));
    }

    @Test
    void achievementRequiresLightArrowDamageToBothAndBothTowersDestroyed() {
        RowenaTowerAchievementTracker tracker = new RowenaTowerAchievementTracker();
        UUID rowena = UUID.randomUUID();
        tracker.recordDamage(ATTACKING_TEAM, OUTER, rowena, true, false);
        assertNull(tracker.achievementOwner(ATTACKING_TEAM, Set.of(OUTER, INNER), Set.of(OUTER, INNER)));
        tracker.recordDamage(ATTACKING_TEAM, INNER, rowena, true, false);
        assertNull(tracker.achievementOwner(ATTACKING_TEAM, Set.of(OUTER), Set.of(OUTER, INNER)));
    }

    @Test
    void anyOtherPlayerTowerDamageVoidsTheRun() {
        RowenaTowerAchievementTracker tracker = new RowenaTowerAchievementTracker();
        UUID rowena = UUID.randomUUID();
        tracker.recordDamage(ATTACKING_TEAM, OUTER, rowena, true, false);
        tracker.recordDamage(ATTACKING_TEAM, INNER, rowena, true, false);
        tracker.recordDamage(ATTACKING_TEAM, INNER, UUID.randomUUID(), false, false);
        assertNull(tracker.achievementOwner(ATTACKING_TEAM,
                Set.of(OUTER, INNER), Set.of(OUTER, INNER)));
    }

    @Test
    void lightArrowsFromDifferentRowenasCannotCombineTheirProgress() {
        RowenaTowerAchievementTracker tracker = new RowenaTowerAchievementTracker();
        tracker.recordDamage(ATTACKING_TEAM, OUTER, UUID.randomUUID(), true, false);
        tracker.recordDamage(ATTACKING_TEAM, INNER, UUID.randomUUID(), true, false);
        assertNull(tracker.achievementOwner(ATTACKING_TEAM,
                Set.of(OUTER, INNER), Set.of(OUTER, INNER)));
    }

    @Test
    void challengeStateDoesNotLeakAfterCleanup() {
        RowenaTowerAchievementTracker tracker = new RowenaTowerAchievementTracker();
        UUID rowena = UUID.randomUUID();
        tracker.recordDamage(ATTACKING_TEAM, OUTER, rowena, true, false);
        tracker.recordDamage(ATTACKING_TEAM, INNER, rowena, true, false);
        tracker.clear();
        assertNull(tracker.achievementOwner(ATTACKING_TEAM,
                Set.of(OUTER, INNER), Set.of(OUTER, INNER)));
    }
}
