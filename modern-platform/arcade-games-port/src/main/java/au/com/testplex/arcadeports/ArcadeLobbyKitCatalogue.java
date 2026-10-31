package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Shared declarative metadata for the legacy kit stations.
 *
 * Game mechanics and lobby rendering both resolve kit IDs through this catalogue,
 * rather than embedding selection metadata in the lobby renderer. The optional
 * hidden-kit list mirrors the legacy KitAvailability.Hide behavior; entries use
 * the form "game-id:kit-id".
 */
final class ArcadeLobbyKitCatalogue {
    private ArcadeLobbyKitCatalogue() {
    }

    static List<KitChoice> forGame(ArcadePortGame game, List<String> hiddenKitIds) {
        return forGame(game, hiddenKitIds, List.of());
    }

    /**
     * Resolves configured order first, then retains the source catalogue order for
     * any remaining choices. Unknown IDs are ignored so config drift is harmless.
     */
    static List<KitChoice> forGame(
            ArcadePortGame game, List<String> hiddenKitIds, List<String> configuredOrder) {
        if (game == null) return List.of();
        List<KitChoice> choices = switch (game) {
            case BACON_BRAWL -> List.of(
                    choice("el-muchacho-pigo", "El Muchacho Pigo", Material.PORKCHOP,
                            Unlock.FREE, Pig.class, null),
                    choice("mama-piggles", "Mama Piggles", Material.COOKED_PORKCHOP,
                            Unlock.GEM, Pig.class, null),
                    choice("pig", "Pig Cloak", Material.PHANTOM_MEMBRANE,
                            Unlock.GEM, Sheep.class, null, DyeColor.PINK),
                    choice("brutus-bacon", "Brutus Bacon", Material.SHIELD,
                            Unlock.ACHIEVEMENT, Pig.class, null));
            case BOMB_LOBBERS -> List.of(
                    choice("jumper", "Jumper", Material.RABBIT_FOOT,
                            Unlock.FREE, Zombie.class, Material.IRON_AXE),
                    choice("armorer", "Armorer", Material.IRON_CHESTPLATE,
                            Unlock.GEM, Zombie.class, Material.IRON_HELMET,
                            null, Material.IRON_BOOTS, Material.IRON_LEGGINGS,
                            Material.IRON_CHESTPLATE, Material.IRON_HELMET),
                    choice("pitcher", "Pitcher", Material.SNOWBALL,
                            Unlock.GEM, Zombie.class, Material.LEVER,
                            null, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS,
                            Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET),
                    choice("waller", "Waller", Material.BRICKS,
                            Unlock.ACHIEVEMENT, Zombie.class, Material.STONE_BRICKS));
            case DEATH_TAG -> List.of(
                    choice("runner-basher", "Runner Basher", Material.IRON_SWORD,
                            Unlock.FREE, Zombie.class, Material.IRON_SWORD),
                    choice("runner-archer", "Runner Archer", Material.BOW,
                            Unlock.GEM, Zombie.class, Material.BOW),
                    choice("runner-traitor", "Runner Traitor", Material.GOLD_INGOT,
                            Unlock.GEM, Zombie.class, Material.IRON_AXE));
            case DRAGON_ESCAPE -> List.of(
                    choice("jumper", "Jumper", Material.FEATHER,
                            Unlock.FREE, Zombie.class, Material.IRON_AXE),
                    choice("disruptor", "Disruptor", Material.TNT,
                            Unlock.GEM, Zombie.class, Material.TNT),
                    choice("warper", "Warper", Material.ENDER_PEARL,
                            Unlock.GEM, Zombie.class, Material.ENDER_PEARL),
                    choice("digger", "Digger", Material.IRON_PICKAXE,
                            Unlock.ACHIEVEMENT, Zombie.class, Material.DIAMOND_PICKAXE));
            case DRAGONS -> List.of(
                    choice("coward", "Coward", Material.FEATHER,
                            Unlock.FREE, Zombie.class, Material.IRON_AXE,
                            null, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                            Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET),
                    choice("marksman", "Marksman", Material.BOW,
                            Unlock.GEM, Zombie.class, Material.BOW,
                            null, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS,
                            Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET),
                    choice("pyrotechnic", "Pyrotechnic", Material.FIREWORK_ROCKET,
                            Unlock.GEM, Zombie.class, Material.FIREWORK_ROCKET));
            case MICRO_BATTLES -> List.of(
                    choice("micro-archer", "Archer", Material.BOW,
                            Unlock.FREE, Zombie.class, Material.BOW),
                    choice("micro-worker", "Worker", Material.IRON_PICKAXE,
                            Unlock.FREE, Zombie.class, Material.STONE_PICKAXE),
                    choice("micro-fighter", "Fighter", Material.IRON_SWORD,
                            Unlock.FREE, Zombie.class, Material.WOODEN_SWORD));
            case MONSTER_MAZE -> List.of(
                    choice("jumper", "Jumper", Material.FEATHER,
                            Unlock.FREE, Skeleton.class, Material.FEATHER),
                    choice("slowball", "Slowballer", Material.SNOWBALL,
                            Unlock.GEM, Skeleton.class, Material.SNOWBALL),
                    choice("body-builder", "Body Builder", Material.APPLE,
                            Unlock.GEM, Skeleton.class, Material.IRON_CHESTPLATE),
                    choice("repulsor", "Repulsor", Material.COAL,
                            Unlock.ACHIEVEMENT, Skeleton.class, Material.COAL),
                    choice("maverick", "Maverick", Material.ARROW,
                            Unlock.ACHIEVEMENT, Skeleton.class, Material.BOW));
            case ONE_IN_THE_QUIVER -> List.of(
                    choice("quiver-leaper", "Leaper", Material.RABBIT_FOOT,
                            Unlock.FREE, Zombie.class, Material.STONE_SWORD),
                    choice("quiver-brawler", "Brawler", Material.IRON_SWORD,
                            Unlock.GEM, Zombie.class, Material.IRON_SWORD),
                    choice("quiver-enchanter", "Enchanter", Material.ENCHANTED_BOOK,
                            Unlock.GEM, Zombie.class, Material.BOW),
                    choice("quiver-slam-shot", "Slam Shot", Material.ARROW,
                            Unlock.GEM, Zombie.class, Material.DIAMOND_SHOVEL),
                    choice("quiver-ninja", "Ninja", Material.ENDER_PEARL,
                            Unlock.ACHIEVEMENT, Zombie.class, Material.GOLDEN_SWORD));
            case PAINTBALL -> List.of(
                    choice("rifle", "Rifle", Material.IRON_HOE,
                            Unlock.FREE, Zombie.class, Material.IRON_HORSE_ARMOR),
                    choice("sniper", "Sniper", Material.CROSSBOW,
                            Unlock.ACHIEVEMENT, Zombie.class, Material.STONE_HOE),
                    choice("shotgun", "Shotgun", Material.BLAZE_ROD,
                            Unlock.GEM, Zombie.class, Material.IRON_HORSE_ARMOR),
                    choice("machine-gun", "Machine Gun", Material.DISPENSER,
                            Unlock.GEM, Zombie.class, Material.DIAMOND_HORSE_ARMOR));
            case RUNNER -> List.of(
                    choice("runnerkit-leaper", "Leaper", Material.RABBIT_FOOT,
                            Unlock.FREE, Skeleton.class, Material.IRON_AXE),
                    choice("runnerkit-archer", "Archer", Material.BOW,
                            Unlock.GEM, Skeleton.class, Material.BOW),
                    choice("runnerkit-frosty", "Frosty", Material.SNOWBALL,
                            Unlock.GEM, Skeleton.class, Material.SNOWBALL));
            case SNEAKY_ASSASSINS -> List.of(
                    choice("escape-artist", "Escape Artist", Material.INK_SAC,
                            Unlock.FREE, Villager.class, Material.INK_SAC),
                    choice("assassin", "Assassin", Material.BOW,
                            Unlock.GEM, Villager.class, Material.BOW),
                    choice("briber", "Briber", Material.EMERALD,
                            Unlock.ACHIEVEMENT, Villager.class, Material.EMERALD),
                    choice("revealer", "Revealer", Material.DIAMOND,
                            Unlock.GEM, Villager.class, Material.DIAMOND));
            case SPLEEF -> List.of(
                    choice("spleef-snowballer", "Snowballer", Material.SNOWBALL,
                            Unlock.FREE, Skeleton.class, Material.SNOWBALL),
                    choice("spleef-brawler", "Brawler", Material.IRON_SWORD,
                            Unlock.GEM, Skeleton.class, Material.IRON_AXE),
                    choice("spleef-archer", "Archer", Material.BOW,
                            Unlock.FREE, Skeleton.class, Material.BOW));
            case TURF_WARS -> List.of(
                    choice("turf-wars-marksman", "Marksman", Material.BOW,
                            Unlock.FREE, Zombie.class, Material.BOW),
                    choice("turf-wars-infiltrator", "Infiltrator", Material.IRON_SWORD,
                            Unlock.GEM, Zombie.class, Material.IRON_SWORD),
                    choice("turf-wars-shredder", "Shredder", Material.ARROW,
                            Unlock.GEM, Zombie.class, Material.BOW));
            default -> List.of();
        };
        if (hiddenKitIds == null || hiddenKitIds.isEmpty()) return choices;
        Set<String> hidden = new HashSet<>();
        for (String value : hiddenKitIds) {
            if (value != null && !value.isBlank()) hidden.add(value.trim().toLowerCase(Locale.ROOT));
        }
        List<KitChoice> visible = choices.stream().filter(choice -> !hidden.contains(
                game.id().toLowerCase(Locale.ROOT) + ":" + choice.id.toLowerCase(Locale.ROOT))).toList();
        if (configuredOrder == null || configuredOrder.isEmpty()) return visible;

        Map<String, KitChoice> byId = new java.util.LinkedHashMap<>();
        for (KitChoice choice : visible) byId.put(choice.id.toLowerCase(Locale.ROOT), choice);
        List<KitChoice> ordered = new java.util.ArrayList<>();
        for (String value : configuredOrder) {
            if (value == null || value.isBlank()) continue;
            KitChoice choice = byId.remove(value.trim().toLowerCase(Locale.ROOT));
            if (choice != null) ordered.add(choice);
        }
        ordered.addAll(byId.values());
        return List.copyOf(ordered);
    }


