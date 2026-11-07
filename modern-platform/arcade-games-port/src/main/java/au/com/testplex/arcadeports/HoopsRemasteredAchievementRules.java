package au.com.testplex.arcadeports;

/** Deterministic event predicates used by Hoops achievement detection. */
final class HoopsRemasteredAchievementRules {
    private HoopsRemasteredAchievementRules() { }

    static boolean isHopsDunk(boolean dunk, boolean hopsSinceGround) {
        return dunk && hopsSinceGround;
    }

    static boolean isAlleyOopDunk(boolean dunk, boolean hopsSinceGround,
            boolean receivedTeammatePassAfterHop) {
        return dunk && hopsSinceGround && receivedTeammatePassAfterHop;
    }

    static boolean isStealScore(boolean eligibleSinceSteal, boolean personallyScored) {
        return eligibleSinceSteal && personallyScored;
    }

    static boolean isBlockedShotRecovery(boolean blockedShot, int blockerTeam, int recoveringTeam) {
        return blockedShot && blockerTeam >= 0 && blockerTeam == recoveringTeam;
    }

    static boolean isGameWinningThree(boolean winningBasket, int points) {
        return winningBasket && points == 3;
    }

    static boolean isValidBuzzerAssist(long now, long passedAt, int passerTeam, int scoringTeam) {
        return passerTeam == scoringTeam && HoopsRemasteredRules.isAssistEligible(now, passedAt);
    }
}
