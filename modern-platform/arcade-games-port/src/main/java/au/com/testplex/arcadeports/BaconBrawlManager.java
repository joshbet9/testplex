package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Bacon Brawl-specific mechanics migrated out of the shared match coordinator.
 * Owns disguise/nameplates, the scoreboard, ability-meter UI and spectator tracking.
 * Combat and projectile handling remain in ArcadePortManager for a later phase.
 */
final class BaconBrawlManager implements ArcadeMatchController {
    private final Logger logger;

    BaconBrawlManager(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override public ArcadePortGame game() { return ArcadePortGame.BACON_BRAWL; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupBaconDisguiseNameTags(session, participants);
        setupBaconMeterScoreboard(session);
    }

    @Override public void onFinish(SessionState session) {
        cleanupBaconMeterScoreboard(session);
    }

    /**
     * Keep Bacon Brawl's disguise scoreboard and hidden real-player state confined to this match.
     * Each player's original scoreboard is held in PlayerSnapshot and restored after the round.
     */
    private void setupBaconDisguiseNameTags(SessionState session, List<Player> participants) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        String teamName = "bb" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        try {
            Team disguiseTeam = scoreboard.registerNewTeam(teamName);
            disguiseTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            // Bacon avatars replace the players visually; prevent the hidden player bodies
            // from pushing against each other while preserving normal hit targeting.
            disguiseTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            session.baconDisguiseScoreboard = scoreboard;
            session.baconDisguiseTeam = disguiseTeam;
            for (Player player : participants) {
                player.setScoreboard(scoreboard);
                String entry = player.getName();
                Team previous = scoreboard.getEntryTeam(entry);
                session.baconPreviousNameTeams.put(player.getUniqueId(), previous == disguiseTeam ? null : previous);
                session.baconNameEntries.put(player.getUniqueId(), entry);
                disguiseTeam.addEntry(entry);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.warning("Could not hide Bacon Brawl player nameplates: " + exception.getMessage());
            cleanupBaconDisguiseNameTags(session);
        }
    }

    void restoreBaconDisguiseNameTagEntry(SessionState session, UUID playerId) {
        String entry = session.baconNameEntries.remove(playerId);
        Team disguiseTeam = session.baconDisguiseTeam;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (entry == null) return;
        if (disguiseTeam != null) {
            try {
                disguiseTeam.removeEntry(entry);
            } catch (IllegalStateException ignored) {
                // Another plugin may already have removed the temporary team.
            }
        }
        Team previous = session.baconPreviousNameTeams.remove(playerId);
        if (previous == null || scoreboard == null) return;
        try {
            Team currentlyRegistered = scoreboard.getTeam(previous.getName());
            if (currentlyRegistered != null && currentlyRegistered != disguiseTeam) {
                currentlyRegistered.addEntry(entry);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.warning("Could not restore scoreboard team for " + entry
                    + " after Bacon Brawl: " + exception.getMessage());
        }
    }

    void cleanupBaconDisguiseNameTags(SessionState session) {
        Team disguiseTeam = session.baconDisguiseTeam;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (disguiseTeam == null || scoreboard == null) return;
        for (UUID playerId : new ArrayList<>(session.baconNameEntries.keySet())) {
            restoreBaconDisguiseNameTagEntry(session, playerId);
        }
        try {
            if (scoreboard.getTeam(disguiseTeam.getName()) != null) disguiseTeam.unregister();
        } catch (IllegalStateException ignored) {
            // Already unregistered by another plugin.
        }
        session.baconDisguiseTeam = null;
        session.baconDisguiseScoreboard = null;
        session.baconPreviousNameTeams.clear();
        session.baconNameEntries.clear();
    }

    private void setupBaconMeterScoreboard(SessionState session) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = session.baconDisguiseScoreboard;
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            for (Player player : session.players.values()) {
                if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
            }
        }
        session.baconMeterScoreboard = scoreboard;
        session.baconPreviousSidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        String objectiveName = "bbm" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Objective objective = scoreboard.registerNewObjective(objectiveName, "dummy",
                ChatColor.GOLD + "BACON BRAWL");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.baconMeterObjective = objective;
        for (int i = 0; i < 15; i++) {
            Team lineTeam = scoreboard.registerNewTeam("bbmline" + i);
            String entry = ChatColor.values()[i].toString();
            lineTeam.addEntry(entry);
            session.baconMeterTeams.add(lineTeam);
            session.baconMeterEntries.add(entry);
        }
        updateBaconMeterScoreboard(session);
    }

