package au.com.testplex.arcadeports;

import java.util.Set;

/** Pure event predicates for Milk the Cow achievements. */
public final class MilkCowAchievementRules {
    public static final double LAST_DROP_MAX_HEALTH = 2.0D;
    public static final int WHOLE_HERD_REQUIRED_COWS = 3;
    public static final int AREA_ABILITY_REQUIRED_FARMERS = 3;
    public static final int STAMPEDE_REQUIRED_FARMERS = 5;
    public static final int STAMPEDE_REQUIRED_ELIMINATIONS = 3;
    public static final int SPILT_MILK_REQUIRED_EMPTYINGS = 3;

    private static final Set<String> FULL_MOO_ABILITIES = Set.of("cow-bomb", "body-slam", "cow-charge");

    private MilkCowAchievementRules() { }

    public static boolean shouldAwardLastDrop(double health, int score, int pointsPerMilk, int scoreToWin) {
        return Double.isFinite(health) && health > 0.0D && health <= LAST_DROP_MAX_HEALTH
                && pointsPerMilk > 0 && score < scoreToWin && score + pointsPerMilk >= scoreToWin;
    }

    public static boolean shouldAwardWholeHerd(int distinctCowPlayers, boolean farmerWon) {
        return farmerWon && distinctCowPlayers >= WHOLE_HERD_REQUIRED_COWS;
    }

    public static boolean shouldAwardAreaAbility(int distinctFarmersHit, int creditedEliminations) {
        return distinctFarmersHit >= AREA_ABILITY_REQUIRED_FARMERS && creditedEliminations > 0;
    }

    public static boolean shouldAwardFullMoo(Set<String> abilitiesUsedOnTarget, boolean creditedElimination) {
        return creditedElimination && abilitiesUsedOnTarget != null
                && abilitiesUsedOnTarget.containsAll(FULL_MOO_ABILITIES);
    }

    public static boolean shouldAwardStampede(int distinctFarmersHit, int creditedEliminations) {
        return distinctFarmersHit >= STAMPEDE_REQUIRED_FARMERS
                && creditedEliminations >= STAMPEDE_REQUIRED_ELIMINATIONS;
    }

    public static boolean shouldAwardSpiltMilk(int qualifyingBucketEmptyings, boolean lostGame) {
        return lostGame && qualifyingBucketEmptyings >= SPILT_MILK_REQUIRED_EMPTYINGS;
    }
}
