package au.com.testplex.arcadeports;

/** Wind Charges are earned through melee; at most three can be held. */
final class DeathTagTurtleChargeProgress {
    static final int MAX_HELD_CHARGES = 3;
    record HitResult(int hitProgress, boolean awardsCharge) { }
    private DeathTagTurtleChargeProgress() { }
    static HitResult recordHit(int currentHitProgress, int hitsPerCharge, int heldCharges) {
        int required = Math.max(1, hitsPerCharge);
        if (heldCharges >= MAX_HELD_CHARGES) return new HitResult(0, false);
        int next = Math.max(0, currentHitProgress) + 1;
        return next >= required ? new HitResult(0, true) : new HitResult(next, false);
    }
    static float experienceProgress(int hitProgress, int hitsPerCharge, int heldCharges) {
        if (heldCharges >= MAX_HELD_CHARGES) return 0.0F;
        int required = Math.max(1, hitsPerCharge);
        return Math.min(1.0F, Math.max(0, hitProgress) / (float) required);
    }
}
