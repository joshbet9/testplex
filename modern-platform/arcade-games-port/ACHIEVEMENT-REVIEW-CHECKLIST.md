# Achievement review and implementation checklist

Counts below come from the current `ArcadeProgressCatalog`; they are catalogue entries, not a claim that every detector has been re-audited. Monster Maze is one game with Original, Speed, and Modern selectable modes.

| Game | Current achievements | Kit-specific coverage | Design approved | Implementation complete | Tests passed | Remaining blockers |
|---|---:|---|---|---|---|---|
| Bacon Brawl | 14 | Review complete earlier; implementation audit pending | Yes | Pending audit | Pending audit | Reconcile this branch with prior approved design and verify detectors |
| Bomb Lobbers | 14 | Review complete earlier; implementation audit pending | Yes | Pending audit | Pending audit | Reconcile this branch with prior approved design and verify detectors |
| Castle Assault | 14 | Review complete earlier; implementation audit pending | Yes | Pending audit | Pending audit | Reconcile this branch with prior approved design and verify detectors |
| Death Tag | 14 | Review complete earlier; implementation audit pending | Yes | Pending audit | Pending audit | Reconcile this branch with prior approved design and verify detectors |
| Dragon Escape | 14 | Review complete earlier; implementation audit pending | Yes | Pending audit | Pending audit | Reconcile this branch with prior approved design and verify detectors |
| Dragons | 11 | 6 selectable kits need final detector audit | Yes | No | No | Approved 14-achievement design is not yet reflected in the current catalogue |
| Draw My Thing | 5 | Not audited in this achievement pass | Not yet | No | No | Review source/current kits and achievement detectors |
| Evolution | 7 | Not audited in this achievement pass | Yes | No | No | Implement approved changes for Health Harvester and Neanderthal and remove rejected entries |
| Gladiators | 9 | Not audited in this achievement pass | Yes | No | No | Implement approved achievements; preserve existing modes and milestones |
| Heroes of GWEN | 15 | 9/9 selectable heroes have a specific achievement | Yes | Yes | Yes | Live Paper 26.3 gameplay acceptance remains |
| Hoops Remastered | 9 | Not audited in this achievement pass | Not yet | No | No | Begin next design review |
| Master Builders | 4 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Micro Battles | 9 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Milk the Cow | 7 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Monster Maze (Original, Speed, Modern) | 9 | Not audited in this achievement pass | Not yet | No | No | Review each mode's shared and mode-specific progression conditions |
| One in the Quiver | 8 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Paintball | 5 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Plexball | 0 | Not audited in this achievement pass | Not yet | No | No | No achievement catalogue entry yet |
| Runner | 7 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Sheep Quest | 12 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| SkyWars | 3 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Sneaky Assassins | 6 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Snake | 9 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Snap | 0 | Not audited in this achievement pass | Not yet | No | No | No achievement catalogue entry yet |
| Speed Builders | 4 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Spleef | 7 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Super Sploor | 0 | Not audited in this achievement pass | Not yet | No | No | No achievement catalogue entry yet |
| Turf Wars | 9 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Wizards | 6 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |
| Wither Assault | 8 | Not audited in this achievement pass | Not yet | No | No | Review source/current achievements, kits, and exact detectors |

## Heroes of GWEN completed work

- 15 catalogue achievements: 6 existing plus 9 approved hero-specific achievements.
- Every new hero achievement description begins with `As <Hero>`.
- Rowena's `Bombardment!` requires the same Rowena to land Light Arrow damage on both enemy towers and both towers to be destroyed. Any player tower damage with another attack voids it; minion damage remains allowed.
- Bob Ross is disabled in the lobby choices and hero normalization. His original source paths are retained in `HEROES-OF-GWEN-BOB-ROSS-REFERENCE.md` for a future complete port.
- Existing role-win milestone definitions and thresholds are unchanged; match wins now increment their existing role-specific counters.
- Achievement awards skip test bots and debug sessions.
- Automated verification: full Arcade module suite, 1,020 tests passed on Java 25.0.5.
- Not live-tested on Paper 26.3.
