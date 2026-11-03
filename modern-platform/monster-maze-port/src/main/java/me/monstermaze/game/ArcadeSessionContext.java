package me.monstermaze.game;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Shared Arcade-owned roster and round state exposed to Monster Maze mechanics. */
public interface ArcadeSessionContext {
    Map<UUID, Player> players();
    Set<UUID> participants();
    Set<UUID> alivePlayers();
    Set<UUID> spectators();
    Map<UUID, Integer> scores();
    void phaseChanged(String phase);
}
