# Runner — Paper 26.3 source-parity acceptance plan

**Readiness:** `MECHANICS_IN_PROGRESS` — disabled in the shared `enabled-games` allow-list.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/runner/Runner.java` and the three `kits/Kit*.java` files.  
**Map source:** `update/maps/Runner/` — 22 original ZIP archives.

This plan is the gate for promoting Runner into the shared test queue. CI packaging and unit tests are necessary but do not substitute for live Paper 26.3 gameplay acceptance.

## Original map inventory

All 22 original map filenames must remain selectable under the single `runner` game ID:

1. `Runner_Amazonian Lake.zip`
2. `Runner_Candyland.zip`
3. `Runner_Clay Plaza.zip`
4. `Runner_ClayPlaza.zip`
5. `Runner_Cloud City.zip`
6. `Runner_Colloseum.zip`
7. `Runner_Crescent.zip`
8. `Runner_Dangerous Sharks.zip`
9. `Runner_Eclipse.zip`
10. `Runner_Escape Room.zip`
11. `Runner_Euphoria.zip`
12. `Runner_Flower_Field.zip`
13. `Runner_Frozen.zip`
14. `Runner_Fruit Salad.zip`
15. `Runner_Imaginary.zip`
16. `Runner_Macrocosm.zip`
17. `Runner_Mesa Canyon.zip`
18. `Runner_Nu Draconis.zip`
19. `Runner_Snowball.zip`
20. `Runner_Tundra.zip`
21. `Runner_Volcanic Fire.zip`
22. `Runner_Whirlpool.zip`

The two Clay Plaza filenames are distinct source archives and must not be collapsed into one map. CI compares the complete sorted source filename set with the JAR's `source-maps/Runner/` entries while Runner is still gated.

## Source-mechanics acceptance

### Match and player rules

- [ ] Start Runner through `/gamepanel` and `/arcadeport play runner`; verify it uses the shared Arcade queue, dedicated map world and normal session start/end path.
- [ ] Verify 2-, 4-, 8- and 16-player rosters. At each size, confirm players spawn at valid source markers and the course bounds are taken from that selected map's `WorldConfig.dat`.
- [ ] Confirm the source defaults: PvP disabled, hunger fixed at 20, Night Vision enabled, no preparation freeze, and the source water penalty (4 damage) is applied at the correct cadence.
- [ ] Confirm last-survivor win resolution; eliminated players become spectators and do not interfere with living players.
- [ ] Verify the nearest-survivor compass follows a valid living target, updates after eliminations and handles the final survivor cleanly.

### Kits

- [ ] **Leaper:** iron axe loadout; Leap has the source 1.2 strength parameters and an 8-second cooldown. Reusing it early is rejected; the cooldown cannot be reset by changing kit or reconnecting.
- [ ] **Archer:** bow loadout; Quickshot retains the source 1.2 parameter and six-second cooldown. Confirm shots do not create unintended player damage because Runner PvP is disabled.
- [ ] **Frosty:** starts with the source 16 Frost Balls; replenishes on the 500 ms source interval up to the cap. A hit applies Slowness III for two seconds, disables knockback, and does not bypass spectator or session-state checks.
- [ ] Kit selection through the lobby pedestal and `/arcadeport kit runner-<kit>` resolves Leaper, Archer and Frosty consistently.

### Course block loop

- [ ] Each live player's full side-standing footprint is sampled; every eligible exposed course block is marked, not just the block at the player's feet.
- [ ] Air, bedrock, liquid, blocks with a non-air block above, and already-marked blocks are never marked.
- [ ] Eligible blocks use the source red stained-clay warning appearance and remain for 600 ms before being removed.
- [ ] Removed blocks spawn falling-block hazards with the source 1.2-second lifetime. They are removed on expiry, chunk unload or collision and never become permanent world blocks.
- [ ] Falling blocks deal six damage only when the target is not above the falling block (the source vertical collision check); spectators are immune.
- [ ] A falling block landing does not replace the map's original block. Its impact feedback remains visible without placing a permanent redstone block.
- [ ] An arrow hitting the course marks eligible blocks in the source 2.5-block radius and the arrow is removed after the trail is created.
- [ ] Confirm the loop remains bounded under simultaneous players and repeated arrow hits; no duplicate scheduling or runaway falling-block entities.

### Stats, progression and cosmetics

- [ ] The source `MarathonRunner` distance stat samples 3D travel at its original one-second cadence, persists to the local Testplex stats view and is not confused with horizontal achievement distance.
- [ ] Teleports, respawns and map transitions do not create artificial distance jumps.
- [ ] `Gettin’ those steps in!` is awarded only when horizontal walking distance is strictly over 400 blocks in one match.
- [ ] `Hop to it` is awarded at 100 jumps in one match.
- [ ] `Flash` is awarded only for a win at or below 90 seconds; a non-win or a 90,001 ms result does not qualify.
- [ ] The 15 non-red trail colours remain visible to both players and spectators. On a map whose exposed palette already uses the selected colour, the resolver selects an unused alternative; if all are present, it chooses the least represented alternative.
- [ ] The cosmetic is applied as shared world-block changes, not only as a private particle effect, and all changed trail blocks are restored at round end.
- [ ] Fake test-player fixtures exercise the course mechanics but never earn personal stats, achievements or distance records.

### Map-by-map run and cleanup

- [ ] Select and load each of the 22 maps once. Confirm its title, bounds, spawn marker(s), route geometry and source block palette are valid; no map silently falls back to a different archive.
- [ ] Complete at least one two-client round on every map. For the map-specific edge-case pass, include all 22 maps across a small roster and repeat the highest-risk maps with 8–16 players.
- [ ] Test a player disconnecting while alive, a survivor disconnecting after elimination, operator stop, a natural round finish and a new round on the same map.
- [ ] Verify all temporarily marked course blocks and cosmetic trail blocks are restored to their pre-round block data; no falling-block entities, scheduled callbacks, scoreboards, compass targets or temporary state survive the session.
- [ ] Verify every participant returns to the shared lobby with their inventory, armour, offhand, mode, effects, food, health, experience and movement settings restored.

## Promotion gate

Runner stays disabled until:
1. The Paper 26.3 build and Runner regression tests pass.
2. CI confirms all 22 original archive filenames match the packaged JAR exactly.
3. The full map loading and multi-client checks above pass on the isolated live server.
4. Any defect found is fixed and retested without weakening source parity or the shared Arcade lifecycle.

After that, add Runner to the enabled-game map policy and config migration in one focused readiness change. This plan does not claim that live acceptance has already occurred.
