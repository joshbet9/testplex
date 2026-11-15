package au.com.testplex.arcadeports;

/** Geometry and damage throttling for source-style tower step-block hits. */
final class HeroesOfGwenTowerInteractionRules {
    private static final double STEP_BLOCK_RADIUS_SQUARED = 16.0D;

    private HeroesOfGwenTowerInteractionRules() { }

    static boolean isStepHitBlock(double distanceSquaredFromTower) {
        return Double.isFinite(distanceSquaredFromTower)
                && distanceSquaredFromTower >= 0.0D
                && distanceSquaredFromTower <= STEP_BLOCK_RADIUS_SQUARED;
    }
}
