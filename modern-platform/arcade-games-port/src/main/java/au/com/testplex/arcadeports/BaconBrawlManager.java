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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Bacon Brawl-specific mechanics migrated out of the shared match coordinator.
 * Owns disguise/nameplates, the scoreboard, ability-meter UI and spectator tracking.
 * Pure combat calculations live here too; combat events and projectile updates
 * remain coordinated by ArcadePortManager until a later extraction phase.
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

    boolean isBaconBrutusBackstab(Player target, Location attackerLocation) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attackerLocation.toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrutusBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    boolean isBaconBrutusBackstab(Player target, Player attacker) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrutusBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    boolean isBaconBrawlBackstab(Player target, Player attacker) {
        Vector targetLook = target.getLocation().getDirection().setY(0.0D);
        Vector fromTargetToAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0.0D);
        return ArcadePortRules.isBaconBrawlBackstab(targetLook.getX(), targetLook.getZ(),
                fromTargetToAttacker.getX(), fromTargetToAttacker.getZ());
    }

    /** Port DamageManager's logarithmic base and UtilAction's strength, vertical cap and ground boost. */
    Vector baconBrawlKnockbackVelocity(Player source, Player target, double rawDamage,
            double knockbackMultipliers, Vector projectileVelocity) {
        return baconBrawlKnockbackVelocity(source.getLocation(), target.getLocation(), rawDamage,
                knockbackMultipliers, projectileVelocity, target.isOnGround());
    }

    Vector baconBrawlKnockbackVelocity(Location source, Location target, double rawDamage,
            double knockbackMultipliers, Vector projectileVelocity, boolean targetOnGround) {
        boolean projectile = projectileVelocity != null;
        Vector direction = projectile
                ? projectileVelocity.clone().setY(0.0D)
                : target.toVector().subtract(source.toVector()).setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = source.getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = new Vector(0.0D, 0.0D, 1.0D);
        double strength = ArcadePortRules.baconBrawlVelocityStrength(rawDamage, knockbackMultipliers, projectile);
        double vertical = ArcadePortRules.baconBrawlVerticalVelocity(
                rawDamage, knockbackMultipliers, projectile, targetOnGround);
        return direction.normalize().multiply(strength).setY(vertical);
    }

    /** Live timing guide: the green section is the 500–900 ms critical window. */
    String baconTimingStatusText(SessionState session, UUID id, long now) {
        long lastHit = session.baconLastSwingAt.getOrDefault(id, 0L);
        if (lastHit <= 0L) {
            return ChatColor.GRAY + "[" + ChatColor.RED + "■■■■"
                    + ChatColor.GREEN + "■■■■" + ChatColor.YELLOW + "■■■■"
                    + ChatColor.GRAY + "] swing";
        }

        long elapsed = Math.max(0L, now - lastHit);
        long lateMax = ArcadePortRules.baconTimingWindowMillis(
                GameDebugRuntimeOverrides.longValue("BACON_TIMED_HIT_LATE_MAX_MILLIS", ArcadePortRules.BACON_TIMED_HIT_LATE_MAX_MILLIS), session.baconKnockback);
        long criticalMin = ArcadePortRules.baconTimingWindowMillis(
                GameDebugRuntimeOverrides.longValue("BACON_TIMED_HIT_GOOD_MIN_MILLIS", ArcadePortRules.BACON_TIMED_HIT_GOOD_MIN_MILLIS), session.baconKnockback);
        long criticalMax = ArcadePortRules.baconTimingWindowMillis(
                GameDebugRuntimeOverrides.longValue("BACON_TIMED_HIT_GOOD_MAX_MILLIS", ArcadePortRules.BACON_TIMED_HIT_GOOD_MAX_MILLIS), session.baconKnockback);
        int pointer = (int) Math.min(14L, elapsed * 15L / Math.max(1L, lateMax));
        boolean criticalWindow = ArcadePortRules.isBaconTimedCritical(elapsed, session.baconKnockback);
        int criticalStart = (int) (criticalMin * 15L / Math.max(1L, lateMax));
        int criticalEnd = Math.min(14, (int) (criticalMax * 15L / Math.max(1L, lateMax)));
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 15; i++) {
            if (i == pointer) {
                bar.append(criticalWindow ? ChatColor.GREEN : ChatColor.WHITE).append("▌");
            } else if (i >= criticalStart && i <= criticalEnd) {
                bar.append(ChatColor.GREEN).append("■");
            } else if (i < criticalStart) {
                bar.append(ChatColor.RED).append("■");
            } else if (i < 14) {
                bar.append(ChatColor.YELLOW).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("■");
            }
        }
        bar.append(ChatColor.GRAY).append("]");

        String state;
        if (ArcadePortRules.isBaconTimedCritical(elapsed, session.baconKnockback)) {
            state = ChatColor.GREEN + "" + ChatColor.BOLD + "CRITICAL NOW";
        } else if (elapsed < criticalMin) {
            state = ChatColor.RED + "wait " + (criticalMin - elapsed) + "ms";
        } else if (elapsed <= lateMax) {
            state = ChatColor.YELLOW + "late";
        } else {
            state = ChatColor.GRAY + "swing";
        }
        return ChatColor.AQUA + bar.toString() + " " + state;
    }

    /** Finds the first non-passable/liquid block cell crossed by the projectile's previous-to-current path. */
    double baconBlastFirstBlockFraction(SessionState session, Location start, Location end) {
        if (session == null || start == null || end == null
                || start.getWorld() != session.world || end.getWorld() != session.world) return -1.0D;
        Vector segment = end.toVector().subtract(start.toVector());
        double length = segment.length();
        if (!Double.isFinite(length) || length <= 1.0E-6D) return -1.0D;
        int samples = Math.max(1, Math.min(512, (int) Math.ceil(length / 0.1D)));
        for (int i = 1; i <= samples; i++) {
            double fraction = i / (double) samples;
            Location sample = start.clone().add(segment.clone().multiply(fraction));
            Block block = sample.getBlock();
            if (block.isLiquid() || !block.isPassable()) return fraction;
        }
        return -1.0D;
    }

    /** First point where the swept bacon item intersects a player's actual entity hitbox. */
    double baconBlastPlayerHitFraction(Player candidate, Location start, Location end,
            double segmentLength) {
        if (candidate == null || start == null || end == null
                || candidate.getWorld() != start.getWorld() || start.getWorld() != end.getWorld()) return -1.0D;
        BoundingBox hitbox = candidate.getBoundingBox().expand(0.05D);
        Vector startVector = start.toVector();
        if (segmentLength <= 1.0E-6D) return hitbox.contains(end.toVector()) ? 0.0D : -1.0D;
        if (hitbox.contains(startVector)) return 0.0D;
        Vector direction = end.toVector().subtract(startVector).multiply(1.0D / segmentLength);
        org.bukkit.util.RayTraceResult hit = hitbox.rayTrace(startVector, direction, segmentLength);
        if (hit == null || hit.getHitPosition() == null) return -1.0D;
        return Math.max(0.0D, Math.min(1.0D,
                startVector.distance(hit.getHitPosition()) / segmentLength));
    }

    boolean baconBlastTouchingAdjacentSolidBlock(Location location) {
        Block center = location.getBlock();
        for (BlockFace face : new BlockFace[] {
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN }) {
            Block adjacent = center.getRelative(face);
            if (adjacent.getType().isSolid() && !adjacent.isLiquid()) return true;
        }
        return false;
    }

    void spawnBaconBlastImpact(SessionState session, Location impact, UUID blastId) {
        spawnBaconBlastImpact(session, impact, blastId, true, true);
    }

    void spawnBaconBlastImpact(SessionState session, Location impact, UUID blastId,
            boolean includeExplosion, boolean playLandingSound) {
        // Reborn creates a 0.5-power, non-incendiary, non-block-breaking explosion on impact.
        // Suppress health damage; crispy impacts are visual-only and have no explosion impulse.
        if (includeExplosion) {
            session.baconBlastExplosionDepth++;
            try {
                session.world.createExplosion(impact.getX(), impact.getY(), impact.getZ(), 0.5F, false, false);
            } finally {
                session.baconBlastExplosionDepth--;
            }
        }
        session.world.spawnParticle(Particle.CLOUD, impact, 14, 0.25D, 0.25D, 0.25D, 0.04D);
        UUID ownerId = session.baconBlastOwners.get(blastId);
        if (playLandingSound && ownerId != null
                && "mama-piggles".equals(session.baconKits.getOrDefault(ownerId, "el-muchacho-pigo"))) {
            // Reborn's item impact effect is silent; Testplex adds the requested landing cue.
            session.world.playSound(impact, Sound.BLOCK_NOTE_BLOCK_PLING, 1.2F, 1.65F);
        }
    }

    void playMamaPigglesBaconBlastHitSound(Player owner) {
        // Send the cue directly to its owner even when impact is outside sound radius
        // or the hit ends the round.
        if (owner != null && owner.isOnline()) {
            owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.25F);
        }
    }

    void removeBaconBlastItem(SessionState session, UUID blastId) {
        Item bacon = session.baconBlastItems.remove(blastId);
        if (bacon != null) {
            session.entities.remove(bacon);
            if (bacon.isValid()) bacon.remove();
        }
        session.baconBlastOwners.remove(blastId);
        session.baconBlastLastLocations.remove(blastId);
        session.baconBlastCreatedAt.remove(blastId);
        session.baconBlastPowerById.remove(blastId);
        session.baconCrispyProjectiles.remove(blastId);
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
