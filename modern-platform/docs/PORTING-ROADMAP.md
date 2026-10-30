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
- Gladiators now has the lobby rules vote, in-duel hotbar editing, parallel double-elimination brackets, per-duel combat timers, two requested achievements, Silky Seconds tracking and player-selectable kill messages. It is an isolated 26.3 acceptance candidate; keep its catalogue status in progress until multiplayer sign-off.
- All 16 entries remain marked MECHANICS_IN_PROGRESS; passing CI means compile/unit tests only, not that player experience has been accepted in-game.
- New cosmetic lines are being added game-by-game; Micro Battles now has personal middle-glass and shared border-particle colour lines. Unique win scenes for the remaining games are still deferred.


## Micro Battles parity checkpoint

The modern Micro Battles port now covers the Reborn source flow at code level: Archer / Worker / Fighter kits; the ten-second starting glass barrier; the four-second own-island restriction; team-coloured, protected armour and the slot-8 team badge; progressive map crumble; hunger floor; team-safe damage; projectile knockback; Fighter Iron Skin's fixed 0.5 damage reduction; block growth/flow prevention; sword-drop protection while allowing ordinary resource drops; and spectator tracking with opposing-team filtering, a player menu and a three-second teleport cooldown.

The four-team timeout resolver compares surviving player counts first, then team kills; a complete tie is a draw. Annihilation uses the source eight-kill threshold, and Last Two fires when exactly two teams remain alive. Both are recorded as local Testplex acceptance achievements/missions because the isolated modern server does not connect to Mineplex's production achievement and mission services.

Micro Battles has two independent 16-colour cosmetic lines:
- **Middle Glass Colour** changes the starting glass only for its owner through per-viewer block updates.
- **Border Particle Colour** renders shared dust particles at three heights, every four ticks, while living players are within three blocks of the nearest border face. A shared floating **⚠ BORDER** label follows nearby players and clears on leaving the warning range, elimination, disconnect or round end.

Still required before sign-off: build/test on the current platform branch, deploy to the isolated server, and live multi-client checks for personal glass isolation, shared border visibility, spectator controls, team match flow, reconnect handling and cleanup. Production Mineplex mission / chat-stat backends are intentionally not represented as connected; this port remains MECHANICS_IN_PROGRESS until live acceptance is complete.

## In-game acceptance

For each game: confirm Game Panel entry; lobby/arena transfer; round start/reset/end; spectators/elimination/respawn; win detection; map cleanup; reconnect/shutdown; items/abilities; inventories and original locations restored; cosmetic choices persist; each cosmetic renders at the right trigger; each win effect renders exactly once to intended viewers; and repeatable bot/multi-client testing.

## Order

1. **Monster Maze** — validate Original/Modern/Classic, maze patterns, Safe Pads, beacon visibility, compass, mobs, kits, round cleanup and two-player flow.
2. **Snake** — validate steering/movement, tail spacing/collisions, slime growth, kit timers, item-box contacts/roulette, wall hits, disconnects and cleanup.
3. **Other 14 games** — compare each rough modern mechanics loop against the existing Reborn source, repair API/round/map discrepancies and only then add further mechanics.
4. After gameplay loops are accepted, go line-by-line through each game's cosmetic line and distinct win effect, rendering and testing each. Keep legacy/global cosmetics separate where they are intentionally shared.
