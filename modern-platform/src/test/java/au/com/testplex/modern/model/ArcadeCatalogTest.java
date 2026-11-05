package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    private static final Set<String> NO_COSMETIC_LINE = Set.of(
            "death-tag", "draw-my-thing", "heroes-of-gwen", "one-in-the-quiver", "master-builders",
            "milk-the-cow", "sneaky-assassins", "skywars", "speed-builders", "spleef",
            "turf-wars", "wither-assault");

    @Test
    void registersAllThirtyTwoSelectableArcadeIds() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(32, catalog.size());
        assertEquals(32, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void preservesApprovedGameSpecificLinesAndAllowsGamesWithoutOne() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        Map<String, String> expectedLineIds = Map.ofEntries(
                Map.entry("bacon-brawl", "ability-effects"),
                Map.entry("bomb-lobbers", "tnt-explosions"),
                Map.entry("castle-assault", "siege-effects"),
                Map.entry("dragon-escape", "winner-beacon-colour"),
                Map.entry("dragons", "sparkler-effects"),
                Map.entry("evolution", "metamorphosis"),
                Map.entry("gladiators", "score-flares"),
                Map.entry("hoops-remastered", "score-flares"),
                Map.entry("micro-battles", "start-glass-colour"),
                Map.entry("monster-maze", "mob-sets"),
                Map.entry("monster-maze-speed", "mob-sets"),
                Map.entry("monster-maze-modern", "mob-sets"),
                Map.entry("paintball", "pellet-styles"),
                Map.entry("plexball", "ball-styles"),
                Map.entry("runner", "trail-colour"),
                Map.entry("sheep-quest", "capture-effects"),
                Map.entry("snake", "ability-effects"),
                Map.entry("super-sploor", "ability-effects"),
                Map.entry("camera-shy", "camera-flash"),
                Map.entry("wizards", "spell-effects"));

        for (GameDefinition game : catalog.getGames()) {
            assertFalse(game.getWinEffects().isEmpty(), game.getId() + " keeps separate win effects");
            assertTrue(game.getWinEffects().stream().anyMatch(effect -> effect.getId().equals(game.getDefaultWinEffectId())),
                    game.getId() + " keeps a valid win-effect default");
            if (NO_COSMETIC_LINE.contains(game.getId())) {
                assertTrue(game.getCosmeticLines().isEmpty(), game.getId() + " should have no game cosmetic line");
                continue;
            }
            String lineId = expectedLineIds.get(game.getId());
            assertNotNull(lineId, "Unexpected game without a reviewed cosmetic decision: " + game.getId());
            assertEquals(1, game.getCosmeticLines().size(), game.getId());
            CosmeticLineDefinition line = game.getCosmeticLines().get(0);
            assertEquals(lineId, line.getId(), game.getId());
            assertTrue(line.getOptions().size() >= 2, game.getId() + " needs test choices");
            assertTrue(line.getOptions().stream().anyMatch(option -> option.getId().equals(line.getDefaultOptionId())),
                    game.getId() + " default option must be selectable");
            Set<String> optionIds = new HashSet<>();
            for (CosmeticOption option : line.getOptions()) {
                assertTrue(optionIds.add(option.getId()), game.getId() + " has duplicate options");
            }
        }
        assertEquals(expectedLineIds.keySet(), catalog.getGames().stream()
                .filter(game -> !game.getCosmeticLines().isEmpty())
                .map(GameDefinition::getId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void monsterMazeModesShareOneMobSetLine() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        List<String> expected = List.of("map_default", "creeper", "skeleton", "spider", "zombie", "slime",
                "witch", "enderman", "guardian", "cave_spider", "silverfish", "blaze", "magma_cube", "bat",
                "endermite", "pig", "sheep", "cow", "chicken", "squid", "wolf", "mooshroom", "snowman",
                "ocelot", "horse", "rabbit", "villager", "axolotl", "allay", "frog", "goat", "fox", "panda",
                "parrot", "turtle", "bee", "sniffer", "armadillo", "random");
        for (String id : List.of("monster-maze", "monster-maze-speed", "monster-maze-modern")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game);
            assertTrue(game.isReadyForTest(), id);
            assertEquals(1, game.getCosmeticLines().size(), id);
            assertEquals("mob-sets", game.getCosmeticLines().get(0).getId(), id);
            assertEquals(CosmeticLineDefinition.SelectionMode.MULTI,
                    game.getCosmeticLines().get(0).getSelectionMode(), id);
            assertEquals(expected, game.getCosmeticLines().get(0).getOptions().stream()
                    .map(CosmeticOption::getId).toList(), id);
        }
    }

    @Test
    void catalogKeepsApprovedStyleSpecificOptions() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(Set.of("ember-ring", "arcane-burst", "frost-shards", "starburst"),
                optionIds(catalog, "bomb-lobbers"));
        assertEquals(Set.of("royal-gold", "void-siege", "verdant-siege"), optionIds(catalog, "castle-assault"));
        assertEquals(Set.of("wool-burst", "golden-bell", "pasture-bloom"), optionIds(catalog, "sheep-quest"));
        assertEquals(Set.of("vintage-flash", "neon-shutter", "polaroid-frame"), optionIds(catalog, "camera-shy"));
        assertEquals("red", line(catalog, "runner").getDefaultOptionId());
    }

    @Test
    void everyGameRemainsAvailableForControlledTesting() {
        for (GameDefinition game : new ArcadeCatalog().getGames()) {
            assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus(), game.getId());
            assertTrue(game.isReadyForTest(), game.getId());
        }
    }

    private static CosmeticLineDefinition line(ArcadeCatalog catalog, String gameId) {
        return catalog.getGame(gameId).getCosmeticLines().get(0);
    }

    private static Set<String> optionIds(ArcadeCatalog catalog, String gameId) {
        return new HashSet<>(line(catalog, gameId).getOptions().stream().map(CosmeticOption::getId).toList());
    }
}
