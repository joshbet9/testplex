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

## Lobby cosmetic showcase coverage

- Cycle through all 16 supported game types and confirm the lobby display changes to the selected game's showcase during Recruit and is removed before the map starts.
- Bacon Brawl, Bomb Lobbers, Death Tag and Dragon Escape retain their dedicated previews. Every other game should show the choices/settings defined in the shared Arcade Cosmetics menu, including composite settings and the larger Monster Maze mob-set list.
- Check that particles are visible from another client, the labels remain paired with the right item, and changing game or restarting the Arcade plugin leaves no stale showcase entities behind.
- Open the normal Cosmetics menu, enter Arcade Cosmetics, and confirm the selected option and the lobby preview are associated with the same game/option.

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
- Land successive melee hits and ability hits. The attacker should get a per-hit action-bar indicator, and the target should get a visible impact burst/sound.
- At the start of the round, confirm the indicator says the timed knockback boost is inactive; after the timed knockback escalation starts, confirm the indicator reports the active multiplier on each hit.
- Hit a summoned Bacon Brawl test dummy and confirm the hit impact and action-bar feedback appear. Dummies are test targets, not real Players, so don't count them as proof of player-only stats/team mechanics.

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

### Bomb Lobbers
- With TNT selected, left- and right-click air, blocks, and another entity. Each input should throw exactly one TNT, consume exactly one item, and preserve team/friendly-fire rules.
- With Pitcher kit, test the velocity selector lever with both left- and right-clicks on air, blocks and another entity. It should adjust the selector only and must not cause an unintended melee hit or duplicate update.
- Throw TNT directly at a summoned Bomb Lobbers test bot. Confirm the direct-hit ding, visible particles and temporary `DIRECT HIT!` label appear. The bot is a test dummy, not a real player, so it must not be counted as a normal player kill.
- Check the pregame TNT impact showcase contains all available impact choices and is cleared when the game leaves Recruit. Repeat the showcase check for every game; games without custom displays should use the generic per-option showcase.

## Snake gameplay mechanics

The timings below are measured from the start of the Live state. Normal green slime food should continue spawning and be edible throughout.

| Approximate live time | Expected behaviour |
| --- | --- |
| About 10 seconds | First physical Magma Cube item-box hazard spawns on a clear floor tile. |
| About 25 seconds | Second cube spawn, if the first spawn succeeded. |
| About 40 seconds | Third cube spawn, subject to the four-active-cube cap. |
| About 55 seconds | Fourth cube spawn; at most four hazards can be active. |

Touch a cube and verify the player's hotbar shows a short item roulette, then the selected negative steering effect begins without rotating the camera. Each player's successive cube hits should apply effects for 1, 3, 5, 10, 15, 20, then at most 30 seconds. The effect types should be distinct: Turn Lag catches up gradually, Drift carries momentum and limits travel-direction changes to 8 degrees per tick, Overshoot adds 35% of the latest yaw change, and Weak Sharp Turning catches up at up to 4 degrees per tick during sharp turns. Turn Lag is capped at 6 degrees per tick. The original item in the roulette hotbar slot must be restored after the roll.

Check that each cube moves toward living players, is consumed when its item roll triggers, and does not kill the player. If a player touches another cube during an active hotbar roll, it should remain available and trigger a second roll immediately after the current one. Failed spawn positions should retry soon rather than producing chat-only announcements. Note stuck cubes, collisions through walls, broken item restoration or any TPS impact.

## Monster Maze safe-pad bounds and optional persistence

