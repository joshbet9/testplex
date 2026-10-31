package au.com.testplex.arcadeports;

import org.bukkit.Material;
import java.util.Locale;

public enum ArcadePortGame {
    BACON_BRAWL("bacon-brawl", "Bacon Brawl", Material.IRON_BLOCK, 0),
    CASTLE_ASSAULT("castle-assault", "Castle Assault", Material.COBBLESTONE, 2),
    BOMB_LOBBERS("bomb-lobbers", "Bomb Lobbers", Material.SMOOTH_STONE, 2),
    DEATH_TAG("death-tag", "Death Tag", Material.MOSS_BLOCK, 0),
    DRAGON_ESCAPE("dragon-escape", "Dragon Escape", Material.STONE_BRICKS, 0),
    DRAGONS("dragons", "Dragons", Material.STONE_BRICKS, 0),
    EVOLUTION("evolution", "Evolution", Material.GRASS_BLOCK, 0),
    GLADIATORS("gladiators", "Gladiators", Material.RED_SANDSTONE, 0),
    HEROES_OF_GWEN("heroes-of-gwen", "Heroes of Gwen", Material.PRISMARINE_BRICKS, 2),
    MICRO_BATTLES("micro-battles", "Micro Battles", Material.LIGHT_BLUE_STAINED_GLASS, 4),
    ONE_IN_THE_QUIVER("one-in-the-quiver", "One in the Quiver", Material.STONE_BRICKS, 0),
    PAINTBALL("paintball", "Paintball", Material.LIME_TERRACOTTA, 2),
    RUNNER("runner", "Runner", Material.WHITE_CONCRETE, 0),
    SHEEP_QUEST("sheep-quest", "Sheep Quest", Material.GRASS_BLOCK, 4),
    SPLEEF("spleef", "Spleef", Material.SNOW_BLOCK, 0),
    TURF_WARS("turf-wars", "Turf Wars", Material.GREEN_WOOL, 2),
    DRAW_MY_THING("draw-my-thing", "Draw My Thing", Material.WHITE_WOOL, 0),
    MASTER_BUILDERS("master-builders", "Master Builders", Material.GRASS_BLOCK, 0),
    MONSTER_MAZE("monster-maze", "Monster Maze", Material.ROTTEN_FLESH, 0),
    SNEAKY_ASSASSINS("sneaky-assassins", "Sneaky Assassins", Material.EMERALD_BLOCK, 0),
    SKY_WARS("skywars", "SkyWars", Material.END_STONE_BRICKS, 0),
    MILK_THE_COW("milk-the-cow", "Milk the Cow", Material.GRASS_BLOCK, 0),
    WITHER_ASSAULT("wither-assault", "Wither Assault", Material.STONE_BRICKS, 0);

    private final String id;
    private final String displayName;
    private final Material floor;
    private final int lobbyTeamCount;
    ArcadePortGame(String id, String displayName, Material floor, int lobbyTeamCount) {
        this.id = id;
        this.displayName = displayName;
        this.floor = floor;
        this.lobbyTeamCount = lobbyTeamCount;
    }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public Material floor() { return floor; }
    /** Number of player-selectable teams exposed at the shared lobby queue station. */
    public int lobbyTeamCount() { return lobbyTeamCount; }
    public static ArcadePortGame fromId(String id) {
        if (id == null) return null;
        String value = id.toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        for (ArcadePortGame game : values()) if (game.id.equals(value) || game.name().toLowerCase(Locale.ROOT).replace('_','-').equals(value)) return game;
        return null;
    }
}
