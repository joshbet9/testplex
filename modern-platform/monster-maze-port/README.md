# Monster Maze — modern Paper 26.3 port

This dedicated gameplay module adapts the **1.21/MonsterMazeStandalone** implementation from [joshbet9/MonsterMaze](https://github.com/joshbet9/MonsterMaze) to the independent Testplex Paper 26.3 platform. It uses the modern Bukkit/Paper implementation instead of copying the 1.8 `v1_8_R3` NMS entity classes.

## Gameplay port in this first pass
- Original, legacy Speed, Modern and Classic modes; three authored maze patterns; timed stages and Safe Pad progression. Speed keeps the 1.8 QOL kit rules and a modern Speed II replacement for the 1.8 ground-level speeding dash, while retaining the original 60s-minus-2s stage timer and 150/15 monster cadence. Only Modern uses the accelerated timer and 225/30 monster cadence; Classic opts out of the speed boost.
- Jumper, Slowball, Body Builder, Repulsor, and mode-gated Maverick kit behavior from the 1.21 source.
- Monster routing, bumps, launch/freeze interactions, stage progression, spectator/elimination paths and cleanup.
- Testplex-owned lifecycle: players enter through the shared Arcade queue and return to the shared Arcade lobby. This module owns only Monster Maze gameplay and its active arena; it does not register a player-join flow, private lobby, NPC selector or separate Bukkit plugin entrypoint.
- Optional ProtocolLib disguise path retained from the 1.21 source. Without ProtocolLib, the existing source fallback remains available.

## Commands
Monster Maze gameplay is compiled into `ArcadeMechanicsModern.jar`; do **not** install `MonsterMazeStandalone.jar` or `MonsterMazeModern.jar` on the Testplex 26.3 server. Once Monster Maze is enabled for tester acceptance, start it through Testplex's shared `/gamepanel` or `/arcadeport play monster-maze` queue. `/mm gui` only prints instructions for the shared Testplex kit selector; it does not open a separate Monster Maze GUI. Other direct commands include `/mm status`, `/mm stop`, `/mm kit <jumper|slowball|body builder|repulsor>`, and the admin map/mode/pattern controls.

## Test isolation and maps
Default config uses `test-mode: true` and `record-runs: false`. Test mode suppresses backend API writes and status heartbeats even if production environment variables exist. Leave test mode enabled on the test platform.

The JAR does not embed Minecraft world folders. The explicit modern Hyper-V deployment now checks out and installs the seven physical source worlds used by `maps.yml` (`mm_void`, `mm_volcano`, `mm_colombia`, `mm_sandycoast`, `mm_siberian`, `mm_swampland`, and `mm_tesorohundido`) from `joshbet9/MonsterMaze/solo/1.21/maps`. It verifies each world has `level.dat` and region data, records the source commit, backs up existing map folders, and restores those backups if startup fails. Manual installations must provision the same folders before selecting these maps; missing folders are rejected by `MapManager`, and startup may otherwise fall back to `mm_void`.

**Status:** mechanics port in progress. Compiling against Paper 26.3 catches source API incompatibilities, but runtime and in-game behavior still need test-server verification before marking it ready.

Reference source commit: `e4f42100f21a0f4a7b00e493220f12f6a6a1dcce` (`feat/personal-mob-themes`), with the 1.8 Speed mode rules retained.
See [compatibility notes](docs/COMPATIBILITY-NOTES.md) and [porting roadmap](../docs/PORTING-ROADMAP.md).
