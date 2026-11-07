package au.com.testplex.arcadeports;

import java.util.Set;

/** Pure qualification rules for Gladiators' tournament achievements. */
final class GladiatorsAchievementRules {
    static final int SWORD_HIT = 1;
    static final int BOW_HIT = 1 << 1;
    static final int ROD_HIT = 1 << 2;
    private static final int FULL_ARSENAL = SWORD_HIT | BOW_HIT | ROD_HIT;
    private static final int BLADE_PROTOCOL_STREAK = 3;
    private static final int UNTOUCHABLE_WINS = 10;

    private GladiatorsAchievementRules() { }

    static int addHit(int hitMask, int hitType) {
        return hitMask | hitType;
    }

    static boolean hasFullArsenal(int hitMask) {
        return (hitMask & FULL_ARSENAL) == FULL_ARSENAL;
    }

    static boolean countsAsPrecisionArrowHit(boolean hitPlayer, boolean sameDuel, boolean targetAlive) {
        return hitPlayer && sameDuel && targetAlive;
    }

    static boolean isTimedCritical(boolean onGround, double fallDistance, double verticalVelocity,
            boolean inWater, boolean blinded, boolean riding) {
        return !onGround && fallDistance > 0.0D && verticalVelocity < 0.0D
                && !inWater && !blinded && !riding;
    }

    static int nextTimedCriticalStreak(int current, boolean timedCritical, boolean receivedOpponentHit) {
        if (receivedOpponentHit || !timedCritical) return 0;
        return Math.max(0, current) + 1;
    }

    static boolean qualifiesBladeProtocol(boolean modernMode, boolean duelWinner, int streak) {
        return modernMode && duelWinner && streak >= BLADE_PROTOCOL_STREAK;
    }

    static boolean qualifiesUntouchableDuelWin(boolean duelWinner, double damageTaken, boolean forfeit) {
        return duelWinner && !forfeit && Double.isFinite(damageTaken) && damageTaken <= 0.0D;
    }

    static boolean qualifiesUntouchable(int qualifyingDuelWins) {
        return qualifyingDuelWins >= UNTOUCHABLE_WINS;
    }

    static boolean qualifiesSecondWind(boolean resetFinal) {
        return resetFinal;
    }

    static boolean qualifiesAllModes(Set<String> wonModes) {
        if (wonModes == null) return false;
        for (GladiatorsMode mode : GladiatorsMode.values()) {
            if (!wonModes.contains(mode.id())) return false;
        }
        return true;
    }

    static boolean qualifiesFullArsenalTournament(boolean normalMode, int duelsFought, boolean anyDuelIncomplete) {
        return normalMode && duelsFought > 0 && !anyDuelIncomplete;
    }

    static boolean qualifiesSilkySecondsLoss(double opposingWinnerHealth, boolean forfeit) {
        return Double.isFinite(opposingWinnerHealth)
                && !forfeit && opposingWinnerHealth > 0.0D && opposingWinnerHealth <= 1.0D;
    }
}
