package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Micro Battles' per-player world-border cue, invoked from the existing match tick. */
final class MicroBattlesManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.MICRO_BATTLES; }

    static boolean shouldScanCrumble(boolean scanComplete, long ticks, int intervalTicks) {
        return !scanComplete && intervalTicks > 0 && Math.floorMod(ticks, intervalTicks) == 0;
    }

    /** Arena scan and block removal run from the existing shared tick, in two-tick vertical slices. */
    void updateCrumble(SessionState session, long now, long startedAt, Supplier<Location> spectatorCenter,
            java.util.function.Consumer<String> log) {
        if (session == null || session.type != ArcadePortGame.MICRO_BATTLES || session.world == null
                || spectatorCenter == null || log == null) return;
        if (!GameDebugRuntimeOverrides.booleanValue("MICRO_BATTLES_CRUMBLE_ENABLED",
                ArcadePortRules.MICRO_BATTLES_CRUMBLE_ENABLED)) return;

        if (shouldScanCrumble(session.microCrumbleScanComplete, session.ticks,
                GameDebugRuntimeOverrides.intValue("MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS",
                        ArcadePortRules.MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS))) {
            int y = session.microCrumbleScanY;
            for (int x = session.microCrumbleMinX; x < session.microCrumbleMaxX; x++) {
                for (int z = session.microCrumbleMinZ; z < session.microCrumbleMaxZ; z++) {
                    Block block = session.world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) session.microCrumbleBlocks.add(block);
                }
            }
            session.microCrumbleScanY++;
            if (ArcadePortRules.isMicroCrumbleScanComplete(session.microCrumbleScanY,
                    session.microCrumbleMaxY)) {
                session.microCrumbleScanComplete = true;
                log.accept("Micro Battles source map scan completed with "
                        + session.microCrumbleBlocks.size() + " crumble candidates.");
            }
        }

        // Reborn starts crumbling after one second and removes three blocks per tick,
        // prioritising blocks furthest from the map's spectator/centre location.
        if (now - startedAt < 1_000L || session.microCrumbleBlocks.isEmpty()) return;
        Location center = spectatorCenter.get();
        if (center == null) return;
        double centerX = center.getX();
        double centerZ = center.getZ();
        for (int i = 0; i < GameDebugRuntimeOverrides.intValue("MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK",
                ArcadePortRules.MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK); i++) {
            Block best = null;
            double farthest = -1.0D;
            for (Block candidate : session.microCrumbleBlocks) {
                double dx = candidate.getX() + 0.5D - centerX;
                double dz = candidate.getZ() + 0.5D - centerZ;
                double distanceSquared = dx * dx + dz * dz;
                if (best == null || distanceSquared > farthest) {
                    best = candidate;
                    farthest = distanceSquared;
                }
            }
            if (best == null) break;

            while (best.getY() > session.world.getMinHeight()
                    && !best.getRelative(BlockFace.DOWN).getType().isAir()) {
                best = best.getRelative(BlockFace.DOWN);
            }
            session.microCrumbleBlocks.remove(best);
            if (best.getWorld() != center.getWorld() || best.getType().isAir()) continue;

            Material material = best.getType();
            if (material.name().endsWith("_DOOR")) {
                best.getRelative(BlockFace.UP).setType(Material.AIR, false);
            } else if (ThreadLocalRandom.current().nextDouble() > 0.95D) {
                // Preserve the server's default falling-block item-drop behaviour.
                session.world.spawnFallingBlock(
                        best.getLocation().add(0.5D, 0.5D, 0.5D), best.getBlockData());
            }
            best.setType(Material.AIR, false);
        }
    }

    boolean shouldReleaseBarrier(SessionState session, long elapsedMillis) {
        if (session == null || !session.microBarrierActive) return false;
        long duration = GameDebugRuntimeOverrides.longValue("MICRO_BATTLES_BARRIER_DURATION_MILLIS",
                ArcadePortRules.MICRO_BATTLES_BARRIER_DURATION_MILLIS);
        return ArcadePortRules.shouldRemoveMicroBarrier(elapsedMillis, duration);
    }

    void releaseBarrier(SessionState session, IntFunction<Location> islandCenter, Consumer<String> announce) {
        if (session == null || !session.microBarrierActive || session.world == null
                || islandCenter == null || announce == null) return;
        for (Block block : session.microBarrierBlocks) {
            if (block.getType() == Material.GLASS) block.setType(Material.AIR, false);
        }
        // Some imported maps have incomplete glass marker metadata. Clear the expected island rings too.
        for (int team = 0; team < 4; team++) {
            Location island = islandCenter.apply(team);
            if (island == null) continue;
            int cx = island.getBlockX();
            int cy = island.getBlockY();
            int cz = island.getBlockZ();
            for (int offset = -4; offset <= 4; offset++) {
                for (int y = cy; y <= ArcadePortRules.microBattlesBarrierTopY(cy); y++) {
                    Block[] ring = {
                            session.world.getBlockAt(cx + offset, y, cz - 4),
                            session.world.getBlockAt(cx + offset, y, cz + 4),
                            session.world.getBlockAt(cx - 4, y, cz + offset),
                            session.world.getBlockAt(cx + 4, y, cz + offset)
                    };
                    for (Block block : ring) {
                        if (block.getType() == Material.GLASS) block.setType(Material.AIR, false);
                    }
                }
            }
        }
        session.microBarrierBlocks.clear();
        session.microBarrierActive = false;
        announce.accept("Glass barriers are down! Fight for your team.");
    }

    void updateBorderParticles(SessionState session, List<Player> alive, Predicate<Player> showForPlayer) {
        if (session == null || session.type != ArcadePortGame.MICRO_BATTLES
                || session.world == null || alive == null || showForPlayer == null) return;
        WorldBorder border = session.world.getWorldBorder();
        Location center = border.getCenter();
        double half = border.getSize() / 2.0D;
        for (Player player : alive) {
            if (!showForPlayer.test(player)) continue;
            Location location = player.getLocation();
            double x = location.getX(), z = location.getZ();
            if (!ArcadePortRules.shouldShowMicroBattlesBorderParticles(
                    x, z, center.getX(), center.getZ(), border.getSize(),
                    GameDebugRuntimeOverrides.doubleValue("MICRO_BATTLES_BORDER_PARTICLE_DISTANCE",
                            ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_DISTANCE))) continue;
            double xFaceDistance = half - Math.abs(x - center.getX());
            double zFaceDistance = half - Math.abs(z - center.getZ());
            boolean xFace = xFaceDistance <= zFaceDistance;
            double edgeX = center.getX() + (x >= center.getX() ? half - 0.06D : -half + 0.06D);
            double edgeZ = center.getZ() + (z >= center.getZ() ? half - 0.06D : -half + 0.06D);
            double along = xFace ? Math.max(center.getZ() - half + 0.08D,
                    Math.min(center.getZ() + half - 0.08D, z))
                    : Math.max(center.getX() - half + 0.08D, Math.min(center.getX() + half - 0.08D, x));
            Color colour = ArcadePortRules.microBattlesParticleColour(
                    session.microBorderColours.getOrDefault(player.getUniqueId(),
                            ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR));
            Particle.DustOptions dust = new Particle.DustOptions(colour, 1.2F);
            // Three points make the border obvious across the player's full height, not just on the floor.
            int verticalPoints = GameDebugRuntimeOverrides.intValue("MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS",
                    ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS);
            for (int point = 0; point < verticalPoints; point++) {
                double height = verticalPoints == 1 ? 0.95D : 0.15D + 1.6D * point / (verticalPoints - 1);
                Location cue = xFace
                        ? new Location(session.world, edgeX, location.getY() + height, along)
                        : new Location(session.world, along, location.getY() + height, edgeZ);
                session.world.spawnParticle(Particle.DUST, cue, 2, 0.015D, 0.025D, 0.015D, 0.0D, dust);
            }
        }
    }

    ItemStack teamArmorPiece(Material material, Color colour, ChatColor chatColor, NamespacedKey itemKey) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(colour);
            meta.setDisplayName(chatColor + "" + ChatColor.BOLD + "Team Armor");
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "armour");
            item.setItemMeta(meta);
        }
        return item;
    }

    Color teamDyeColor(int team) {
        return switch (team) {
            case 0 -> Color.RED;
            case 1 -> Color.AQUA;
            case 2 -> Color.LIME;
            default -> Color.YELLOW;
        };
    }

    ChatColor teamChatColor(int team) {
        return switch (team) {
            case 0 -> ChatColor.RED;
            case 1 -> ChatColor.AQUA;
            case 2 -> ChatColor.GREEN;
            default -> ChatColor.YELLOW;
        };
    }

    String teamName(int team) {
        return switch (team) {
            case 0 -> "Red";
            case 1 -> "Blue";
            case 2 -> "Green";
            default -> "Yellow";
        };
    }

    boolean isTeamLockedItem(ItemStack item, NamespacedKey itemKey) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.STRING);
    }

    /** Reborn locks the slot-eight badge, but allows ordinary armour to be dropped. */
    boolean isTeamBadgeItem(ItemStack item, NamespacedKey itemKey) {
        return item != null && item.hasItemMeta()
                && "hotbar".equals(item.getItemMeta().getPersistentDataContainer()
                        .get(itemKey, PersistentDataType.STRING));
    }

    boolean isTrackingCompass(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
    }

    void giveTrackingCompass(Player player) {
        if (player == null || !player.isOnline()) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrackingCompass(item)) return;
        }
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + "Left-click to teleport to the nearest opponent.",
                    ChatColor.GRAY + "Right-click to choose an opponent."));
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }

    Player nearestSpectatorTarget(SessionState session, Player spectator) {
        if (session == null || spectator == null) return null;
        int spectatorTeam = session.teams.getOrDefault(spectator.getUniqueId(), -1);
        return alivePlayers(session).stream()
                .filter(target -> !target.getUniqueId().equals(spectator.getUniqueId()))
                .filter(target -> session.teams.getOrDefault(target.getUniqueId(), -1) != spectatorTeam)
                .min(java.util.Comparator.comparingDouble(target ->
                        target.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    void updateSpectatorCompass(SessionState session) {
        if (session == null || session.ticks % 4 != 0) return;
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline() || session.alive.contains(player.getUniqueId())) continue;
            giveTrackingCompass(player);
            Player target = nearestSpectatorTarget(session, player);
            if (target != null) {
                player.setCompassTarget(target.getLocation());
                if (isTrackingCompass(player.getInventory().getItemInMainHand())) {
                    double distance = Math.sqrt(player.getLocation().distanceSquared(target.getLocation()));
                    double height = target.getLocation().getY() - player.getLocation().getY();
                    player.sendActionBar(ChatColor.WHITE + "Nearest Target: "
                            + teamChatColor(session.teams.getOrDefault(target.getUniqueId(), -1))
                            + target.getName() + ChatColor.WHITE + " | Distance: "
                            + String.format(java.util.Locale.ROOT, "%.1f", distance)
                            + " | Height: " + String.format(java.util.Locale.ROOT, "%+.1f", height));
                }
            }
        }
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
