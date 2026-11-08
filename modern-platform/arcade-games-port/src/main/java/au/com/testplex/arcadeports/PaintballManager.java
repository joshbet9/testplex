package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Paintball's scoreboard; the shared Arcade runtime retains match ownership. */
final class PaintballManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.PAINTBALL; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("paintball", "dummy", ChatColor.AQUA + "PAINTBALL");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.paintballScoreboard = scoreboard;
        session.paintballScoreboardObjective = objective;
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session, System.currentTimeMillis());
    }

    void updateScoreboard(SessionState session, long now) {
        if (session == null || session.paintballScoreboardObjective == null
                || now < session.paintballNextScoreboardAt) return;
        session.paintballNextScoreboardAt = now + 500L;
        for (String entry : session.paintballScoreboardEntries) {
            session.paintballScoreboard.resetScores(entry);
        }
        session.paintballScoreboardEntries.clear();

        List<String> lines = new ArrayList<>();
        List<Player> living = alivePlayers(session);
        if (living.size() < 13) {
            for (int team = 0; team < 2; team++) {
                final int teamIndex = team;
                ChatColor colour = team == 0 ? ChatColor.AQUA : ChatColor.RED;
                living.stream()
                        .filter(player -> session.teams.getOrDefault(player.getUniqueId(), -1) == teamIndex)
                        .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                        .forEach(player -> lines.add(colour + player.getName()));
            }
        } else {
            for (int team = 0; team < 2; team++) {
                int teamIndex = team;
                ChatColor colour = team == 0 ? ChatColor.AQUA : ChatColor.RED;
                String name = team == 0 ? "Frost" : "Nether";
                long alive = living.stream()
                        .filter(player -> session.teams.getOrDefault(player.getUniqueId(), -1) == teamIndex)
                        .count();
                lines.add(colour + name + ChatColor.WHITE + ": " + alive + colour + " Alive");
            }
        }
        int score = lines.size();
        for (String line : lines) {
            session.paintballScoreboardEntries.add(line);
            session.paintballScoreboardObjective.getScore(line).setScore(score--);
        }
        for (Player player : session.players.values()) {
            if (player != null && player.isOnline() && player.getScoreboard() != session.paintballScoreboard) {
                player.setScoreboard(session.paintballScoreboard);
            }
        }
    }

    void setTeamArmor(SessionState session, Player player) {
        Color teamColor = ArcadePortRules.paintballTeamColor(session.teams.getOrDefault(player.getUniqueId(), 0));
        ItemStack[] armor = new ItemStack[] {
                armorPiece(Material.LEATHER_BOOTS, teamColor),
                armorPiece(Material.LEATHER_LEGGINGS, teamColor),
                armorPiece(Material.LEATHER_CHESTPLATE, teamColor),
                armorPiece(Material.LEATHER_HELMET, teamColor)
        };
        player.getInventory().setArmorContents(armor);
    }

    /** Refill only empty slots so painted purple pieces retain their progress. */
    void ensureTeamArmor(SessionState session, Player player) {
        if (session == null || player == null) return;
        Color teamColor = ArcadePortRules.paintballTeamColor(session.teams.getOrDefault(player.getUniqueId(), 0));
        Material[] materials = {
                Material.LEATHER_BOOTS,
                Material.LEATHER_LEGGINGS,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_HELMET
        };
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int slot = 0; slot < materials.length; slot++) {
            if (armor[slot] != null && !armor[slot].getType().isAir()) continue;
            armor[slot] = armorPiece(materials[slot], teamColor);
            changed = true;
        }
        if (changed) player.getInventory().setArmorContents(armor);
    }

    private ItemStack armorPiece(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Player> alivePlayers(SessionState session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }
}
