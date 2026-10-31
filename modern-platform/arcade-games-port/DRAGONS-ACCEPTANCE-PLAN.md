# Dragons (Solo) — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — enabled in the shared controlled queue. Live multiplayer acceptance is still outstanding.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/dragons/Dragons.java`, `MineplexDragon.java` and the three `kits/Kit*.java` files.  
**Map source:** `update/maps/Dragons/` — 40 original ZIP archives.  
**Scope:** Solo Dragons only. The separate legacy **Dragons Teams** variant is not included or implied to be implemented.

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

- [ ] Start `dragons` using the shared Arcade Game Panel and `/arcadeport play dragons`; confirm it is the SoloGame ruleset and uses the ordinary shared Session, spectator, stop and lobby-restore lifecycle.
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
- [ ] All three kits are selectable from the common lobby pedestal and `/arcadeport kit dragons-<kit>`; kit changes lock at match start and do not duplicate kit gear after respawn/reconnect.

### Dragon Flare (modern test-queue extension)

- [ ] Coward receives one flare charge, Marksman three and Pyrotechnic two. Charges cannot be dropped, moved to containers or transferred to another player.
- [ ] A flare protects a 24-block horizontal radius and a 24-block vertical envelope for six seconds. Check the exact boundary and just-outside values.
- [ ] Existing dragons retarget immediately when entering the protected area; future target selection and contact checks also respect the flare.
- [ ] Sparkler attraction does not pull a dragon into a protected location.
- [ ] Test all six server-visible effects (Classic, Ember, Frost, Ender, Solar and Aurora) from another player and spectator. Effects must not change protection, charge consumption or target physics.
- [ ] Verify a second charge cannot extend or duplicate the active state unless that is the explicitly implemented rule; expiry and re-use feedback must be consistent.

### Spectators, stats and cleanup

- [ ] Living players receive source-style opponent/dragon awareness; eliminated spectators can browse surviving players and teleport only to valid living targets.
- [ ] Spectator teleport has a three-second cooldown and does not allow a spectator to re-enter combat, trigger target-selection logic or move entities out of the dedicated world.
- [ ] Track the source Sparklez stat for Pyrotechnic throws. Local survival-time and placement reward values remain Testplex-only ledger entries; no production Mineplex gem API or backend write is permitted.
- [ ] Fake test-player fixtures exercise attacks, dragon targeting, contact and spectator transitions but never earn persistent personal stats or rewards.
- [ ] Check scoreboards, damage counters, kit displays, participant snapshots and original locations are restored when a match finishes or is stopped.
- [ ] No dragons, sparkler entities, flare protection, scheduled tasks, target references, world items, cooldown state or temporary effects survive a session reset.

## Controlled-queue acceptance gate

Dragons (Solo) is enabled for combined controlled testing because its source implementation, regression coverage and exact 40-map packaging comparison are present in the Paper 26.3 build. This is **not** a claim that live gameplay has already been accepted.

Before calling Dragons gameplay-accepted, load all 40 maps, verify RED-marker dragon spawns and run the kit, target/contact, flare, spectator and cleanup scenarios above on the isolated live server. Fix each observed defect and rerun affected tests. The separate legacy Dragons Teams variant remains excluded from the catalogue and test commands unless it receives its own port and acceptance work.
