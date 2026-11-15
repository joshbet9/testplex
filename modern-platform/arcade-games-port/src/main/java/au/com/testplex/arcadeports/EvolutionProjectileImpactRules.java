package au.com.testplex.arcadeports;

/** Event filtering for Evolution's custom ability projectile impacts. */
public final class EvolutionProjectileImpactRules {
    private EvolutionProjectileImpactRules() {
    }

    /**
     * Native damage cancellation must not suppress the custom hit behavior for a
     * projectile launched by an Evolution ability. Other games retain Bukkit's
     * normal ignore-cancelled behavior.
     */
    public static boolean shouldProcessHit(ArcadePortGame game, boolean trackedEvolutionProjectile,
            boolean eventCancelled) {
        return !eventCancelled || (game == ArcadePortGame.EVOLUTION && trackedEvolutionProjectile);
    }
}
