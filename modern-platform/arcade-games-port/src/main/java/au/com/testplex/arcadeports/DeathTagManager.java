package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Death Tag's game-specific sidebar and tracking compass.
 * Shared match ownership, infection rules and event listeners remain in
 * {@link ArcadePortManager}; this controller uses the existing game cadence.
 */
final class DeathTagManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DEATH_TAG; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupDeathTagScoreboard(session);
    }

    void updateDeathTagCompass(SessionState session) {
        List<Player> alive = alivePlayers(session);
        for (Player viewer : alive) {
            boolean viewerChaser = session.tagged.contains(viewer.getUniqueId());
            Player target = alive.stream()
                    .filter(candidate -> session.tagged.contains(candidate.getUniqueId()) != viewerChaser)
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(viewer.getLocation())))
                    .orElse(null);
            if (target == null) continue;

            boolean hasCompass = false;
            for (ItemStack item : viewer.getInventory().getContents()) {
                if (isDeathTagTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                viewer.getInventory().addItem(namedTrackingCompass("Tracks the nearest opposing Death Tag player."));
            }
            viewer.setCompassTarget(target.getLocation());
        }
    }

    boolean isDeathTagTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    private void setupDeathTagScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("deathtag", "dummy",
                ChatColor.GOLD + "DEATH TAG");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.deathTagScoreboard = scoreboard;
        session.deathTagScoreboardObjective = objective;

        session.deathTagRunnerScoreboardLine = scoreboard.registerNewTeam("dtrunners");
        session.deathTagRunnerScoreboardEntry = ChatColor.GREEN.toString();
        session.deathTagRunnerScoreboardLine.addEntry(session.deathTagRunnerScoreboardEntry);
        session.deathTagRunnerScoreboardLine.setPrefix(ChatColor.GREEN.toString());
        objective.getScore(session.deathTagRunnerScoreboardEntry).setScore(3);

        Team blankLine = scoreboard.registerNewTeam("dtblank");
        String blankEntry = ChatColor.BLACK.toString();
        blankLine.addEntry(blankEntry);
        blankLine.setPrefix(" ");
        objective.getScore(blankEntry).setScore(2);

        session.deathTagChaserScoreboardLine = scoreboard.registerNewTeam("dtchasers");
        session.deathTagChaserScoreboardEntry = ChatColor.RED.toString();
        session.deathTagChaserScoreboardLine.addEntry(session.deathTagChaserScoreboardEntry);
        session.deathTagChaserScoreboardLine.setPrefix(ChatColor.RED.toString());
        objective.getScore(session.deathTagChaserScoreboardEntry).setScore(1);

        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateDeathTagScoreboard(session);
    }

    void updateDeathTagScoreboard(SessionState session) {
        if (session == null || session.deathTagScoreboardObjective == null) return;
        int chasers = (int) session.tagged.stream().filter(session.alive::contains).count();
        int runners = Math.max(0, session.alive.size() - chasers);
        if (session.deathTagRunnerScoreboardLine != null) {
            session.deathTagRunnerScoreboardLine.setSuffix(ChatColor.WHITE + " " + runners + " Runners");
        }
        if (session.deathTagChaserScoreboardLine != null) {
            session.deathTagChaserScoreboardLine.setSuffix(ChatColor.WHITE + " " + chasers + " Chasers");
        }
    }

    private static ItemStack namedTrackingCompass(String lore) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<Player> alivePlayers(SessionState session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }
}
