package au.com.testplex.arcadeports;

/** Guards the impact fallback for arrows that did not produce a normal damage event. */
final class TurfWarsProjectileImpactRules {
    private TurfWarsProjectileImpactRules() { }

    static boolean shouldApplyFallback(boolean hitPlayer, boolean playerShooter) {
        return hitPlayer && playerShooter;
    }
}
