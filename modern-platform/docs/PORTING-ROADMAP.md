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
- `MonsterMazeModern.jar`: initial import of `joshbet9/MonsterMaze`'s `1.21/MonsterMazeStandalone`, adapted as a separate Java 25 / Paper 26.3 plugin. Default test mode blocks production backend writes and heartbeats.
- Monster Maze remains `MECHANICS_IN_PROGRESS` until Paper 26.3 compile, runtime and in-game acceptance are complete. New cosmetics are not the focus of this stage.

## Per-game acceptance

For each game, confirm lobby/entry; round start/reset/end; spectators and elimination; win detection; arena/map cleanup; reconnect/shutdown; game-specific items/abilities; persistent cosmetic selections; each cosmetic rendered at the intended trigger; each win effect rendered once to the intended viewers; and a repeatable bot or multi-client test path.

## Order

1. **Monster Maze** — close Paper 26.3 compatibility gaps in the copied 1.21 implementation; test modes, maze patterns, Safe Pads, beacon visibility, compass, mobs, kits, round cleanup and two-player flow.
2. **Snake** — port gameplay on the same platform conventions; validate food, green slime behaviour, physical magma-cube item boxes, hotbar item roll and escalating effect durations.
3. After Monster Maze and Snake mechanics are sound, go line-by-line through their cosmetics and win effects, wiring and testing each one individually.
4. Port the remaining 14 games one at a time. They can start as rough mechanical/compatibility ports; fix version differences first, then add new mechanics, distinct cosmetic lines and unique win scenes game by game.
