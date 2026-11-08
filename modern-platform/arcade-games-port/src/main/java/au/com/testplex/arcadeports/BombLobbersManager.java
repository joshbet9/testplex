package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Bomb Lobbers' scoreboard and spectator UI; the shared Arcade runtime owns each match. */
final class BombLobbersManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.BOMB_LOBBERS; }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupScoreboard(session);
    }

    private void setupScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("bomblob", "dummy",
                ChatColor.GOLD + "BOMB LOBBERS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.bombLobbersScoreboard = scoreboard;
        session.bombLobbersScoreboardObjective = objective;

        session.bombRedScoreboardLine = scoreboard.registerNewTeam("bombred");
        session.bombRedScoreboardEntry = ChatColor.RED.toString();
        session.bombRedScoreboardLine.addEntry(session.bombRedScoreboardEntry);
        session.bombRedScoreboardLine.setPrefix(ChatColor.RED + "Red Team: ");
        objective.getScore(session.bombRedScoreboardEntry).setScore(3);

        Team blank = scoreboard.registerNewTeam("bombblank");
        String blankEntry = ChatColor.DARK_GRAY.toString();
        blank.addEntry(blankEntry);
        blank.setPrefix(" ");
        objective.getScore(blankEntry).setScore(2);

        session.bombBlueScoreboardLine = scoreboard.registerNewTeam("bombblue");
        session.bombBlueScoreboardEntry = ChatColor.BLUE.toString();
        session.bombBlueScoreboardLine.addEntry(session.bombBlueScoreboardEntry);
        session.bombBlueScoreboardLine.setPrefix(ChatColor.BLUE + "Blue Team: ");
        objective.getScore(session.bombBlueScoreboardEntry).setScore(1);

        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateScoreboard(session);
    }

    void updateScoreboard(SessionState session) {
        if (session == null || session.bombLobbersScoreboardObjective == null) return;
        int red = countAliveTeammates(session, 0);
        int blue = countAliveTeammates(session, 1);
        if (session.bombRedScoreboardLine != null) {
            session.bombRedScoreboardLine.setSuffix(powerupScoreboardText(session, 0)
                    + ChatColor.WHITE + Integer.toString(red));
        }
        if (session.bombBlueScoreboardLine != null) {
            session.bombBlueScoreboardLine.setSuffix(powerupScoreboardText(session, 1)
                    + ChatColor.WHITE + Integer.toString(blue));
        }
    }

    int nearestSpawnTeam(SessionState session, Location location) {
        if (session == null || location == null || location.getWorld() != session.world
                || session.bombTeamSpawnCentres.isEmpty()) return -1;
        int nearestTeam = -1;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, Location> entry : session.bombTeamSpawnCentres.entrySet()) {
            double distanceSquared = location.distanceSquared(entry.getValue());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestTeam = entry.getKey();
            }
        }
        return nearestTeam;
    }

    private int onlineMatchPlayers(SessionState session) {
        if (session == null) return 0;
        return (int) session.players.values().stream()
                .filter(player -> player != null && player.isOnline() && player.getWorld() == session.world)
                .count();
    }

    long nextPowerupDelayMillis() {
        // The same bounded interval is used for the first wave and subsequent paired waves.
        return ArcadePortRules.bombLobbersPowerupSpawnDelayMillis(ThreadLocalRandom.current().nextLong());
    }

    void updatePowerups(SessionState session, long now, SourceMapCatalog.MapData map,
            NamespacedKey powerupKey, NamespacedKey teamKey) {
        if (session == null) return;
        if (!ArcadePortRules.bombLobbersPowerupsEnabled()) {
            for (Item drop : session.bombPowerupDrops.values()) {
                if (drop != null && drop.isValid()) drop.remove();
                if (drop != null) session.entities.remove(drop);
            }
            session.bombPowerupDrops.clear();
            session.bombPowerupTeamByDrop.clear();
            session.bombPowerupStatusByTeam.clear();
            session.bombNextPowerupSpawnAtByTeam.clear();
            return;
        }
        for (Map.Entry<UUID, Item> entry : new ArrayList<>(session.bombPowerupDrops.entrySet())) {
            Item drop = entry.getValue();
            if (drop == null || !drop.isValid()) {
                session.bombPowerupDrops.remove(entry.getKey());
                session.bombPowerupTeamByDrop.remove(entry.getKey());
                if (drop != null) session.entities.remove(drop);
            }
        }
        if (onlineMatchPlayers(session) <= 0) return;
        long nextWaveAt = session.bombNextPowerupSpawnAtByTeam.computeIfAbsent(
                0, ignored -> now + nextPowerupDelayMillis());
        if (now < nextWaveAt) return;

        String[] choices = {"wind-charge", "super-tnt", "fiery-tnt", "speed-boost"};
        int spawned = 0;
        for (int team = 0; team < 2; team++) {
            BombLobbersPowerupStatus status = session.bombPowerupStatusByTeam.get(team);
            if (status == BombLobbersPowerupStatus.SPAWNED
                    || status == BombLobbersPowerupStatus.OBTAINED) continue;
            Location spawn = findPowerupSpawn(session, team, map);
            if (spawn == null) continue;
            String powerup = choices[ThreadLocalRandom.current().nextInt(choices.length)];
            ItemStack powerupStack = createPowerupStack(powerup, powerupKey);
            ItemMeta powerupMeta = powerupStack.getItemMeta();
            if (powerupMeta != null) {
                powerupMeta.getPersistentDataContainer().set(teamKey, PersistentDataType.INTEGER, team);
                powerupStack.setItemMeta(powerupMeta);
            }
            Item drop = session.world.dropItem(spawn, powerupStack);
            drop.setVelocity(new Vector(0.0D, 0.04D, 0.0D));
            drop.setPickupDelay(10);
            drop.setInvulnerable(true);
            drop.setGlowing(true);
            drop.setCustomName(ChatColor.GOLD + ChatColor.BOLD.toString() + powerupName(powerup));
            drop.setCustomNameVisible(true);
            drop.getPersistentDataContainer().set(powerupKey, PersistentDataType.STRING, powerup);
            session.entities.add(drop);
            session.bombPowerupDrops.put(drop.getUniqueId(), drop);
            session.bombPowerupTeamByDrop.put(drop.getUniqueId(), team);
            session.bombPowerupStatusByTeam.put(team, BombLobbersPowerupStatus.SPAWNED);
            session.world.spawnParticle(Particle.HAPPY_VILLAGER, spawn.clone().add(0.0D, 0.5D, 0.0D),
                    28, 0.55D, 0.65D, 0.55D, 0.04D);
            session.world.spawnParticle(Particle.END_ROD, spawn.clone().add(0.0D, 0.6D, 0.0D),
                    8, 0.25D, 0.35D, 0.25D, 0.02D);
            session.world.playSound(spawn, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9F, 1.6F);
            spawned++;
        }
        // Both sides share one unpredictable wave clock, not two frequent announcements.
        long retryDelay = Math.max(1L, GameDebugRuntimeOverrides.longValue("BOMB_LOBBERS_POWERUP_RETRY_DELAY_MILLIS",
                ArcadePortRules.BOMB_LOBBERS_POWERUP_RETRY_DELAY_MILLIS));
        long nextWave = now + (spawned == 0 ? retryDelay : nextPowerupDelayMillis());
        for (int team = 0; team < 2; team++) {
            session.bombNextPowerupSpawnAtByTeam.put(team, nextWave);
        }
        if (spawned > 0) updateScoreboard(session);
    }

    Location findPowerupSpawn(SessionState session, int requestedTeam, SourceMapCatalog.MapData map) {
        if (session == null || session.world == null || requestedTeam < 0 || requestedTeam > 1) return null;
        int minX = map != null && map.hasBounds() ? map.minX() + 3 : -18;
        int maxX = map != null && map.hasBounds() ? map.maxX() - 3 : 18;
        int minZ = map != null && map.hasBounds() ? map.minZ() + 3 : -18;
        int maxZ = map != null && map.hasBounds() ? map.maxZ() - 3 : 18;
        if (maxX <= minX || maxZ <= minZ) return null;
        for (int attempt = 0; attempt < 120; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
            int y = session.world.getHighestBlockYAt(x, z);
            Block ground = session.world.getBlockAt(x, y, z);
            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);
            if (!ground.getType().isSolid() || ground.isLiquid()
                    || !feet.isPassable() || !head.isPassable()
                    || feet.isLiquid() || head.isLiquid()) continue;
            Location candidate = new Location(session.world, x + 0.5D, y + 1.1D, z + 0.5D);
            if (nearestSpawnTeam(session, candidate) != requestedTeam) continue;
            boolean nearSpawn = session.bombTeamSpawnCentres.values().stream()
                    .anyMatch(center -> center != null && center.distanceSquared(candidate) < 100.0D);
            if (nearSpawn) continue;
            boolean underPlayer = alivePlayers(session).stream()
                    .anyMatch(player -> player.getLocation().distanceSquared(candidate) < 25.0D);
            if (underPlayer) continue;
            return candidate;
        }
        return null;
    }

    /** Sample line-of-sight like vanilla 1.8 World#getBlockDensity. */
    double blastExposure(Location blast, Player target) {
        if (blast == null || target == null || blast.getWorld() == null
                || target.getWorld() != blast.getWorld()) return 0.0D;
        BoundingBox box = target.getBoundingBox();
        double xStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxX() - box.getMinX()));
        double yStep = ArcadePortRules.bombLobbersExposureSampleStep(box.getHeight());
        double zStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxZ() - box.getMinZ()));
        if (!Double.isFinite(xStep) || !Double.isFinite(yStep) || !Double.isFinite(zStep)) return 0.0D;
        double xOffset = ArcadePortRules.bombLobbersExposureSampleOffset(xStep);
        double zOffset = ArcadePortRules.bombLobbersExposureSampleOffset(zStep);
        int visible = 0;
        int total = 0;
        for (float fx = 0.0F; fx <= 1.0F; fx = (float) (fx + xStep)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float) (fy + yStep)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float) (fz + zStep)) {
                    Location sample = new Location(blast.getWorld(),
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * fx + xOffset,
                            box.getMinY() + box.getHeight() * fy,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * fz + zOffset);
                    Vector ray = sample.toVector().subtract(blast.toVector());
                    double distance = ray.length();
                    total++;
                    if (distance <= 1.0E-6D
                            || blast.getWorld().rayTraceBlocks(blast, ray.normalize(), distance,
                                    org.bukkit.FluidCollisionMode.NEVER, true) == null) visible++;
                }
            }
        }
        return total == 0 ? 0.0D : visible / (double) total;
    }

    void renderTrail(SessionState session, UUID ownerId, Location previous, Location current) {
        if (session == null || ownerId == null || previous == null || current == null
                || previous.getWorld() != session.world || current.getWorld() != session.world) return;
        Vector segment = current.toVector().subtract(previous.toVector());
        double distance = segment.length();
        if (!Double.isFinite(distance) || distance <= 1.0E-6D) return;
        int samples = ArcadePortRules.bombLobbersTrailSampleCount(distance);
        if (samples <= 0) return;

        int team = session.teams.getOrDefault(ownerId, 0);
        double spread = ArcadePortRules.bombLobbersTrailOffset(team);
        double speed = ArcadePortRules.bombLobbersTrailSpeed(team);
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0F);
        Vector direction = segment.multiply(1.0D / distance);
        for (int sample = 0; sample < samples; sample++) {
            double offset = sample * GameDebugRuntimeOverrides.doubleValue("BOMB_LOBBERS_TRAIL_SAMPLE_SPACING", ArcadePortRules.BOMB_LOBBERS_TRAIL_SAMPLE_SPACING);
            if (offset > distance) break;
            Location point = previous.clone().add(direction.clone().multiply(offset)).add(0.0D, 0.5D, 0.0D);
            session.world.spawnParticle(Particle.DUST, point, 2,
                    spread, spread, spread, speed, dust);
        }
    }

    /** The original compass needle follows enemies, while spectator teleport can target anyone alive. */
    void updateSpectatorCompasses(SessionState session) {
        if (session == null) return;
        List<Player> targets = alivePlayers(session);
        if (targets.isEmpty()) return;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            int spectatorTeam = session.teams.getOrDefault(spectator.getUniqueId(), -1);
            List<Player> enemies = targets.stream()
                    .filter(target -> ArcadePortRules.shouldBombLobbersCompassTrackTeam(
                            spectatorTeam, session.teams.getOrDefault(target.getUniqueId(), -1)))
                    .toList();
            if (enemies.isEmpty()) continue;

            boolean hasCompass = false;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (isTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) spectator.getInventory().addItem(trackingCompass("Tracks the nearest enemy player."));

            Player target = enemies.stream()
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(spectator.getLocation())))
                    .orElse(null);
            if (target != null) spectator.setCompassTarget(target.getLocation());
        }
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equals("Tracking Compass");
    }

    ItemStack createPowerupStack(String type, NamespacedKey powerupKey) {
        Material icon = switch (type) {
            case "wind-charge" -> Material.WIND_CHARGE;
            case "super-tnt" -> Material.TNT;
            case "fiery-tnt" -> Material.FIRE_CHARGE;
            default -> Material.SUGAR;
        };
        String lore = switch (type) {
            case "wind-charge" -> "Right-click to launch a gust that blasts opponents away.";
            case "super-tnt" -> "Throw this TNT for a bigger blast and stronger damage.";
            case "fiery-tnt" -> "Throw this TNT to ignite opponents caught in its blast.";
            default -> "Picked up instantly: Speed II for 12 seconds.";
        };
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + powerupName(type));
            meta.setLore(List.of(ChatColor.GRAY + lore));
            meta.getPersistentDataContainer().set(powerupKey, PersistentDataType.STRING, type);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    String powerupName(String type) {
        return switch (type) {
            case "wind-charge" -> "Wind Charge";
            case "super-tnt" -> "Super Explosive TNT";
            case "fiery-tnt" -> "Fiery TNT";
            case "speed-boost" -> "Speed Boost";
            default -> "Power-up";
        };
    }

    String powerupType(ItemStack item, NamespacedKey powerupKey) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(powerupKey, PersistentDataType.STRING);
    }

    boolean isThrownTnt(ItemStack item, NamespacedKey powerupKey) {
        if (item == null) return false;
        String powerupType = powerupType(item, powerupKey);
        return item.getType() == Material.TNT
                || "super-tnt".equals(powerupType)
                || "fiery-tnt".equals(powerupType);
    }

    Integer powerupTeam(ItemStack item, NamespacedKey teamKey) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(teamKey, PersistentDataType.INTEGER);
    }

    Player nearestSpectateTarget(SessionState session, Player spectator) {
        if (session == null || spectator == null) return null;
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    void openSpectatorMenu(SessionState session, Player spectator) {
        if (session == null || spectator == null) return;
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Spectator Menu");
        inventory.setItem(4, trackingCompass("Choose an enemy player to follow."));

        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43,
                46, 47, 48, 49, 50, 51, 52
        };
        Map<Integer, UUID> targetsBySlot = new HashMap<>();
        // Source Spectator Menu lists everyone alive, even though the needle follows enemies only.
        List<Player> targets = alivePlayers(session).stream()
                .sorted(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (int i = 0; i < targets.size() && i < slots.length; i++) {
            Player target = targets.get(i);
            int slot = slots[i];
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                int targetTeam = session.teams.getOrDefault(target.getUniqueId(), -1);
                ChatColor color = targetTeam == 1 ? ChatColor.AQUA : ChatColor.RED;
                meta.setDisplayName(color + target.getName());
                double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                double height = target.getLocation().getY() - spectator.getLocation().getY();
                String kit = session.bombLobbersKits.getOrDefault(target.getUniqueId(), "acrobat");
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
        session.bombCompassMenuTargets.put(spectator.getUniqueId(), targetsBySlot);
        spectator.openInventory(inventory);
    }

    boolean isSpectatorMenuTitle(String title) {
        return (ChatColor.DARK_GREEN + "Spectator Menu").equals(title);
    }

    void handleSpectatorMenuClick(SessionState session, Player spectator, int slot, boolean rightClick) {
        if (session == null || spectator == null) return;
        Map<Integer, UUID> targets = session.bombCompassMenuTargets.get(spectator.getUniqueId());
        UUID targetId = targets == null ? null : targets.get(slot);
        if (targetId == null) return;
        Player target = session.players.get(targetId);
        session.bombCompassMenuTargets.remove(spectator.getUniqueId());
        spectator.closeInventory();
        if (target == null || !target.isOnline() || !session.alive.contains(targetId)) {
            spectator.sendMessage(ChatColor.YELLOW + "That opponent is no longer available.");
            return;
        }
        if (rightClick) {
            spectator.setSpectatorTarget(target);
        } else {
            long now = System.currentTimeMillis();
            long readyAt = session.bombSpectateNextTeleportAt.getOrDefault(spectator.getUniqueId(), 0L);
            if (!ArcadePortRules.isBombLobbersSpectatorTeleportReady(now, readyAt)) return;
            spectator.teleport(target.getLocation().clone().add(0.0D, 1.0D, 0.0D));
            session.bombSpectateNextTeleportAt.put(spectator.getUniqueId(),
                    ArcadePortRules.nextBombLobbersSpectatorTeleportAt(now));
        }
        spectator.setCompassTarget(target.getLocation());
    }

    private ItemStack trackingCompass(String lore) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + lore));
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

    private int countAliveTeammates(SessionState session, int team) {
        int count = 0;
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()
                    && session.teams.getOrDefault(id, -1) == team) count++;
        }
        return count;
    }

    private String powerupScoreboardText(SessionState session, int team) {
        BombLobbersPowerupStatus status = session.bombPowerupStatusByTeam.get(team);
        if (status == null) return "";
        ChatColor color = switch (status) {
            case SPAWNED -> ChatColor.DARK_GRAY;
            case OBTAINED -> ChatColor.YELLOW;
            case ACTIVATED -> ChatColor.GREEN;
        };
        return color + " Powerup " + ChatColor.RESET;
    }
}
