package au.com.testplex.arcadeports;

import org.bukkit.Material;
import java.util.Locale;

public enum ArcadePortGame {
    BACON_BRAWL("bacon-brawl", "Bacon Brawl", Material.IRON_BLOCK),
    CASTLE_ASSAULT("castle-assault", "Castle Assault", Material.COBBLESTONE),
    BOMB_LOBBERS("bomb-lobbers", "Bomb Lobbers", Material.SMOOTH_STONE),
    DEATH_TAG("death-tag", "Death Tag", Material.MOSS_BLOCK),
    DRAGON_ESCAPE("dragon-escape", "Dragon Escape", Material.STONE_BRICKS),
    DRAGONS("dragons", "Dragons", Material.STONE_BRICKS),
    EVOLUTION("evolution", "Evolution", Material.GRASS_BLOCK),
    GLADIATORS("gladiators", "Gladiators", Material.RED_SANDSTONE),
    HEROES_OF_GWEN("heroes-of-gwen", "Heroes of Gwen", Material.PRISMARINE_BRICKS),
    MICRO_BATTLES("micro-battles", "Micro Battles", Material.LIGHT_BLUE_STAINED_GLASS),
    ONE_IN_THE_QUIVER("one-in-the-quiver", "One in the Quiver", Material.STONE_BRICKS),
    PAINTBALL("paintball", "Paintball", Material.LIME_TERRACOTTA),
    SUPER_SPLOOR("super-sploor", "Super Sploor", Material.RED_WOOL),
    PLEXBALL("plexball", "Plexball", Material.WHITE_CONCRETE),
    RUNNER("runner", "Runner", Material.WHITE_CONCRETE),
    SHEEP_QUEST("sheep-quest", "Sheep Quest", Material.GRASS_BLOCK),
    SPLEEF("spleef", "Spleef", Material.SNOW_BLOCK),
    TURF_WARS("turf-wars", "Turf Wars", Material.GREEN_WOOL),
    DRAW_MY_THING("draw-my-thing", "Draw My Thing", Material.WHITE_WOOL),
    MASTER_BUILDERS("master-builders", "Master Builders", Material.GRASS_BLOCK),
    MONSTER_MAZE("monster-maze", "Monster Maze", Material.ROTTEN_FLESH),
    SNEAKY_ASSASSINS("sneaky-assassins", "Sneaky Assassins", Material.EMERALD_BLOCK),
    SKY_WARS("skywars", "SkyWars", Material.END_STONE_BRICKS),
    MILK_THE_COW("milk-the-cow", "Milk the Cow", Material.GRASS_BLOCK),
    WITHER_ASSAULT("wither-assault", "Wither Assault", Material.STONE_BRICKS),
    CAMERA_SHY("camera-shy", "Camera Shy", Material.SPYGLASS);

    private final String id;
    private final String displayName;
    private final Material floor;
    ArcadePortGame(String id, String displayName, Material floor) {
        this.id = id;
        this.displayName = displayName;
        this.floor = floor;
    }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public Material floor() { return floor; }
    public static ArcadePortGame fromId(String id) {
        if (id == null) return null;
        String value = id.toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        for (ArcadePortGame game : values()) if (game.id.equals(value) || game.name().toLowerCase(Locale.ROOT).replace('_','-').equals(value)) return game;
        return null;
    }
}
