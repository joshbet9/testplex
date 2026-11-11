# Dragon Escape — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — available in the shared controlled Arcade queue. Live multiplayer and map-by-map acceptance remain outstanding.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/dragonescape/DragonEscape.java`, `DragonEscapeData.java`, `DragonScore.java` and the four `kits/Kit*.java` files.  
**Map source:** `update/maps/Dragon Escape/` — all 30 original ZIP archives.  
**Scope:** The original Dragon Escape race. Dragons (Solo) is a separate game; this plan does not cover or merge the two games.

The source archive list is enforced by `DragonEscapeMapArchiveTest`: it compares all 30 source filenames with the resources copied into the Paper 26.3 plugin classpath. The legacy map selector must preserve each filename as a distinct choice, including punctuation and spacing variants.

## Original map inventory

All 30 original map filenames must remain selectable under the single `dragon-escape` game ID:

1. `DragonEscape_A Builder's Bedroom.zip`
2. `DragonEscape_Acropolis.zip`
3. `DragonEscape_Ancient Clash.zip`
4. `DragonEscape_Birch Falls.zip`
5. `DragonEscape_Blue Orchid.zip`
6. `DragonEscape_Crystalline Colony 2.0.zip`
7. `DragonEscape_Dark Magic Woods.zip`
8. `DragonEscape_Darkened Heights.zip`
9. `DragonEscape_Fen.zip`
10. `DragonEscape_Frost Run.zip`
11. `DragonEscape_Fungi Galore.zip`
12. `DragonEscape_Hell.zip`
13. `DragonEscape_Into the Jungle.zip`
14. `DragonEscape_Journey Reborn.zip`
15. `DragonEscape_Kamakoa Blossom.zip`
16. `DragonEscape_Lotus Crescent.zip`
17. `DragonEscape_Maple Treeway.zip`
18. `DragonEscape_Mystical Mines.zip`
19. `DragonEscape_Pirate Bay.zip`
20. `DragonEscape_Poseidon's Realm.zip`
21. `DragonEscape_Quiescent Pines.zip`
22. `DragonEscape_Sandstorm.zip`
23. `DragonEscape_Skylands.zip`
24. `DragonEscape_Steve's Swamp.zip`
25. `DragonEscape_Terracola Gorge.zip`
26. `DragonEscape_Through Heaven.zip`
27. `DragonEscape_Through Hell.zip`
28. `DragonEscape_Time Travel.zip`
29. `DragonEscape_Utopia.zip`
30. `DragonEscape_Zodiac.zip`

Do not collapse maps with similar names or rely on the convenience `maps/dragon-escape.zip` archive when a selected original archive exists.

## Source-mechanics acceptance

### Shared lifecycle and race rules

- [ ] Start from the Game Panel and `/arcadeport play dragon-escape`; confirm it enters the shared queue, dedicated source-map world, session setup and standard end/restore lifecycle.
- [ ] Confirm the round is an elapsed race, not a short fixed-duration match. After the first finisher, verify the 30-second finish window lets remaining racers finish and save valid times; end earlier only after no active racer remains.
- [ ] Confirm all participants receive their correct source map spawn and map-specific bounds, and that map reloads do not leak the prior world's blocks, marker state or scheduled tasks.
- [ ] Verify disconnect, operator stop, natural finish and immediate rematch all restore inventories, armour, effects, mode, food, health, experience, movement state, scoreboards and lobby location.

### Map markers, Douglas and course destruction

- [ ] On all 30 maps, confirm the RED start marker, ordered BLACK route checkpoints and GREEN speed marker are read from that selected archive's `WorldConfig.dat`; no map falls back to a different archive.
- [ ] Confirm Douglas first targets the first BLACK waypoint after the RED origin, follows authored marker order through switchbacks, and applies the GREEN marker's speed multiplier.
- [ ] Confirm route progress is monotonic at close switchbacks, effectively coincident markers alone are deduplicated, and players cannot trigger a false shortcut by crossing a nearby waypoint out of order.
- [ ] Confirm Douglas destroys eligible course blocks within the source 10-block radius, with movement throttling that processes newly entered destruction areas without leaving stale blocks behind.
- [ ] Verify the map-specific finish condition uses its authored final route point / finish proximity, not a generic Z-plane shortcut. Check low-altitude fall thresholds on Hell and Pirate maps.
- [ ] Hell maps start at source time 16000. Pirate maps apply the source two-damage water penalty on the 500 ms cadence; other maps must not inherit that Pirate-only penalty.

### Kits and abilities

- [ ] **Jumper:** four Leap charges, source launch vector and grounded boost, eight-second recharge, no charge duplication through death, reconnect or kit changes.
- [ ] **Disruptor:** three Leap charges and up to two TNT mines; each thrown mine starts its own eight-second recharge. Mines arm after the source delay, trigger only on an opponent pickup within range, and do not trigger on their owner.
- [ ] **Warper:** two Leap charges and one pearl that targets a valid player ahead. Its one-second shortcut grace must not permit route-progress regression or false switchback eliminations.
- [ ] **Digger:** six uses, ten-second opening lockout, source nearby-player restriction, and 120-second block restoration. Verify restored blocks match the original map and Digger cannot overwrite protected/non-course blocks.
- [ ] Kits can be selected from the common pedestal and `/arcadeport kit dragon-escape-<kit>`; changes are rejected once a player is an active racer.

