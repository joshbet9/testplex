#!/usr/bin/env bash
set -euo pipefail

BASE_BRANCH="feat/testplex-modern-26.3-platform"
INTEGRATION_BRANCH="feat/testplex-26-game-integration-20261031"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

git fetch origin "refs/heads/$BASE_BRANCH:refs/remotes/origin/$BASE_BRANCH"
git checkout -B "$INTEGRATION_BRANCH" "origin/$BASE_BRANCH"

for branch in \
  feat/skywars-mixed-arcade-26.3-20261030 \
  feat/milk-cow-wither-assault-26.3 \
  feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6 \
  feat/sheep-quest-parity-20261030-rebased \
  feat/dmt-round-scaling-master-builders-20261030-jb1 \
  feat/micro-battles-parity-current-20261030 \
  feat/evolution-parity-20261030; do
  echo "Integrating $branch"
  git fetch origin "refs/heads/$branch:refs/remotes/origin/$branch"
  if ! git merge-base --is-ancestor "origin/$branch" HEAD; then
    git merge --no-edit -X ours "origin/$branch" -m "Merge modern game progress from $branch"
  fi
done

python3 - <<'PY'
from pathlib import Path
import re

enum_path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortGame.java")
enum = enum_path.read_text()
enum_additions = [
    'SKY_WARS("skywars", "SkyWars", Material.END_STONE_BRICKS)',
    'MILK_THE_COW("milk-the-cow", "Milk the Cow", Material.GRASS_BLOCK)',
    'WITHER_ASSAULT("wither-assault", "Wither Assault", Material.STONE_BRICKS)',
]
missing = [entry for entry in enum_additions if entry.split("(", 1)[0] + "(" not in enum]
if missing:
    enum_start = enum.index("public enum ArcadePortGame {")
    field_start = enum.index("private final String id;")
    enum_block = enum[enum_start:field_start]
    enum_end = enum_block.rfind(";")
    prefix = enum_block[:enum_end].rstrip()
    if enum_end < 0 or not prefix.endswith(")"):
        raise SystemExit("Unexpected ArcadePortGame enum block; refusing to add IDs.")
    replacement = prefix[:-1] + "),\n    " + ",\n    ".join(missing) + ";"
    enum = enum[:enum_start] + replacement + enum[enum_start + enum_end + 1:]
    enum_path.write_text(enum)

catalog_path = Path("modern-platform/src/main/java/au/com/testplex/modern/model/ArcadeCatalog.java")
catalog = catalog_path.read_text()
catalog = re.sub(
    r'register\(game\("skywars",\s*"SkyWars"',
    'register(game("skywars", "SkyWars Mixed"',
    catalog
)
insert_before = '        register(game("turf-wars",'
new_entries = [
    ('milk-the-cow', '''        register(game("milk-the-cow", "Milk the Cow", "MILK_BUCKET",
                "Collect milk for your team, protect the herd and use Cow charge and body-slam abilities.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, List.of(),
                win("dairy-champion", "Dairy Champion", "The winning farmer is celebrated above the dairy arena")));'''),
    ('wither-assault', '''        register(game("wither-assault", "Wither Assault", "WITHER_SKELETON_SKULL",
                "Defend your team and control the Wither Assault objective using source-derived movement rules.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, List.of(),
                win("wither-victory", "Wither Victory", "The surviving team stands above the defeated Wither")));''')
]
for game_id, block in new_entries:
    if f'register(game("{game_id}"' not in catalog:
        if insert_before not in catalog:
            raise SystemExit("Could not find the ArcadeCatalog insertion point.")
        catalog = catalog.replace(insert_before, block + "\n\n" + insert_before, 1)

if 'register(game("skywars"' not in catalog:
    skywars = '''        register(game("skywars", "SkyWars Mixed", "TNT",
                "Fast 16-player Mixed Arcade free-for-all: loot your island, throw TNT and survive the collapse.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS,
                List.of(line("sky-combat-signature", "Sky Combat Signature",
                        "A visible identity for throwable TNT and fast island collapse.", "TNT",
                        single("tnt-burst", "TNT Burst", "A bright TNT burst with smoke and ember sparks."),
                        single("skyfall", "Skyfall", "A short wind ring marks the explosion path."),
                        single("island-collapse", "Island Collapse", "A block-shatter cue marks the island crumble."))),
                win("sky-island", "Sky Island", "The winner is raised above a floating island."),
                win("last-survivor", "Last Survivor", "A beacon shines over the last player standing."),
                win("skyfall-crown", "Skyfall Crown", "A floating crown of cloud and TNT frames the winner.")));'''
    if insert_before not in catalog:
        raise SystemExit("Could not find the ArcadeCatalog insertion point for SkyWars.")
    catalog = catalog.replace(insert_before, skywars + "\n\n" + insert_before, 1)
catalog_path.write_text(catalog)




test_path = Path("modern-platform/src/test/java/au/com/testplex/modern/model/ArcadeCatalogTest.java")
test = test_path.read_text()
test = test.replace("registersAllTwentyOneArcadeGames", "registersAllTwentyFourArcadeGames")
test = test.replace("assertEquals(21, catalog.size())", "assertEquals(24, catalog.size())")
test = test.replace(
    "assertEquals(21, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size())",
    "assertEquals(24, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size())"
)
test = test.replace('"heroes-of-gwen").contains(game.getId())',
                    '"heroes-of-gwen", "milk-the-cow", "wither-assault").contains(game.getId())')
test = test.replace("assertEquals(19, inProgress)", "assertEquals(22, inProgress)")
marker = "    @Test\n    void allGameLookupsAreCaseInsensitive()"
addition = '''    @Test
    void newlyIntegratedGamesRemainInProgressUntilAcceptance() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (String id : List.of("skywars", "milk-the-cow", "wither-assault")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game, id);
            assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus(), id);
            assertFalse(game.isReadyForTest(), id);
        }
    }

'''
if "newlyIntegratedGamesRemainInProgressUntilAcceptance" not in test:
    if marker not in test:
        raise SystemExit("Could not find the ArcadeCatalogTest insertion point.")
    test = test.replace(marker, addition + marker, 1)
test_path.write_text(test)

# The mechanics enum contains the 20 existing host-managed games plus the
# three newly integrated SkyWars/Milk/Wither variants. Snake remains separate.
port_test_path = Path("modern-platform/arcade-games-port/src/test/java/au/com/testplex/arcadeports/ArcadePortGameTest.java")
port_test = port_test_path.read_text()
port_test = port_test.replace(
    "exposesTheTwentyMechanicsGamesAndIntegratedMonsterMazeModule",
    "exposesTheTwentyThreeMechanicsGamesAndIntegratedMonsterMazeModule"
)
port_test = port_test.replace("assertEquals(20, ArcadePortGame.values().length)",
                              "assertEquals(23, ArcadePortGame.values().length)")
