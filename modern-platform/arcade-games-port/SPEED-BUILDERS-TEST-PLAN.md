# Speed Builders — Paper 26.3 test plan

## Status

This is an experimental, in-process ArcadeMechanicsModern module. It is not on the normal playable-game allow-list and must not be deployed as a ready game until mechanics, templates, statistics and multiplayer acceptance are complete.

## Current prototype

- 2–16 players, queued through the shared Testplex Arcade host.
- Generated isolated arena with a 4×4 plot grid and a 7×7×7 build volume per plot.
- Ten generated starter models: Tiny House, Oak Tree, Rocket, Flower, Pyramid, Mushroom, Snowman, Stone Bridge, Golden Crown and Fountain.
- Ten-second model-viewing phase, forty-second first build phase, then one second less per round down to five seconds.
- Players receive the model's block palette after the view phase.
- Material-and-coordinate scoring for expected template cells.
- Empty builds are eliminated; otherwise one lowest scorer is eliminated unless every player ties or every build is perfect.
- A player who completes every expected block early is locked for the remainder of the build phase.
- Per-player action bar and sidebar show phase, model, round, remaining time, score and builders remaining.
- Disconnect handling removes the player from the active match. Operator stop returns everyone to the shared Arcade lobby.

The deterministic rules are in SpeedBuildersRules and are covered by SpeedBuildersRulesTest.

## Commands

- /arcadeport play speed-builders — quick queue.
- /arcadeport join speed-builders — standard queue.
- /arcadeport leave speed-builders — leave before the match starts.
- /arcadeport start speed-builders — operator-only start of the queued players.
- /arcadeport status — includes the Speed Builders phase/round/queue status.
- /arcadeport stop — operator reset.

A match requires at least two online queued players and supports at most sixteen. No separate plugin, lobby, NPC lifecycle or playable-game enablement is introduced.

## Known parity gaps

This is a mechanics-first vertical slice, not a claim of full Reborn parity:

1. The current models are a small generated starter pack, not the original Speed Builders map's full set of BuildData templates.
2. Scoring currently compares block material and relative coordinates. Legacy handling for block data/orientation edge cases (stairs, doors, beds, end-portal frames, leaves and similar blocks) is not ported yet.
3. Mob placement/sign templates, spawn-egg demolition, queued demolition animation and corresponding scoring are not implemented.
4. Original map selection, judge NPC presentation, central review display and original stat-tracker/achievement award paths remain outstanding.
5. The current build has a single shared default builder kit and does not add a separate kit-selection menu.
6. The generated world and arena reset path still require live restart/reconnect/cleanup acceptance.

## Acceptance checklist

- [ ] Two players can queue and start; a one-player queue is rejected safely.
- [ ] Queue rejects a seventeenth player and handles disconnects before start.
- [ ] All players see the same model for the full viewing phase.
- [ ] The model is cleared and the player receives the correct block palette when building begins.
- [ ] Blocks cannot be placed or broken outside the player's own build volume.
- [ ] Exact matches score correctly; wrong block types and wrong coordinates do not score.
- [ ] A perfect build locks immediately and ends the build phase when every remaining player is perfect.
- [ ] Timeout enters judging; empty builds are eliminated.
- [ ] A unique lowest score is eliminated; a full tie or all-perfect round does not eliminate a player.
- [ ] Eliminated players remain spectators and cannot alter builds.
- [ ] Disconnect, operator stop, plugin disable and a subsequent new match do not leave stale players, inventories or build blocks.
- [ ] Test with two to sixteen real clients, including latency variance and simultaneous placements.
- [ ] Add source-map templates and full block-state/mob parity before readiness promotion.

## Deployment policy

Do not add speed-builders to READY_FOR_TEST_GAME_IDS or enable it in the live lobby rotation as part of this prototype. The normal Modern 26.3 allow-list remains unchanged.
