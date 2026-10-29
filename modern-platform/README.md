# Testplex Modern Arcade — Paper 26.3

This is a separate modern platform inside the Testplex repository. It does **not** compile as part of the legacy Mineplex Reborn Maven reactor, does not use the 1.8 NMS classes, and does not deploy to or replace the existing testplex-reborn.service.

## Requirements

- Minecraft Java Edition **26.3**
- Paper **26.3**
- Java **25**
- Maven 3.9+

The build uses Paper's official Maven repository and a version range for the current 26.3 stable API. The dedicated workflow is .github/workflows/testplex-modern-build.yml; it builds, tests, and publishes the plugin artifact only. It has no automatic server-deployment step.

## Current implementation

- /gamepanel — registered-game catalogue with honest port statuses.
- /arcadecosmetics — game-specific cosmetic lines and a separate win-effect selector.
- /arcadetest status — administrative port-status summary.
- player-preferences.yml — per-player, per-game, per-line preferences; separate from the legacy profile format.
- All 16 Arcade games are catalogued with optional future concepts for cosmetic lines and win scenes; these are not current parity acceptance criteria.
- `monster-maze-port/` builds `MonsterMazeModern.jar`, based on the 1.21 implementation in the separate MonsterMaze repository.
- `snake-port/` builds `SnakeModern.jar`, a Paper-native mechanics-first port with an isolated arena.
- `arcade-games-port/` builds `ArcadeMechanicsModern.jar`, first-pass game loops and dedicated test arenas for the other 14 games.
- Game Details now routes to the correct module's Start / Test command when the associated module is installed.

**Important:** the 14 prototype loops are intentionally rough starting points, not a claim of original Mineplex parity. All 16 games remain in progress until their mechanics have passed in-game acceptance. CI confirms compile/unit-level checks only. Cosmetic renderers, progression milestones, and win scenes are out of scope during this parity pass for every game except Monster Maze, where customisation is a core pillar and remains part of acceptance.

## Isolation from 1.8

Keep the existing feat/testbed-bootstrap branch, legacy deployment workflow, installed maps, and testplex-reborn.service as the 1.8 reference. The modern build listens only to modern-platform/** and its own workflow file, and only creates a build artifact.

Modern server provisioning and deployment should be a separate, explicit step with its own directory, unit (testplex-modern.service), Java 25 runtime and rollback path. The current VM has limited memory, so both server processes must not be assumed safe to run simultaneously; deployment must never stop or overwrite the 1.8 server silently.

## Next porting order

1. Run Monster Maze and Snake on Paper 26.3 and fix runtime/gameplay discrepancies against the original 1.8 and adapted 1.21 source.
2. Compare each of the other 14 rough game loops to the existing Reborn source and resolve mechanics/API/lifecycle issues game by game.
3. Keep cosmetic lines, progression milestones and win scenes parked for all games during the mechanics pass. Exception: Monster Maze customisation remains a core pillar and is tested alongside its mechanics.