enum_anchor = '        assertEquals(ArcadePortGame.SNEAKY_ASSASSINS, ArcadePortGame.fromId("sneaky-assassins"));'
enum_additions_test = '''
        assertEquals(ArcadePortGame.SKY_WARS, ArcadePortGame.fromId("skywars"));
        assertEquals(ArcadePortGame.MILK_THE_COW, ArcadePortGame.fromId("milk-the-cow"));
        assertEquals(ArcadePortGame.WITHER_ASSAULT, ArcadePortGame.fromId("wither-assault"));'''
if "ArcadePortGame.SKY_WARS, ArcadePortGame.fromId" not in port_test:
    if enum_anchor not in port_test:
        raise SystemExit("Could not locate game enum assertion anchor.")
    port_test = port_test.replace(enum_anchor, enum_anchor + enum_additions_test, 1)

old_map_name = "originalMapCatalogEmbedsThirteenSourceWorldsAndLeavesEvolutionExplicitlyUnmapped"
port_test = port_test.replace(old_map_name,
                              "originalMapCatalogEmbedsEighteenSourceWorldFoldersAndLeavesUnmappedGamesExplicitlyUnmapped")
map_anchor = "        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.BACON_BRAWL));"
map_additions_test = """        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.CASTLE_ASSAULT));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.HEROES_OF_GWEN));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.DRAW_MY_THING));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.SKY_WARS));
        assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.MILK_THE_COW));
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.WITHER_ASSAULT).isEmpty());
        assertTrue(SourceMapCatalog.mapFoldersFor(ArcadePortGame.MASTER_BUILDERS).isEmpty());"""
if "hasMap(ArcadePortGame.SKY_WARS)" not in port_test:
    if map_anchor not in port_test:
        raise SystemExit("Could not locate source map catalogue assertion anchor.")
    port_test = port_test.replace(map_anchor, map_anchor + "\n" + map_additions_test, 1)
port_test_path.write_text(port_test)

# Restore the union of previously supported legacy map folders while adding
# the two new archive-backed game maps. Keep Evolution, Master Builders and
# Wither Assault unmapped instead of inventing source maps for them.
map_path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/SourceMapCatalog.java")
map_source = map_path.read_text()
map_method = map_source.find("static List<String> mapFoldersFor(ArcadePortGame game)")
map_switch_start = map_source.find("        return switch (game) {", map_method)
map_switch_end = map_source.find("        };", map_switch_start)
if map_method < 0 or map_switch_start < 0 or map_switch_end < 0:
    raise SystemExit("Could not locate SourceMapCatalog.mapFoldersFor switch.")
map_switch_end += len("        };")
map_switch = """        return switch (game) {
            case BACON_BRAWL -> List.of("Bacon Brawl");
            case CASTLE_ASSAULT -> List.of("Castle Assault");
            case BOMB_LOBBERS -> List.of("Bomb Lobbers");
            case DEATH_TAG -> List.of("Death Tag");
            case DRAGON_ESCAPE -> List.of("Dragon Escape");
            case DRAGONS -> List.of("Dragons");
            case EVOLUTION -> List.of();
            case GLADIATORS -> List.of("Gladiators");
            case HEROES_OF_GWEN -> List.of("Heroes of GWEN");
            case MICRO_BATTLES -> List.of("Micro Battle");
            case ONE_IN_THE_QUIVER -> List.of("One in the Quiver");
            case PAINTBALL -> List.of("Paintball", "Super Paintball");
            case RUNNER -> List.of("Runner");
            case SHEEP_QUEST -> List.of("Sheep Quest");
            case SPLEEF -> List.of("Speef", "Super Spleef");
            case TURF_WARS -> List.of("Turf Wars");
            case DRAW_MY_THING -> List.of("Draw My Thing");
            case MASTER_BUILDERS -> List.of();
            case SKY_WARS -> List.of("Skywars");
            case MILK_THE_COW -> List.of("Milk the Cow");
            case WITHER_ASSAULT -> List.of();
            default -> List.of();
        };"""
map_source = map_source[:map_switch_start] + map_switch + map_source[map_switch_end:]
resource_method = map_source.find("static String resourceFor(ArcadePortGame game)")
resource_switch_end = map_source.find("        };", map_source.find("        return switch (game) {", resource_method))
resource_insert = '            case TURF_WARS -> "maps/turf-wars.zip";'
if resource_method < 0 or resource_switch_end < 0:
    raise SystemExit("Could not locate SourceMapCatalog.resourceFor switch.")
resource_switch = map_source[map_source.find("        return switch (game) {", resource_method):resource_switch_end]
if 'case MILK_THE_COW -> "maps/milk-the-cow.zip";' not in resource_switch:
    if resource_insert not in resource_switch:
        raise SystemExit("Could not locate default map resource insertion point.")
    resource_switch = resource_switch.replace(resource_insert,
        resource_insert + '\n            case MILK_THE_COW -> "maps/milk-the-cow.zip";', 1)
    map_source = (map_source[:map_source.find("        return switch (game) {", resource_method)]
                  + resource_switch + map_source[resource_switch_end:])
map_path.write_text(map_source)

config_path = Path("modern-platform/arcade-games-port/src/main/resources/config.yml")
config = config_path.read_text()
config = config.replace("# All 21 catalogue games and their mechanics are packaged in ArcadeMechanicsModern.jar.",
                        "# The 24 shared Arcade catalogue entries are packaged in ArcadeMechanicsModern.jar.")
config = config.replace("# Only games marked READY_FOR_TEST in ArcadeCatalog may be started in the isolated server.",
                        "# Only games formally marked READY_FOR_TEST may be started in the isolated server.")
config = config.replace("# The other 19 ports remain installed but unavailable until their parity/acceptance gate is cleared.",
                        "# The other 22 catalogue entries remain disabled until their parity/acceptance gate is cleared.")
config_path.write_text(config)

build_path = Path(".github/workflows/testplex-modern-build.yml")
build = build_path.read_text()
build = build.replace("21 Arcade catalogue games are packaged", "24 shared Arcade catalogue entries are packaged")
build = build.replace(
    "21 Arcade catalogue entries plus the resource-packless Wizards prototype are packaged;",
    "24 Arcade catalogue entries plus the separate Wizards module are built; Speed Builders Arcade remains planned;"
)
build_path.write_text(build)

