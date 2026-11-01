# Dragons — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — enabled in the shared controlled queue. Live multiplayer acceptance is still outstanding.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/dragons/Dragons.java`, `MineplexDragon.java` and the three `kits/Kit*.java` files.  
**Map source:** `update/maps/Dragons/` — 40 original ZIP archives.  
**Scope:** One Dragons experience is exposed through the shared `dragons` game ID.

CI and deterministic rule tests are necessary but do not replace an in-game two-client / multi-client run on the isolated Paper 26.3 server.

## Original map inventory

All 40 original map filenames must remain selectable under the single `dragons` game ID:

1. `DR_Voyage.zip`
2. `Dragons_3 o'clock Train.zip`
3. `Dragons_Acetaria.zip`
4. `Dragons_All Hail Sky Whales.zip`
5. `Dragons_Aubergine.zip`
6. `Dragons_Balloon Town.zip`
7. `Dragons_BalloonTown.zip`
8. `Dragons_Beans.zip`
9. `Dragons_Blumengarten.zip`
10. `Dragons_Bony Morass.zip`
11. `Dragons_Cabbage.zip`
12. `Dragons_Caribbean Islands.zip`
13. `Dragons_Cascade.zip`
14. `Dragons_Cutting Board.zip`
15. `Dragons_Deadly Ruins.zip`
16. `Dragons_Docks.zip`
17. `Dragons_Dust Devil.zip`
18. `Dragons_Eden.zip`
19. `Dragons_Egg.zip`
20. `Dragons_Faisal.zip`
21. `Dragons_Flora.zip`
22. `Dragons_Island.zip`
23. `Dragons_Lamb Sauce.zip`
24. `Dragons_Lettuce.zip`
25. `Dragons_Ocean Village.zip`
26. `Dragons_Parsnip.zip`
27. `Dragons_Pepper.zip`
28. `Dragons_Pink.zip`
29. `Dragons_Preservation.zip`
30. `Dragons_Quiescent Jungle.zip`
31. `Dragons_Salt & Pepper.zip`
32. `Dragons_Sands of Time.zip`
33. `Dragons_Scarcity.zip`
34. `Dragons_Sky Ship.zip`
35. `Dragons_Sky Village.zip`
36. `Dragons_SkyShip.zip`
37. `Dragons_SkyVillage.zip`
38. `Dragons_Stone Cold Grasp.zip`
39. `Dragons_Voyage.zip`
40. `Dragons_violet.zip`

The similarly named Balloon Town, Sky Ship and Voyage files are distinct original archives. CI must preserve the exact 40-name set and must not normalise away spaces, punctuation, capitalisation differences or legacy duplicate-style names.

## Source-mechanics acceptance

### Match, map and world rules

- [ ] Start `dragons` using the shared Arcade Game Panel and `/arcadeport play dragons`; confirm it uses the ordinary shared Session, spectator, stop and lobby-restore lifecycle.
- [ ] Select and load all 40 maps once. Read the original RED marker locations as dragon spawn points; verify no map falls back to the convenience archive or another map when the chosen archive is valid.
- [ ] Confirm Adventure mode, PvP disabled (including player-fired projectiles), hunger fixed at 20, the source four-damage water hazard on its 500 ms cadence, and one point of fall-damage reduction.
- [ ] Only source-permitted emerald world items may remain; ordinary dropped items are suppressed. Verify this does not remove kit items, test harness equipment or legitimate emeralds.
- [ ] Confirm last-player-standing placement and no arbitrary short round timer. Disconnects and eliminations must update the alive roster without leaving stale dragon targets.
- [ ] Check map bounds and spawn positions on compact and widely separated source maps.

### Dragon spawn, targeting and contact

- [ ] One dragon is added every five seconds from the original RED spawn markers, up to a maximum of five. Invalid/despawned dragons are removed from the tracked set before the next spawn.
- [ ] Dragon target/contact processing follows the source 125 ms cadence. The target loop stays inside the selected map bounds and continues for far-away dragons as required by the source tick-when-far-away behaviour.
- [ ] Reproduce the source target state machine: low dragons are sent skyward; when a dragon reaches its target it alternates between a sky target and a random living player based on its height relative to the spectator location.
- [ ] Dragons remain non-collidable visual hazards and do not receive normal player damage. Melee hits repel/retarget them skyward; arrows do not damage or kill them.
- [ ] Contact with a living player applies the source hurt feedback and trajectory knockback, not health damage. The two-second contact cooldown is per player/per dragon, so overlapping dragons cannot create a rapid hit loop.
- [ ] Dead players and spectators are never selected as living targets and cannot be contact-hit.
- [ ] Verify dragon entities and any native movement/target references are fully removed at match end, forced stop, world unload and server shutdown.

