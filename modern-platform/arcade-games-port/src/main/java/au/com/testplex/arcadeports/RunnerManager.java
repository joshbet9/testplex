package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.BlockData;

/** Runner's palette-aware footprints, block decay and spectator tracking. */
final class RunnerManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.RUNNER; }

    boolean insideSourceBounds(SourceMapCatalog.MapData mapData, int halfSize, int x, int z) {
        if (mapData != null && mapData.hasBounds()) {
            return x >= mapData.minX() && x < mapData.maxX()
                    && z >= mapData.minZ() && z < mapData.maxZ();
        }
        return Math.abs(x) <= halfSize && Math.abs(z) <= halfSize;
    }

    void markFootprints(SessionState session, long now, SourceMapCatalog.MapData mapData,
            int halfSize, Predicate<Player> trailAllowed) {
        for (UUID id : new HashSet<>(session.alive)) {
            Player player = session.players.get(id);
            if (player == null || !player.isOnline() || player.getWorld() != session.world) continue;

            org.bukkit.util.BoundingBox bounds = player.getBoundingBox();
            int minX = (int) Math.floor(bounds.getMinX());
            int maxX = (int) Math.floor(bounds.getMaxX() - 1.0E-6D);
            int minZ = (int) Math.floor(bounds.getMinZ());
            int maxZ = (int) Math.floor(bounds.getMaxZ() - 1.0E-6D);
            // Match Reborn's player-location minus half a block; jumps sample air.
            int trailY = (int) Math.floor(player.getLocation().getY() - 0.5D);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!insideSourceBounds(mapData, halfSize, x, z)) continue;
                    markBlock(session, session.world.getBlockAt(x, trailY, z), player, now,
                            mapData, halfSize, trailAllowed);
                }
            }
        }
    }

    void markBlock(SessionState session, Block block, Player owner, long now,
            SourceMapCatalog.MapData mapData, int halfSize, Predicate<Player> trailAllowed) {
        if (block == null || block.getY() < 0
                || block.getY() >= session.world.getMaxHeight()
                || !insideSourceBounds(mapData, halfSize, block.getX(), block.getZ())) return;
        if (owner != null && !trailAllowed.test(owner)) return;
        Long graceUntil = session.runnerBlockGraceUntil.get(block);
        if (graceUntil != null) {
            if (now < graceUntil) return;
            session.runnerBlockGraceUntil.remove(block);
        }
        boolean aboveIsAir = block.getRelative(BlockFace.UP).getType().isAir();
        if (!ArcadePortRules.shouldMarkRunnerBlock(block.getType().isSolid(),
                block.getType() == Material.BEDROCK, block.isLiquid(), aboveIsAir,
                session.runnerBlocks.containsKey(block))) return;
        // World-block changes are sent by the server to all players and spectators.
        Material trail = trailMaterial(session, owner);
        session.runnerOriginalBlockData.putIfAbsent(block, block.getBlockData().clone());
        session.runnerBlockOwners.put(block, owner.getUniqueId());
        block.setType(trail, false);
        session.runnerBlocks.put(block, now);
        session.runnerBlockMaterials.put(block, trail);
    }

    void processBlocks(SessionState session, long now, Predicate<Player> trailAllowed) {
        java.util.Iterator<Map.Entry<Block, Long>> iterator = session.runnerBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Block, Long> entry = iterator.next();
            Block block = entry.getKey();
            Material trail = session.runnerBlockMaterials.get(block);
            if (block == null || trail == null || block.getType() != trail) {
                iterator.remove();
                session.runnerBlockMaterials.remove(block);
                continue;
            }
            UUID ownerId = session.runnerBlockOwners.get(block);
            Player owner = ownerId == null ? null : session.players.get(ownerId);
            if (owner != null && !trailAllowed.test(owner)) {
                BlockData original = session.runnerOriginalBlockData.remove(block);
                if (original != null) block.setBlockData(original, false);
                iterator.remove();
                session.runnerBlockMaterials.remove(block);
                session.runnerBlockOwners.remove(block);
                continue;
            }
            Long graceUntil = session.runnerBlockGraceUntil.get(block);
            if (graceUntil != null) {
                if (now < graceUntil) continue;
                session.runnerBlockGraceUntil.remove(block);
            }
            if (!ArcadePortRules.shouldDropRunnerBlock(now - entry.getValue())) continue;
            block.setType(Material.AIR, false);
            iterator.remove();
            session.runnerBlockMaterials.remove(block);
            session.runnerBlockOwners.remove(block);
            session.runnerOriginalBlockData.remove(block);
        }
        session.runnerBlockGraceUntil.entrySet().removeIf(entry -> entry.getValue() <= now
                && !session.runnerBlocks.containsKey(entry.getKey()));
    }

    void updateDistanceAndSpectatorCompass(SessionState session, long now) {
        if (now >= session.runnerMarathonNextSampleAt) {
            session.runnerMarathonNextSampleAt = now + 1_000L;
            for (Player player : alivePlayers(session)) {
                UUID id = player.getUniqueId();
                Location current = player.getLocation().clone();
                Location previous = session.runnerMarathonLastSample.put(id, current);
                if (previous != null && previous.getWorld() == current.getWorld()) {
                    double distance = previous.distance(current);
                    if (Double.isFinite(distance) && distance >= 0.0D) {
                        session.runnerMarathonDistance.merge(id, distance, Double::sum);
                    }
                }
            }
        }
        if (now >= session.runnerCompassNextUpdateAt) {
            session.runnerCompassNextUpdateAt = now + 250L;
            List<Player> targets = alivePlayers(session);
            if (targets.isEmpty()) return;
            for (Player spectator : session.players.values()) {
                if (spectator == null || !spectator.isOnline()
                        || session.alive.contains(spectator.getUniqueId())
                        || spectator.getWorld() != session.world) continue;
                boolean hasCompass = java.util.Arrays.stream(spectator.getInventory().getContents())
                        .anyMatch(this::isTrackingCompass);
                if (!hasCompass) spectator.getInventory().addItem(trackingCompass());
                Player target = targets.stream().min(Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(spectator.getLocation()))).orElse(null);
                if (target == null) continue;
                spectator.setCompassTarget(target.getLocation());
                if (isTrackingCompass(spectator.getInventory().getItemInMainHand())) {
                    double distance = Math.sqrt(target.getLocation().distanceSquared(spectator.getLocation()));
                    double height = target.getLocation().getY() - spectator.getLocation().getY();
                    spectator.sendActionBar(ChatColor.WHITE + "Nearest Target: " + ChatColor.GREEN + target.getName()
                            + ChatColor.WHITE + " | Distance: " + ChatColor.GREEN
                            + String.format(Locale.ROOT, "%.1f", distance)
                            + ChatColor.WHITE + " | Height: " + ChatColor.GREEN
                            + String.format(Locale.ROOT, "%.1f", height));
                }
            }
        }
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    private ItemStack trackingCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + "Tracks the nearest surviving Runner."));
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

    Map<String, Integer> scanMapPalette(SessionState session, SourceMapCatalog.MapData mapData, int halfSize) {
        Map<String, Integer> palette = new HashMap<>();
        int minX = mapData != null && mapData.hasBounds() ? mapData.minX() : -halfSize;
        int maxX = mapData != null && mapData.hasBounds() ? mapData.maxX() : halfSize + 1;
        int minZ = mapData != null && mapData.hasBounds() ? mapData.minZ() : -halfSize;
        int maxZ = mapData != null && mapData.hasBounds() ? mapData.maxZ() : halfSize + 1;
        int minY = mapData != null && mapData.hasVerticalBounds()
                ? mapData.minY() : Math.max(session.world.getMinHeight(), 0);
        int maxY = mapData != null && mapData.hasVerticalBounds()
                ? mapData.maxY() : Math.min(session.world.getMaxHeight() - 1, 128);
        if (mapData != null && !mapData.hasVerticalBounds()) {
            List<Location> spawns = mapData.spawnLocations();
            if (!spawns.isEmpty()) {
                double lowestSpawn = spawns.stream().mapToDouble(Location::getY).min().orElse(minY);
                double highestSpawn = spawns.stream().mapToDouble(Location::getY).max().orElse(maxY);
                minY = Math.max(session.world.getMinHeight(), (int) Math.floor(lowestSpawn) - 16);
                maxY = Math.min(session.world.getMaxHeight() - 1, (int) Math.ceil(highestSpawn) + 32);
            }
        }
        minY = Math.max(minY, session.world.getMinHeight());
        maxY = Math.min(maxY, session.world.getMaxHeight() - 1);
        if (maxX <= minX || maxZ <= minZ || maxY < minY) return palette;

        long area = (long) (maxX - minX) * (long) (maxZ - minZ);
        int columnStride = Math.max(1, (int) Math.ceil(Math.sqrt(area / 12_000.0D)));
        for (int x = minX; x < maxX; x += columnStride) {
            for (int z = minZ; z < maxZ; z += columnStride) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = session.world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid() || block.isLiquid()
                            || !block.getRelative(BlockFace.UP).getType().isAir()) continue;
                    String colour = paletteColour(block.getType());
                    if (colour != null) palette.merge(colour, 1, Integer::sum);
                }
            }
        }
        return palette;
    }

    String paletteColour(Material material) {
        String name = material.name();
        for (String suffix : List.of("_CONCRETE_POWDER", "_STAINED_GLASS_PANE", "_STAINED_GLASS",
                "_CONCRETE", "_GLAZED_TERRACOTTA", "_TERRACOTTA", "_WOOL")) {
            if (!name.endsWith(suffix)) continue;
            String colour = name.substring(0, name.length() - suffix.length())
                    .toLowerCase(Locale.ROOT).replace('_', '-');
            if (ArcadePortRules.runnerTrailColours().contains(colour)) return colour;
        }
        return null;
    }

    Material trailMaterial(SessionState session, Player owner) {
        String selected = session.runnerTrailOptions.getOrDefault(owner.getUniqueId(), "trail-cyan");
        String colour = selected.startsWith("trail-") ? selected.substring("trail-".length()) : selected;
        Material material = Material.matchMaterial(
                colour.toUpperCase(Locale.ROOT).replace('-', '_') + "_TERRACOTTA");
        return material == null ? Material.CYAN_TERRACOTTA : material;
    }
}
