# Monster Maze compatibility notes: 1.8 → 1.21 → Paper 26.3

## What the source comparison shows

### 1.8 implementation
The old module includes server-version-locked `v1_8_R3` classes such as `AddonGhostSnowman`, `AddonGhostOcelot`, and `AddonGhostVillager`. Those depend on 1.8 server internals and must not be copied into the modern target.

### Legacy 1.8 Speed mode retained in the modern port
- The 1.8 source exposes Original, Speed and Modern. Speed enables QOL kit rules while preserving the original stage timer (60 seconds, minus 2 per stage, floored at 15 seconds), 150 initial monsters, and 15 additional monsters per Safe Pad.
- The 1.21 source exposes Original, Modern and Classic instead. The modern port keeps both historical Speed and current Classic so neither mode family is lost: only Modern uses the accelerated stage timer, 225 initial monsters, and 30 additional monsters per Safe Pad.
- Contract tests cover the mode names, kit availability, timer boundaries, and monster-count cadence.

### 1.21 implementation used as the baseline
- `GameManager` owns the round/stage rules; `MazeGenerator` owns maze geometry and block placement; `MonsterManager` owns monster routing and bump/freeze behavior.
- Safe Pads use modern `Material`, `BlockData` stair orientation and block snapshots rather than legacy block metadata.
- Entity behavior is mostly expressed through Bukkit `LivingEntity`/`Mob` APIs. ProtocolLib remains optional for per-client ghost-mob disguises; mechanics can still run with the source's Snow Golem fallback.
- Maze generation batches block work using the server scheduler rather than generating the entire layout in one blocking operation.
- External API/run submission and status heartbeat are suppressed by default for this isolated test module.

## Paper 26.3 port boundary
Java 21/Paper 1.21 code is not assumed to be compatible with Paper 26.3 automatically. The modern module uses Java 25 and compiles the entire engine against the targeted API so changed or removed API calls become build failures. Reflection and packet-based behavior still need runtime testing.

## In-game acceptance checklist
1. Start a random run and each of the three patterns; maze generation must not freeze the server.
2. Verify Original/Speed/Modern/Classic timing, Safe Pad generation/claim/decay/restoration, beacon visibility and compass targeting.
3. Verify monster movement/routing, bumps, launch/freeze interactions and cleanup.
4. Verify kits, jumps, abilities, death/spectator handling and solo runs.
5. Repeat with two players, a late join, quit/rejoin and a second run after cleanup.
6. Confirm no backend API write or heartbeat is sent while test mode remains on.
7. Only after mechanics pass, wire and test the per-mob-set presentation, per-set beacon/Safe Pad palettes and distinct game-specific win scenes.
