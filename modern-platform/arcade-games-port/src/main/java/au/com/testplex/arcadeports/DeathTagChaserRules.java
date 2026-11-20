package au.com.testplex.arcadeports;

/** Player-representation Skeletons must not burn in daylight. */
final class DeathTagChaserRules {
    private DeathTagChaserRules() { }
    static boolean shouldBurnInDaylight() { return false; }
}
