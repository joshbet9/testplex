package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GladiatorsAchievementRulesTest {
    @Test
    void fullArsenalRequiresEveryCombatToolAndACompletedTournamentPath() {
        int hits = GladiatorsAchievementRules.addHit(0, GladiatorsAchievementRules.SWORD_HIT);
        hits = GladiatorsAchievementRules.addHit(hits, GladiatorsAchievementRules.BOW_HIT);
        assertFalse(GladiatorsAchievementRules.hasFullArsenal(hits));
        hits = GladiatorsAchievementRules.addHit(hits, GladiatorsAchievementRules.ROD_HIT);
        assertTrue(GladiatorsAchievementRules.hasFullArsenal(hits));
        assertTrue(GladiatorsAchievementRules.countsAsPrecisionArrowHit(true, true, true));
        assertFalse(GladiatorsAchievementRules.countsAsPrecisionArrowHit(false, true, true));
        assertFalse(GladiatorsAchievementRules.countsAsPrecisionArrowHit(true, false, true));
        assertFalse(GladiatorsAchievementRules.countsAsPrecisionArrowHit(true, true, false));
        assertTrue(GladiatorsAchievementRules.qualifiesFullArsenalTournament(true, 4, false));
        assertFalse(GladiatorsAchievementRules.qualifiesFullArsenalTournament(true, 0, false));
        assertFalse(GladiatorsAchievementRules.qualifiesFullArsenalTournament(false, 4, false));
        assertFalse(GladiatorsAchievementRules.qualifiesFullArsenalTournament(true, 4, true));
    }

    @Test
    void bladeProtocolRequiresThreeTimedCriticalsInModernModeAndResetsOnInterruption() {
        assertTrue(GladiatorsAchievementRules.isTimedCritical(false, 0.1D, -0.1D, false, false, false));
        assertFalse(GladiatorsAchievementRules.isTimedCritical(true, 0.1D, -0.1D, false, false, false));
        assertFalse(GladiatorsAchievementRules.isTimedCritical(false, 0.1D, 0.1D, false, false, false));
        assertFalse(GladiatorsAchievementRules.isTimedCritical(false, 0.1D, -0.1D, true, false, false));
        assertFalse(GladiatorsAchievementRules.isTimedCritical(false, 0.1D, -0.1D, false, true, false));
        assertFalse(GladiatorsAchievementRules.isTimedCritical(false, 0.1D, -0.1D, false, false, true));

        int streak = GladiatorsAchievementRules.nextTimedCriticalStreak(0, true, false);
        streak = GladiatorsAchievementRules.nextTimedCriticalStreak(streak, true, false);
        assertFalse(GladiatorsAchievementRules.qualifiesBladeProtocol(true, true, streak));
        streak = GladiatorsAchievementRules.nextTimedCriticalStreak(streak, true, false);
        assertTrue(GladiatorsAchievementRules.qualifiesBladeProtocol(true, true, streak));
        assertFalse(GladiatorsAchievementRules.qualifiesBladeProtocol(false, true, streak));
        assertEquals(0, GladiatorsAchievementRules.nextTimedCriticalStreak(streak, true, true));
        assertEquals(0, GladiatorsAchievementRules.nextTimedCriticalStreak(streak, false, false));
    }

    @Test
    void modeCollectionRequiresAWinInAllFiveModes() {
        Set<String> wins = new HashSet<>();
        for (GladiatorsMode mode : GladiatorsMode.values()) {
            if (!mode.id().equals(GladiatorsMode.POT.id())) wins.add(mode.id());
        }
        assertFalse(GladiatorsAchievementRules.qualifiesAllModes(wins));
        wins.add(GladiatorsMode.POT.id());
        assertTrue(GladiatorsAchievementRules.qualifiesAllModes(wins));
    }

    @Test
    void untouchableRequiresTenDamageFreeDuelWinsAndRejectsForfeits() {
        assertTrue(GladiatorsAchievementRules.qualifiesUntouchableDuelWin(true, 0.0D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesUntouchableDuelWin(false, 0.0D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesUntouchableDuelWin(true, 0.01D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesUntouchableDuelWin(true, 0.0D, true));
        assertFalse(GladiatorsAchievementRules.qualifiesUntouchable(9));
        assertTrue(GladiatorsAchievementRules.qualifiesUntouchable(10));
    }

    @Test
    void resetFinalAndSilkySecondsUseTheirActualMatchOutcomes() {
        assertTrue(GladiatorsAchievementRules.qualifiesSecondWind(true));
        assertFalse(GladiatorsAchievementRules.qualifiesSecondWind(false));
        assertTrue(GladiatorsAchievementRules.qualifiesSilkySecondsLoss(1.0D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesSilkySecondsLoss(1.01D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesSilkySecondsLoss(0.0D, false));
        assertFalse(GladiatorsAchievementRules.qualifiesSilkySecondsLoss(Double.NaN, false));
        assertFalse(GladiatorsAchievementRules.qualifiesSilkySecondsLoss(1.0D, true));
    }
}
