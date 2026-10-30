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
  feat/sheep-quest-parity-20261030-rebased; do
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
    helpers = extract("    private Player nearestSkyWarsTarget(", "    private int skyWarsMetalArmorPieces(")
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

path.write_text(source)
print("Reconciled SkyWars helper declarations, spawn placement, shared queue, and Sheep Quest holder cleanup.")
PY

python3 - <<'PY'
from pathlib import Path
path = Path(".github/workflows/deploy-testplex-modern-hyperv.yml")
source = path.read_text()
source = source.replace(
    "group: testplex-isolated-hyperv-server",
    "group: testplex-isolated-hyperv-server-v2-20261031"
)

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
path.write_text(source)
print("Hardened deployment concurrency, SSH and external command deadlines.")
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
git status --short
git push origin "HEAD:refs/heads/$INTEGRATION_BRANCH"
echo "Validated integration branch pushed at $(git rev-parse HEAD)"
