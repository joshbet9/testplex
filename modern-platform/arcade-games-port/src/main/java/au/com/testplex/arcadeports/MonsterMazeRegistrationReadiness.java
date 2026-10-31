package au.com.testplex.arcadeports;

import java.util.function.BooleanSupplier;

/** Monotonic startup state for the embedded Monster Maze Arcade registration. */
final class MonsterMazeRegistrationReadiness {
    enum State { NEW, INITIALIZING, INITIALIZED, ATTACHED, FAILED }

    private State state = State.NEW;

    void initializeAndAttach(Runnable initialize, BooleanSupplier gameManagerInitialized,
                             Runnable attach, BooleanSupplier acceptedByArcadeManager) {
        beginInitialization();
        try {
            initialize.run();
            markInitialized(gameManagerInitialized.getAsBoolean());
            attach.run();
            markAttached(acceptedByArcadeManager.getAsBoolean());
        } catch (RuntimeException | Error startupFailure) {
            markFailed();
            throw startupFailure;
        }
    }

    synchronized void beginInitialization() {
        require(State.NEW, "initialization can only begin once");
        state = State.INITIALIZING;
    }

    synchronized void markInitialized(boolean gameManagerAvailable) {
        require(State.INITIALIZING, "module initialization has not begun");
        if (!gameManagerAvailable) fail("Monster Maze GameManager was not initialized.");
        state = State.INITIALIZED;
    }

    synchronized void markAttached(boolean acceptedByArcadeManager) {
        require(State.INITIALIZED, "module cannot attach before initialization succeeds");
        if (!acceptedByArcadeManager) fail("Testplex Arcade manager did not accept Monster Maze.");
        state = State.ATTACHED;
    }

    synchronized void markFailed() {
        state = State.FAILED;
    }

    synchronized boolean isReady() { return state == State.ATTACHED; }
    synchronized State state() { return state; }

    synchronized void requireReady() {
        if (state != State.ATTACHED) {
            throw new IllegalStateException("Monster Maze is not registered with Testplex Arcade (state=" + state + ").");
        }
    }

    private void require(State expected, String message) {
        if (state != expected) throw new IllegalStateException(message + " (state=" + state + ").");
    }

    private void fail(String message) {
        state = State.FAILED;
        throw new IllegalStateException(message);
    }
}
