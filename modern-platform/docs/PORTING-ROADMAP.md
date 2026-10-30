# Modern Arcade porting roadmap

## Platform rules

- Keep modern code on Java 25 / Paper API 26.3. Do not copy legacy v1_8_R3 packet/NMS classes into modern modules.
- Preserve the existing 1.8 Testplex instance and deployment workflow as the reference; modern builds only create artifacts and never deploy automatically.
- Each game owns its mechanics, cosmetic line definitions, render hooks, win-effect renderer and repeatable test checklist.
- Preference keys stay namespaced by player UUID, game ID, line ID and setting ID. Win effects are separate from regular cosmetics and from other games' effects.
- Shared infrastructure (Game Panel, player preferences, world/arena lifecycle and test tools) is reusable; game visuals should not be generic recolours.
- No game becomes READY_FOR_TEST until its gameplay lifecycle and acceptance checks pass on a live server.

## Current implementation

- `TestplexModernArcade.jar`: Game Panel, cosmetic catalogue and per-player preferences.
- `MonsterMazeModern.jar`: mechanics-first import of the 1.21 standalone implementation, with production backend writes/heartbeats blocked by default.
- `SnakeModern.jar`: independent mechanics-first port with its own test arena, sheep tails, slime food, kits, magma-cube item boxes and steering roulette.
- `ArcadeMechanicsModern.jar`: first-pass, distinct mechanical loops for the other 14 games; separate generated arena per game. These are prototypes for rapid compatibility testing, not source-parity sign-off.
- `WizardsModern.jar`: separate resource-pack-independent survival port; four original kits, loot/spell progression and fifteen initial spell effects in a generated arena. Six spells and late-game parity remain in progress.
- Gladiators now has the lobby rules vote, in-duel hotbar editing, parallel double-elimination brackets, per-duel combat timers, two requested achievements, Silky Seconds tracking and player-selectable kill messages. It is an isolated 26.3 acceptance candidate; keep its catalogue status in progress until multiplayer sign-off.
- All 16 entries remain marked MECHANICS_IN_PROGRESS; passing CI means compile/unit tests only, not that player experience has been accepted in-game.
- New cosmetic lines and unique win scenes are still deferred.

## In-game acceptance

For each game: confirm Game Panel entry; lobby/arena transfer; round start/reset/end; spectators/elimination/respawn; win detection; map cleanup; reconnect/shutdown; items/abilities; inventories and original locations restored; cosmetic choices persist; each cosmetic renders at the right trigger; each win effect renders exactly once to intended viewers; and repeatable bot/multi-client testing.

## Order

1. **Monster Maze** — validate Original/Modern/Classic, maze patterns, Safe Pads, beacon visibility, compass, mobs, kits, round cleanup and two-player flow.
2. **Snake** — validate steering/movement, tail spacing/collisions, slime growth, kit timers, item-box contacts/roulette, wall hits, disconnects and cleanup.
3. **Wizards** — complete the remaining spell effects, source-accurate late-game events, map strategy and multi-client round lifecycle/restore checks; keep the modern item presentation vanilla and resource-pack independent.
4. **Other 14 games** — compare each rough modern mechanics loop against the existing Reborn source, repair API/round/map discrepancies and only then add further mechanics.
4. After gameplay loops are accepted, go line-by-line through each game's cosmetic line and distinct win effect, rendering and testing each. Keep legacy/global cosmetics separate where they are intentionally shared.
