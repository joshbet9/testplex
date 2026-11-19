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
    void registersAllThirtySelectableArcadeIds() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(30, catalog.size());
        assertEquals(30, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
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
    void monsterMazeLegacyIdsResolveToTheSingleMobSetLine() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        List<String> expected = List.of(
                "map_default", "allay", "armadillo", "axolotl", "bat", "bee",
                "blaze", "bogged", "breeze", "camel", "camel_husk", "cat",
                "cave_spider", "chicken", "cod", "copper_golem", "cow", "creaking",
                "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "enderman",
                "endermite", "evoker", "fox", "frog", "glow_squid", "goat",
                "guardian", "hoglin", "horse", "husk", "illusioner", "iron_golem",
                "llama", "magma_cube", "mooshroom", "mule", "nautilus", "ocelot",
                "panda", "parched", "parrot", "phantom", "pig", "piglin",
                "piglin_brute", "pillager", "polar_bear", "pufferfish", "rabbit", "ravager",
                "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse",
                "slime", "sniffer", "snowman", "spider", "squid", "stray",
                "strider", "sulfur_cube", "tadpole", "trader_llama", "tropical_fish", "turtle",
                "vex", "villager", "vindicator", "wandering_trader", "warden", "witch",
                "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_nautilus",
                "zombie_pigman", "zombie_villager", "random");
        assertTrue(expected.containsAll(List.of("wither_skeleton", "copper_golem", "sulfur_cube", "warden", "nautilus")));
        assertFalse(expected.contains("ghast"));
        assertFalse(expected.contains("happy_ghast"));
        for (String id : List.of("monster-maze", "monster-maze-speed", "monster-maze-modern")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game);
            assertTrue(game.isReadyForTest(), id);
            assertEquals(1, game.getCosmeticLines().size(), id);
            assertEquals("mob-sets", game.getCosmeticLines().get(0).getId(), id);
            assertEquals(CosmeticLineDefinition.SelectionMode.MULTI,
                    game.getCosmeticLines().get(0).getSelectionMode(), id);
            assertEquals(expected, game.getCosmeticLines().get(0).getOptions().stream()
                    .filter(option -> option.getSelectionGroup() == null)
                    .map(CosmeticOption::getId).toList(), id);
            assertTrue(game.getCosmeticLines().get(0).getOptions().size() > 45,
                    id + " needs the paginated options view");
            assertTrue(game.getCosmeticLines().get(0).getOptions().stream()
                    .anyMatch(option -> "beacon-creeper".equals(option.getSelectionGroup())
                            && option.getId().equals("beacon-creeper-red")), id + " beacon palette");
            assertTrue(game.getCosmeticLines().get(0).getOptions().stream()
                    .anyMatch(option -> "pad-creeper".equals(option.getSelectionGroup())
                            && option.getId().equals("pad-creeper-blue")), id + " Safe Pad palette");
            assertTrue(game.getCosmeticLines().get(0).getOptions().stream()
                    .anyMatch(option -> "beacon-wither_skeleton".equals(option.getSelectionGroup())
                            && option.getId().equals("beacon-wither_skeleton-purple")),
                    id + " tall narrow mob beacon palette");
            assertTrue(game.getCosmeticLines().get(0).getOptions().stream()
                    .anyMatch(option -> "pad-sulfur_cube".equals(option.getSelectionGroup())
                            && option.getId().equals("pad-sulfur_cube-yellow")),
                    id + " new 26.3 mob safe pad palette");
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
