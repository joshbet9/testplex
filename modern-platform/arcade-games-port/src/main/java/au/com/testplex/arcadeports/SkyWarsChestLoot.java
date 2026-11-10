package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.Chunk;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/** Builds the three source-defined SkyWars chest families in the loaded map. */
final class SkyWarsChestLoot {
    private SkyWarsChestLoot() { }

    static int populate(SourceMapCatalog.MapData map) {
        if (map == null) return 0;
        int total = 0;
        total += populateFamily(map.dataLocations("BROWN"), islandPools());
        total += populateFamily(map.dataLocations("GRAY"), connectorPools());
        total += populateFamily(map.dataLocations("BLACK"), middlePools());
        if (total == 0) total = populateExistingSourceChests(map);
        return total;
    }

    /**
     * Some original WorldConfig files omit the coloured chest markers even though
     * their map archives contain real chests. Keep those maps playable by filling
     * their authored chests rather than leaving an apparently empty SkyWars map.
     */
    private static int populateExistingSourceChests(SourceMapCatalog.MapData map) {
        List<org.bukkit.Location> starts = map.spawnLocations();
        if (starts.isEmpty()) starts = map.dataLocations("GREEN");
        if (starts.isEmpty() || starts.getFirst().getWorld() == null) return 0;
        org.bukkit.World world = starts.getFirst().getWorld();
        double centerX = map.hasBounds() ? map.centerX() : world.getSpawnLocation().getX();
        double centerZ = map.hasBounds() ? map.centerZ() : world.getSpawnLocation().getZ();
        int count = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (map.hasBounds() && (chunk.getX() * 16 > map.maxX() || chunk.getX() * 16 + 15 < map.minX()
                    || chunk.getZ() * 16 > map.maxZ() || chunk.getZ() * 16 + 15 < map.minZ())) continue;
            for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                if (!(state instanceof Chest chest)) continue;
                org.bukkit.Location location = chest.getLocation();
                double nearestIsland = starts.stream().mapToDouble(spawn -> {
                    double dx = location.getX() - spawn.getX();
                    double dz = location.getZ() - spawn.getZ();
                    return dx * dx + dz * dz;
                }).min().orElse(Double.POSITIVE_INFINITY);
                double fromCenterX = location.getX() - centerX;
                double fromCenterZ = location.getZ() - centerZ;
                double centerDistance = fromCenterX * fromCenterX + fromCenterZ * fromCenterZ;
                List<Pool> pools = nearestIsland <= 32.0D * 32.0D
                        ? islandPools()
                        : centerDistance <= 20.0D * 20.0D ? middlePools() : connectorPools();
                populateChest(chest, pools);
                count++;
            }
        }
        return count;
    }

    static ItemStack randomMiddleItem() {
        List<Pool> pools = middlePools();
        return pools.get(ThreadLocalRandom.current().nextInt(pools.size())).randomItem();
    }

    static void populateSupplementalIslandChest(Chest chest) {
        if (chest != null) populateChest(chest, islandPools());
    }

    private static int populateFamily(List<org.bukkit.Location> locations, List<Pool> pools) {
        int count = 0;
        for (org.bukkit.Location location : locations) {
            if (location == null || location.getWorld() == null) continue;
            Block block = location.getBlock();
            block.setType(Material.CHEST, false);
            if (block.getBlockData() instanceof Directional directional) {
                List<BlockFace> openFaces = new ArrayList<>();
                for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                    Block neighbor = block.getRelative(face);
                    if (neighbor.isPassable() || neighbor.getType().isAir()) openFaces.add(face);
                }
                if (!openFaces.isEmpty()) {
                    directional.setFacing(openFaces.get(ThreadLocalRandom.current().nextInt(openFaces.size())));
                    block.setBlockData(directional, false);
                }
            }
            if (!(block.getState() instanceof Chest chest)) continue;
            populateChest(chest, pools);
            count++;
        }
        return count;
    }

    private static void populateChest(Chest chest, List<Pool> pools) {
        chest.getBlockInventory().clear();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < chest.getBlockInventory().getSize(); slot++) slots.add(slot);
        for (Pool pool : pools) {
            if (slots.isEmpty() || ThreadLocalRandom.current().nextDouble() >= pool.probability) continue;
            int amount = randomInclusive(pool.minItems, pool.maxItems);
            for (int item = 0; item < amount && !slots.isEmpty(); item++) {
                ItemStack stack = pool.randomItem();
                if (stack == null) continue;
                int slotIndex = ThreadLocalRandom.current().nextInt(slots.size());
                chest.getBlockInventory().setItem(slots.remove(slotIndex), stack);
            }
        }
        if (chest.getBlockInventory().isEmpty()) {
            ItemStack fallback = pools.getFirst().randomItem();
            if (fallback != null) chest.getBlockInventory().setItem(13, fallback);
        }
        chest.update(true, false);
    }

    private static Enchantment weightedEnchantment(List<WeightedEnchantment> entries) {
        int total = entries.stream().mapToInt(WeightedEnchantment::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (WeightedEnchantment entry : entries) {
            roll -= entry.weight;
            if (roll < 0) return entry.enchantment;
        }
        return entries.get(entries.size() - 1).enchantment;
    }

    private static int randomInclusive(int minimum, int maximum) {
        return minimum + ThreadLocalRandom.current().nextInt(maximum - minimum + 1);
    }

    private static Pool pool(double probability, int minItems, int maxItems, Loot... loot) {
        return new Pool(probability, minItems, maxItems, List.of(loot), 0.0D, List.of(), 1);
    }

    private static Pool enchantedPool(double probability, int minItems, int maxItems,
            double enchantChance, int maxLevel, List<WeightedEnchantment> enchantments, Loot... loot) {
        return new Pool(probability, minItems, maxItems, List.of(loot), enchantChance, enchantments, maxLevel);
    }

    private static List<Pool> islandPools() {
        return List.of(
                pool(1, 2, 3, loot(Material.GOLDEN_HELMET), loot(Material.GOLDEN_CHESTPLATE),
                        loot(Material.GOLDEN_LEGGINGS), loot(Material.GOLDEN_BOOTS)),
                enchantedPool(1, 1, 1, .5, 1, List.of(ench(Enchantment.SHARPNESS, 100)), loot(Material.STONE_SWORD)),
                pool(.8, 1, 1, loot(Material.STONE_AXE), loot(Material.STONE_SHOVEL)),
                pool(.15, 1, 1, loot(Material.BOW)),
                pool(.8, 1, 2, loot(Material.COOKED_BEEF, 1, 3), loot(Material.COOKED_CHICKEN, 1, 3),
                        loot(Material.COOKED_COD, 1, 3)),
                pool(.9, 1, 1, loot(Material.OAK_PLANKS, 16, 32), loot(Material.COBBLESTONE, 16, 32)),
                pool(.4, 1, 1, loot(Material.SNOWBALL, 2, 5), loot(Material.EGG, 2, 5)));
    }

    private static List<Pool> connectorPools() {
        return List.of(
                enchantedPool(1, 1, 2, .6, 1, List.of(
                        ench(Enchantment.PROTECTION, 100), ench(Enchantment.PROJECTILE_PROTECTION, 100)),
                        loot(Material.CHAINMAIL_HELMET), loot(Material.CHAINMAIL_CHESTPLATE),
                        loot(Material.CHAINMAIL_LEGGINGS), loot(Material.CHAINMAIL_BOOTS)),
                pool(.8, 1, 1, loot(Material.STONE_SWORD)),
                pool(.7, 1, 1, loot(Material.IRON_AXE), loot(Material.IRON_PICKAXE), loot(Material.FISHING_ROD)),
                pool(.25, 1, 1, loot(Material.BOW)),
                pool(.8, 1, 2, loot(Material.COOKED_BEEF, 1, 3), loot(Material.COOKED_CHICKEN, 1, 3),
                        loot(Material.COOKED_COD, 1, 3)),
                pool(.9, 1, 1, loot(Material.OAK_PLANKS, 16, 32), loot(Material.COBBLESTONE, 16, 32),
                        loot(Material.GLASS, 16, 32)),
                pool(1, 1, 2, weighted(Material.ARROW, 4, 8, 150), loot(Material.SNOWBALL, 2, 5),
                        loot(Material.EGG, 2, 5), weighted(Material.LAVA_BUCKET, 1, 1, 80),
                        loot(Material.WATER_BUCKET), loot(Material.ENDER_PEARL, 1, 2)),
                pool(.25, 1, 1, lootBook(Enchantment.SHARPNESS), lootBook(Enchantment.KNOCKBACK),
                        lootBook(Enchantment.POWER), lootBook(Enchantment.PUNCH), lootBook(Enchantment.PROTECTION)));
    }

    private static List<Pool> middlePools() {
        return List.of(
                pool(1, 1, 2,
                        loot(Material.IRON_HELMET), loot(Material.IRON_CHESTPLATE),
                        loot(Material.IRON_LEGGINGS), loot(Material.IRON_BOOTS)),
                pool(.4, 1, 1, loot(Material.IRON_SWORD)),
                pool(.4, 1, 1, weighted(Material.DIAMOND_SWORD, 1, 1, 30), loot(Material.DIAMOND, 1, 3)),
                pool(.8, 1, 1, loot(Material.IRON_AXE), loot(Material.IRON_PICKAXE), loot(Material.FISHING_ROD)),
                pool(.35, 1, 1, loot(Material.BOW)),
                pool(.8, 1, 2, loot(Material.COOKED_BEEF, 1, 3), loot(Material.COOKED_CHICKEN, 1, 3),
                        loot(Material.COOKED_COD, 1, 3)),
                pool(.9, 1, 1, loot(Material.OAK_PLANKS, 16, 32), loot(Material.COBBLESTONE, 16, 32),
                        loot(Material.GLASS, 16, 32)),
                pool(1, 2, 3, weighted(Material.EXPERIENCE_BOTTLE, 5, 10, 130),
                        weighted(Material.ARROW, 6, 10, 160), loot(Material.SNOWBALL, 2, 5),
                        loot(Material.EGG, 2, 5), weighted(Material.LAVA_BUCKET, 1, 1, 60),
                        weighted(Material.WATER_BUCKET, 1, 1, 130), loot(Material.ENDER_PEARL, 1, 2),
                        weighted(Material.MUSHROOM_STEW, 1, 1, 130)),
                pool(.5, 1, 1, lootBook(Enchantment.SHARPNESS), lootBook(Enchantment.KNOCKBACK),
                        lootBook(Enchantment.POWER), lootBook(Enchantment.PUNCH), lootBook(Enchantment.PROTECTION)));
    }

    private static Loot loot(Material material) { return loot(material, 1, 1, 100); }
    private static Loot loot(Material material, int min, int max) { return loot(material, min, max, 100); }
    private static Loot weighted(Material material, int min, int max, int weight) {
        return loot(material, min, max, weight);
    }
    private static Loot loot(Material material, int min, int max, int weight) {
        return new Loot(material, min, max, weight, null);
    }
    private static Loot lootBook(Enchantment enchantment) {
        return new Loot(Material.ENCHANTED_BOOK, 1, 1, 100, enchantment);
    }
    private static WeightedEnchantment ench(Enchantment enchantment, int weight) {
        return new WeightedEnchantment(enchantment, weight);
    }

    private record Loot(Material material, int min, int max, int weight, Enchantment bookEnchantment) { }
    private record WeightedEnchantment(Enchantment enchantment, int weight) { }

    private record Pool(double probability, int minItems, int maxItems, List<Loot> entries,
            double enchantChance, List<WeightedEnchantment> enchantments, int enchantmentMaxLevel) {
        ItemStack randomItem() {
            int totalWeight = entries.stream().mapToInt(Loot::weight).sum();
            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            Loot selected = entries.get(entries.size() - 1);
            for (Loot entry : entries) {
                roll -= entry.weight;
                if (roll < 0) { selected = entry; break; }
            }
            ItemStack stack = new ItemStack(selected.material, randomInclusive(selected.min, selected.max));
            if (selected.bookEnchantment != null && stack.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                meta.addStoredEnchant(selected.bookEnchantment, 1, true);
                stack.setItemMeta(meta);
            }
            if (!enchantments.isEmpty() && ThreadLocalRandom.current().nextDouble() < enchantChance) {
                stack.addUnsafeEnchantment(weightedEnchantment(enchantments),
                        randomInclusive(1, enchantmentMaxLevel));
            }
            return stack;
        }
    }
}