- With `monstermaze.safe-pad-enhancements.enabled: false` (the default), confirm the original beacon lifecycle is unchanged.
- Reach a safe pad from its centre and each of the four edges.
- Check a player near the left/right and front/back edge is counted consistently; the boundary should be symmetric around the beacon.
- Stand just outside each edge and confirm it is not counted as on the pad.
- Confirm normal safe-pad countdown, first-player reward, old-pad decay and next-pad transition are unchanged.
- With beacon persistence explicitly enabled, remove/replace only the objective beacon during recruit and verify the one-time audit repairs it roughly 2–3 seconds after the first pad is created.
- While live, remove/replace the active objective beacon and verify it is restored by the once-per-second check. Repeat for the preview pad.
- Confirm adjacent stained clay, pad base, stairs and decaying old pads are never rebuilt by beacon persistence.
- For optional colour testing, set `surface-colour-enabled: true` and `surface-colour-data` to a valid dye value (0–15). Confirm only the new pad's stained-clay surface uses the chosen starting colour and that the normal decay colours still function.
- Confirm beacon persistence is independently controlled from any diagnostics setting.

## Report template

For each check, record **Pass / Fail / Not tested**, the game/map, number of players, the approximate live time, and any relevant console exception. Gameplay/visual checks are not considered complete until they have been performed on the running isolated server; compile success alone does not validate them.


## Native Monster Maze modes and globally ported systems

Before testing, ensure the isolated Arcade server group's configured **Games** list includes the new enum names `MonsterMazeSpeed` and `MonsterMazeModern` alongside `MonsterMaze`. The variants are built into `Arcade.jar`; no standalone Monster Maze plugin or extra JAR should be installed.

### Original, Speed and Modern

| Check | Original `MonsterMaze` | `MonsterMazeSpeed` | `MonsterMazeModern` |
| --- | --- | --- | --- |
| Maze and lifecycle | Existing native Reborn game | Same Reborn maze/map/lifecycle | Same Reborn maze/map/lifecycle |
| Starter monsters | 150 | 150 | 225 |
| Safe Pad timing | Original pacing | Original pacing | Starts at 35s, decreases by 2s per stage to a 15s floor |
| Additional transition wave | Original 15-mob wave | Original 15-mob wave | 30-mob transition wave (not stacked with the Original 15) |
| Jumper | 5 charges; original rules | 3 charges; Safe Pads refill charges; jumps while on a Safe Pad are free | Same enhanced Jumper mechanics as Speed |
| Slowballer | Slowball only | Cryo Blitz freezes monsters within 6 blocks for 3s; 30s cooldown | Same enhanced Slowballer mechanics as Speed |
| Body Builder | Original max-health reward | Two Body Rush activations per run, 10s each; monster contacts deflect the monster and cost 2s of duration | Same enhanced Body Builder mechanics as Speed |
| Repulsor | Original kit | Native registered enhanced-game kit entry | Native registered enhanced-game kit entry |
| Maverick | Not available | Monster-hit knockback targets the next Safe Pad | Same Maverick mechanics as Speed |

Run each game type from the normal Reborn game selector/admin game tooling, not through a standalone-plugin selector. Confirm the **original** Monster Maze's timer, kit roster and behaviour are unchanged.

### Pattern selection and original maze layouts

- Set `monstermaze.pattern: 0` in the Arcade plugin config for random layout selection, or `1`, `2`, or `3` to force an original layout for repeatable testing; restart/reload via the server's normal safe procedure after changing config.
- Run the same forced pattern twice. The scoreboard's **Maze Pattern** field should match the requested pattern both times.
- Return to `0` and verify normal selection works and the chooser does not return a different layout than the one it selected.
- Confirm maze geometry, spawn markers, central safe area and Safe Pad spawn positions are still derived through Reborn's native map/world data. The port deliberately reuses the original three layouts and does not add a second world/map manager.

### Personal bests and Mineplex infrastructure

- Reach Safe Pad 1 and later reach a higher Safe Pad using a particular kit.
- Confirm a “New Monster Maze personal best” message is shown when the recorded value improves.
- Switch kits, patterns and game types and confirm the records are separate by native game display, pattern and kit.
- Replay a lower stage with the same mode/pattern/kit; it must not replace the stored PB.
- Rejoin and verify the record remains stored by Mineplex account custom data. This feature uses Mineplex's custom-data manager; it does not use the standalone project's competitive backend.
- If the test server lacks a functioning account/custom-data database, record that as a test-environment blocker rather than assuming PB persistence passed.

