# Testplex Modern Arcade — Paper 26.3

This is a separate modern platform inside the Testplex repository. It does **not** compile as part of the legacy Mineplex Reborn Maven reactor, does not use the 1.8 NMS classes, and does not deploy to or replace the existing testplex-reborn.service.

## Requirements

- Minecraft Java Edition **26.3**
- Paper **26.3**
- Java **25**
- Maven 3.9+

The build uses Paper's official Maven repository and a version range for the current 26.3 stable API. The dedicated workflow is .github/workflows/testplex-modern-build.yml; it builds, tests, and publishes the plugin artifact only. It has no automatic server-deployment step.

## Current scaffold

- /gamepanel — registered-game catalogue with honest port statuses.
- /arcadecosmetics — game-specific cosmetic lines and a separate win-effect selector.
- /arcadetest status — administrative port-status summary.
- player-preferences.yml — per-player, per-game, per-line preferences; separate from the legacy profile format.
- All 16 Arcade games are catalogued with distinct concepts for their cosmetic lines and win scenes.
- Monster Maze and Snake are marked as the first mechanics ports. No games are advertised as ready to play yet.

**Important:** menu selections currently prove the catalogue and profile storage only. They do not claim that the visual renderers, win scenes, game mechanics, maps, lobby or bot harness have already been ported. Each effect becomes functional only when its game-specific handler is implemented and tested.

## Isolation from 1.8

Keep the existing feat/testbed-bootstrap branch, legacy deployment workflow, installed maps, and testplex-reborn.service as the 1.8 reference. The modern build listens only to modern-platform/** and its own workflow file, and only creates a build artifact.

Modern server provisioning and deployment should be a separate, explicit step with its own directory, unit (testplex-modern.service), Java 25 runtime and rollback path. The current VM has limited memory, so both server processes must not be assumed safe to run simultaneously; deployment must never stop or overwrite the 1.8 server silently.

## Next porting order

1. Finish and verify Monster Maze and Snake requirements against the 1.8 reference.
2. Implement those two mechanics in the Paper 26.3 game lifecycle and map framework.
3. Test gameplay before finalising their new cosmetic options.
4. Port and test each remaining game's mechanics, then build and validate its unique cosmetic line and win scene game by game.
