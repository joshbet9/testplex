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
- `monster-maze-port/` builds `MonsterMazeModern.jar`, an initial Paper 26.3 port based on the 1.21 implementation in the separate MonsterMaze repository.
- The Monster Maze entry has a Start / Test action in Game Details when the engine plugin is installed; `/mm start [1|2|3]` is available for direct testing.
- Snake is the next mechanics port. No game is advertised as fully ready until live-server acceptance checks pass.

**Important:** the Monster Maze source has been brought into a modern module, but its gameplay on Paper 26.3 is not yet validated in-game. Cosmetic preferences persist, but new visual renderers and win scenes still need to be connected and tested.

## Isolation from 1.8

Keep the existing feat/testbed-bootstrap branch, legacy deployment workflow, installed maps, and testplex-reborn.service as the 1.8 reference. The modern build listens only to modern-platform/** and its own workflow file, and only creates a build artifact.

Modern server provisioning and deployment should be a separate, explicit step with its own directory, unit (testplex-modern.service), Java 25 runtime and rollback path. The current VM has limited memory, so both server processes must not be assumed safe to run simultaneously; deployment must never stop or overwrite the 1.8 server silently.

## Next porting order

1. Compile and test the Monster Maze port on Paper 26.3 using the 1.8 server as the reference and its 1.21 source as the compatibility guide.
2. Port Snake mechanics next; then complete gameplay acceptance for both games.
3. Only then connect and test their new cosmetic lines and separate win effects.
4. Port and test the remaining games individually, fixing incompatibilities first and adding new features/cosmetics afterwards.
