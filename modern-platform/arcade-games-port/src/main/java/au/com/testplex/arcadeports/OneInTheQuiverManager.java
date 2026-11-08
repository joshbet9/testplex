package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Quiver's ranking and below-name kill counter, driven by the shared session. */
final class OneInTheQuiverManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.ONE_IN_THE_QUIVER; }

    List<Location> spawnLocations(World world, SourceMapCatalog.MapData mapData, int count, Logger logger) {
        if (world == null || mapData == null || mapData.spawnLocations().isEmpty()) {
            logger.severe("One in the Quiver selected map has no authored spawn markers.");
            return List.of();
        }
        List<Location> safe = new ArrayList<>();
        for (Location authored : mapData.spawnLocations()) {
            Location resolved = isSafeSpawn(authored) ? authored.clone() : findSafeSpawnNearby(authored);
            if (resolved != null) safe.add(resolved);
        }
        if (safe.isEmpty()) {
            logger.severe("One in the Quiver selected map has no safe surface near its authored spawns.");
            return List.of();
        }
        if (safe.size() != mapData.spawnLocations().size()) {
            logger.warning("One in the Quiver resolved " + safe.size() + "/"
                    + mapData.spawnLocations().size() + " safe authored spawns on " + mapData.mapName() + ".");
        }
        List<Location> chosen = new ArrayList<>();
        for (int i = 0; i < Math.max(1, count); i++) chosen.add(safe.get(i % safe.size()).clone());
        return chosen;
    }

    static boolean isSafeSpawn(Location candidate) {
        if (candidate == null || candidate.getWorld() == null
                || candidate.getY() < candidate.getWorld().getMinHeight() + 1
                || candidate.getY() >= candidate.getWorld().getMaxHeight() - 2) return false;
        Block feet = candidate.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        return feet.isPassable() && head.isPassable() && ground.getType().isSolid()
                && !feet.isLiquid() && !head.isLiquid() && !ground.isLiquid()
                && !isSpawnHazard(feet.getType())
                && !isSpawnHazard(head.getType())
                && !isSpawnHazard(ground.getType());
    }

    static boolean isSpawnHazard(Material material) {
        return material == Material.LAVA || material == Material.FIRE || material == Material.SOUL_FIRE
                || material == Material.MAGMA_BLOCK || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH || material == Material.POWDER_SNOW
                || material == Material.WITHER_ROSE;
    }

    private static Location findSafeSpawnNearby(Location base) {
        if (base == null || base.getWorld() == null) return null;
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int offset = 0; offset <= 4; offset++) {
                        for (int sign : new int[] {1, -1}) {
                            if (offset == 0 && sign < 0) continue;
                            Location candidate = base.clone().add(dx, offset * sign, dz);
                            if (isSafeSpawn(candidate)) return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session.type != ArcadePortGame.ONE_IN_THE_QUIVER || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("quiver", "dummy",
                ChatColor.GOLD + "ONE IN THE QUIVER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Objective belowName = scoreboard.registerNewObjective("quiverkills", "dummy",
                ChatColor.RED + "Kills");
        belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
        session.quiverScoreboard = scoreboard;
        session.quiverScoreboardObjective = objective;
        session.quiverBelowNameObjective = belowName;
        for (int i = 0; i < 14; i++) {
            Team line = scoreboard.registerNewTeam("qvrank" + i);
            String entry = ChatColor.values()[i].toString();
            line.addEntry(entry);
            session.quiverScoreboardTeams.add(line);
            session.quiverScoreboardEntries.add(entry);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session);
    }

    void updateScoreboard(SessionState session) {
        if (session == null) return;
        Scoreboard scoreboard = session.quiverScoreboard;
        Objective objective = session.quiverScoreboardObjective;
        Objective belowName = session.quiverBelowNameObjective;
        if (scoreboard == null || objective == null || session.type != ArcadePortGame.ONE_IN_THE_QUIVER) return;
        List<Player> ranked = new ArrayList<>(session.players.values());
        ranked.removeIf(player -> player == null || !player.isOnline());
        ranked.sort((left, right) -> {
            int byKills = Integer.compare(session.scores.getOrDefault(right.getUniqueId(), 0),
                    session.scores.getOrDefault(left.getUniqueId(), 0));
            return byKills != 0 ? byKills
                    : String.CASE_INSENSITIVE_ORDER.compare(left.getName(), right.getName());
        });
        for (int i = 0; i < session.quiverScoreboardTeams.size(); i++) {
            Team line = session.quiverScoreboardTeams.get(i);
            String entry = session.quiverScoreboardEntries.get(i);
            if (i < ranked.size()) {
                Player rankedPlayer = ranked.get(i);
                boolean alive = session.alive.contains(rankedPlayer.getUniqueId());
                int kills = session.scores.getOrDefault(rankedPlayer.getUniqueId(), 0);
                line.setPrefix((alive ? ChatColor.GREEN : ChatColor.RED) + "#" + (i + 1)
                        + " " + rankedPlayer.getName());
                line.setSuffix(ChatColor.GRAY + " " + kills + "/" + ArcadePortRules.quiverScoreToWin());
                objective.getScore(entry).setScore(session.quiverScoreboardTeams.size() - i);
            } else {
                line.setPrefix("");
                line.setSuffix("");
                scoreboard.resetScores(entry);
            }
        }
        if (belowName != null) {
            for (Player player : ranked) {
                belowName.getScore(player.getName()).setScore(
                        session.scores.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }
}
