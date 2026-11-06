package au.com.testplex.arcadeports;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeathTagAchievementTrackerTest {
    @Test
    void hastyHunterNeedsThreeRunnerConversionsWithinRollingThirtySeconds() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID chaser = UUID.randomUUID();
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 10_000));
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 25_000));
        assertTrue(tracker.recordChaserKillForHastyHunter(chaser, 40_000));
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 100_000));
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 120_000));
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 151_000));
        assertFalse(tracker.recordChaserKillForHastyHunter(chaser, 152_000));
        assertTrue(tracker.recordChaserKillForHastyHunter(chaser, 153_000));
    }

    @Test
    void lotsOfSubjectMatterNeedsFiveDifferentChasersFromAnyRunnerKit() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID runner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        assertFalse(tracker.recordRunnerMeleeHit(runner, first));
        assertFalse(tracker.recordRunnerMeleeHit(runner, first));
        for (int i = 0; i < 3; i++) {
            assertFalse(tracker.recordRunnerMeleeHit(runner, UUID.randomUUID()));
        }
        assertTrue(tracker.recordRunnerMeleeHit(runner, UUID.randomUUID()));
        assertFalse(tracker.recordRunnerMeleeHit(runner, first));
    }

    @Test
    void fletchingFrenzyCountsTenRegisteredHitsRatherThanShots() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID archer = UUID.randomUUID();
        for (int hit = 1; hit < 10; hit++) assertFalse(tracker.recordArcherHit(archer));
        assertTrue(tracker.recordArcherHit(archer));
        assertFalse(tracker.recordArcherHit(archer));
    }

    @Test
    void smokeAndMirrorsNeedsNonlethalDamageFromThreeDistinctChasers() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID runner = UUID.randomUUID();
        UUID chaser = UUID.randomUUID();
        tracker.recordChaserDamageToRunner(chaser, runner);
        tracker.recordChaserDamageToRunner(chaser, runner);
        assertFalse(tracker.survivedThreeChasers(runner));
        tracker.recordChaserDamageToRunner(UUID.randomUUID(), runner);
        assertFalse(tracker.survivedThreeChasers(runner));
        tracker.recordChaserDamageToRunner(UUID.randomUUID(), runner);
        assertTrue(tracker.survivedThreeChasers(runner));
        assertFalse(tracker.survivedThreeChasers(UUID.randomUUID()));
    }

    @Test
    void shellShockRequiresOneTurtleToTripleHitEachOfTwoDifferentChasers() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID turtle = UUID.randomUUID();
        UUID secondTurtle = UUID.randomUUID();
        UUID chaserA = UUID.randomUUID();
        UUID chaserB = UUID.randomUUID();
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 1_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 2_000));
        assertFalse(tracker.recordTurtleHit(secondTurtle, chaserA, 3_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 4_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserB, 5_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserB, 6_000));
        assertTrue(tracker.recordTurtleHit(turtle, chaserB, 7_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserB, 8_000));
    }

    @Test
    void shellShockResetsAHitChainAfterFiveSeconds() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID turtle = UUID.randomUUID();
        UUID chaserA = UUID.randomUUID();
        UUID chaserB = UUID.randomUUID();
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 1_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 2_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 7_001));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 7_002));
        assertFalse(tracker.recordTurtleHit(turtle, chaserA, 7_003));
        assertFalse(tracker.recordTurtleHit(turtle, chaserB, 8_000));
        assertFalse(tracker.recordTurtleHit(turtle, chaserB, 8_001));
        assertTrue(tracker.recordTurtleHit(turtle, chaserB, 8_002));
    }

    @Test
    void gallowsCountsOnlyTimedTeammateKnockbackAssistsAndConsumesEachOnce() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID traitor = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        tracker.recordTraitorKnockback(traitor, victim, 1_000);
        assertEquals(traitor, tracker.consumeTraitorAssist(victim, 9_000));
        assertNull(tracker.consumeTraitorAssist(victim, 9_001));
        assertFalse(tracker.creditTraitorAssist(traitor));
        assertFalse(tracker.creditTraitorAssist(traitor));
        assertTrue(tracker.creditTraitorAssist(traitor));
        assertFalse(tracker.creditTraitorAssist(traitor));
        tracker.recordTraitorKnockback(traitor, victim, 12_000);
        assertNull(tracker.consumeTraitorAssist(victim, 20_001));
    }

    @Test
    void patientZeroNeedsFourGenerationsStartingWithOriginalAlphasFirstVictim() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID alpha = UUID.randomUUID();
        UUID otherForcedAlpha = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        assertNull(tracker.recordConversion(alpha, null, true));
        assertNull(tracker.recordConversion(otherForcedAlpha, null, true));
        assertNull(tracker.recordConversion(a, alpha, false));

        // The Alpha's second direct victim must not establish an alternate chain.
        UUID secondVictim = UUID.randomUUID();
        assertNull(tracker.recordConversion(secondVictim, alpha, false));
        UUID wrongB = UUID.randomUUID();
        UUID wrongC = UUID.randomUUID();
        UUID wrongD = UUID.randomUUID();
        assertNull(tracker.recordConversion(wrongB, secondVictim, false));
        assertNull(tracker.recordConversion(wrongC, wrongB, false));
        assertNull(tracker.recordConversion(wrongD, wrongC, false));
        assertNull(tracker.recordConversion(UUID.randomUUID(), otherForcedAlpha, false));

        assertNull(tracker.recordConversion(b, a, false));
        assertNull(tracker.recordConversion(c, b, false));
        assertEquals(alpha, tracker.recordConversion(d, c, false));
        assertNull(tracker.recordConversion(UUID.randomUUID(), d, false));
    }

    @Test
    void forcedPromotionsCannotExtendPatientZeroLineage() {
        DeathTagAchievementTracker tracker = new DeathTagAchievementTracker();
        UUID alpha = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID forcedB = UUID.randomUUID();
        assertNull(tracker.recordConversion(alpha, null, true));
        assertNull(tracker.recordConversion(a, alpha, false));
        assertNull(tracker.recordConversion(forcedB, a, true));
        assertNull(tracker.recordConversion(UUID.randomUUID(), forcedB, false));
    }
}
