package au.com.testplex.arcadeports;

/** Small, deterministic rules shared by Evolution gameplay and its regression tests. */
final class EvolutionAchievementRules {
    static final double NEANDERTHAL_PITY_RATE_MULTIPLIER = 1.25D;
    static final double DARWINIST_COOLDOWN_MULTIPLIER = 0.67D;
    static final int BLOOD_HARVEST_KILLS = 3;
    static final int SECOND_WIND_HEALTH_POINTS = 60;

    private EvolutionAchievementRules() { }

    static long neanderthalPityClock(long baseMillis) {
        return Math.max(1L, Math.round(baseMillis / NEANDERTHAL_PITY_RATE_MULTIPLIER));
    }

    static long neanderthalPityReduction(long baseMillis) {
        return Math.max(0L, Math.round(baseMillis * NEANDERTHAL_PITY_RATE_MULTIPLIER));
    }

    static long darwinistCooldown(long baseMillis) {
        return Math.max(1L, Math.round(baseMillis * DARWINIST_COOLDOWN_MULTIPLIER));
    }

    static boolean qualifiesBloodHarvestKill(double healthBeforeKill, double maxHealth,
            double healthAfterKill, int qualifiedKillsInLife) {
        return maxHealth > 0.0D && healthBeforeKill <= maxHealth * 0.25D
                && healthAfterKill >= maxHealth && qualifiedKillsInLife >= BLOOD_HARVEST_KILLS;
    }

    static boolean shouldAwardSecondWind(boolean won, double restoredHealthPoints) {
        return won && restoredHealthPoints >= SECOND_WIND_HEALTH_POINTS;
    }

    static boolean shouldAwardPityParty(boolean won, int successfulPityEvolutions) {
        return won && successfulPityEvolutions >= 2;
    }

    static boolean shouldAwardQuickChangeArtist(boolean won, boolean quickEvolver,
            boolean neverInterrupted, int successfulTransformations) {
        return won && quickEvolver && neverInterrupted && successfulTransformations >= 4;
    }

    static boolean shouldAwardPityTheAncient(boolean won, boolean neanderthal,
            int pityCyclesWithTwoKillReductions) {
        return won && neanderthal && pityCyclesWithTwoKillReductions >= 5;
    }

    static boolean shouldAwardOverkill(boolean won, boolean allEvolutionsQualified) {
        return won && allEvolutionsQualified;
    }

    static boolean shouldAwardGroundbreaker(int distinctOpponentsHit) {
        return distinctOpponentsHit >= 3;
    }

    static boolean shouldAwardBounceHouse(int separateSlamsHittingSameOpponent) {
        return separateSlamsHittingSameOpponent >= 3;
    }

    static boolean shouldAwardEggspert(boolean chickenJumpedThisLife, boolean targetWasAirborne,
            boolean projectileOwnerGotKill) {
        return chickenJumpedThisLife && targetWasAirborne && projectileOwnerGotKill;
    }

    static boolean shouldAwardWrongKitChamp(String selectedKit, int attemptedLevel,
            boolean currentlyEvolving, long elapsedTicks) {
        return "darwinist".equals(selectedKit)
                && ArcadePortRules.shouldAwardEvolutionWrongKitChamp(
                        attemptedLevel, false, currentlyEvolving, elapsedTicks);
    }
}
