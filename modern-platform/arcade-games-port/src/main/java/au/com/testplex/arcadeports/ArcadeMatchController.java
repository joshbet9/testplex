package au.com.testplex.arcadeports;

import java.util.Set;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Game-specific mechanics for matches coordinated by {@link ArcadePortManager}.
 *
 * <p>This is deliberately separate from {@link ArcadeGameModule}: external game
 * modules manage their own startup, whereas these controllers run inside the
 * existing shared Arcade session. They must not register their own listeners,
 * create a second tick loop or take ownership of match selection and teardown.
 */
interface ArcadeMatchController {
    ArcadePortGame game();

    /** Allows a single controller to serve selectable variants of one game. */
    default Set<ArcadePortGame> games() {
        return Set.of(game());
    }

    /** Invoked at the game's existing preparation point, after shared setup. */
    default void onPrepare(SessionState session, List<Player> participants) { }

    /** Invoked from the shared match tick, not a new scheduled task. */
    default void onTick(SessionState session, long now) { }

    /** Invoked during existing session cleanup, before player snapshots restore. */
    default void onFinish(SessionState session) { }
}
