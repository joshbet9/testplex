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
- All 16 Arcade games are catalogued with distinct concepts for their cosmetic lines and win scenes.
- `monster-maze-port/` builds `MonsterMazeModern.jar`, based on the 1.21 implementation in the separate MonsterMaze repository.
- `snake-port/` builds `SnakeModern.jar`, a Paper-native mechanics-first port with an isolated arena.
- Both have Start / Test actions in Game Details when their plugins are installed.

**Important:** both game modules are initial passes. CI confirms compilation and unit-level checks only; runtime/gameplay, maps and lifecycle still require validation in-game. The new cosmetic options and win scenes are not claimed complete.

## Isolation from 1.8

Keep the existing feat/testbed-bootstrap branch, legacy deployment workflow, installed maps, and testplex-reborn.service as the 1.8 reference. The modern build listens only to modern-platform/** and its own workflow file, and only creates a build artifact.

Modern server provisioning and deployment should be a separate, explicit step with its own directory, unit (testplex-modern.service), Java 25 runtime and rollback path. The current VM has limited memory, so both server processes must not be assumed safe to run simultaneously; deployment must never stop or overwrite the 1.8 server silently.

## Next porting order

1. Run the Monster Maze and Snake modules on Paper 26.3 and close runtime/gameplay issues using the original 1.8 and adapted 1.21 sources as references.
2. Only after both mechanics are accepted, connect and test their new cosmetic lines and separate win effects.
3. Port the remaining 14 Arcade games individually, fixing compatibility/lifecycle first and then iterating on new features, cosmetics and win scenes.
