package au.com.testplex.snake.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.Objects;

/** Fired once when a player consumes a green slime, matching Reborn's SlimeUpgradeEvent. */
public final class SlimeUpgradeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    public SlimeUpgradeEvent(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }
    public Player getPlayer() { return player; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
