package au.com.testplex.arcadeports;

import org.bukkit.Material;
import java.util.List;

/** One canonical player-facing Arcade directory for Stats and game preferences. */
final class ArcadeGameDirectory {
    record Game(String id, String name, Material icon) { }

    private static final List<Game> GAMES = List.of(
            new Game("bacon-brawl", "Bacon Brawl", Material.PORKCHOP),
            new Game("bomb-lobbers", "Bomb Lobbers", Material.FIRE_CHARGE),
            new Game("castle-assault", "Castle Assault", Material.DIAMOND_CHESTPLATE),
            new Game("death-tag", "Death Tag", Material.SKELETON_SKULL),
            new Game("dragon-escape", "Dragon Escape", Material.DRAGON_EGG),
            new Game("dragons", "Dragons", Material.END_STONE),
            new Game("draw-my-thing", "Draw My Thing", Material.WRITABLE_BOOK),
            new Game("evolution", "Evolution", Material.EMERALD),
            new Game("gladiators", "Gladiators", Material.IRON_SWORD),
            new Game("heroes-of-gwen", "Heroes of Gwen", Material.PRISMARINE_BRICKS),
            new Game("hoops-remastered", "Hoops Remastered", Material.MAGMA_CREAM),
            new Game("master-builders", "Master Builders", Material.OAK_PLANKS),
            new Game("micro-battles", "Micro Battles", Material.LAVA_BUCKET),
            new Game("milk-the-cow", "Milk the Cow", Material.MILK_BUCKET),
            new Game("monster-maze", "Monster Maze", Material.ROTTEN_FLESH),
            new Game("one-in-the-quiver", "One in the Quiver", Material.ARROW),
            new Game("paintball", "Paintball", Material.ENDER_PEARL),
            new Game("plexball", "Plexball", Material.CHAINMAIL_HELMET),
            new Game("runner", "Runner", Material.LEATHER_BOOTS),
            new Game("sheep-quest", "Sheep Quest", Material.YELLOW_WOOL),
            new Game("skywars", "SkyWars", Material.FEATHER),
            new Game("snake", "Snake", Material.WHITE_WOOL),
            new Game("camera-shy", "Snap", Material.SPYGLASS),
            new Game("sneaky-assassins", "Sneaky Assassins", Material.INK_SAC),
            new Game("speed-builders", "Speed Builders", Material.QUARTZ_BLOCK),
            new Game("spleef", "Spleef", Material.IRON_SHOVEL),
            new Game("super-sploor", "Super Sploor", Material.DIAMOND_HORSE_ARMOR),
            new Game("turf-wars", "Turf Wars", Material.RED_TERRACOTTA),
            new Game("wither-assault", "Wither Assault", Material.WITHER_SKELETON_SKULL),
            new Game("wizards", "Wizards", Material.BLAZE_ROD)
    );

    private ArcadeGameDirectory() { }
    static List<Game> games() { return GAMES; }
    static Game find(String id) {
        if (id == null) return null;
        return GAMES.stream().filter(game -> game.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
}
