# Testplex Modern Arcade — Paper 26.3

This is a separate modern platform inside the Testplex repository. It does **not** compile as part of the legacy Mineplex Reborn Maven reactor, does not use the 1.8 NMS classes, and does not deploy to or replace the existing testplex-reborn.service.

## Requirements

- Minecraft Java Edition **26.3**
- Paper **26.3**
- Java **25**
- Maven 3.9+
- **FakePlayerPlugin 2.1.7** installed separately on the modern Paper server for actual server-side fake-player participants. It targets Paper 26.3; set its `body.damageable` option to `true`. On first use, Testplex configures the dedicated FPP runtime for respawn-on-death, 16-player capacity, disabled restart persistence and disposable (no item-drop) teardown. FPP is a separate proprietary dependency; this repository neither bundles nor redistributes it: https://hangar.papermc.io/Janky-Development/FakePlayerPlugin/versions/2.1.7

## Game-scoped test players

Every Arcade `/arcadeport start <game-id>` or quick-start fills the roster to **16 total players**: the number of real humans is subtracted, and the remainder are created as AFK/frozen server-side `Player` entities through FakePlayerPlugin's public API. They enter the same session setup as humans (team assignment, kit/loadout, player snapshots, scoreboards, damage and ability paths); they are not pigs, sheep, zombies, armour stands or packet-only tab entries.

There is no persistent lobby bot roster. Legacy NPC bots are purged from loaded worlds and from chunks as they load; the old `test-bots` config is cleared. Match players are despawned on normal round completion, explicit stop, plugin shutdown, failed starts, and embedded Monster Maze cleanup. A startup sweep removes reserved `TPXTEST` names left by an interrupted prior process before a new roster can be created.

Game start fails closed when FakePlayerPlugin is missing/disabled, its API is unavailable, `body.damageable` is false, or the full fake-player batch cannot join. Testplex does not fall back to mob NPCs, because those do not exercise player-targeted gameplay code.
The build uses Paper's official Maven repository and a version range for the current 26.3 stable API. The dedicated workflow is `.github/workflows/testplex-modern-build.yml`; it builds, tests, and publishes the plugin artifact. A successful push to `feat/testplex-modern-26.3-platform` then automatically invokes `.github/workflows/deploy-testplex-modern-hyperv.yml` with the artifact from that same run. Pull requests and manually dispatched build-only runs do not auto-deploy. The isolated server uses `/home/monstermaze/servers/testplex-modern`, `testplex-modern.service`, and port `25568`; deployment stops only Testplex's own legacy service when necessary; the separate Monster Maze project and its services are not touched.

## Current implementation

- /gamepanel — registered-game catalogue with honest port statuses.
- /arcadecosmetics — game-specific cosmetic lines and a separate win-effect selector.
- /arcadetest status — administrative port-status summary.
- player-preferences.yml — per-player, per-game, per-line preferences; separate from the legacy profile format.
- All 18 Arcade games are catalogued with optional future concepts for cosmetic lines and win scenes; these are not current parity acceptance criteria.
- `monster-maze-port/` contains the 1.21 Monster Maze gameplay source. It is compiled directly into `ArcadeMechanicsModern.jar` as an in-process game module; it has no Bukkit plugin descriptor, join listener, private lobby, NPC selector or separate server lifecycle.
- `snake-port/` builds `SnakeModern.jar`, a Paper-native mechanics-first port with an isolated arena.
- `arcade-games-port/` builds `ArcadeMechanicsModern.jar`, first-pass game loops and dedicated test arenas for the other 16 games, including Draw My Thing and Master Builders.
- Game Details routes Monster Maze through `/arcadeport play monster-maze`; its participants, queue, stop handling and return-to-lobby flow are owned by Testplex Arcade.
- Deployment migrates saved module data from both old standalone data folders (`plugins/MonsterMazeStandalone/` and `plugins/MonsterMazeModern/`), then inspects plugin descriptors and quarantines any standalone Monster Maze Bukkit JAR (including the source project's actual `MonsterMazeStandalone.jar` name) in the deployment backup before Paper restarts. The independent `joshbet9/MonsterMaze` project and its separate services are not modified.

**Important:** the 16 prototype loops are intentionally rough starting points, not a claim of original Mineplex parity. All 18 games remain in progress until their mechanics have passed in-game acceptance. CI confirms compile/unit-level checks only. Cosmetic renderers, progression milestones, and win scenes are out of scope during this parity pass for every game except Monster Maze, where customisation is a core pillar and remains part of acceptance.

## Isolation from 1.8

Keep the existing feat/testbed-bootstrap branch, legacy deployment workflow, installed maps, and testplex-reborn.service as the 1.8 reference. The modern build listens only to modern-platform/** and its own workflow file, and only creates a build artifact.

Modern server provisioning and deployment use a separate directory, unit (`testplex-modern.service`), Java 25 runtime and rollback path. The VM has limited memory, so deployment may stop Testplex's own 1.8 reference service for the modern run and restore it on failure. It does not inspect, stop, restart or overwrite the separate Monster Maze project's services or data.

## Next porting order

1. Run Monster Maze and Snake on Paper 26.3 and fix runtime/gameplay discrepancies against the original 1.8 and adapted 1.21 source.
2. Compare each of the other 14 rough game loops to the existing Reborn source and resolve mechanics/API/lifecycle issues game by game.
3. Keep cosmetic lines, progression milestones and win scenes parked for all games during the mechanics pass. Exception: Monster Maze customisation remains a core pillar and is tested alongside its mechanics.

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
