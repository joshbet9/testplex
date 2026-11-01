# Speed Builders — Paper 26.3 source-map acceptance plan

## Status

Speed Builders uses the original Mineplex Reborn Shroomland Lake map and its authored build definitions. The Arcade implementation supports **2–16 players** by loading two isolated copies of the original eight-plot arena. It remains a controlled test module and is not automatically enabled in public rotation.

## Arena layout and allocation

- Each arena instance is an authentic copy of `SpeedBuilders_Shroomland_Lake.zip` with its eight authored YELLOW player plots.
- Players 1–8 are assigned to the first map instance; players 9–16 are assigned to the second. The second world is only created when the queue contains more than eight players.
- Each mirrored arena has its own world, spawn markers, plot bounds, fluid/build isolation and original map geometry. Players can see and play alongside the other builders in their own eight-player arena. The first world remains the only active map for matches of eight or fewer.
- Both copies use the same selected template, round number, viewing period and build timer. The manager advances both instances together as one match.
- Player allocation is deterministic by queue order and never indexes beyond the eight plots in either map.

## Gameplay and scoring

- There is **no judging phase, no judge NPC, no central judging gallery, and no spectator transfer**. Players remain in their own plots throughout the match.
- Every player's model is shown directly in their own plot during the viewing phase. At the end of viewing, the model is cleared and the player receives its palette/spawn eggs.
- Each player's build is scored automatically at the end of the build timer, or immediately when all connected players have completed a perfect build.
- Per-build accuracy is calculated as a percentage from correctly matched expected blocks and mobs. Every player remains in the match regardless of their score.
- The match winner is determined by highest cumulative accuracy over the configured rounds.
- Build time is recorded when a player completes a perfect build. Cumulative build time is used only to break a tie between top-scoring players who perfected every build. Players who missed any build are ineligible for the time tie-break; their time is not shown on the final scoreboard.
- The same random template sequence, per-template time modifier and round timer are shared across both arenas.
- Template blocks preserve modern block-state strings and the existing Mineplex compatibility exceptions. Mobs are built with spawn eggs and scored by entity type and relative position.
- The five-round match length is configurable through `speed-builders.match-rounds` (default 5). The existing source-derived viewing/build timers and per-template `time+N`, `time-N`, `time=N`, and `time*X` modifiers remain in force.

## Commands

- `/arcadeport play speed-builders` — quick queue.
- `/arcadeport join speed-builders` — standard queue.
- `/arcadeport leave speed-builders` — leave before match start.
- `/arcadeport start speed-builders` — operator-only start of the queued players.
- `/arcadeport status` — includes Speed Builders queue/phase/round/template status.
- `/arcadeport stop` — operator stop and player-state restoration.
- `/stats speed-builders` — own local Speed Builders lifetime statistics.
- `/stats <player> speed-builders` — view an online player's local statistics.

## Remaining acceptance requirements

1. Run a fresh Paper 26.3 / Java 25 build and the full Arcade test suite.
2. Perform live two-client acceptance in one arena and a second test with at least nine clients to prove that the ninth player is placed into the mirrored arena.
3. Confirm the two map copies present the same model and synchronized viewing/build countdowns.
4. Confirm every player remains in their own arena and plot through viewing, building, intermission and results; verify block placement, demolition, buckets, spawn eggs and fluid isolation in both worlds.
5. Verify per-build percentage scoring is identical across both arena copies for equivalent builds.
6. Verify perfect-build completion timing and the all-round-perfect tie-break across players in both arenas.
7. Verify leave, disconnect/reconnect, operator stop, normal completion and plugin shutdown restore every participant's pre-match snapshot.
8. Verify both source worlds reset cleanly between matches and no build/entity state leaks from one match to the next.

## Enablement boundary

Do not add `speed-builders` to `READY_FOR_TEST_GAME_IDS`, do not set it in `enabled-games`, and do not deploy it to the live VM as part of this implementation pass. Enablement requires a passing build plus the 2–16-player multiplayer/map/lifecycle acceptance above.
