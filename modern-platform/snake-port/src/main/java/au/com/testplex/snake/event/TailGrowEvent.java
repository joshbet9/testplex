package au.com.testplex.snake.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.Objects;

/** Fired once for every sheep segment appended to a Snake, matching Reborn's TailGrowEvent. */
public final class TailGrowEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int length;
    public TailGrowEvent(Player player, int length) {
        this.player = Objects.requireNonNull(player, "player");
        this.length = length;
    }
    public Player getPlayer() { return player; }
    public int getLength() { return length; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