roadmap_path = Path("modern-platform/docs/PORTING-ROADMAP.md")
roadmap = roadmap_path.read_text() if roadmap_path.exists() else "# Testplex Modern porting roadmap\n"
section = """
## Complete 26-game programme register (31 October 2026)

The overall programme tracks 26 titles/variants: **24 shared Arcade catalogue games**, the separately packaged **Wizards** module, and the planned **Speed Builders Arcade** variant. Speed Builders currently has no implementation branch or runtime code; it is tracked as planned work rather than represented by a non-functional placeholder.

| # | Game | Current implementation state | Test access |
|---:|---|---|---|
| 1 | Bacon Brawl | Mechanics in progress | Disabled |
| 2 | Bomb Lobbers | Mechanics in progress | Disabled |
| 3 | Death Tag | Acceptance-ready | Enabled |
| 4 | Dragon Escape | Mechanics in progress | Disabled |
| 5 | Dragons (Solo) | Mechanics in progress | Disabled |
| 6 | Evolution | Mechanics in progress | Disabled |
| 7 | Gladiators | Mechanics implemented; runtime acceptance still required | Disabled |
| 8 | Micro Battles | Mechanics in progress | Disabled |
| 9 | Monster Maze | Embedded in Testplex Arcade lifecycle; mechanics in progress | Disabled |
| 10 | One in the Quiver | Acceptance-ready | Enabled |
| 11 | Paintball | Mechanics in progress | Disabled |
| 12 | Runner | Mechanics in progress | Disabled |
| 13 | Sheep Quest | Mechanics in progress | Disabled |
| 14 | Snake | Separate modern module; mechanics in progress | Disabled |
| 15 | Spleef | Mechanics in progress | Disabled |
| 16 | Turf Wars | Mechanics in progress | Disabled |
| 17 | Draw My Thing | Mechanics in progress; large-lobby turn scaling present | Disabled |
| 18 | Master Builders | Mechanics in progress; plot/review work present | Disabled |
| 19 | Castle Assault | Mechanics in progress | Disabled |
| 20 | Heroes of Gwen | Mechanics in progress | Disabled |
| 21 | Sneaky Assassins | Mechanics in progress; no archived source map | Disabled |
| 22 | SkyWars Mixed | Mechanics-first 16-player port integrated | Disabled |
| 23 | Milk the Cow | Mechanics-first port integrated | Disabled |
| 24 | Wither Assault | Mechanics-first port integrated; generated test arena | Disabled |
| 25 | Wizards | Separate resource-packless module; 10 of 21 spell effects implemented | Disabled |
| 26 | Speed Builders Arcade | Planned; implementation not started | Not available |

**Readiness rule:** only Death Tag and One in the Quiver are in the READY_FOR_TEST allow-list and enabled-games config. Catalogue presence and successful compilation do not automatically make a game available. Every other title remains disabled until source parity and isolated multi-client acceptance are completed.
"""
if "Complete 26-game programme register" not in roadmap:
    roadmap_path.write_text(roadmap.rstrip() + "\n\n" + section.strip() + "\n")

modern_readme = Path("modern-platform/README.md")
if modern_readme.exists():
    readme = modern_readme.read_text()
    if "Complete 26-game programme register" not in readme:
        modern_readme.write_text(readme.rstrip() + "\n\n" + section.strip() + "\n")

print("Updated shared Arcade catalogue, readiness tests and 26-game register.")
PY


# Git's -X ours merge deliberately kept the newer platform manager around conflicting
# regions. Re-introduce only the missing SkyWars declarations from the feature branch,
# retaining the rest of the newer manager.
python3 - <<'PY'
from pathlib import Path
import re
import subprocess

path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortManager.java")
source = path.read_text()
# Keep the newer Testplex queue/session manager, but take the complete v6
# source-mechanics runtime for Castle Assault and Heroes of Gwen. The v6
# runtime keeps the existing Hooks contract and adds the requested hero skills.
castle_runtime_path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/CastleAssaultHeroesOfGwenRuntime.java")
castle_runtime = subprocess.check_output([
    "git", "show",
    "origin/feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/CastleAssaultHeroesOfGwenRuntime.java"
], text=True)
castle_runtime_path.write_text(castle_runtime)
sky = subprocess.check_output([
    "git", "show",
    "origin/feat/skywars-mixed-arcade-26.3-20261030:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortManager.java"
], text=True)

def extract(start_marker: str, end_marker: str) -> str:
    start = sky.find(start_marker)
    end = sky.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Could not extract SkyWars source block: {start_marker}")
    return sky[start:end]

if "Map<UUID, String> skyWarsKitPreferences" not in source:
    anchor = "    private final Map<UUID, String> dragonEscapeKitPreferences = new HashMap<>();"
    if anchor not in source:
        raise SystemExit("Could not find kit-preference field anchor.")
    source = source.replace(anchor, anchor + "\n    private final Map<UUID, String> skyWarsKitPreferences = new HashMap<>();", 1)

if "private ItemStack skyWarsKitItem(" not in source:
    helper = extract("    private ItemStack skyWarsKitItem(", "    private void prepareSkyWarsMap(")
    anchor = "    private void updateQuiverKit("
    if anchor not in source:
        raise SystemExit("Could not find stable kit-update insertion point.")
    source = source.replace(anchor, helper + anchor, 1)

if "private void updateSkyWarsCompass(" not in source:
    helpers = extract("    private boolean isSkyWarsTrackingCompass(", "    private int skyWarsMetalArmorPieces(")
    anchor = "    private void updateQuiverKit("
    if anchor not in source:
        raise SystemExit("Could not find stable compass-helper insertion point.")
    source = source.replace(anchor, helpers + anchor, 1)

if "private record SkyWarsCrumbleCandidate(" not in source:
    record = re.search(r"    private record SkyWarsCrumbleCandidate\([^\n]+\n", sky)
    if not record:
        raise SystemExit("Could not find SkyWars crumble candidate record.")
    anchor = "    private static final class Session {"
    if anchor not in source:
        raise SystemExit("Could not find Arcade session declaration.")
    source = source.replace(anchor, record.group(0) + "\n" + anchor, 1)

# The feature branch referenced spawn before declaring it. Move the Air kit safe
# location assignment to immediately after the actual selected spawn is created.
source = source.replace(
    '                if ("air".equals(selectedKit)) session.skyWarsSafeLocations.put(player.getUniqueId(), spawn.clone());\n',
    ""
)
spawn_line = "            Location spawn = spawns.get(i % spawns.size()).clone();"
if spawn_line not in source:
    raise SystemExit("Could not find participant spawn assignment for SkyWars Air spawn fix.")
