# Monster Maze — modern Paper 26.3 port

This dedicated gameplay module adapts the **1.21/MonsterMazeStandalone** implementation from [joshbet9/MonsterMaze](https://github.com/joshbet9/MonsterMaze) to the independent Testplex Paper 26.3 platform. It uses the modern Bukkit/Paper implementation instead of copying the 1.8 `v1_8_R3` NMS entity classes.

## Gameplay port in this first pass
- Original, Modern and Classic modes, three authored maze patterns, timed stages and Safe Pad progression.
- Jumper, Slowball, Body Builder, Repulsor, and mode-gated Maverick kit behavior from the 1.21 source.
- Monster routing, bumps, launch/freeze interactions, stage progression, spectator/elimination paths and cleanup.
- Dedicated arena/lobby handling, maze generation and admin control.
- Optional ProtocolLib disguise path retained from the 1.21 source. Without ProtocolLib, the existing source fallback remains available.

## Commands
Use `/gamepanel`, choose Monster Maze, then select **Start / Test Monster Maze** (available once `MonsterMazeModern.jar` is installed). Direct commands: `/mm start [1|2|3]`, `/mm status`, `/mm stop`, `/mm kit <jumper|slowball|body builder|repulsor>`, `/mm gui`.

## Test isolation and maps
Default config uses `test-mode: true` and `record-runs: false`. Test mode suppresses backend API writes and status heartbeats even if production environment variables exist. Leave test mode enabled on the test platform.

Historical map worlds are not copied as part of this source port. The source falls back to `mm_void` when no active map world is present; complete map parity requires importing/installing the proper world assets into the modern server.

**Status:** mechanics port in progress. Compiling against Paper 26.3 catches source API incompatibilities, but runtime and in-game behavior still need test-server verification before marking it ready.

Reference source commit: `231fb1830ca53a3cdd7451391ac3d384a03ec5bb`.
See [compatibility notes](docs/COMPATIBILITY-NOTES.md) and [porting roadmap](../docs/PORTING-ROADMAP.md).
