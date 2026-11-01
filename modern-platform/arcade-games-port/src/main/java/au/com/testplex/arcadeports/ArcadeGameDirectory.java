package au.com.testplex.arcadeports;

import org.bukkit.Material;
import java.util.List;

/** One canonical player-facing Arcade directory for Stats and game preferences. */
final class ArcadeGameDirectory {
    record Game(String id, String name, Material icon) { }

    private static final List<Game> GAMES = List.of(
            new Game("bacon-brawl", "Bacon Brawl", Material.PORKCHOP),
            new Game("bomb-lobbers", "Bomb Lobbers", Material.TNT),
            new Game("castle-assault", "Castle Assault", Material.COBBLESTONE),
            new Game("death-tag", "Death Tag", Material.SKELETON_SKULL),
            new Game("dragon-escape", "Dragon Escape", Material.DRAGON_HEAD),
            new Game("dragons", "Dragons", Material.END_STONE),
            new Game("draw-my-thing", "Draw My Thing", Material.PAINTING),
            new Game("evolution", "Evolution", Material.DRAGON_EGG),
            new Game("gladiators", "Gladiators", Material.IRON_SWORD),
            new Game("heroes-of-gwen", "Heroes of Gwen", Material.PRISMARINE_BRICKS),
            new Game("hoops-remastered", "Hoops Remastered", Material.BIRCH_PLANKS),
            new Game("master-builders", "Master Builders", Material.GRASS_BLOCK),
            new Game("micro-battles", "Micro Battles", Material.LIGHT_BLUE_STAINED_GLASS),
            new Game("milk-the-cow", "Milk the Cow", Material.MILK_BUCKET),
            new Game("monster-maze", "Monster Maze", Material.ROTTEN_FLESH),
            new Game("one-in-the-quiver", "One in the Quiver", Material.BOW),
            new Game("paintball", "Paintball", Material.SNOWBALL),
            new Game("plexball", "Plexball", Material.SNOWBALL),
            new Game("runner", "Runner", Material.LEATHER_BOOTS),
            new Game("sheep-quest", "Sheep Quest", Material.WHITE_WOOL),
            new Game("skywars", "SkyWars", Material.TNT),
            new Game("snake", "Snake", Material.SLIME_BALL),
            new Game("camera-shy", "Snap", Material.SPYGLASS),
            new Game("sneaky-assassins", "Sneaky Assassins", Material.VILLAGER_SPAWN_EGG),
            new Game("speed-builders", "Speed Builders", Material.CRAFTING_TABLE),
            new Game("spleef", "Spleef", Material.IRON_SHOVEL),
            new Game("super-sploor", "Super Sploor", Material.INK_SAC),
            new Game("turf-wars", "Turf Wars", Material.BOW),
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