### Race presentation, personal bests and ghost

- [ ] Verify the race HUD shows each player's per-map PB delta, including ahead/behind sign changes, and updates through the final three-block finish radius.
- [ ] Verify the sidebar and tab list agree on the top-14 race ranking, leader elapsed time, estimated `~+` gaps and eliminated `OUT` rows; pre-race scoreboards are restored at the end.
- [ ] Complete the same map twice and confirm PBs and checkpoint splits are isolated per archive, persist across restart, and do not carry over to a differently named map.
- [ ] Enable the PB ghost and confirm the owner's saved skin, location, yaw/pitch, sprint/sneak and grounded pose replay continuously. It must be translucent, visible only to its owner, have no server hitbox/collision and disappear at PB completion or when disabled.
- [ ] During a live Dragon Escape race, verify slot nine contains a PB Ghost eye in all kits. Right-click to switch ON/OFF; the item's label updates, the setting survives subsequent rounds, and the item cannot be moved out of its hotbar slot.
- [ ] On the current Minecraft client, verify the ghost uses the modern player-entity spawn packet; ensure that rendering succeeds both with a fresh recorded route and with a legacy best-time replay. Check multiple players toggling independently.
- [ ] Verify Beacon Colour can only be selected through in-game cosmetics, and neither Beacon Colour nor PB Ghost is exposed as a Personal Bests-page button.
- [ ] Install PacketEvents 2.14.0 on the Paper 26.3 acceptance server for the optional packet ghost path. Without it, the plugin must log the documented warning and keep the race playable without the ghost.
- [ ] Confirm the ghost profile, entity, viewer-local team membership and packets are removed on map change, match end, disable, disconnect and reconnect.

### Tester reports — November 12 follow-up

- [ ] **Time Travel (#358, #344):** run beneath the pterodactyl after the first leap and finish the map. The playable boundary now includes a 32-block corridor around every authored route waypoint, in addition to the map's authored bounds.
- [ ] **Builder (#359, #346):** start with two bricks, place and break with the intended Adventure-mode input, and verify charges/recharge remain correct. Initial charges now use the configured Builder maximum.
- [ ] **Builder anti-cheat (#360):** repeat Builder placements while moving. A recent Builder placement receives a 3.5-second exemption only from the route-shortcut heuristic; global anti-cheat remains enabled and other checks are unchanged.
- [ ] **Zodiac (#361):** confirm vanilla lava does not spread into the final jump path. Lava flow is cancelled only in the active Zodiac Dragon Escape world; other games and maps retain normal fluid behavior.
- [ ] **PB/WR (#362):** perform a normal, non-practice run with Game Debug and all gameplay overrides disabled; confirm map and kit records persist across reconnect/restart. Unlock-all-kits is a debug override, so debug runs intentionally do not persist PBs/WRs.
- [ ] **Race clock (#363):** confirm the first Live scoreboard frame is `0:00.000` while saved run timing remains measured from the actual start timestamp.
- [ ] **Finish window and notices (#364):** confirm a 30.0-second countdown appears after the first completion, updates in tenths, and lets other runners finish. Verify the chat emits at most one record milestone per run, preferring global over kit PB/WR notices.
- [ ] **Live verification:** the changes above have unit coverage, but require a Paper 26.3 multiplayer acceptance run before marking the reports fixed.

## Controlled-queue acceptance gate

Dragon Escape is enabled for combined controlled testing because the modern source-map loader, race implementation, four kits, PB timing and optional packet ghost are present with deterministic rule coverage. This promotion is **not** a claim of live gameplay acceptance. Complete the map-by-map checks and at least one two-client race on every map, then repeat the highest-risk switchback, Hell and Pirate maps with multiple racers. Record and fix defects without weakening source route, kit or shared-lifecycle parity.


### Dragon Escape follow-up: collision and leap display

- [ ] In a live race, run through another human player and an active bot without either being pushed or blocked. Confirm this remains true after scoreboard refreshes and game ticks.
- [ ] On the four-leap Jumper kit, inventory contains exactly one iron axe named Leap. Its display name/lore does not contain the charge count. The XP level/bar show 4/full initially, then 3/75%, 2/50%, 1/25%, and 0/empty after successive leaps.
- [ ] On the three-leap Disruptor and Warper kits, the XP level/bar show 3/full initially, then 2/about 66%, 1/about 33%, and 0/empty. Inventory still contains exactly one Leap iron axe.
- [ ] During Leap recharge, the green/red timed-ability bar owns the action bar and replaces TIME/PB until recharge ends. TIME/PB resumes after the cooldown display expires; verify this for all leap kits.
- [ ] Reset/stopping the match restores the participant's original XP level, XP bar, inventory and collision behavior.