spawn_replacement = spawn_line + """
            if (type == ArcadePortGame.SKY_WARS
                    && "air".equals(session.skyWarsKits.get(player.getUniqueId()))) {
                session.skyWarsSafeLocations.put(player.getUniqueId(), spawn.clone());
            }"""
source = source.replace(spawn_line, spawn_replacement, 1)


if "private boolean isSkyWarsTrackingCompass(" not in source:
    compass_identity = extract("    private boolean isSkyWarsTrackingCompass(", "    private Player nearestSkyWarsTarget(")
    anchor = "    private void updateQuiverKit("
    if anchor not in source:
        raise SystemExit("Could not find compass identity helper insertion point.")
    source = source.replace(anchor, compass_identity + anchor, 1)

# Reconcile the shared Testplex queue with SkyWars' separate 16-player Mixed queue.
queue_start = source.find("    public void queue(ArcadePortGame type, Player player, boolean quickStart, boolean mixedArcadeVariant) {")
queue_end = source.find("    private void scheduleQuickStart", queue_start)
if queue_start < 0 or queue_end < 0:
    raise SystemExit("Could not find the complete Arcade queue method to reconcile.")
queue_method = """    public void queue(ArcadePortGame type, Player player, boolean quickStart, boolean mixedArcadeVariant) {
        if (monsterMazeRunning()) {
            player.sendMessage(ChatColor.RED + "Monster Maze is already running. Wait for the current Testplex game to finish.");
            return;
        }
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Unknown game id. Use /arcadeport help for the available IDs.");
            return;
        }
        if (mixedArcadeVariant && type != ArcadePortGame.SKY_WARS && !supportsMixedArcadeVariant(type)) {
            player.sendMessage(ChatColor.RED + "That Mixed Arcade variant is not available for this game.");
            return;
        }
        if (type == ArcadePortGame.SKY_WARS && !mixedArcadeVariant) {
            player.sendMessage(ChatColor.RED + "Use /arcadeport join skywars-mixed for the 16-player Mixed Arcade rules.");
            return;
        }
        if (!isGameEnabled(type)) {
            player.sendMessage(ChatColor.RED + type.displayName()
                    + " is not cleared for in-game parity testing yet. Cleared game IDs: " + enabledGamesLabel() + ".");
            return;
        }
        if (active != null && active.type == type) {
            player.sendMessage(ChatColor.RED + "That game is already in progress. Use /arcadeport status or queue the next game.");
            return;
        }
        if (manualStartCountdown != null && manualStartType != type) {
            player.sendMessage(ChatColor.RED + manualStartType.displayName()
                    + " is already counting down. Wait for that round to start before queueing another game.");
            return;
        }

        String queueId = queueKey(type, mixedArcadeVariant);
        LinkedHashMap<UUID, Player> targetQueue = queues.computeIfAbsent(queueId, ignored -> new LinkedHashMap<>());
        int queuedPlayers = (int) targetQueue.values().stream()
                .filter(queued -> queued != null && queued.isOnline()
                        && !queued.getUniqueId().equals(player.getUniqueId())).count();
        if (type == ArcadePortGame.SKY_WARS && mixedArcadeVariant
                && !SkyWarsMixedArcadeRules.hasRoom(queuedPlayers)) {
            player.sendMessage(ChatColor.RED + "SkyWars Mixed Arcade is full (16/16 players).");
            return;
        }

        lobby.ensurePlayerInLobby(player);
        for (LinkedHashMap<UUID, Player> existingQueue : queues.values()) existingQueue.remove(player.getUniqueId());
        if (type != ArcadePortGame.GLADIATORS) gladiatorLobbyVotes.remove(player.getUniqueId());
        queues.computeIfAbsent(queueId, ignored -> new LinkedHashMap<>()).put(player.getUniqueId(), player);
        String variantName = mixedArcadeVariant ? " (Mixed Arcade)" : "";
        String nextAction = manualStartCountdown != null && manualStartType == type
                ? "The existing start countdown is already running."
                : quickStart ? "Starting after a short join window so friends can join too."
                : "An operator/player can use /arcadeport start "
                        + (mixedArcadeVariant ? type.id() + "-mixed" : type.id()) + ".";
        player.sendMessage(ChatColor.GREEN + "Queued for " + type.displayName() + variantName + ". " + nextAction);
        if (quickStart) scheduleQuickStart(type, mixedArcadeVariant);
        if (type == ArcadePortGame.GLADIATORS) openGladiatorLobbyVoteMenu(player);
    }

"""
source = source[:queue_start] + queue_method + source[queue_end:]

# Keep holder ownership alongside the existing sheep->carrier map so ownership is
# always removed when a sheep is dropped, invalidated or scored.
if "final Map<Sheep, UUID> sheepHolders" not in source:
    anchor = "        final Map<Sheep, UUID> sheepCarriers = new LinkedHashMap<>();"
    if anchor not in source:
        raise SystemExit("Could not find Sheep Quest session carrier map.")
    source = source.replace(anchor, anchor + "\n        final Map<Sheep, UUID> sheepHolders = new HashMap<>();", 1)
pickup = "        session.sheepCarriers.put(sheep, playerId);"
if pickup in source and "session.sheepHolders.put(sheep, playerId);" not in source:
    source = source.replace(pickup, pickup + "\n        session.sheepHolders.put(sheep, playerId);", 1)
drop_start = source.find("    private void dropSheepFromCarrier(")
drop_end = source.find("    private void updateSheepQuest(", drop_start)
if drop_start < 0 or drop_end < 0:
    raise SystemExit("Could not find Sheep Quest drop method.")
drop = source[drop_start:drop_end]
drop = drop.replace("            session.sheepCarriers.remove(sheep);\n",
                    "            session.sheepCarriers.remove(sheep);\n            session.sheepHolders.remove(sheep);\n", 1)
source = source[:drop_start] + drop + source[drop_end:]
update_start = source.find("    private void updateSheepQuest(")
update_end = source.find("\n    private ", update_start + 20)
if update_start < 0 or update_end < 0:
    raise SystemExit("Could not find Sheep Quest update method.")
update = source[update_start:update_end]
update = update.replace("                session.sheep.remove(sheep);\n",
                        "                session.sheep.remove(sheep);\n                session.sheepHolders.remove(sheep);\n", 1)
update = update.replace("            int previousOwner = session.sheepOwners.getOrDefault(sheep, -1);\n",
                        "            int previousOwner = session.sheepOwners.getOrDefault(sheep, -1);\n            UUID holderId = session.sheepHolders.remove(sheep);\n", 1)
