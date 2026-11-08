package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tracks the dedicated owner of every selectable title. This checks ownership coverage,
 * not whether every method has already been migrated out of ArcadePortManager.
 */
class ArcadeGameOwnershipCoverageTest {
    @Test
    void everySelectableGameHasAnExplicitDedicatedOwner() {
        Map<ArcadePortGame, Class<?>> owners = new EnumMap<>(ArcadePortGame.class);
        owners.put(ArcadePortGame.BACON_BRAWL, BaconBrawlManager.class);
        owners.put(ArcadePortGame.BOMB_LOBBERS, BombLobbersManager.class);
        owners.put(ArcadePortGame.CASTLE_ASSAULT, CastleAssaultManager.class);
        owners.put(ArcadePortGame.DEATH_TAG, DeathTagManager.class);
        owners.put(ArcadePortGame.DRAGON_ESCAPE, DragonEscapeManager.class);
        owners.put(ArcadePortGame.DRAGONS, DragonsManager.class);
        owners.put(ArcadePortGame.DRAW_MY_THING, DrawMyThingManager.class);
        owners.put(ArcadePortGame.EVOLUTION, EvolutionManager.class);
        owners.put(ArcadePortGame.GLADIATORS, GladiatorsManager.class);
        owners.put(ArcadePortGame.HEROES_OF_GWEN, HeroesOfGwenManager.class);
        owners.put(ArcadePortGame.HOOPS_REMASTERED, HoopsRemasteredRuntime.class);
        owners.put(ArcadePortGame.MASTER_BUILDERS, MasterBuildersManager.class);
        owners.put(ArcadePortGame.MICRO_BATTLES, MicroBattlesManager.class);
        owners.put(ArcadePortGame.MILK_THE_COW, MilkCowManager.class);
        owners.put(ArcadePortGame.MONSTER_MAZE, me.monstermaze.MonsterMazePlugin.class);
        owners.put(ArcadePortGame.MONSTER_MAZE_SPEED, me.monstermaze.MonsterMazePlugin.class);
        owners.put(ArcadePortGame.MONSTER_MAZE_MODERN, me.monstermaze.MonsterMazePlugin.class);
        owners.put(ArcadePortGame.ONE_IN_THE_QUIVER, OneInTheQuiverManager.class);
        owners.put(ArcadePortGame.PAINTBALL, PaintballManager.class);
        owners.put(ArcadePortGame.PLEXBALL, PlexballGame.class);
        owners.put(ArcadePortGame.RUNNER, RunnerManager.class);
        owners.put(ArcadePortGame.SHEEP_QUEST, SheepQuestManager.class);
        owners.put(ArcadePortGame.SKY_WARS, SkyWarsManager.class);
        owners.put(ArcadePortGame.SNAKE, au.com.testplex.snake.SnakeGameManager.class);
        owners.put(ArcadePortGame.SNEAKY_ASSASSINS, SneakyAssassinsMechanics.class);
        owners.put(ArcadePortGame.SPEED_BUILDERS, SpeedBuildersGame.class);
        owners.put(ArcadePortGame.SPLEEF, SpleefManager.class);
        owners.put(ArcadePortGame.SUPER_SPLOOR, SuperSploorRuntime.class);
        owners.put(ArcadePortGame.TURF_WARS, TurfWarsManager.class);
        owners.put(ArcadePortGame.WITHER_ASSAULT, WitherAssaultManager.class);
        owners.put(ArcadePortGame.WIZARDS, au.com.testplex.wizards.WizardsGameManager.class);
        owners.put(ArcadePortGame.CAMERA_SHY, CameraShyGame.class);

        assertEquals(EnumSet.allOf(ArcadePortGame.class), owners.keySet(),
                "Every Arcade selectable ID must identify an owning game class");
        assertEquals(30, new HashSet<>(owners.values()).size(),
                "Monster Maze's three selectors intentionally share one owner");
        for (Class<?> owner : owners.values()) assertNotNull(owner);
    }
}
