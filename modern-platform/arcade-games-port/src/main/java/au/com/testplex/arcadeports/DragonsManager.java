package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Dragons' per-match scoreboard, using the existing shared Arcade lifecycle. */
final class DragonsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DRAGONS; }


    interface SpectatorActions {
        boolean isCurrent(SessionState session);
        boolean isTamerControlled(SessionState session, UUID playerId);
    }

    private static final class SpectatorMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private final Map<Integer, UUID> targetsBySlot = new HashMap<>();
        private Inventory inventory;

        private SpectatorMenuHolder(UUID ownerId) { this.ownerId = ownerId; }
        @Override public Inventory getInventory() { return inventory; }
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    boolean isSpectatorMenu(InventoryHolder holder) {
        return holder instanceof SpectatorMenuHolder;
    }

    Player nearestSpectatorTarget(SessionState session, Player spectator, SpectatorActions actions) {
        if (session == null || spectator == null) return null;
        Player nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Player candidate : aliveTrackTargets(session, actions)) {
            double distance = candidate.getLocation().distanceSquared(spectator.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    static List<Integer> spectatorMenuSlots(int targetCount) {
        List<Integer> slots = new ArrayList<>(Math.max(0, targetCount));
        int slot = 19;
        for (int i = 0; i < Math.max(0, targetCount) && slot < 54; i++) {
            slots.add(slot);
            slot = (slot + 2) % 9 == 0 ? slot + 3 : slot + 1;
        }
        return slots;
    }

    static int spectatorMenuSize(int targetCount) {
        List<Integer> slots = spectatorMenuSlots(targetCount);
        int lastSlot = slots.isEmpty() ? 18 : slots.get(slots.size() - 1);
        return Math.min(54, Math.max(27, ((lastSlot / 9) + 1) * 9));
    }

    void openSpectatorMenu(SessionState session, Player spectator, SpectatorActions actions) {
        List<Player> targets = aliveTrackTargets(session, actions);
        if (targets.isEmpty()) {
            spectator.sendMessage(ChatColor.YELLOW + "There are no living Dragons players to track.");
            return;
        }
        targets.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        SpectatorMenuHolder holder = new SpectatorMenuHolder(spectator.getUniqueId());
        holder.inventory = Bukkit.createInventory(holder, spectatorMenuSize(targets.size()), "Spectator Menu");
        populateSpectatorMenu(session, spectator, holder, targets, actions);
        spectator.openInventory(holder.inventory);
    }

    private boolean populateSpectatorMenu(
            SessionState session, Player spectator, SpectatorMenuHolder holder, SpectatorActions actions) {
        return populateSpectatorMenu(session, spectator, holder, aliveTrackTargets(session, actions), actions);
    }

    private boolean populateSpectatorMenu(
            SessionState session, Player spectator, SpectatorMenuHolder holder, List<Player> targets,
            SpectatorActions actions) {
        targets.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        holder.inventory.clear();
        holder.targetsBySlot.clear();
        ItemStack teamHeader = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta teamMeta = teamHeader.getItemMeta();
        teamMeta.setDisplayName(ChatColor.YELLOW + "Players");
        teamMeta.setLore(List.of(" ", ChatColor.YELLOW + "Players Alive: " + ChatColor.WHITE + targets.size()));
        teamHeader.setItemMeta(teamMeta);
        holder.inventory.setItem(13, teamHeader);
        holder.inventory.setItem(4, namedItem(Material.BARRIER, ChatColor.RED + "Close",
                "Close the Dragons spectator menu."));

        List<Integer> slots = spectatorMenuSlots(targets.size());
        for (int i = 0; i < targets.size() && i < slots.size(); i++) {
            Player target = targets.get(i);
            int slot = slots.get(i);
            if (slot >= holder.inventory.getSize()) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(target);
                skull.setDisplayName(ChatColor.YELLOW + target.getName());
                double distance = Math.sqrt(spectator.getLocation().distanceSquared(target.getLocation()));
                double heightDifference = target.getLocation().getY() - spectator.getLocation().getY();
                String kit = session.dragonKits.getOrDefault(target.getUniqueId(), "coward");
                skull.setLore(List.of(" ",
                        ChatColor.YELLOW + "Kit: " + ChatColor.WHITE + kit,
                        ChatColor.YELLOW + "Distance: " + ChatColor.WHITE
                                + String.format(java.util.Locale.ROOT, "%.1f", distance),
                        ChatColor.YELLOW + "Height Difference: " + ChatColor.WHITE
                                + String.format(java.util.Locale.ROOT, "%.1f", heightDifference),
                        " ", ChatColor.YELLOW + "Left Click " + ChatColor.RESET + "Teleport",
                        ChatColor.YELLOW + "Right Click " + ChatColor.RESET + "Spectate"));
                head.setItemMeta(skull);
            }
            holder.inventory.setItem(slot, head);
            holder.targetsBySlot.put(slot, target.getUniqueId());
        }
        return !targets.isEmpty();
    }

    boolean handleCompassInteract(SessionState session, Player player, ItemStack heldItem,
            boolean leftClick, boolean rightClick, SpectatorActions actions) {
        if (session == null || session.type != ArcadePortGame.DRAGONS || player == null
                || session.alive.contains(player.getUniqueId()) || !isTrackingCompass(heldItem)) return false;
        if (leftClick) {
            long now = System.currentTimeMillis();
            long nextAllowed = session.dragonCompassNextUseAt.getOrDefault(player.getUniqueId(), 0L);
            if (!ArcadePortRules.canUseDragonSpectatorTeleport(now, nextAllowed)) return true;
            session.dragonCompassNextUseAt.put(player.getUniqueId(),
                    now + GameDebugRuntimeOverrides.longValue("DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS",
                            ArcadePortRules.DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS));
            Player target = nearestSpectatorTarget(session, player, actions);
            if (target != null) {
                player.setSpectatorTarget(null);
                player.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
                player.setCompassTarget(target.getLocation());
            } else {
                player.sendMessage(ChatColor.YELLOW + "There are no living Dragons players to track.");
            }
        } else if (rightClick) {
            openSpectatorMenu(session, player, actions);
        }
        return true;
    }

    boolean handleSpectatorMenuClick(InventoryClickEvent event, SessionState session, SpectatorActions actions) {
        Inventory top = event.getView().getTopInventory();
        if (!isSpectatorMenu(top.getHolder())) return false;
        event.setCancelled(true);
        SpectatorMenuHolder holder = (SpectatorMenuHolder) top.getHolder();
        if (!(event.getWhoClicked() instanceof Player spectator)
                || !holder.ownerId.equals(spectator.getUniqueId())
                || event.getClickedInventory() != top) return true;
        if (actions == null || !actions.isCurrent(session)
                || !session.players.containsKey(spectator.getUniqueId())
                || session.alive.contains(spectator.getUniqueId())) {
            spectator.closeInventory();
            return true;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return true;
        if (slot == 53 || slot == 4) {
            spectator.closeInventory();
            return true;
        }

        UUID targetId = holder.targetsBySlot.get(slot);
        if (targetId == null) return true;
        Player target = session.players.get(targetId);
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)
                || actions == null || actions.isTamerControlled(session, targetId) || target.getWorld() != session.world) {
            spectator.sendMessage(ChatColor.YELLOW + "That player is no longer available to track.");
            populateSpectatorMenu(session, spectator, holder, actions);
            return true;
        }

        if (event.isLeftClick()) {
            long now = System.currentTimeMillis();
            long nextAllowed = session.dragonCompassNextUseAt.getOrDefault(spectator.getUniqueId(), 0L);
            if (!ArcadePortRules.canUseDragonSpectatorTeleport(now, nextAllowed)) return true;
            session.dragonCompassNextUseAt.put(spectator.getUniqueId(),
                    now + GameDebugRuntimeOverrides.longValue("DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS",
                            ArcadePortRules.DRAGONS_SPECTATOR_TELEPORT_COOLDOWN_MILLIS));
            spectator.setSpectatorTarget(null);
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
            spectator.setCompassTarget(target.getLocation());
            spectator.closeInventory();
        } else if (event.isRightClick()) {
            spectator.setSpectatorTarget(target);
            spectator.setCompassTarget(target.getLocation());
            spectator.closeInventory();
        }
        return true;
    }

    void updateSpectatorCompasses(SessionState session, long now, SpectatorActions actions) {
        if (session == null || session.type != ArcadePortGame.DRAGONS) return;
        if (now >= session.dragonNextCompassUpdateAt) {
            session.dragonNextCompassUpdateAt = now + 250L; // CompassModule uses UpdateType.FASTER.
            List<Player> alive = aliveTrackTargets(session, actions);
            if (!alive.isEmpty()) {
                for (Player spectator : session.players.values()) {
                    if (spectator == null || !spectator.isOnline()
                            || session.alive.contains(spectator.getUniqueId())) continue;
                    if (inventoryCount(spectator, Material.COMPASS) <= 0) {
                        spectator.getInventory().addItem(trackingCompass());
                    }
                    Player target = nearestTarget(alive, spectator);
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
        }

        if (now >= session.dragonNextCompassMenuRefreshAt) {
            session.dragonNextCompassMenuRefreshAt = now + 1_000L; // CompassMenu.refresh() uses UpdateType.SEC.
            for (Player spectator : session.players.values()) {
                if (spectator == null || !spectator.isOnline()
                        || !(spectator.getOpenInventory().getTopInventory().getHolder()
                                instanceof SpectatorMenuHolder holder)
                        || !holder.ownerId.equals(spectator.getUniqueId())) continue;
                if (!populateSpectatorMenu(session, spectator, holder, actions)) {
                    spectator.closeInventory();
                    spectator.sendMessage(ChatColor.YELLOW + "There are no living Dragons players to track.");
                }
            }
        }
    }

    private List<Player> aliveTrackTargets(SessionState session, SpectatorActions actions) {
        List<Player> alive = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()
                    && (actions == null || !actions.isTamerControlled(session, id))) alive.add(player);
        }
        return alive;
    }

    private static Player nearestTarget(List<Player> candidates, Player spectator) {
        Player nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Player candidate : candidates) {
            double distance = candidate.getLocation().distanceSquared(spectator.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static int inventoryCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    private static ItemStack trackingCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + "Points to the nearest survivor. Left-click to spectate them."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack namedItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
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