    void updateBaconMeterScoreboard(SessionState session) {
        if (session.baconMeterScoreboard == null || session.baconMeterObjective == null
                || session.type != ArcadePortGame.BACON_BRAWL) return;
        List<Player> alive = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> lines = ArcadePortRules.baconBrawlSidebarLines(
                alive.stream().map(Player::getName).toList());
        for (int i = 0; i < session.baconMeterTeams.size(); i++) {
            Team lineTeam = session.baconMeterTeams.get(i);
            String entry = session.baconMeterEntries.get(i);
            if (i < lines.size()) {
                lineTeam.setPrefix(ChatColor.WHITE + lines.get(i));
                lineTeam.setSuffix("");
                session.baconMeterObjective.getScore(entry).setScore(15 - i);
            } else {
                lineTeam.setPrefix("");
                lineTeam.setSuffix("");
                session.baconMeterScoreboard.resetScores(entry);
            }
        }
    }

    private void cleanupBaconMeterScoreboard(SessionState session) {
        Scoreboard scoreboard = session.baconMeterScoreboard;
        Objective objective = session.baconMeterObjective;
        if (scoreboard == null || objective == null) return;
        try {
            if (scoreboard.getObjective(DisplaySlot.SIDEBAR) == objective
                    && session.baconPreviousSidebar != null) {
                session.baconPreviousSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            for (Team team : new ArrayList<>(session.baconMeterTeams)) {
                if (scoreboard.getTeam(team.getName()) != null) team.unregister();
            }
            objective.unregister();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.fine("Bacon Brawl meter scoreboard was already removed: " + exception.getMessage());
        }
        session.baconMeterTeams.clear();
        session.baconMeterEntries.clear();
        session.baconMeterScoreboard = null;
        session.baconMeterObjective = null;
        session.baconPreviousSidebar = null;
    }

    private String baconMeterBar(int percent) {
        int charge = Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX), percent));
        int filled = (charge + 9) / 10;
        return ChatColor.GREEN + "█".repeat(filled) + ChatColor.DARK_GRAY + "░".repeat(10 - filled);
    }

    void recordBaconMeterHit(SessionState session, Player player, boolean critical) {
        UUID id = player.getUniqueId();
        int previous = session.baconMeters.getOrDefault(id, 0);
        int meter = ArcadePortRules.nextBaconMeter(previous, critical, session.alive.size());
        session.baconMeters.put(id, meter);
        if (previous < GameDebugRuntimeOverrides.intValue("BACON_METER_MIN_ABILITY", ArcadePortRules.BACON_METER_MIN_ABILITY)
                && meter >= GameDebugRuntimeOverrides.intValue("BACON_METER_MIN_ABILITY", ArcadePortRules.BACON_METER_MIN_ABILITY)) {
            player.sendMessage(ChatColor.GREEN + baconAbilityName(session, id) + " is ready!");
        }
        if (previous < GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX) && meter >= GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX)) {
            player.sendMessage(ChatColor.GOLD + baconAbilityName(session, id) + " is maxed!");
        }
        updateBaconMeterItem(session, player);
        updateBaconMeterScoreboard(session);
    }

    String baconAbilityName(SessionState session, UUID playerId) {
        return switch (session.baconKits.getOrDefault(playerId, "el-muchacho-pigo")) {
            case "mama-piggles" -> "Bacon Blast";
            case "chris-p-bacon" -> "Crispy Bacon";
            case "pig" -> "Cloak";
            case "brutus-bacon" -> "Brutus Bacon";
            default -> "Body Slam";
        };
    }

    /** The vanilla XP bar is the ability meter during Bacon Brawl. */
    void updateBaconMeterItem(SessionState session, Player player) {
        if (session.type != ArcadePortGame.BACON_BRAWL || player == null || !player.isOnline()
                || !session.alive.contains(player.getUniqueId())) return;
        int percent = Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX),
                session.baconMeters.getOrDefault(player.getUniqueId(), 0)));
        float progress = Math.min(0.999F, percent / (float) GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX));
        if (Math.abs(player.getExp() - progress) > 0.001F) player.setExp(progress);
        if (player.getLevel() != percent) player.setLevel(percent);

        ItemStack ability = player.getInventory().getItem(0);
        if (ability != null && ability.getType() == Material.IRON_AXE
                && ability.hasItemMeta() && ability.getItemMeta().hasDisplayName()
                && "Kit Ability".equals(ChatColor.stripColor(ability.getItemMeta().getDisplayName()))) {
            ItemMeta meta = ability.getItemMeta();
            if (meta != null) {
                List<String> lore = List.of(ChatColor.GRAY + baconAbilityMeterLore(
                        session.baconKits.getOrDefault(player.getUniqueId(), "el-muchacho-pigo"), percent));
                if (!lore.equals(meta.getLore())) {
                    meta.setLore(lore);
                    ability.setItemMeta(meta);
                }
            }
        }

        // Remove the old slot-9 star if a tester is upgrading from an earlier plugin build.
        ItemStack oldMeter = player.getInventory().getItem(8);
        if (isBaconMeterItem(oldMeter)) player.getInventory().setItem(8, null);
    }

    String baconAbilityMeterLore(String kit, int charge) {
        int safeCharge = Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue("BACON_METER_MAX", ArcadePortRules.BACON_METER_MAX), charge));
        if (safeCharge < GameDebugRuntimeOverrides.intValue("BACON_METER_MIN_ABILITY", ArcadePortRules.BACON_METER_MIN_ABILITY)) {
            return "Right-click at 50%+. " + safeCharge + "% charged.";
        }
        if ("chris-p-bacon".equals(kit)) {
            return "Crispy shots and crouch-dash use fixed strength; each ability costs 50%. "
                    + safeCharge + "% charged.";
        }
        return switch (kit) {
            case "mama-piggles" -> String.format(java.util.Locale.ROOT,
                    "Blast force %.2fx at %d%% charge; right-click spends all charge.",
                    ArcadePortRules.baconBlastPowerMultiplier(safeCharge), safeCharge);
            case "pig" -> String.format(java.util.Locale.ROOT,
                    "Cloak lasts %.1fs at %d%% charge; right-click spends all charge.",
                    ArcadePortRules.baconAbilityDurationMillis(safeCharge) / 1_000.0D, safeCharge);
            case "brutus-bacon" -> String.format(java.util.Locale.ROOT,
                    "Brutus guard lasts %.1fs at %d%% charge; right-click spends all charge.",
                    ArcadePortRules.baconAbilityDurationMillis(safeCharge) / 1_000.0D, safeCharge);
            default -> String.format(java.util.Locale.ROOT,
                    "Body Slam launch and hit force %.2fx at %d%% charge; right-click spends all charge.",
                    ArcadePortRules.baconBodySlamMeterMultiplier(safeCharge), safeCharge);
        };
    }

    boolean isBaconMeterItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).startsWith("Ability Meter:");
    }

    /** Reborn's CompassModule gives eliminated Bacon Brawl players a live nearest-opponent tracker. */
    void updateBaconSpectatorCompasses(SessionState session) {
        List<Player> targets = alivePlayers(session);
        if (targets.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            boolean hasCompass = false;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (isBaconTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                spectator.getInventory().addItem(namedTrackingCompass("Tracks the nearest surviving pig."));
            }
            Player target = targets.stream()
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(spectator.getLocation())))
                    .orElse(null);
            if (target != null) spectator.setCompassTarget(target.getLocation());
        }
    }

    boolean isBaconTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equals("Tracking Compass");
    }

    void openBaconCompassMenu(SessionState session, Player spectator) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Spectator Menu");
        ItemStack header = namedTrackingCompass("Choose a surviving pig to follow.");
        inventory.setItem(4, header);

        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43,
                46, 47, 48, 49, 50, 51, 52
        };
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
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                String kit = session.baconKits.getOrDefault(target.getUniqueId(), "el-muchacho-pigo");
                meta.setLore(List.of(
                        ChatColor.GRAY + "Kit: " + kit,
                        ChatColor.GRAY + "Distance: " + String.format(java.util.Locale.ROOT, "%.1f", distance),
                        ChatColor.GRAY + "Height difference: " + String.format(java.util.Locale.ROOT, "%.1f", height),
                        ChatColor.YELLOW + "Left-click: Teleport",
                        ChatColor.YELLOW + "Right-click: Spectate"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            targetsBySlot.put(slot, target.getUniqueId());
        }
        session.baconCompassMenuTargets.put(spectator.getUniqueId(), targetsBySlot);
        spectator.openInventory(inventory);
    }

    void handleBaconCompassMenuClick(SessionState session, InventoryClickEvent event) {
        if (session == null || session.type != ArcadePortGame.BACON_BRAWL
                || !isBaconCompassMenuTitle(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spectator)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        Map<Integer, UUID> targets = session.baconCompassMenuTargets.get(spectator.getUniqueId());
        UUID targetId = targets == null ? null : targets.get(slot);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        session.baconCompassMenuTargets.remove(spectator.getUniqueId());
        spectator.closeInventory();
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)) {
            spectator.sendMessage(ChatColor.YELLOW + "That pig is no longer alive.");
            return;
        }
        if (event.isRightClick()) {
            spectator.setSpectatorTarget(target);
        } else {
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
        }
        spectator.setCompassTarget(target.getLocation());
    }

    boolean isBaconCompassMenuTitle(String title) {
        return (ChatColor.DARK_GREEN + "Spectator Menu").equals(title);
    }

    Player nearestAliveBaconTarget(SessionState session, Player spectator) {
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
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
