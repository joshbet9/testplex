package au.com.testplex.arcadeports;

/** Routing for hits resolved by custom game logic instead of Bukkit health damage. */
final class ArcadeCustomHitFeedback {
    private ArcadeCustomHitFeedback() {}

    static boolean shouldShowLegacyHurtFeedback(ArcadePortGame game, boolean hitApplied) {
        if (!hitApplied || game == null) return false;
        return game == ArcadePortGame.PAINTBALL || game == ArcadePortGame.SPLEEF;
    }
}
