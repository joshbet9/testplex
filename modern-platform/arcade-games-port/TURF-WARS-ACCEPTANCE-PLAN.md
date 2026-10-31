# Turf Wars — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — standard Turf Wars and its separate Mixed Arcade variant are available in the shared controlled queue. Live map-by-map and multiplayer acceptance remain outstanding.  
**Legacy reference:** Mineplex Reborn `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/turfforts/TurfForts.java` and `kits/KitMarksman.java`, `KitInfiltrator.java`, `KitShredder.java`. The legacy implementation is registered as `GameType.TurfWars`.  
**Map source:** `update/maps/Turf Wars/` — all 42 original ZIP archives, including the three `TF_*.zip` archives.  
**Scope:** One Turf Wars game with standard and Mixed Arcade rulesets. The mixed variant must not alter standard timing, frontline speed or queue behaviour.

The exact archive list is enforced by `TurfWarsMapArchiveTest`, which compares source filenames with the resources copied into the Paper 26.3 plugin classpath.

## Original map inventory

All 42 original map files must remain selectable under the same `turf-wars` game ID:

1. `TF_DoubleForts.zip`
2. `TF_FireWater.zip`
3. `TF_FrostForts.zip`
4. `TurfWars_Agni Kai.zip`
5. `TurfWars_Atlantis.zip`
6. `TurfWars_Battle Of The Beasts.zip`
7. `TurfWars_Battle Of The Heroes.zip`
8. `TurfWars_Booksheleves.zip`
9. `TurfWars_Candyland.zip`
10. `TurfWars_Cathedral.zip`
11. `TurfWars_Church.zip`
12. `TurfWars_Coastal Cliffs.zip`
13. `TurfWars_Cyberspace.zip`
14. `TurfWars_Dispiriting War.zip`
15. `TurfWars_Domum.zip`
16. `TurfWars_Double Forts.zip`
17. `TurfWars_Dynasty.zip`
18. `TurfWars_Elemental Collision.zip`
19. `TurfWars_Fire and Water.zip`
20. `TurfWars_First Response.zip`
21. `TurfWars_Frost Forts.zip`
22. `TurfWars_Gardens Of Arcadia.zip`
23. `TurfWars_Hilly.zip`
24. `TurfWars_Ice Age.zip`
25. `TurfWars_Indiana.zip`
26. `TurfWars_Kiasfour.zip`
27. `TurfWars_Komorebi.zip`
28. `TurfWars_Library.zip`
29. `TurfWars_Lush Jungle.zip`
30. `TurfWars_Northern Lights.zip`
31. `TurfWars_Palace of Harmony.zip`
32. `TurfWars_Palace.zip`
33. `TurfWars_Pearlescent.zip`
34. `TurfWars_Sanctuary Of Ulu.zip`
35. `TurfWars_Sinkhole.zip`
36. `TurfWars_Stadium.zip`
37. `TurfWars_Temple Of The Lost Delta.zip`
38. `TurfWars_The Empress.zip`
39. `TurfWars_The Greatest Show.zip`
40. `TurfWars_Torrid.zip`
41. `TurfWars_Vetus.zip`
42. `TurfWars_Villa.zip`

Retain source filenames exactly. Similar archive names such as `DoubleForts` / `Double Forts` and `FireWater` / `Fire and Water` are separate source assets and must not be collapsed.

## Source-mechanics acceptance

### Map setup and teams

- [ ] Start from the shared Game Panel and `/arcadeport play turf-wars`; confirm the standard mode uses the normal Arcade queue and a dedicated world from the selected original archive.
- [ ] Resolve YELLOW turf floor markers, RED and BLUE team spawn markers, PINK and LIGHT_BLUE base markers, and ORANGE / CYAN kit-station markers from each selected archive's `WorldConfig.dat`.
- [ ] Derive the course axis and travel direction from the opposing spawn coordinates. Test maps that progress on X, Z and diagonal directions; do not assume one fixed north/south layout.
- [ ] Paint the original turf surface RED / BLUE according to nearest spawn and confirm all map bounds and initial frontline coordinates are derived from marker data.
- [ ] Validate both teams' spawns, bases and all three kit NPC positions on all 42 maps. A missing marker group must produce a clear map setup failure rather than a silently misconfigured match.
- [ ] Confirm rejoin, spectator compass, four-second respawn, inventory restore and per-world cleanup continue to use the shared lifecycle.