update = update.replace("            if (previousOwner != team) {\n",
                        "            if (previousOwner != team && (holderId == null || holderId.equals(carrierId))) {\n", 1)
source = source[:update_start] + update + source[update_end:]


# Carry the v6 rule helpers that the upgraded Castle/Heroes runtime requires.
rules_path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortRules.java")
rules = rules_path.read_text()
v6_rules = subprocess.check_output([
    "git", "show",
    "origin/feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortRules.java"
], text=True)
rule_start = v6_rules.find("    public static final long CASTLE_ASSAULT_ARCHER_REFILL_MILLIS")
rule_end = v6_rules.find("    /** Heroes of Gwen's Mixed Arcade store prices", rule_start)
if rule_start < 0 or rule_end < 0:
    raise SystemExit("Could not extract Heroes/Castle rule helpers from v6 source.")
rule_block = v6_rules[rule_start:rule_end]
required_rule_symbols = [
    "CASTLE_ASSAULT_ARCHER_REFILL_MILLIS",
    "HEROES_OF_GWEN_WITHER_EARTHQUAKE_INTERVAL_MILLIS",
    "heroesOfGwenWitherStepPerTick",
    "castleAssaultBlastDamageMultiplier",
    "normalizeCastleAssaultKit",
    "normalizeHeroesOfGwenHero",
    "heroesOfGwenHeroRole",
    "heroesOfGwenHeroDisplayName",
]
missing_rule_symbols = [symbol for symbol in required_rule_symbols if symbol not in rules]
if missing_rule_symbols:
    closing = rules.rfind("}")
    if closing < 0:
        raise SystemExit("ArcadePortRules class closing brace not found.")
    rules = rules[:closing] + "\n" + rule_block + rules[closing:]
    rules_path.write_text(rules)

# The v6 selection methods are missing from the newer manager even though their
# command dispatch calls were merged. Bring over only those methods and state maps.
v6_manager = subprocess.check_output([
    "git", "show",
    "origin/feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortManager.java"
], text=True)
for field in [
    "    private final Map<UUID, String> castleAssaultKitPreferences = new HashMap<>();",
    "    private final Map<UUID, String> heroesOfGwenHeroPreferences = new HashMap<>();",
]:
    if ("Map<UUID, String> " + field.split("Map<UUID, String> ", 1)[1].split(" =", 1)[0]) not in source:
        anchor = "    private final Map<UUID, String> paintballKitPreferences = new HashMap<>();"
        if anchor not in source:
            raise SystemExit("Could not find kit preference field insertion point.")
        source = source.replace(anchor, anchor + "\n" + field, 1)

if "    public void selectCastleAssaultKit(" not in source:
    select_start = v6_manager.find("    public void selectCastleAssaultKit(")
    hero_start = v6_manager.find("    public void selectHeroesOfGwenHero(", select_start)
    methods_end = v6_manager.find("\n    public ", hero_start + 1)
    if select_start < 0 or hero_start < 0 or methods_end < 0:
        raise SystemExit("Could not extract Castle/Heroes kit selection methods.")
    selection_methods = v6_manager[select_start:methods_end]
    anchor = "    public void selectDeathTagRunnerKit("
    if anchor not in source:
        raise SystemExit("Could not find public kit-selection insertion point.")
    source = source.replace(anchor, selection_methods + "\n\n" + anchor, 1)

# Sheep Quest's final-tick/overtime flow uses this method-local guard at two
# sites. Define it once at the top of tick(), after source damage updates.
if "boolean sheepQuestUpdatedBeforeTimeout = false;" not in source:
    tick_anchor = "        updateSourceWaterDamage(session, now);"
    if tick_anchor not in source:
        raise SystemExit("Could not find tick() updateSourceWaterDamage anchor.")
    source = source.replace(tick_anchor,
                            tick_anchor + "\n        boolean sheepQuestUpdatedBeforeTimeout = false;", 1)

# Keep the shared status switch exhaustive as the new port IDs are registered.
status_start = source.find("    private String gameStatusText(Session session, Player player) {")
status_end = source.find("\n    private ", status_start + 20)
if status_start < 0 or status_end < 0:
    raise SystemExit("Could not find the game status switch for the expanded game enum.")
status_method = source[status_start:status_end]
status_cases = [
    ('case SKY_WARS ->', '            case SKY_WARS -> (session.skyWarsLive ? "alive " : "starting ")\n                    + session.alive.size() + " | TNT " + inventoryCount(player, Material.TNT);\n'),
    ('case MILK_THE_COW ->', '            case MILK_THE_COW -> "milk " + scoreFor(session, id) + "/" + MilkCowRules.SCORE_TO_WIN\n                    + (session.teams.getOrDefault(id, 0) == 1 ? " | COW" : " | FARMER");\n'),
    ('case WITHER_ASSAULT ->', '            case WITHER_ASSAULT -> (session.teams.getOrDefault(id, 0) == 1 ? "WITHER" : "HUMAN")\n                    + " | " + Math.max(0, (int) ((session.endsAt - System.currentTimeMillis()) / 1_000L)) + "s";\n'),
]
for case_marker, case_text in status_cases:
    if case_marker not in status_method:
        turf_anchor = "            case TURF_WARS ->"
        if turf_anchor not in status_method:
            raise SystemExit(f"Could not find status switch insertion anchor for {case_marker}.")
        status_method = status_method.replace(turf_anchor, case_text + turf_anchor, 1)
source = source[:status_start] + status_method + source[status_end:]


# Add preference fields and selection methods from the Heroes v6 port without
# replacing the newer Testplex lifecycle/queue manager.
if "private final Map<UUID, String> castleAssaultKitPreferences" not in source:
    anchor = "    private final Map<UUID, String> paintballKitPreferences = new HashMap<>();"
    if anchor not in source:
        raise SystemExit("Could not find kit preference map insertion point.")
    source = source.replace(anchor, anchor + """
    private final Map<UUID, String> castleAssaultKitPreferences = new HashMap<>();
    private final Map<UUID, String> heroesOfGwenHeroPreferences = new HashMap<>();""", 1)

v6_manager = subprocess.check_output([
    "git", "show",
    "origin/feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortManager.java"
], text=True)

def extract_v6(start_marker: str, end_marker: str) -> str:
    start = v6_manager.find(start_marker)
    end = v6_manager.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Could not extract Heroes v6 manager code: {start_marker}")
    return v6_manager[start:end]

