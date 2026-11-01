# Speed Builders — Paper 26.3 source-parity test plan

## Status

Speed Builders now loads the original Mineplex Reborn Shroomland Lake map archive and its authored build definitions. It is an isolated in-process module hosted by `ArcadeMechanicsModern`; it is **not** in `READY_FOR_TEST_GAME_IDS`, not part of the normal lobby rotation, and not enabled for public play.

This is an implementation milestone, not a readiness or live-acceptance claim. The branch has not been merged or deployed.

## Source assets and layout

- Uses `update/maps/Speed Builders/SpeedBuilders_Shroomland_Lake.zip`, packaged from the shared source-map resource tree by Maven.
- Imports the map into a reserved, isolated Paper world and reads its embedded `WorldConfig.dat`.
- Uses the source `YELLOW` data markers for up to eight player plots and the `RED` marker for the central judging display.
- Uses the archived `TEAM_SPAWNS` as the source spawn locations, choosing the closest source spawn to each assigned plot.
- Enumerates the original custom-location groups as the template library; there is no generated demo-template fallback. A missing archive, invalid marker layout or empty template set fails the match setup safely.
- Retains each template's original 7×7 ground layer and 7×7×7 block volume, modern block-state strings, mob markers, display name and `time+N`, `time-N`, `time=N`, `time*X` modifiers.
- Resets only the reserved Speed Builders source world before the next match, so player construction is not allowed to contaminate the next import.

## Implemented mechanics

- Queues 2–8 online builders. Quick start and standard queue have separate join windows; an undersized queue is retained rather than starting a one-player match. Once started, players are never removed for a low or empty score.
- Ten-second model viewing, then the source-derived build timer starting at 40 seconds. The base timer decreases by one second after each subsequent round's duration is calculated, with source template modifiers clamped to 5–60 seconds.
- The model's block palette and spawn eggs are issued at build start. Multiblock palette entries are deduplicated; water/lava templates receive buckets.
- Players can build anywhere inside their own 7×7×7 volume. They are not restricted to only the materials present in the model. Door/bed companion blocks must also remain inside the plot.
- Source-style instant demolition returns the dismantled block to the player's inventory, including multiblock handling for doors, beds and double-height blocks. Mob demolition returns a matching spawn egg.
- Template mobs are frozen and silent while displayed. Spawn-egg mobs are frozen in place, can be hit for removal, and score only when the entity type and relative block position match.
- Scores count each expected non-air block and each expected mob once; extra blocks do not deduct points. Block-data comparison preserves orientation/state and includes the original end-portal-frame, sapling, leaves, anvil, redstone-ore and stair-corner exceptions.
- A perfect build locks further placement/interactions and announces its completion. If every remaining player completes a perfect build early, the build phase ends.
- Ten-second judging phase moves all builders to temporary spectator viewpoints around the central model and calculates a normalized accuracy score out of 100 for every connected builder. All builders return to their own plots in the next round regardless of score, including an empty plot. The five-round match length is configurable through `speed-builders.match-rounds` (default 5); highest cumulative points wins, with tied top scores sharing the win.
- The original template is shown in the central judging plot during review. Gwen is a non-attacking Elder Guardian who slowly looks between active build plots. An isolated sidebar/action bar shows round, phase, model, time, per-round accuracy and cumulative points.
- Source-style local progress counters cover wins, perfect builds, first perfect build, builds completed under ten seconds, perfectionist wins, blocks placed/broken and mobs built.
- A durable pre-match player snapshot is recorded before the participant is modified. It is restored on normal finish, operator stop, disconnect/rejoin and plugin shutdown. A pending snapshot remains saved when a player is offline.

## Commands

- `/arcadeport play speed-builders` — quick queue.
- `/arcadeport join speed-builders` — standard queue.
- `/arcadeport leave speed-builders` — leave before match start.
- `/arcadeport start speed-builders` — operator-only start of the queued players.
- `/arcadeport status` — includes Speed Builders queue/phase/round/template status.
- `/arcadeport stop` — operator stop and player-state restoration.
- `/stats speed-builders` — own local Speed Builders lifetime statistics.
- `/stats <player> speed-builders` — view an online player's local statistics.

## Remaining gaps before readiness

1. The Paper 26.3 module has not yet been compiled and exercised on the live Testplex Reborn VM after this source-map conversion.
2. Gwen currently uses a native Elder Guardian with simple plot-to-plot gaze tracking. It is not a packet-level Mineplex disguise, and the gaze/rotation should be validated in-game on multiple clients. There is deliberately no Guardian attack, elimination beam, player elimination or plot explosion in the Arcade variant.
3. The original demolition presentation/tick animation is not reproduced; the configured Reborn mode is instant-break, but the precise legacy click/refund timing still needs live confirmation.
4. The full set of modern block-state mappings needs archive-driven audit. The listed legacy exceptions have unit tests, but unusual tile entities, fluids and non-cube blocks need in-game verification.
5. Source score edge cases, empty-plot scoring, cumulative totals, tied final scores, multiblock refund behaviour, mob spawn-egg placement, water/lava templates and jump flight need live two-client acceptance across the complete configured match.
6. The snapshot restores inventory, armour, offhand, effects, movement/flight flags, health, food, experience and location. Scoreboard restoration across a server restart and unusual custom item metadata still need explicit acceptance.
7. The implementation currently uses the bundled Shroomland Lake archive. Map selection across any additional Speed Builders source archives is not exposed.
8. Lifetime statistics are local Testplex config values and do not write to production Arcade databases.

## Automated coverage

- `SpeedBuildersRulesTest`: source timing modifiers, block-state scoring exceptions, extra-block scoring, perfect builds and normalized 0–100 non-elimination scoring.
- `SpeedBuildersSourceMapTest`: the Maven-packaged original ZIP contains WorldConfig data, a RED judging marker, at least eight YELLOW plots and an authored custom-template library.
- `SpeedBuildersProgressStoreTest`: allowed local counter names, positive increments and persistence paths.
- The full Modern Paper 26.3 workflow must compile the plugin, run the complete Arcade test suite, and verify the packaged source ZIP before this branch is considered build-ready.

## Enablement boundary

Do not add `speed-builders` to `READY_FOR_TEST_GAME_IDS`, do not set it in `enabled-games`, and do not deploy it to the live VM as part of this implementation pass. Enablement requires a passing build plus the multiplayer/map/lifecycle acceptance above.
