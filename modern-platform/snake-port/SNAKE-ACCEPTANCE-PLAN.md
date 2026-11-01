# Snake — Paper 26.3 controlled acceptance plan

Snake is promoted to the shared controlled-testing queue. It remains a separate `SnakeModern.jar` module, launched from the Testplex Game Panel and bridged to the shared Arcade statistics/cosmetics. The one-time readiness migration enables the plugin on an existing isolated VM; the module still fails closed if the original map set or command is unavailable.

## Startup and maps

- [ ] Confirm `SnakeModern` loads and installs the configured original map before advertising the game as available.
- [ ] Confirm the packaged resources include exactly the three source archives: Snake Pit, Snake Volcanic and Snake WinterBabylon.
- [ ] Load each map and verify its `WorldConfig.dat` team spawns, bounds, spectator marker and world storage conversion.
- [ ] Start a second match after a map change and verify no stale world, participant or item-box state survives.

## Core movement and growth

- [ ] Exercise the frozen preparation countdown, starting heading selection, mount state and camera presentation with multiple real clients.
- [ ] Verify one-segment-per-second growth, radial green-slime spawns and the +2 growth pickup.
- [ ] Verify source length-dependent speed progression and the 126-segment speed plateau while the tail continues growing.
- [ ] Test tail-follow spacing, participant colours, self-tail grace and the 1.2-block collision radius.
- [ ] Verify idle elimination, lava elimination and cancellation of ordinary damage, combustion and vanilla entity targeting.

## Kits and item boxes

- [ ] Test Speed, Reverse and Invulnerable with source item counts, click interactions, effect durations and cooldowns.
- [ ] Verify Reverse rebuilds the path without collapsing the tail and refreshes the idle position.
- [ ] Verify physical Magma Cube item-box spawn cadence, four-cube cap, steering, contact radius, 12-step item roulette and per-player steering-debuff escalation.
- [ ] Check occupied roulette slots and multiple players contacting the same item box.

## Spectators, progression and cleanup

- [ ] Verify the Tracking Compass selects the nearest survivor, left-click spectates with its cooldown, and right-click opens the survivor menu.
- [ ] Verify Cannibal (six kills), ChooChoo (tail length 60) and SlimySheep (20 slime pickups) award at the correct thresholds.
- [ ] Verify round statistics and lifetime totals are saved and appear in the shared `/stats` page.
- [ ] Exercise leave, death, disconnect/rejoin, game stop and server shutdown; confirm player inventory, position, effects, movement and mode are restored.
- [ ] Verify the combined cosmetic line keeps snake style, legacy palette and ability visuals independently selectable.

## Acceptance record

Record build SHA, source map, player count, client versions, kit, movement/camera observations, item-box interactions, spectator flow and restoration results. Passing rule/map tests and loading the default map are prerequisites, not a substitute for all three maps and multiplayer acceptance.
