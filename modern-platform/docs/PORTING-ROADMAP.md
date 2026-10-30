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
- Gladiators is receiving a dedicated parity pass on `feat/gladiators-parity`: lobby PvP vote, in-duel hotbar editor, concurrent double-elimination brackets, per-duel timers, challenge achievements, lower-bracket win milestone and kill-message cosmetics. It remains disabled for live play until the branch builds and passes multiplayer acceptance.
- Gladiators now has the lobby rules vote, in-duel hotbar editing, parallel double-elimination brackets, per-duel combat timers, two requested achievements, Silky Seconds tracking and player-selectable kill messages. It is an isolated 26.3 acceptance candidate; keep its catalogue status in progress until multiplayer sign-off.
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

## Complete 26-game programme register (31 October 2026)

The overall programme tracks 26 titles/variants: **24 shared Arcade catalogue games**, the separately packaged **Wizards** module, and the planned **Speed Builders Arcade** variant. Speed Builders currently has no implementation branch or runtime code; it is tracked as planned work rather than represented by a non-functional placeholder.

| # | Game | Current implementation state | Test access |
|---:|---|---|---|
| 1 | Bacon Brawl | Mechanics in progress | Disabled |
| 2 | Bomb Lobbers | Mechanics in progress | Disabled |
| 3 | Death Tag | Acceptance-ready | Enabled |
| 4 | Dragon Escape | Mechanics in progress | Disabled |
| 5 | Dragons (Solo) | Mechanics in progress | Disabled |
| 6 | Evolution | Mechanics in progress | Disabled |
| 7 | Gladiators | Mechanics implemented; runtime acceptance still required | Disabled |
| 8 | Micro Battles | Mechanics in progress | Disabled |
| 9 | Monster Maze | Embedded in Testplex Arcade lifecycle; mechanics in progress | Disabled |
| 10 | One in the Quiver | Acceptance-ready | Enabled |
| 11 | Paintball | Mechanics in progress | Disabled |
| 12 | Runner | Mechanics in progress | Disabled |
| 13 | Sheep Quest | Mechanics in progress | Disabled |
| 14 | Snake | Separate modern module; mechanics in progress | Disabled |
| 15 | Spleef | Mechanics in progress | Disabled |
| 16 | Turf Wars | Mechanics in progress | Disabled |
| 17 | Draw My Thing | Mechanics in progress; large-lobby turn scaling present | Disabled |
| 18 | Master Builders | Mechanics in progress; plot/review work present | Disabled |
| 19 | Castle Assault | Mechanics in progress | Disabled |
| 20 | Heroes of Gwen | Mechanics in progress | Disabled |
| 21 | Sneaky Assassins | Mechanics in progress; no archived source map | Disabled |
| 22 | SkyWars Mixed | Mechanics-first 16-player port integrated | Disabled |
| 23 | Milk the Cow | Mechanics-first port integrated | Disabled |
| 24 | Wither Assault | Mechanics-first port integrated; generated test arena | Disabled |
| 25 | Wizards | Separate resource-packless module; 10 of 21 spell effects implemented | Disabled |
| 26 | Speed Builders Arcade | Planned; implementation not started | Not available |

**Readiness rule:** only Death Tag and One in the Quiver are in the READY_FOR_TEST allow-list and enabled-games config. Catalogue presence and successful compilation do not automatically make a game available. Every other title remains disabled until source parity and isolated multi-client acceptance are completed.
