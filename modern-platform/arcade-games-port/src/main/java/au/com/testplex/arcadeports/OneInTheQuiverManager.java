package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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

    ItemStack trackingCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY
                    + "Left-click to teleport to the nearest player; right-click to open the spectator menu."));
            item.setItemMeta(meta);
        }
        return item;
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    Player nearestSpectateTarget(SessionState session, Player spectator) {
        if (session == null || spectator == null) return null;
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    void updateSpectatorCompass(SessionState session) {
        if (session == null) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())
                    || spectator.getWorld() != session.world) continue;
            Player target = nearestSpectateTarget(session, spectator);
            if (target == null) continue;
            spectator.setCompassTarget(target.getLocation());
            if (isTrackingCompass(spectator.getInventory().getItemInMainHand())) {
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                spectator.sendActionBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Nearest Target: "
                        + ChatColor.AQUA + target.getName() + ChatColor.WHITE + " | Distance: "
                        + ChatColor.YELLOW + String.format(java.util.Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + " | Height: "
                        + ChatColor.YELLOW + String.format(java.util.Locale.ROOT, "%+.1f", height));
            }
        }
    }

    void openSpectatorMenu(SessionState session, Player spectator) {
        if (session == null || spectator == null) return;
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Spectator Menu");
        inventory.setItem(4, trackingCompass());
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52};
        Map<Integer, UUID> targetsBySlot = new HashMap<>();
        List<Player> targets = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (int i = 0; i < targets.size() && i < slots.length; i++) {
            Player target = targets.get(i);
            int slot = slots[i];
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                meta.setDisplayName(ChatColor.AQUA + target.getName());
                meta.setLore(List.of(
                        ChatColor.GRAY + "Kit: " + session.quiverKits.getOrDefault(target.getUniqueId(), "leaper"),
                        ChatColor.GRAY + "Kills: " + session.scores.getOrDefault(target.getUniqueId(), 0),
                        ChatColor.GRAY + "Distance: " + String.format(java.util.Locale.ROOT, "%.1f", distance),
                        ChatColor.GRAY + "Height difference: " + String.format(java.util.Locale.ROOT, "%+.1f", height),
                        ChatColor.YELLOW + "Left-click: Teleport",
                        ChatColor.YELLOW + "Right-click: Spectate"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            targetsBySlot.put(slot, target.getUniqueId());
        }
        session.quiverCompassMenuTargets.put(spectator.getUniqueId(), targetsBySlot);
        spectator.openInventory(inventory);
    }

    boolean isSpectatorMenuTitle(String title) {
        return (ChatColor.DARK_GREEN + "Spectator Menu").equals(title);
    }

    void handleSpectatorMenuClick(SessionState session, Player spectator, int slot, boolean rightClick) {
        if (session == null || spectator == null) return;
        Map<Integer, UUID> targets = session.quiverCompassMenuTargets.get(spectator.getUniqueId());
        UUID targetId = targets == null ? null : targets.get(slot);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        session.quiverCompassMenuTargets.remove(spectator.getUniqueId());
        spectator.closeInventory();
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)) {
            spectator.sendMessage(ChatColor.YELLOW + "That player is no longer alive.");
            return;
        }
        if (rightClick) {
            spectator.setSpectatorTarget(target);
        } else {
            long now = System.currentTimeMillis();
            long readyAt = session.quiverSpectateNextTeleportAt.getOrDefault(spectator.getUniqueId(), 0L);
            if (!ArcadePortRules.isQuiverSpectatorTeleportReady(now, readyAt)) return;
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
            session.quiverSpectateNextTeleportAt.put(spectator.getUniqueId(),
                    ArcadePortRules.nextQuiverSpectatorTeleportAt(now));
        }
        spectator.setCompassTarget(target.getLocation());
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