if "public void selectCastleAssaultKit(" not in source:
    selections = extract_v6("    public void selectCastleAssaultKit(", "    public void selectBombLobbersKit(")
    selections = selections.replace(
        "Hero abilities are still being ported; your current combat loadout is unchanged.",
        "Your selected hero's role-specific abilities are now equipped."
    )
    anchor = "    public void selectBombLobbersKit("
    if anchor not in source:
        raise SystemExit("Could not find kit-selection method insertion point.")
    source = source.replace(anchor, selections + anchor, 1)

# Route the common Testplex kit selector to Castle Assault and Heroes of Gwen.
switch_anchor = "            switch (running.type) {\n                case BACON_BRAWL"
switch_replacement = """            switch (running.type) {
                case CASTLE_ASSAULT -> { selectCastleAssaultKit(player, value); return; }
                case HEROES_OF_GWEN -> { selectHeroesOfGwenHero(player, value); return; }
                case BACON_BRAWL"""
if "case CASTLE_ASSAULT -> { selectCastleAssaultKit(player, value); return; }" not in source:
    if switch_anchor not in source:
        raise SystemExit("Could not find active-game kit selection switch.")
    source = source.replace(switch_anchor, switch_replacement, 1)

fallback_anchor = "        if (value.startsWith(\"quiver-\")) { selectQuiverKit(player, value); }"
fallback_replacement = """        if (ArcadePortRules.normalizeCastleAssaultKit(value) != null) selectCastleAssaultKit(player, value);
        else if (ArcadePortRules.normalizeHeroesOfGwenHero(value) != null) selectHeroesOfGwenHero(player, value);
        else if (value.startsWith("quiver-")) { selectQuiverKit(player, value); }"""
if "normalizeCastleAssaultKit(value) != null) selectCastleAssaultKit" not in source:
    if fallback_anchor not in source:
        raise SystemExit("Could not find shared kit-selection fallback chain.")
    source = source.replace(fallback_anchor, fallback_replacement, 1)

# Use the source Castle kits / selected Heroes hero when the shared Testplex
# runtime gives a player their match inventory.
old_castle = """            case CASTLE_ASSAULT:
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD),
                        new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 16),
                        new ItemStack(Material.TNT, 2));
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                break;"""
new_castle = """            case CASTLE_ASSAULT: {
                String kit = castleAssaultKitPreferences.getOrDefault(player.getUniqueId(), "fighter");
                if (session.variantRuntime != null && session.variantRuntime.isReady()) {
                    session.variantRuntime.giveCastleAssaultKit(player, kit);
                } else {
                    player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
                    player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                    player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                    player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                }
                break;
            }"""
if "castleAssaultKitPreferences.getOrDefault(player.getUniqueId(), \"fighter\")" not in source:
    if old_castle not in source:
        raise SystemExit("Could not find the existing Castle Assault generic kit.")
    source = source.replace(old_castle, new_castle, 1)

old_heroes = """            case HEROES_OF_GWEN:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD),
                        new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 8),
                        namedItem(Material.GOLD_INGOT, "Gold & Store", "Right-click to open the Heroes of Gwen store."));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                break;"""
new_heroes = """            case HEROES_OF_GWEN:
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD),
                        new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 8),
                        namedItem(Material.GOLD_INGOT, "Gold & Store", "Right-click to open the Heroes of Gwen store."));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                if (session.variantRuntime != null && session.variantRuntime.isReady()) {
                    session.variantRuntime.setHeroesOfGwenHero(player,
                            heroesOfGwenHeroPreferences.getOrDefault(player.getUniqueId(), "hattori"));
                }
                break;"""
if "heroesOfGwenHeroPreferences.getOrDefault(player.getUniqueId(), \"hattori\")" not in source:
    if old_heroes not in source:
        raise SystemExit("Could not find the existing Heroes of Gwen generic kit.")
    source = source.replace(old_heroes, new_heroes, 1)

# Sheep Quest timeout branch has been partially merged from the newer branch.
# Ensure its one-tick guard is declared in the preserved tick() method.
tick_start = source.find("    private void tick(Session session) {")
tick_end = source.find("\n    private void ", tick_start + 25)
if tick_start < 0 or tick_end < 0:
    raise SystemExit("Could not find the Testplex game tick method.")
tick = source[tick_start:tick_end]
if "sheepQuestUpdatedBeforeTimeout" in tick and "boolean sheepQuestUpdatedBeforeTimeout" not in tick:
    marker = "        long now = System.currentTimeMillis();"
    if marker not in tick:
        raise SystemExit("Could not find current-time marker in game tick.")
    tick = tick.replace(marker, marker + "\n        boolean sheepQuestUpdatedBeforeTimeout = false;", 1)
    source = source[:tick_start] + tick + source[tick_end:]

# Preserve required Paper API imports for the Evolution egg displays / knockback hooks.
if "import org.bukkit.entity.BlockDisplay;" not in source:
    anchor = "import org.bukkit.entity.Block;"
    if anchor not in source:
        raise SystemExit("Could not find Bukkit Block entity import anchor.")
    source = source.replace(anchor, anchor + "\nimport org.bukkit.entity.BlockDisplay;", 1)
if "import io.papermc.paper.event.entity.EntityKnockbackEvent;" not in source:
    anchor = "import org.bukkit.event.entity.EntityDamageEvent;"
    if anchor not in source:
        raise SystemExit("Could not find Bukkit entity damage event import anchor.")
    source = source.replace(anchor, "import io.papermc.paper.event.entity.EntityKnockbackEvent;\n" + anchor, 1)

# A later branch conflict discarded this existing session map while retaining its
# update/cleanup methods. Restore the declaration without replacing Session state.
if "final Map<Block, Long> evolutionWebs" not in source:
    session_anchor = "    private static final class Session {"
    if session_anchor not in source:
        raise SystemExit("Could not find Arcade Session class declaration.")
    source = source.replace(session_anchor, session_anchor + """
        final Map<Block, Long> evolutionWebs = new HashMap<>();""", 1)

path.write_text(source)
print("Reconciled SkyWars helper declarations, spawn placement, shared queue, and Sheep Quest holder cleanup.")
PY

python3 - <<'PY'
from pathlib import Path
import re
import subprocess

path = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortRules.java")
source = path.read_text()
v6 = subprocess.check_output([
    "git", "show",
    "origin/feat/castle-assault-gwen-mixed-arcade-variants-20261030-v6:modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortRules.java"
], text=True)

start_marker = "    public static final long CASTLE_ASSAULT_ARCHER_REFILL_MILLIS ="
end_marker = "    /** Heroes of Gwen's Mixed Arcade store prices are half their standard cost. */"
start = v6.find(start_marker)
end = v6.find(end_marker, start + len(start_marker))
if start < 0 or end < 0:
    raise SystemExit("Could not extract the Castle/Heroes source rules block.")
