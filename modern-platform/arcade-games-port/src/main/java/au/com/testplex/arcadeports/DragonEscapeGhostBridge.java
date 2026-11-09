package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Viewer-local player-model renderer for Dragon Escape PB ghosts.
 *
 * Kept free of PacketEvents types so the Arcade module can still boot when the
 * optional packet plugin is not installed.
 */
public interface DragonEscapeGhostBridge {
    void update(Player viewer, Location location, boolean sneaking, boolean sprinting, boolean onGround);
    default void update(Player viewer, UUID sourcePlayerId, Location location,
                        boolean sneaking, boolean sprinting, boolean onGround) {
        update(viewer, location, sneaking, sprinting, onGround);
    }
    void hide(Player viewer);
}