### Kits

- [ ] **Coward:** leather armour and iron axe; Leap uses source 1.2 horizontal / 1.0 vertical parameters and an eight-second cooldown. Verify grounded/airborne vector handling and no stale recharge after death or kit changes.
- [ ] **Marksman:** chainmail armour and bow; starts with one arrow, replenishes up to four at four-second intervals, and Barrage charges six arrows with a one-second initial charge and 200 ms firing cadence. The modern 50 ms server fire processing must not produce duplicate arrows or bypass the source charge timing.
- [ ] **Pyrotechnic:** gold armour; first Sparkler charge after 25 seconds, capped at two, with the source five-second lifetime. Sparks attract only dragons within 48 blocks; low or submerged sparkler items are ignored for targeting.
- [ ] **Frost:** right-click the Snowball ability to create the forward Ice Bridge. Verify the 25-second cooldown, safe placement only on eligible air/snow surfaces, bounded path length, six-second temporary blocks, and restoration on expiry, death, kit change and round cleanup.
- [ ] **Glider:** right-click the Feather to activate Elytra gliding for 3.5 seconds, with a 15-second cooldown. The player's original chestplate must be restored exactly once on expiry, death, disconnect, kit change and round cleanup; no temporary Elytra may be kept, dropped or duplicated.
- [ ] **Tamer:** right-click the End Crystal to take control of an available dragon for 15 seconds, with a 90-second cooldown. Verify dragon selection, camera/control movement, player return location and game mode restoration, and safe release if the dragon or player becomes invalid.
- [ ] All six kits are selectable from the common lobby pedestal and `/arcadeport kit dragons-<kit>`; kit changes lock at match start and do not duplicate kit gear or temporary ability state after respawn/reconnect.

### Community achievements

- [ ] **Win 3 in a row:** award only after the player's third consecutive Dragons win; a non-win breaks the streak and a single win must not award it.
- [ ] **Survive 5 minutes:** award once when the player survives five full minutes of Live gameplay; Prepare time and spectator time do not count.
- [ ] **Win without taking damage:** award only to a round winner who took no damage during that match, including dragon contact and environmental damage; a damage event must disqualify the award even if the player later heals.
- [ ] Achievement progress persists across rounds and reconnects without double-awarding a completed achievement.

### Removed Flare mechanics

- [ ] Dragon Flares are removed entirely: no Flare kit, charge item, protection radius, six-second protection state or visual-effect variants remain.
- [ ] The old `flare` kit name is rejected; it must not silently select another kit or grant a substitute ability.
- [ ] Sparkler attraction and dragon target/contact logic operate without any Flare protection exception.

### Spectators, stats and cleanup

- [ ] Living players receive source-style opponent/dragon awareness; eliminated spectators can browse surviving players and teleport only to valid living targets.
- [ ] Spectator teleport has a three-second cooldown and does not allow a spectator to re-enter combat, trigger target-selection logic or move entities out of the dedicated world.
- [ ] Track the source Sparklez stat for Pyrotechnic throws. Local survival-time and placement reward values remain Testplex-only ledger entries; no production Mineplex gem API or backend write is permitted.
- [ ] Fake test-player fixtures exercise attacks, dragon targeting, contact and spectator transitions but never earn persistent personal stats or rewards.
- [ ] Check scoreboards, damage counters, kit displays, participant snapshots and original locations are restored when a match finishes or is stopped.
- [ ] No dragons, sparkler entities, Frost bridge blocks, temporary Glider equipment, Tamer control state, scheduled tasks, target references, world items, cooldown state or temporary effects survive a session reset.

## Controlled-queue acceptance gate

Dragons is enabled for combined controlled testing because its source implementation, regression coverage and exact 40-map packaging comparison are present in the Paper 26.3 build. This is **not** a claim that live gameplay has already been accepted.

Before calling Dragons gameplay-accepted, load all 40 maps, verify RED-marker dragon spawns and run all six kits, the three community achievements, target/contact, spectator and cleanup scenarios above on the isolated live server. Fix each observed defect and rerun affected tests.