package au.com.testplex.arcadeports;

/** Exact match-result predicates for Plexball's kit-specific achievement conditions. */
final class PlexballAchievementRules {
    private PlexballAchievementRules() { }

    static boolean walkOffPowerDive(boolean wonMatch, boolean fairHomeRunWhileDiveActive) {
        return wonMatch && fairHomeRunWhileDiveActive;
    }

    static boolean sureHands(boolean wonMatch, boolean technician, int consecutiveAbilityCatches) {
        return wonMatch && technician && consecutiveAbilityCatches >= 3;
    }

    static boolean bendItIn(boolean wonMatch, boolean curveArtist, int fairCurvedHits) {
        return wonMatch && curveArtist && fairCurvedHits >= 3;
    }

    static boolean firstToHome(boolean wonMatch, boolean sprinter, int burstHomeRuns) {
        return wonMatch && sprinter && burstHomeRuns >= 2;
    }

    static boolean highPoint(boolean wonMatch, boolean skyguard, int elevatedCatches) {
        return wonMatch && skyguard && elevatedCatches >= 3;
    }
}
