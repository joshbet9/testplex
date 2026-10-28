# Modern Arcade porting roadmap

## Platform rules

- Keep modern code on Java 25 / Paper API 26.3. Do not copy legacy v1_8_R3 packet/NMS classes into modern modules.
- Preserve the existing 1.8 Testplex instance and deployment workflow as the reference; modern builds only create artifacts and never deploy automatically.
- Each game owns its mechanics, cosmetic line definitions, render hooks, win-effect renderer and repeatable test checklist.
- Preference keys stay namespaced by player UUID, game ID, line ID and setting ID. Win effects are separate from regular cosmetics and from other games' effects.
- Shared infrastructure (Game Panel, player preferences, world/arena lifecycle and test tools) is reusable; game visuals should not be generic recolours.
- Port status only advances to READY_FOR_TEST after gameplay lifecycle and acceptance checks are completed on a live server.

## Current implementation

- `TestplexModernArcade.jar`: Game Panel, cosmetic catalogue and per-player preferences.
- `MonsterMazeModern.jar`: mechanics-first import of the 1.21 standalone implementation, with production backend writes/heartbeats blocked by default.
- `SnakeModern.jar`: independent mechanics-first port with its own test arena, snake tails, slime food, kits, magma-cube item boxes and steering roulette.
- Both game modules compile against Paper 26.3 and have automated contract/rule tests. Neither is marked ready for in-game testing until the live server acceptance checklist is completed.
- Neither module's new cosmetic line/win-scene renderers are considered complete.

## Per-game acceptance

For each game: confirm lobby/entry; round start/reset/end; spectators and elimination; win detection; arena/map cleanup; reconnect/shutdown; game-specific items/abilities; persistent cosmetic selections; each cosmetic rendered at the intended trigger; each win effect rendered once to intended viewers; and repeatable bot/multi-client testing.

## Order

1. **Monster Maze** — validate Original/Modern/Classic, maze patterns, Safe Pads, beacon visibility, compass, mobs, kits, round cleanup and two-player flow.
2. **Snake** — validate steering/movement, tail spacing/collisions, slime growth, kit timers, item-box contacts/roulette, wall hits, disconnects and cleanup.
3. After both games' mechanics are accepted, walk line-by-line through their cosmetic lines and win effects and test each.
4. Port the remaining 14 games sequentially. Initial ports may be rough; resolve API/version compatibility and round lifecycle first, then add new mechanics, distinct cosmetics and win scenes for each game.
