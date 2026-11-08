package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
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

    void updateLights(SessionState session, long now) {
        if (session.deathTagOriginalLightBlocks.isEmpty() || now < session.deathTagNextLightAt) return;
        session.deathTagNextLightAt = now + 1_000L;
        for (Block block : session.deathTagOriginalLightBlocks.keySet()) {
            if (block == null || block.getWorld() != session.world) continue;
            block.setType(ArcadePortRules.deathTagNextLightMaterial(block.getType()), false);
        }
    }

    void updateRunnerTrails(SessionState session) {
        if (session.ticks % 3 != 0) return;
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (session.tagged.contains(id)) continue;
            Location point = player.getLocation().clone().add(0.0D, 0.15D, 0.0D);
            Material glass = trailGlass(session.deathTagRunnerTrailColours.get(id));
            session.world.spawnParticle(Particle.BLOCK, point, 2,
                    0.06D, 0.06D, 0.06D, 0.01D, glass.createBlockData());

            String style = session.deathTagRunnerTrailStyles.getOrDefault(id, "classic");
            switch (style) {
                case "soul" -> session.world.spawnParticle(Particle.WITCH, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "sparks" -> session.world.spawnParticle(Particle.FIREWORK, point, 3,
                        0.12D, 0.12D, 0.12D, 0.02D);
                case "smoke" -> session.world.spawnParticle(Particle.SMOKE, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "ender" -> session.world.spawnParticle(Particle.PORTAL, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "hearts" -> session.world.spawnParticle(Particle.HEART, point, 2,
                        0.12D, 0.12D, 0.12D, 0.01D);
                default -> { /* Classic uses only the selected stained-glass trail. */ }
            }
        }
    }

    private static Material trailGlass(String colour) {
        String name = switch (colour == null ? "white" : colour.toLowerCase(java.util.Locale.ROOT)) {
            case "orange" -> "ORANGE_STAINED_GLASS";
            case "magenta" -> "MAGENTA_STAINED_GLASS";
            case "lightblue" -> "LIGHT_BLUE_STAINED_GLASS";
            case "yellow" -> "YELLOW_STAINED_GLASS";
            case "lime" -> "LIME_STAINED_GLASS";
            case "pink" -> "PINK_STAINED_GLASS";
            case "gray" -> "GRAY_STAINED_GLASS";
            case "silver" -> "LIGHT_GRAY_STAINED_GLASS";
            case "cyan" -> "CYAN_STAINED_GLASS";
            case "purple" -> "PURPLE_STAINED_GLASS";
            case "blue" -> "BLUE_STAINED_GLASS";
            case "brown" -> "BROWN_STAINED_GLASS";
            case "green" -> "GREEN_STAINED_GLASS";
            case "red" -> "RED_STAINED_GLASS";
            case "black" -> "BLACK_STAINED_GLASS";
            default -> "WHITE_STAINED_GLASS";
        };
        Material material = Material.matchMaterial(name);
        return material == null ? Material.WHITE_STAINED_GLASS : material;
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
