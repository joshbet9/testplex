package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Owns the initialization and shutdown order for dedicated Arcade game modules.
 * Registration is atomic: a module that fails initialization is removed and cleaned
 * up, so it can never be advertised as available.
 */
final class ArcadeGameModuleRegistry {
    private final Map<ArcadePortGame, ArcadeGameModule> modules = new LinkedHashMap<>();
    private boolean shutDown;

    synchronized void register(ArcadeGameModule module) {
        if (shutDown) throw new IllegalStateException("The Arcade game-module registry is shut down.");
        if (module == null || module.game() == null || module.games() == null || module.games().isEmpty()) {
            throw new IllegalArgumentException("An Arcade game module and its game identities are required.");
        }
        ArcadePortGame primaryGame = module.game();
        Set<ArcadePortGame> games = new LinkedHashSet<>(module.games());
        if (games.contains(null) || !games.contains(primaryGame)) {
            throw new IllegalArgumentException("A module's game identities must include its primary game and contain no nulls.");
        }
        for (ArcadePortGame game : games) {
            if (modules.containsKey(game)) {
                throw new IllegalStateException("An Arcade module is already registered for " + game.id() + ".");
            }
        }

        for (ArcadePortGame game : games) modules.put(game, module);
        try {
            module.initialize();
        } catch (RuntimeException | Error startupFailure) {
            for (ArcadePortGame game : games) modules.remove(game, module);
            try {
                module.shutdown();
            } catch (RuntimeException | Error cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            throw startupFailure;
        }
    }

    synchronized ArcadeGameModule get(ArcadePortGame game) {
        return game == null ? null : modules.get(game);
    }

    synchronized boolean isRegistered(ArcadePortGame game, ArcadeGameModule module) {
        return game != null && module != null && modules.get(game) == module;
    }

    synchronized boolean isRegistered(ArcadePortGame game) {
        return game != null && modules.containsKey(game);
    }

    synchronized List<ArcadePortGame> registeredGames() {
        return List.copyOf(modules.keySet());
    }

    void shutdown() {
        List<ArcadeGameModule> shutdownOrder;
        synchronized (this) {
            if (shutDown) return;
            shutDown = true;
            // Several selectable game identities can refer to one shared module.
            // Initialize and shut down that controller once, never once per alias.
            shutdownOrder = new ArrayList<>(new LinkedHashSet<>(modules.values()));
            Collections.reverse(shutdownOrder);
            modules.clear();
        }

        Throwable firstFailure = null;
        for (ArcadeGameModule module : shutdownOrder) {
            try {
                module.shutdown();
            } catch (RuntimeException | Error failure) {
                if (firstFailure == null) firstFailure = failure;
                else firstFailure.addSuppressed(failure);
            }
        }
        if (firstFailure != null) {
            throw new IllegalStateException("One or more Arcade game modules failed to shut down.", firstFailure);
        }
    }
}
