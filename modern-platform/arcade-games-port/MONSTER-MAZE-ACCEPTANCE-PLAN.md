# Monster Maze — Paper 26.3 controlled acceptance plan

Monster Maze is part of the 20-game controlled Testplex queue. Its gameplay is hosted inside `ArcadeMechanicsModern` and registered with the Testplex Arcade manager; it is not a separate runtime plugin or lobby. The recent progression/achievement hooks are included, but live acceptance is still required.

## Startup and lifecycle

- [ ] Confirm the module initializes inside the shared Arcade host and is accepted by the Arcade manager.
- [ ] Confirm the deployment-correlated `.monster-maze-readiness.properties` record identifies the current build, nonce, systemd invocation and process.
- [ ] Launch Monster Maze through the Game Panel while another game is idle; verify the shared lobby remains available if module initialization fails.
- [ ] Verify match start, finish, leave, disconnect/rejoin and server shutdown restore participants to the shared Arcade lobby without leaving an active session or modifying unrelated worlds.

## Maps and round flow

- [ ] Enumerate every map configured in the bundled Monster Maze map catalogue and load each one from the deployed source-world set.
- [ ] Verify source spawn locations, maze bounds, stage transitions, objective locations and spectator return points.
- [ ] Exercise early stages, stage 10 and later stages, including repeated map changes and a fresh match after a completed round.
- [ ] Verify the active Safe Pad is identified consistently by the objective beacon, HUD and compass, including when the next pad changes.

## Gameplay and kits

- [ ] Test monster pursuit, collision/elimination, player movement and stage advancement with multiple real clients.
- [ ] Test every supported mob form and ensure per-player disguise/visibility does not leak into the lobby or spectators.
- [ ] Exercise Jumper charges, Slowballer / Cryo Blitz and Body Builder / Body Rush, including cooldowns, HUD values, hit registration and edge cases at round boundaries.
- [ ] Verify Safe Pad arrival, survival feedback, objective transitions and any configured pad/beacon colour choices.
- [ ] Confirm spectators cannot interfere with active players, mobs, objectives or progression.

## Progression and regression

- [ ] Verify first Safe Pad arrivals, highest stage and multiplayer wins persist in Testplex-local statistics.
- [ ] Verify Jumpman, Just to Suffer, Blizzard, Stampede, Out of the Cup, Breakaway, Photo Finish, Blackout and Perfect Run unlock only from their intended in-round events.
- [ ] Confirm round-scoped counters reset between matches and no achievement is awarded by a stale event from a previous round.
- [ ] Confirm all statistics and unlocks remain local to the isolated Testplex server; no production currency or rewards are issued.

## Acceptance record

Record build SHA, map, player count, client versions, kit, steps exercised, failures and log excerpts. A passing Maven/CI run and a valid startup readiness signal are prerequisites, not substitutes for the real-client map and multiplayer checks above.