### Safe Pad onboarding, return and per-round theme reset

- In a new account with no Monster Maze PB in Original, Speed or Modern, confirm the first-stage particle breadcrumb leads toward Safe Pad 1. An account with a PB in any of those variants must not see the breadcrumb.
- In the same match, confirm the first and second Safe Pad spawns each show an elevated firework to **all** players, including experienced players. Repeat in another match; the first two fireworks must appear again.
- After reaching the active Safe Pad, walk off each edge. The return teleport must place the player on the centre of that same Safe Pad (not the map's central start), preserve the direction of travel, and not loop.
- Right-click the Mob Theme toggle to select a non-default mob set, then start a fresh game. The toggle and disguise must start at **Map Default** in Original, Speed and Modern. The saved Safe Pad and beacon colour choices for each mob set should remain available.

### Global lifecycle and regression checks

- Allow a player to use Cryo Blitz while monsters are moving. Affected monsters should stop movement and contact behaviour for 3s, then resume.
- Test Body Rush against a monster contact while normal hit cooldown is active; the active ability should intercept the hit, launch the monster away and shorten its remaining duration by 2s.
- Test Maverick's knockback direction toward the active Safe Pad (and toward the preview pad only when no active pad exists).
- Stop/end games repeatedly. Confirm active/preview/old Safe Pads restore their original blocks, active mobs are removed, and no stuck/frozen entities leak into the next game.
- Set a fixed pattern and check the same layout in Original, Speed and Modern.
- Verify Safe Pad bounds from every edge and test beacon persistence disabled (default) and enabled; only the central objective beacon may be restored by the optional persistence feature.

Gameplay parity, player-facing PB writes, mob cleanup and performance remain runtime checks. A successful Maven build alone does not mark these scenarios as passed.


### Quick administrator launch path

On the isolated test server, after the new Arcade JAR is installed, an administrator with the existing game-command permission can select and start the variants through Reborn's own commands:

```text
/game set MonsterMazeSpeed
/game start 10
```

Repeat with `/game set MonsterMazeModern`. To test a fixed layout, set `monstermaze.pattern` to `1`, `2`, or `3` in the Arcade plugin config before the round/server starts; `0` is random. The current layout number is shown on the in-game scoreboard. While a Monster Maze game is selected, use `/game pb` to display the caller's saved PB for each of the three layouts for their selected kit. The command is part of Reborn's normal `/game` command tree, not a standalone Monster Maze command. The new types prefer their own native map folder when it has maps, and otherwise fall back to the original Monster Maze map catalog; duplicate map folders are not required for initial testing.


### Additional global regression fixes

- Right-click the objective beacon on an active pad. It should not open the vanilla Beacon UI. After the pad becomes an old/decaying pad, its interaction listener should be removed; confirm this across several transitions and after the game ends.
- As Jumper, get bumped by a monster and repeatedly press jump while still airborne/being knocked. The bump grace window should prevent an accidental charged-jump deduction for 2 seconds after the hit. Then confirm normal charged jumps resume after the window.
- Test Pilot: being bumped while already standing on a Safe Pad must not set up a Pilot attempt. Being knocked from away from the pad and landing directly on the active pad within the knock window may qualify. Repeat at the edge and after an unsuccessful landing to ensure stale bumps don't award the stat later.


### Mob-spawn performance regression

- Start each variant from Recruit and observe monster population over the first several ticks. The initial population should appear in small bursts (up to 25 entities per tick), rather than all 150/225 appearing in one server tick.
- Count the starter population after queued spawning settles: Original = 150; Speed = 150; Modern = 225. Do not use the short-lived intermediate count to judge the result.
- Leave the game before the queue has finished, then start a fresh round. The prior round's pending spawns must be cleared, and no late mob should appear in the ended game or leak into the next round.
- Check console/TPS while spawning. The change reduces the creation burst but intentionally does not trim nearby mobs or reduce the total population.