    private static KitChoice choice(String id, String name, Material icon,
                                    Unlock unlock, Class<? extends LivingEntity> npcType,
                                    Material heldItem, Material... armour) {
        return new KitChoice(id, name, icon, unlock, npcType, heldItem, null, armour);
    }

    private static KitChoice choice(String id, String name, Material icon,
                                    Unlock unlock, Class<? extends LivingEntity> npcType,
                                    Material heldItem, DyeColor entityColor) {
        return new KitChoice(id, name, icon, unlock, npcType, heldItem, entityColor, new Material[0]);
    }

    enum Unlock {
        FREE(Material.YELLOW_WOOL, ChatColor.YELLOW),
        GEM(Material.LIME_WOOL, ChatColor.GREEN),
        ACHIEVEMENT(Material.MAGENTA_WOOL, ChatColor.LIGHT_PURPLE);

        final Material pedestalColor;
        final ChatColor nameColor;

        Unlock(Material pedestalColor, ChatColor nameColor) {
            this.pedestalColor = pedestalColor;
            this.nameColor = nameColor;
        }
    }

    static final class KitChoice {
        final String id;
        final String name;
        final Material icon;
        final Unlock unlock;
        final Class<? extends LivingEntity> npcType;
        final Material heldItem;
        final DyeColor entityColor;
        final Material[] armour;

        KitChoice(String id, String name, Material icon, Unlock unlock,
                  Class<? extends LivingEntity> npcType, Material heldItem,
                  DyeColor entityColor, Material... armour) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.unlock = unlock;
            this.npcType = npcType;
            this.heldItem = heldItem;
            this.entityColor = entityColor;
            this.armour = armour == null ? new Material[0] : armour.clone();
        }
    }


}
