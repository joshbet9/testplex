package au.com.testplex.arcadeports;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Lightweight main-thread dispatch table for game-specific match controllers.
 * The shared manager remains responsible for match lifecycle and event routing.
 */
final class ArcadeMatchControllerRegistry {
    private final Map<ArcadePortGame, ArcadeMatchController> controllers =
            new EnumMap<>(ArcadePortGame.class);

    void register(ArcadeMatchController controller) {
        if (controller == null || controller.game() == null || controller.games() == null
                || controller.games().isEmpty()) {
            throw new IllegalArgumentException("A match controller and its game identities are required.");
        }
        Set<ArcadePortGame> games = new LinkedHashSet<>(controller.games());
        if (games.contains(null) || !games.contains(controller.game())) {
            throw new IllegalArgumentException("Controller identities must contain its primary game and no nulls.");
        }
        for (ArcadePortGame game : games) {
            if (controllers.containsKey(game)) {
                throw new IllegalStateException("A match controller is already registered for " + game.id() + ".");
            }
        }
        for (ArcadePortGame game : games) controllers.put(game, controller);
    }

    ArcadeMatchController get(ArcadePortGame game) {
        return game == null ? null : controllers.get(game);
    }

    void prepare(SessionState session, List<Player> participants) {
        ArcadeMatchController controller = session == null ? null : get(session.type);
        if (controller != null) controller.onPrepare(session, participants);
    }

    void tick(SessionState session, long now) {
        ArcadeMatchController controller = session == null ? null : get(session.type);
        if (controller != null) controller.onTick(session, now);
    }

    void finish(SessionState session) {
        ArcadeMatchController controller = session == null ? null : get(session.type);
        if (controller != null) controller.onFinish(session);
    }
}
