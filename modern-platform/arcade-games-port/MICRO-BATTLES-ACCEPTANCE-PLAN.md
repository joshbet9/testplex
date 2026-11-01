# Micro Battles — shared Paper 26.3 acceptance plan

## Test objective

Micro Battles joins the shared acceptance queue with its original archived map, four-team round lifecycle, three source kits, team combat, map crumble and spectator/achievement systems.

Start a round:

```text
/arcadeport play micro-battles
```

Choose a kit before joining or starting:

```text
/arcadeport kit archer
/arcadeport kit worker
/arcadeport kit fighter
```

## Map, teams and round lifecycle

- [ ] The original Micro Battle source map archive loads into its own isolated game world.
- [ ] Four teams — Red, Blue, Green and Yellow — use the source team spawn groups and team indices.
- [ ] Players spawn at the expected islands, with team-coloured armour and a protected team badge.
- [ ] The two-block glass barriers surround the correct islands and drop at the source ten-second point.
- [ ] Per-player middle-glass colour is visible only to its owner and is restored when that player is eliminated or the round ends.
- [ ] Friendly fire is rejected for melee and projectile damage.
- [ ] Team respawns work while the team remains in the game; a team is eliminated when it has no remaining players.
- [ ] A round ends when only one team remains. A timeout uses team survival/kills and produces a draw when the standings are tied.
- [ ] Starting with one player is a useful practice session and does not create a false team victory.
- [ ] Disconnects and operator stops clean up respawns, tracked projectiles, TextDisplays, block overlays and player state.

## Kits and combat

- [ ] Archer receives the bow, wooden sword, apples and three Fletched Arrows; arrows replenish at the source cadence and never exceed the cap.
- [ ] Worker receives the source sword, shovel, pickaxe, axe and apple loadout.
- [ ] Fighter receives the source sword/apple loadout and Iron Skin reduces applicable incoming damage.
- [ ] Melee and projectile damage, armour reduction, fall damage and knockback match the tested Reborn rules.
- [ ] Team hits, self-hits and hits on spectators/dead players are rejected.
- [ ] Recent combat contribution is used for fall/environmental elimination credit; assists are awarded only to fresh contributors.
- [ ] Hunger remains at the intended minimum and does not regenerate naturally.
- [ ] Map crumble completes its vertical scan, begins after the one-second live gate and removes three farthest supported blocks per tick without corrupting other worlds.

## Border, cosmetics and spectators

- [ ] Border warning text appears only when a living player is within the warning distance and follows that player without duplicates.
- [ ] Vertical border particles appear on the nearest border face at the expected cadence and use the selected border colour.
- [ ] The spectator compass tracks the nearest opponent; left-click teleports to the nearest living player and right-click opens the opponent selector.
- [ ] Eliminated players cannot keep a stale warning or personal glass overlay.
- [ ] The combined `Island Glass & Border Colours` menu offers two independent 16-choice groups: private starting glass and shared border particles.
- [ ] Selecting either colour group preserves the other group selection; the two groups can be changed independently.
- [ ] All 16 colour selections resolve to a valid glass material and border particle colour; saved preferences persist across rounds, and existing saved selections from the legacy separate lines still load.
- [ ] The selected personal glass is not visible to other players, while the border particle colour is shared as intended.

## Achievements and stats

- [ ] Annihilation is awarded at eight kills in one game, not seven or nine.
- [ ] Last Two is awarded once when exactly two teams remain and only to eligible living human players.
- [ ] Test-roster players do not persist achievements or progress.
- [ ] Both achievement entries appear in `/stats` → Micro Battles and persist after reconnecting.
- [ ] Team wins, kills, deaths, assists and round results are recorded once, including timeout draws.

## Exit criteria

Keep Micro Battles enabled in the combined queue while fixing issues discovered. Only mark gameplay acceptance complete after original-map spawns, all four teams, all three kits, barrier timing, crumble, combat, spectator tools, achievements and cleanup have been exercised on Paper 26.3.