### Standard and Mixed Arcade phase timing

| Phase | Standard | Mixed Arcade |
| --- | ---: | ---: |
| First build | 40 seconds | 20 seconds |
| Later build | 20 seconds | 10 seconds |
| Combat | 90 seconds | 45 seconds |

- [ ] Standard begins with the source 40-second build, then cycles 20-second builds and 90-second combat rounds.
- [ ] Mixed Arcade uses 20-second first build, 10-second later builds and 45-second combat. Starting mixed must not mutate or shorten the standard queue.
- [ ] Combat is the only phase in which bows, arrows and melee can damage opponents. Building phases reject attacks and restore the correct build inventory.
- [ ] After each combat phase, living players receive the source 24 team-coloured wool for construction. Rejoined/spectating players receive their missed build supply on kit give without duplicating it.

### Build permissions and turf progression

- [ ] Players can place only their own team's wool on their own coloured territory, within original map bounds and the source five-block build height above the clay turf.
- [ ] Players cannot place blocks on enemy territory, neutral terrain, in spectator mode or outside the source region. Placed wool cannot be picked up or dropped as an item.
- [ ] Players cannot break the enemy team's wool by hand. Friendly wool and legal map interactions remain consistent with source rules.
- [ ] Arrows that hit wool remove the block, play the source block-hit feedback and credit the correct blocks-broken / bow-break mission progress. Arrow-impact cosmetics are presentation-only and must not change territory or combat logic.
- [ ] In Standard, a kill advances the frontline by one column in the first combat round, then two, three, four, etc. in later rounds. In Mixed Arcade, the progression is doubled (two, four, six, eight, etc.).
- [ ] Every newly captured column is repainted to the advancing team's clay colour, and temporary wool above the captured floor is cleared without modifying unrelated map blocks.
- [ ] Confirm the frontline moves along the derived X, Z or diagonal marker axis in both directions, and the game ends immediately when a team's frontline reaches the opposing base.
- [ ] Confirm the scoreboard reports remaining territory lines for both teams and the currently active Build / Combat countdown.

### Kits and combat

- [ ] **Marksman:** Bow; Constructor capacity eight wool, four-second regeneration; Fletcher capacity two arrows, two-second regeneration.
- [ ] **Infiltrator:** Iron sword and bow; Constructor capacity four wool, four-second regeneration; Fletcher capacity one arrow, eight-second regeneration. On enemy territory in combat the kit can infiltrate, accumulating Slowness over time; a kill shortens the slowness escalation interval. Other kits are repelled by enemy turf on the source two-second cooldown.
- [ ] **Shredder:** Bow; Constructor capacity six wool, four-second regeneration; Fletcher capacity two arrows, four-second regeneration; five-charge Barrage with one-second first charge and 250 ms follow-up charges.
- [ ] Verify source damage: Marksman / Infiltrator arrows are one-hit; Shredder arrows deal nine damage; melee deals six, or twelve with the Infiltrator's iron sword. No build-phase damage, friendly-fire bypass or self-damage is allowed.
- [ ] Verify the 15-minute match timeout uses remaining territory first and kill count as the tie-break; an exact tie remains unresolved rather than inventing a winner.
- [ ] Verify enemy-spawn boundary damage and off-territory/void handling are phase-correct (two damage during combat, lethal boundary damage during build where the source applies it), without applying that logic to spectators or valid team turf.
- [ ] Confirm block physics protection for ladders on ice, wool item-spawn suppression, comeback / behind-enemy-lines / block-break / block-place stat tracking and the mid-air kill mission.
- [ ] Test disconnect and rejoin during Build and Combat, death/respawn, team changes, game stop and immediate rematch. All player snapshots, kits, wool supplies, scoreboard state, scheduled timers and temporary turf colours must be restored.

## Controlled-queue acceptance gate

Turf Wars is enabled for controlled combined testing because the standard and Mixed Arcade rules, all three source kits, source-map marker rules, territory progression helpers and a substantial deterministic regression suite are present. This promotion is **not** live gameplay acceptance. Test every map with both teams, at least two players, both variants, each kit, a frontline win and a timeout resolution. Repeat axis-sensitive maps, enemy-spawn edges, wool placement/arrow clearing and disconnect/rejoin cases on the isolated Paper 26.3 server.
