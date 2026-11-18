package au.com.testplex.arcadeports;

/** Source-faithful health handling for Bacon Brawl's successful player hits. */
final class BaconBrawlCombatRules {
    private BaconBrawlCombatRules() { }

    /** Source CustomDamageEvent heals the receiver by its damage amount, capped at max health. */
    static double healthAfterHit(double currentHealth, double maximumHealth, double hitDamage) {
        if (!Double.isFinite(currentHealth) || !Double.isFinite(maximumHealth) || maximumHealth <= 0.0D) {
            return currentHealth;
        }
        double current = Math.max(0.0D, Math.min(maximumHealth, currentHealth));
        double restore = Double.isFinite(hitDamage) ? Math.max(0.0D, hitDamage) : 0.0D;
        return Math.min(maximumHealth, current + restore);
    }
}
