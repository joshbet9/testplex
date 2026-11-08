package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
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

/** Dragons' per-match scoreboard, using the existing shared Arcade lifecycle. */
final class DragonsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DRAGONS; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    Player nearestSpectatorTarget(SessionState session, Player spectator) {
        if (session == null || spectator == null) return null;
        return survivingTargets(session).stream()
                .min(Comparator.comparingDouble(target ->
                        target.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    /** CompassModule uses UpdateType.FASTER for the nearest living, non-Tamer target. */
    void updateSpectatorCompasses(SessionState session, long now) {
        if (session == null || now < session.dragonNextCompassUpdateAt) return;
        session.dragonNextCompassUpdateAt = now + 250L;
        List<Player> alive = survivingTargets(session);
        if (alive.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            int compasses = 0;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) compasses += item.getAmount();
            }
            if (compasses <= 0) spectator.getInventory().addItem(trackingCompass());
            Player target = alive.stream().min(Comparator.comparingDouble(player ->
                    player.getLocation().distanceSquared(spectator.getLocation()))).orElse(null);
            if (target == null) continue;
            spectator.setCompassTarget(target.getLocation());
            if (isTrackingCompass(spectator.getInventory().getItemInMainHand())) {
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                spectator.sendActionBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Nearest Target: "
                        + ChatColor.YELLOW + target.getName()
                        + ChatColor.WHITE + ChatColor.BOLD + " | Distance: " + ChatColor.YELLOW
                        + String.format(java.util.Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + ChatColor.BOLD + " | Height: " + ChatColor.YELLOW
                        + String.format(java.util.Locale.ROOT, "%.1f", height));
            }
        }
    }

    private List<Player> survivingTargets(SessionState session) {
        List<Player> result = new ArrayList<>();
        for (UUID playerId : session.alive) {
            Player player = session.players.get(playerId);
            if (player != null && player.isOnline()
                    && !session.dragonTamerControls.containsKey(playerId)) result.add(player);
        }
        return result;
    }

    private ItemStack trackingCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY
                    + "Points to the nearest survivor. Left-click to teleport; right-click for the spectator menu."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("dragons", "dummy",
                ChatColor.GOLD + "DRAGONS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.dragonScoreboard = scoreboard;
        session.dragonScoreboardObjective = objective;
        session.dragonAliveScoreboardTeam = scoreboard.registerNewTeam("drgalive");
        session.dragonAliveScoreboardTeam.setPrefix(ChatColor.GREEN.toString());
        session.dragonDeadScoreboardTeam = scoreboard.registerNewTeam("drgdead");
        session.dragonDeadScoreboardTeam.setPrefix(ChatColor.GRAY.toString());
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session, System.currentTimeMillis());
    }

    void updateScoreboard(SessionState session, long now) {
        if (session == null || session.type != ArcadePortGame.DRAGONS
                || session.dragonScoreboard == null || session.dragonScoreboardObjective == null
                || now < session.dragonNextScoreboardUpdateAt) return;
        session.dragonNextScoreboardUpdateAt = now + 500L; // SoloGame.ScoreboardUpdate uses UpdateType.FAST.
        Scoreboard scoreboard = session.dragonScoreboard;
        Objective objective = session.dragonScoreboardObjective;
        for (String entry : new ArrayList<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
            if (session.dragonAliveScoreboardTeam.hasEntry(entry)) session.dragonAliveScoreboardTeam.removeEntry(entry);
            if (session.dragonDeadScoreboardTeam.hasEntry(entry)) session.dragonDeadScoreboardTeam.removeEntry(entry);
        }

        List<Player> roster = new ArrayList<>(session.players.values());
        int total = roster.size();
        int alive = session.alive.size();
        int mode = ArcadePortRules.dragonsScoreboardMode(total, alive);
        if (mode == 0) {
            int score = total;
            for (Player player : roster) {
                if (player == null) continue;
                String entry = player.getName();
                if (session.alive.contains(player.getUniqueId())) {
                    session.dragonAliveScoreboardTeam.addEntry(entry);
                } else {
                    session.dragonDeadScoreboardTeam.addEntry(entry);
                }
                objective.getScore(entry).setScore(score--);
            }
        } else if (mode == 1) {
            int score = alive;
            for (Player player : roster) {
                if (player == null || !session.alive.contains(player.getUniqueId())) continue;
                String entry = player.getName();
                session.dragonAliveScoreboardTeam.addEntry(entry);
                objective.getScore(entry).setScore(score--);
            }
        } else {
            objective.getScore(ChatColor.GREEN + "Players Alive").setScore(4);
            objective.getScore(ChatColor.WHITE + Integer.toString(alive)).setScore(3);
            objective.getScore(ChatColor.RED + "Players Dead").setScore(2);
            objective.getScore(ChatColor.WHITE + Integer.toString(Math.max(0, total - alive))).setScore(1);
        }
        for (Player player : roster) {
            if (player != null && player.isOnline() && player.getScoreboard() != scoreboard) {
                player.setScoreboard(scoreboard);
            }
        }
    }
}
