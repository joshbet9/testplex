package au.com.testplex.arcadeports;

/**
 * Lifecycle contract for game controllers owned by the single Arcade Bukkit plugin.
 * Implementations own game mechanics only; the Arcade manager owns registration,
 * startup ordering, match arbitration, and shutdown.
 */
public interface ArcadeGameModule {
    ArcadePortGame game();

    /** Every selectable Arcade identity served by this module; initialization still runs once. */
    default java.util.Set<ArcadePortGame> games() {
        return java.util.Set.of(game());
    }

    void initialize();

    void shutdown();

    boolean isGameRunning();
}
