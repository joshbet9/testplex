# Sheep Quest — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — available in the shared controlled Arcade queue. Live multiplayer and map-by-map acceptance remain outstanding.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/sheep/SheepGame.java`, `SheepData.java` and `kits/KitArcher.java`, `KitBeserker.java`, `KitBrute.java`.  
**Map source:** `update/maps/Sheep Quest/` — one original ZIP archive.  
**Scope:** Original four-team Sheep Quest. This is one game ID and one shared Arcade lifecycle, not four separate team modes.

The exact archive list is enforced by `SheepQuestMapArchiveTest`, which compares the source folder with the resources packaged into the Paper 26.3 plugin.

## Original map inventory

The source selector must preserve the one original map:

1. `Sheep_BiomeSplit.zip`

The four pen markers and the sheep-spawn marker are read from this map's `WorldConfig.dat`; do not replace the map with a generated arena or infer marker locations from the convenient `maps/sheep-quest.zip` bundle.

## Source-mechanics acceptance

### Map and team setup

- [ ] Start from the shared Game Panel and `/arcadeport play sheep-quest`; confirm it enters the normal Arcade queue/session and loads the dedicated source world.
- [ ] Resolve exactly four source pens: RED, BLUE, GREEN and YELLOW. Resolve WHITE markers as the sheep-spawn locations. Fail map setup clearly if any required marker group is missing or empty.
- [ ] Verify the team-to-marker mapping is stable: RED, BLUE, GREEN, YELLOW. Check all four team spawns, pen floor blocks and bounds on the source map.
- [ ] Confirm Adventure mode, source day time 2000, hunger fixed at 20, team armour, and spectator-only compass distribution. Dead players remain in the team game and respawn after the source six-second spectator interval.
- [ ] Confirm the shared session restores the full player snapshot and all temporary entities/tasks/world changes after natural end, operator stop, disconnect and immediate rematch.

### Sheep spawn, movement and ownership

- [ ] Spawn four adult sheep when Live begins, then spawn one additional sheep every 20 seconds from a random WHITE marker. Sheep use the source high-health survivability rather than dying to ordinary incidental damage.
- [ ] A free/unowned sheep returns toward a valid WHITE spawn when it strays more than 14 blocks; a stuck sheep receives the source recovery impulse after one second without meaningful movement.
- [ ] When a sheep is delivered into a pen, assign that pen's team as its owner, colour it with that team's wool colour, clear its holder/target state and play the source capture feedback.
- [ ] Owned sheep move toward their owner's pen centre when outside it and roam between pen blocks while safely inside. A sheep already owned by a team and inside that team's pen cannot be farmed by picking it up again.
- [ ] When another team carries an owned sheep, it must not be recoloured or re-owned by the enemy until the carrying chain is released and the sheep is actually deposited in their pen.

### Player pickup, carrying and capture

- [ ] Hold the Saddle and approach a valid sheep within 2.5 blocks; it should automatically join the player's passenger stack. Repeated pickups obey the 500 ms pickup cooldown.
- [ ] Carry at most three sheep. A fourth pickup is rejected without losing or duplicating any sheep.
- [ ] Each carried sheep is represented by the correct wool item in the legacy hotbar slots and by the carry-count experience/progress indicator. Stale wool icons and carry slowdown must clear when sheep are dropped or captured.
- [ ] Carry slowdown scales by sheep count (one, two, then three); verify the fourth-count boundary cannot apply a stronger effect.
- [ ] Carrying a sheep into the team's exact pen block deposits it, increments the team's live pen count and clears the passenger chain. Check enemy sheep steals count toward the receiving team's score.
- [ ] Pickup and capture effects/messages and the local `Thief` / `AnimalRescue` counters are credited to the correct player. Verify the legacy `Selfish` win stat threshold is strictly more than 12 sheep.

### Dropping, combat and Brute interactions

- [ ] Switching hotbar slots, dying or dropping the held item releases the passenger stack; death also ejects any passenger player. The wool display, experience bar and slowdown reset without leaving orphaned passengers.
- [ ] Damage to a player carrying sheep passes through the passenger chain to the carrier. A qualifying melee, projectile or custom hit drops the sheep and credits the opponent's animal-rescue event once.
- [ ] Suffocation damage is cancelled as in the source game. Normal combat remains active; hit, death, respawn and team-protection handling must follow the shared team lifecycle.
- [ ] **Brute / Thrower:** right-click a living teammate to pick them up; dropping the held item throws the carried sheep or player with source launch velocity. A thrown player collision deals six damage and applies the source bounce/knockback; spectators and invalid targets are ignored.
- [ ] The Brute's throw action must not be intercepted by the ordinary carrier-drop handler. A thrown sheep/player must not remain attached to the thrower or another entity after collision, chunk unload or round end.

### Kits

- [ ] **Beserker** (preserve the source spelling/aliases): iron axe and Saddle; Double Jump uses 1.2 horizontal and vertical strength with an eight-second cooldown.
- [ ] **Archer:** wooden sword, bow and Saddle; Fletcher replenishes arrows at the source two-second cadence up to five, and Barrage charges five arrows with a one-second initial delay, 250 ms charge cadence and 50 ms volley cadence.
- [ ] **Brute:** iron sword and Saddle; Thrower supports teammate pickup and the drop-to-throw interaction above. Kit selection is available from the common kit station and `/arcadeport kit sheep-quest-<kit>`; kit changes cannot duplicate carried sheep or bypass ability cooldowns.
- [ ] Confirm kit gear and Saddle remain available after legitimate respawns and do not leak into the lobby.

### Scoring, timer and tie resolution

- [ ] The sidebar reports every team's live count of sheep physically inside its pen, the next-sheep timer and remaining match time.
- [ ] The base game lasts five minutes. At time expiry, the team with the highest pen count wins when it has a unique lead.
- [ ] A tie at five minutes does **not** end in a draw: the original game announces that the first team to take the lead wins and continues until a team breaks the tie. Announcements are rate-limited to the source ten-second interval.
- [ ] If the shared team lifecycle leaves at most one team with living players, resolve the remaining team as winner and end cleanly.
- [ ] Verify winning-team / participation reward handling remains local to the Testplex acceptance ledger and does not write to production Mineplex stats or gem services.

## Controlled-queue acceptance gate

Sheep Quest is enabled for combined controlled testing because the source implementation, four-team rules, kit logic, archive mapping and deterministic regression coverage are present. This promotion is **not** a claim of live gameplay acceptance. Load the original map with two or more clients, then test all four teams, sheep capture/steal, passenger chains, combat drops, Brute throws, Archer barrage, Beserker jump cooldown, five-minute tie overtime, disconnects and full session cleanup on the isolated Paper 26.3 server.
