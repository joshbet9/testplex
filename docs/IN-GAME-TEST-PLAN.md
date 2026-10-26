# In-game acceptance test plan

This is the manual runtime checklist for the isolated Mineplex Reborn test server. The Java 8 Maven build and source-level smoke checks pass, but these scenarios still require an actual 1.8.x server and at least two Minecraft clients to validate gameplay and visibility.

## Before starting

- Deploy the exact artifact produced by a successful workflow run to the **isolated** Reborn server, not production.
- Start the server using its own verified launcher and configuration; join with two accounts if possible.
- Confirm the server has loaded the newly built Arcade plugin and that `/cosmetics` is registered.
- Keep the server console/log available. Record exceptions, missing sounds/particles, entity AI issues, and any impact on TPS.

## Shared cosmetic selector

| Check | Expected result |
| --- | --- |
| Run `/cosmetics` | Main inventory opens and presents Runner, Bacon Brawl, Dragon Escape, Evolution, Micro Battles and Snake. |
| Open each category and equip a choice | The selected option becomes marked as equipped and a confirmation message appears. |
| Unequip an option | The game returns to its stock/default effect; no stale custom effect remains. |
| Close/reopen the menu | Current selection remains equipped. |
| Rejoin, then reopen the menu | Selection is still stored by UUID. |
| Watch from the second client | The equipped effect is visible to the other client as well as its owner. |
| Choose nothing on a fresh player | Stock game visuals remain unchanged. |

## Native game cosmetics

### Snake
- Equip each of Elemental, Neon and Sunset and start Snake.
- Confirm the normal tail/player disguise uses that line's Super Snake colour, Speed uses the line's Speed colour, and Reverse uses the line's Reverse colour.
- Unequip the line; confirm Super Snake returns to the existing random/rainbow colour behaviour.
- Watch from the second client and verify the disguise/tail colours are server-visible and don't remain stuck after the ability ends.

### Runner
- Equip two different trail colours across two players and run side by side.
- Confirm trail clay and its falling-block effect use the selected colour for the correct owner.
- Confirm an unequipped player retains the original red trail.

### Bacon Brawl
- Equip Crown, Top Hat, Chef and Diamond in separate runs.
- Confirm each theme changes the sparkle trail around that player's pig disguise and that each player's effect remains independent.

### Dragon Escape
- Equip different trail colours on two runners.
- Confirm the stained-glass dust follows the correct runner while they are moving and remains visible despite Dragon Escape's normal invisibility effect.
- Confirm unequipped players have no custom dust trail.

### Evolution
- Equip Dragon Egg and evolve at least once.
- Confirm the burst appears at the evolving player's position for both clients.
- Unequip it and confirm ordinary Evolution effects remain unchanged.

### Micro Battles
- Equip two different palette themes on teammates or opponents.
- Confirm the stained-glass dust trail matches each player's theme and is visible to the second client.
- Confirm the game loop remains smooth and the effect stops after the player is eliminated.

## Snake gameplay mechanics

Keep a round running long enough to test the full hazard ramp. Timings are measured from the start of the Live state.

| Approximate live time | Expected behaviour |
| --- | --- |
| 25 seconds | First per-player steering roulette announcement. |
| 45 seconds | First Magma Cube hazard enters the arena. |
| 50 seconds | Next steering roulette roll. |
| 75 seconds | Second hazard; the roulette also rolls at 75 seconds. |
| 105 seconds | Third hazard. |
| 135 seconds | Fourth hazard; total active hazards are capped at four. |

For every roulette type, check that steering changes the actual travel direction without rotating the client's camera: Turn Lag catches up gradually, Drift carries momentum across ticks, Overshoot exaggerates the turn, and Weak Sharp Turning follows rapid turns sluggishly. Check that a fresh roll can change the effect but doesn't immediately repeat the prior one for the same player.

For hazards, check that each cube spawns away from the player's current position, moves toward living players, eliminates on contact, respects the existing Snake invulnerability window, and disappears at game end. Note unexpected spawn positions, collisions through walls, stuck cubes, or any TPS impact.

## Monster Maze safe-pad bounds

- Reach a safe pad from its centre and each of the four edges.
- Check a player near the left/right and front/back edge is counted consistently; the boundary should be symmetric around the beacon.
- Stand just outside each edge and confirm it is not counted as on the pad.
- Confirm normal safe-pad countdown, first-player reward, old-pad decay and next-pad transition are unchanged.
- Confirm no non-beacon clay blocks are modified by the new boundary logic.

## Report template

For each check, record **Pass / Fail / Not tested**, the game/map, number of players, the approximate live time, and any relevant console exception. Gameplay/visual checks are not considered complete until they have been performed on the running isolated server; compile success alone does not validate them.
