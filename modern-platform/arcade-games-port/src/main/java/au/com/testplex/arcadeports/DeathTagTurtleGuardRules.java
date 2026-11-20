package au.com.testplex.arcadeports;

/** Defensive Turtle counterplay. Basher retains the movement-control role. */
final class DeathTagTurtleGuardRules {
    static final long DURATION_MILLIS = 2_500L;
    static final long COOLDOWN_MILLIS = 30_000L;

    private DeathTagTurtleGuardRules() { }

    static boolean isActive(long now, long until) {
        return now < until;
    }

    static double damageAfterGuard(double ordinaryDamage, boolean active) {
        return Math.max(0.0D, ordinaryDamage) * (active ? 0.5D : 1.0D);
    }
}