block = v6[start:end]

# The current platform already owns Mixed Arcade price scaling and tower
# selection. Keep those newer definitions and import only missing v6 rules.
known = [
    "CASTLE_ASSAULT_ARCHER_REFILL_MILLIS",
    "CASTLE_ASSAULT_ARCHER_MAX_ARROWS",
    "CASTLE_ASSAULT_DEMOLITIONIST_TNT_REFILL_MILLIS",
    "CASTLE_ASSAULT_DEMOLITIONIST_MAX_TNT",
    "HEROES_OF_GWEN_WITHER_EARTHQUAKE_INTERVAL_MILLIS",
    "HEROES_OF_GWEN_WITHER_TARGET_SPEED",
    "HEROES_OF_GWEN_WITHER_HOME_SPEED",
    "HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS",
    "HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE",
]
missing = [name for name in known if name not in source]
if missing:
    constant_lines = []
    for line in block.splitlines():
        if any(name in line for name in missing):
            constant_lines.append(line)
    for name in missing:
        if not any(name in line for line in constant_lines):
            raise SystemExit(f"Missing source rule constant {name} in v6 branch.")
    insertion = "\n".join(constant_lines) + "\n\n"
    anchor = "    /** Heroes of Gwen's Mixed Arcade store prices are half their standard cost. */"
    if anchor not in source:
        raise SystemExit("Could not find common Mixed Arcade rules insertion point.")
    source = source.replace(anchor, insertion + anchor, 1)

method_names = [
    "heroesOfGwenWitherStepPerTick",
    "heroesOfGwenWitherEarthquakeDamage",
    "castleAssaultBlastDamageMultiplier",
    "castleAssaultBlastKnockbackMultiplier",
    "normalizeCastleAssaultKit",
    "normalizeHeroesOfGwenHero",
    "heroesOfGwenHeroRole",
    "heroesOfGwenHeroDisplayName",
]
for name in method_names:
    if name in source:
        continue
    marker = re.search(r"(?m)^    public static (?:final )?[\w<>?,.\[\] ]+\s+" + re.escape(name) + r"\s*\(", v6)
    if not marker:
        raise SystemExit(f"Could not locate source method {name} in v6 rules.")
    method_start = marker.start()
    brace = v6.find("{", marker.end())
    if brace < 0:
        raise SystemExit(f"Could not locate method body for {name}.")
    depth = 0
    method_end = -1
    for index in range(brace, len(v6)):
        if v6[index] == "{":
            depth += 1
        elif v6[index] == "}":
            depth -= 1
            if depth == 0:
                method_end = index + 1
                break
    if method_end < 0:
        raise SystemExit(f"Unbalanced method body for {name}.")
    snippet = v6[method_start:method_end]
    anchor = "    /** Heroes of Gwen's Mixed Arcade store prices are half their standard cost. */"
    source = source.replace(anchor, snippet + "\n\n" + anchor, 1)

# Evolution/Quiver branch commits overlap the current modern branch. Retain
# the baseline implementation and remove only the duplicate exact signatures.
duplicate_methods = [
    "isQuiverPerfectRun",
    "isQuiverSpeedrunnerWin",
    "shouldAwardQuiverFistPunchKills",
    "quiverBrawlerHealthAfterKill",
    "isQuiverKillstreakAnnouncement",
    "quiverKillstreakTitle",
    "isQuiverReboundImpactInRange",
]

def method_signature_matches(text: str, method_name: str):
    pattern = re.compile(
        r"(?m)^[ \t]*public static [^\n;{]*\b" + re.escape(method_name) + r"\s*\(([^)]*)\)\s*\{"
    )
    return list(pattern.finditer(text))

