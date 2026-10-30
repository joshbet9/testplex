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
- All 16 entries remain marked MECHANICS_IN_PROGRESS; passing CI means compile/unit tests only, not that player experience has been accepted in-game.
- New cosmetic lines are being added game-by-game; Micro Battles now has personal middle-glass and shared border-particle colour lines. Unique win scenes for the remaining games are still deferred.

## Micro Battles parity checkpoint

The modern port now mirrors these source behaviours at code level: Archer / Worker / Fighter loadouts; the 10-second starting glass barrier; the four-second own-island restriction; team-coloured, protected armour and slot-8 team badge; map crumble; hunger floor; team-safe damage and projectile knockback; block growth/flow prevention; sword-drop protection while allowing other ordinary drops; spectator tracking compass with opposing-team filtering, menu selection and the three-second teleport cooldown; four-team timeout standings (surviving team count first, team kills as the tiebreak, draw on an exact tie); the eight-kill Annihilation stat, Last Two mission progress (exactly two surviving teams), the Reborn melee/arrow raw-damage path, flat Fighter Iron Skin reduction, legacy armour calculation, projectile knockback and 15-second combat assists. Isolated-server stats persist kills, assists, damage taken/dealt, Annihilation counts and Last Two mission progress.

Micro Battles cosmetics are now separately configurable in player preferences:
- **Middle Glass Colour** changes the starting glass only for the owning player, using per-viewer block updates. Other players retain the normal map glass.
- **Border Particle Colour** colours a vertical dust cue that is broadcast to nearby viewers. It appears for living players within three blocks of the nearest world-border face, at three heights, every four ticks. It is not tied to the temporary starting glass, so it remains available after that barrier is removed.

Backend integration remains test-server-local because the isolated modern server does not run Mineplex's legacy mission/stat services. The local equivalents preserve the original trigger semantics and persist the corresponding stats. Four-team match flow, cosmetics, reconnect/cleanup and multi-client visibility still require live-server acceptance; this game remains MECHANICS_IN_PROGRESS.

## In-game acceptance

For each game: confirm Game Panel entry; lobby/arena transfer; round start/reset/end; spectators/elimination/respawn; win detection; map cleanup; reconnect/shutdown; items/abilities; inventories and original locations restored; cosmetic choices persist; each cosmetic renders at the right trigger; each win effect renders exactly once to intended viewers; and repeatable bot/multi-client testing.

## Order

1. **Monster Maze** — validate Original/Modern/Classic, maze patterns, Safe Pads, beacon visibility, compass, mobs, kits, round cleanup and two-player flow.
2. **Snake** — validate steering/movement, tail spacing/collisions, slime growth, kit timers, item-box contacts/roulette, wall hits, disconnects and cleanup.
3. **Other 14 games** — compare each rough modern mechanics loop against the existing Reborn source, repair API/round/map discrepancies and only then add further mechanics.
4. After gameplay loops are accepted, go line-by-line through each game's cosmetic line and distinct win effect, rendering and testing each. Keep legacy/global cosmetics separate where they are intentionally shared.