def method_body_end(text: str, opening_brace: int) -> int:
    depth = 0
    for index in range(opening_brace, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return index + 1
    raise SystemExit("Unbalanced Java method body while removing duplicate Quiver helper.")

for method_name in duplicate_methods:
    while True:
        matches = method_signature_matches(source, method_name)
        seen = {}
        duplicate = None
        for match in matches:
            signature = re.sub(r"\s+", " ", match.group(1).strip())
            if signature in seen:
                duplicate = match
            else:
                seen[signature] = match
        if duplicate is None:
            break
        start = duplicate.start()
        brace = source.find("{", duplicate.end() - 1)
        end = method_body_end(source, brace)
        while end < len(source) and source[end] in " \t":
            end += 1
        if end < len(source) and source[end] == "\n":
            end += 1
        source = source[:start] + source[end:]

path.write_text(source)
print("Integrated Castle Assault and Heroes of Gwen source rule constants/helpers.")
PY

python3 - <<'PY'
from pathlib import Path
path = Path(".github/workflows/deploy-testplex-modern-hyperv.yml")
source = path.read_text()
source = source.replace(
    "group: testplex-isolated-hyperv-server",
    "group: testplex-isolated-hyperv-server-v2-20261031"
)

# Bound the SSH/SCP upload and preflight operations that stalled in the previous run.
source = source.replace('          ssh "${ssh_opts[@]}" "$remote" "rm -rf', '          timeout --foreground --signal=TERM --kill-after=15s 90s ssh "${ssh_opts[@]}" "$remote" "rm -rf', 1)
source = source.replace('          scp -p -P "$SSH_PORT"', '          timeout --foreground --signal=TERM --kill-after=30s 12m scp -p -P "$SSH_PORT"', 1)
source = source.replace('          scp -p -P "$SSH_PORT"', '          timeout --foreground --signal=TERM --kill-after=15s 3m scp -p -P "$SSH_PORT"', 1)
source = source.replace('          ssh "${ssh_opts[@]}" "$remote" "cd', '          timeout --foreground --signal=TERM --kill-after=15s 3m ssh "${ssh_opts[@]}" "$remote" "cd', 1)
source = source.replace('          ssh "${ssh_opts[@]}" "$remote" bash -s <<\'REMOTE_PREFLIGHT\'', '          timeout --foreground --signal=TERM --kill-after=15s 5m ssh "${ssh_opts[@]}" "$remote" bash -s <<\'REMOTE_PREFLIGHT\'', 1)

needle = """          ssh "${ssh_opts[@]}" "$remote" bash -s -- "$BUILD_SHA" "$UPLOAD_DIR" "$ACCEPT_EULA" <<'REMOTE_DEPLOY'"""
replacement = """          echo "[deploy] opening an exclusive remote deployment lock."
          timeout --foreground --signal=TERM --kill-after=30s 28m ssh "${ssh_opts[@]}" "$remote" sudo -n flock -n /var/lock/testplex-modern-deploy.lock bash -s -- "$BUILD_SHA" "$UPLOAD_DIR" "$ACCEPT_EULA" <<'REMOTE_DEPLOY'"""
if needle not in source:
    raise SystemExit("Could not find the remote deployment SSH command.")
source = source.replace(needle, replacement, 1)
source = source.replace(
    "            sudo -n apt-get update",
    "            echo '[deploy] installing missing host prerequisites (8 minute limit).'\n            timeout --foreground --signal=TERM 8m sudo -n apt-get update"
)
source = source.replace(
    "            sudo -n apt-get install -y curl jq unzip",
    "            timeout --foreground --signal=TERM 8m sudo -n apt-get install -y curl jq unzip"
)
old_java = '''            curl --fail --location --retry 3 --user-agent "$USER_AGENT" \
              "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jre/hotspot/normal/eclipse" \
              -o "$UPLOAD_DIR/temurin25-jre.tar.gz"'''
new_java = '''            echo "[deploy] downloading isolated Java 25 JRE (6 minute limit)."
            timeout --foreground --signal=TERM 6m curl --fail --location --retry 3 --user-agent "$USER_AGENT" \
              "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jre/hotspot/normal/eclipse" \
              -o "$UPLOAD_DIR/temurin25-jre.tar.gz"'''
source = source.replace(old_java, new_java)

old_api = '''          builds_json="$(curl --fail --silent --show-error --retry 3 --user-agent "$USER_AGENT" \
            "https://fill.papermc.io/v3/projects/paper/versions/26.3/builds")"'''
new_api = '''          echo "[deploy] querying Paper 26.3 stable build metadata (4 minute limit)."
          builds_json="$(timeout --foreground --signal=TERM 4m curl --fail --silent --show-error --retry 3 --user-agent "$USER_AGENT" \
            "https://fill.papermc.io/v3/projects/paper/versions/26.3/builds")"'''
source = source.replace(old_api, new_api)

old_paper = '''          curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$paper_url" -o "$paper_tmp"'''
new_paper = '''          echo "[deploy] downloading Paper build $paper_build (8 minute limit)."
          timeout --foreground --signal=TERM 8m curl --fail --location --retry 3 --user-agent "$USER_AGENT" "$paper_url" -o "$paper_tmp"'''
source = source.replace(old_paper, new_paper)

source = source.replace(
    "          if ! sudo -n systemctl restart testplex-modern.service; then",
    '          echo "[deploy] restarting testplex-modern.service (90 second limit)."\n          if ! timeout --foreground --signal=TERM 90s sudo -n systemctl restart testplex-modern.service; then'
)
if "testplex-isolated-hyperv-server-v2-20261031" not in source:
    raise SystemExit("Failed to rotate the old Actions concurrency group.")
if "28m ssh" not in source or "flock -n /var/lock/testplex-modern-deploy.lock" not in source:
    raise SystemExit("Failed to add bounded SSH and remote lock.")
if "12m scp" not in source or "3m scp" not in source or "5m ssh" not in source:
    raise SystemExit("Failed to bound upload SCP and preflight SSH stages.")
path.write_text(source)
print("Hardened deployment concurrency, SSH and external command deadlines.")
PY

python3 - <<'PY'
from pathlib import Path
import re

plugin = Path("modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadeMechanicsModernPlugin.java").read_text()
match = re.search(r"READY_FOR_TEST_GAME_IDS\\s*=\\s*(?:java\\.util\\.)?List\\.of\\s*\\(([^)]*)\\)", plugin, re.S)
if not match:
    raise SystemExit("Could not read the runtime READY_FOR_TEST allow-list.")
runtime_enabled = re.findall(r'"([^"]+)"', match.group(1))
expected = ["death-tag", "one-in-the-quiver"]
if runtime_enabled != expected:
    raise SystemExit(f"Unexpected runtime ready allow-list: {runtime_enabled}; expected exactly {expected}")

config_lines = Path("modern-platform/arcade-games-port/src/main/resources/config.yml").read_text().splitlines()
try:
    start = config_lines.index("enabled-games:") + 1
except ValueError:
    raise SystemExit("Missing enabled-games config section.")
config_enabled = []
for line in config_lines[start:]:
    if line.startswith("  - "):
        config_enabled.append(line[4:].strip())
    elif line and not line.startswith(" ") and not line.startswith("#"):
        break
if config_enabled != expected:
    raise SystemExit(f"Unexpected config enabled-games list: {config_enabled}; expected exactly {expected}")
print("Verified the only enabled test games are Death Tag and One in the Quiver.")
PY

grep -q 'READY_FOR_TEST_GAME_IDS' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadeMechanicsModernPlugin.java
grep -q '"death-tag", "one-in-the-quiver"' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadeMechanicsModernPlugin.java
grep -q 'SKY_WARS' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortGame.java
grep -q 'MILK_THE_COW' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortGame.java
grep -q 'WITHER_ASSAULT' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadePortGame.java
grep -q 'class MilkCowRules' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/MilkCowRules.java
grep -q 'class WitherAssaultMovementRules' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/WitherAssaultMovementRules.java
grep -q 'class SkyWarsMixedArcadeRules' modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/SkyWarsMixedArcadeRules.java
grep -q 'register(game("milk-the-cow"' modern-platform/src/main/java/au/com/testplex/modern/model/ArcadeCatalog.java
grep -q 'register(game("wither-assault"' modern-platform/src/main/java/au/com/testplex/modern/model/ArcadeCatalog.java
grep -q 'register(game("skywars"' modern-platform/src/main/java/au/com/testplex/modern/model/ArcadeCatalog.java

mvn --batch-mode --no-transfer-progress -f modern-platform/pom.xml clean verify
mvn --batch-mode --no-transfer-progress -f modern-platform/snake-port/pom.xml clean verify
mvn --batch-mode --no-transfer-progress -f modern-platform/arcade-games-port/pom.xml clean verify
mvn --batch-mode --no-transfer-progress -f modern-platform/wizards-port/pom.xml clean verify

git diff --check
git add -A
if ! git diff --cached --quiet; then
  git commit -m "Integrate reconciled 26-game modern Arcade progress"
fi
git status --short
git fetch origin "refs/heads/$INTEGRATION_BRANCH:refs/remotes/origin/$INTEGRATION_BRANCH" || true
remote_sha="$(git rev-parse refs/remotes/origin/$INTEGRATION_BRANCH 2>/dev/null || true)"
git push --force-with-lease="refs/heads/$INTEGRATION_BRANCH:$remote_sha" origin "HEAD:refs/heads/$INTEGRATION_BRANCH"
echo "Validated integration branch pushed at $(git rev-parse HEAD)"
