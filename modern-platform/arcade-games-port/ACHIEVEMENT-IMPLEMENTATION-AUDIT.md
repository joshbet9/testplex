# Achievement detection and achievement-kit integrity audit — review draft

**Status:** Static source audit and implementation checklist, 2026-11-06. The historical findings below describe the **pre-implementation baseline**, not the current branch. Josh approved the listed kit prerequisites and requested repairing achievement detection. All changes remain in a draft PR until gameplay verification. This is a companion to `ACHIEVEMENT-AND-KIT-REVIEW.md`.

## Implementation checkpoint (draft, not yet live-verified)

| Area | Progress and remaining checks |
| --- | --- |
| Purple kit entitlements | Reviewed prerequisites for **20** existing purple designs are declared and checked at lobby selection, shared match setup, and affected specialist selectors. **Chris P. Bacon** is retained and unlocks from `flash-fried` + `raw-pork`, as Josh approved. Test every command/NPC/direct-kit route in Paper; Monster Maze external kit manager still needs a direct bypass check. |
| Bacon Brawl | All four additional approved achievements have event-derived detectors: Iron Hog **8** spaced guard blocks per game; Extra Crispy **5** distinct ignited opponents per game; Variety Pack tracks a win with all five kits across games; Quadruple Serving tracks **4** credited knock-off KOs with a **new 10-second window per kill**. Existing 10 awards/records are retained; test live multiplayer edge cases. |
| Kit display / `/stats` | Locked-kit progress and required feats appear in kit menus. Hovering over a prerequisite achievement now adds a gold **Unlocks:** line naming the kit. The separate `/stats` layout PR owns positioning, rarity sorting and 14-achievements-per-page pagination. |
| Sheep Quest | All six formerly unawarded catalogue IDs now have event-derived conditions and unit tests; team winners credited together. Needs multiplayer timing and carry/kill attribution playtests. |
| Speed Builders | All four formerly unawarded IDs now have conditions using cumulative time tie-break and final-round precision. Needs 16-player mirrored-arena verification and test-player exclusion validation. |
| Heroes of Gwen | `moba-master` records wins on the existing ten heroes; `gold-mine` measures 60 seconds holding both beacons; `know-your-enemy` uses role-matched kill evidence. Needs role-vs-role multiplayer playtest. |
| Wizards | Four of six missing IDs now have detectors from successful casts, spell categories, levels, and defeats. **Still missing:** multi-kill attribution for `trifecta` and self-spell finalisation for `spell-it-out`. These must not be granted by raw kill-count approximations. |
| Wither Assault | Both all-map wins now track the human/Wither role separately using stable archive identities. **Still missing:** exact Labyrinth-top detector for `eye-candy` and teammate-following miner-tunnel detector for `split-up`. |
| Snake | The progression bridge no longer overwrites previously earned achievements with `false` when a later snapshot omits them. Still inspect the standalone Snake producer and verify all nine mapped identifiers end-to-end. |
| Unreviewed award candidates | Existing code references across other games are **not yet proof of valid detection**. Each game needs positive, negative and no-bot test coverage before sign-off. New achievement concepts and new achievement kits for games lacking them are still design proposals, not implemented here. |

## Agreed constraints

- Keep all **252 original achievements** and their earned records, including Snap's 20 and Super Sploor's 15. Bacon Brawl's four approved additions and Constructor's approved Bomb Lobbers achievement raise the current total to **257**; **170** further carefully designed achievements remain to bring each of 30 game families to **at least 14**, with 14 per `/stats` page; never archive the seven above-14 achievements.
- Achievement-kit unlock rules must be **specific to each game and kit**. A purple NPC, an unlock requirement, and an ability must correspond to the same authoritative entitlement at selection, save, match start, and respawn.
- No award can require using the locked kit to earn the kit. Do not automatically increase a legacy kit's unlock requirement just because the game gets seven more achievements. Do not rewrite completed achievements or milestone history.
- All proposed awards require a detector and dedicated tests; live rarity calculation must exclude bot/test accounts and ineligible games.

## Confirmed integration findings (prioritised)

| Priority | Observation and evidence | Recommended resolution |
| --- | --- | --- |
| P0 | `ModernArcadeLobbyRuntime.onMenuClick` calls `manager.selectKit(p, kit)` **then unconditionally writes** `lobby-kit-preferences.<uuid>.<game>` and confirms selection. `selectKit` is `void`, even if its kit guard rejected the request. The rejected selection therefore appears successful and can persist; Sheep Quest, Wither Assault, Monster Maze, and Super Sploor read saved preferences when starting matches. | Return an explicit **accepted/rejected** result from a single kit-selection service; save, ding, refresh scoreboard and close the UI **only on success**. Validate again when creating the match and respawning. |
| P0 | `ArcadeLobbyKitCatalogue` labels **20** options `Unlock.ACHIEVEMENT`, but `ArcadePortManager.achievementKitUnlock` declares only **four** mappings: Death Tag Turtle/Trickster, Sheep Quest Shepherd, and Wither Assault Warden. The other purple options do not have this shared lobby guard. Some (Spleef Gust, Micro Battles Dasher, Runner Switcher, Turf Wars Fortifier) have separate checks in their selector; the rest need an authoritative, audited rule. | Centralise a **per-kit declarative unlock predicate** (single award, named bundle, or stable mastery set) and always invoke it in lobby, command, and match setup. Keep a documented exception for admin testing without granting regular-player unlocks. |
| P0 | Sheep Quest **Shepherd** requires `cheeky` through `achievementKitUnlock`, but there is **no award of `cheeky`** in the inspected runtime. Its catalogue defines it as capturing five sheep in the final ten seconds. | Implement and test the exact end-of-round event, or propose a different obtainable Shepherd unlock before enabling the lock. Preserve any existing `cheeky` flags. |
| P1 | The shared kit lock and display currently hard-code `local-achievements.<uuid>.<game>.<id>`, while Bacon Brawl, Bomb Lobbers, Micro Battles and Gladiators award through `test-achievements...` paths. Plexball achievements live in separate `plexball` progression data. Other game-specific records exist (Snake). | Create one **achievement-earned query** that delegates to the actual persistence backend, including distinct Plexball/Snake adapters, and use it for kits, `/stats` and rarity. |
| P1 | At least **23 declared achievement IDs** have no matching literal award ID in the inspected game implementations: Sheep Quest 6, Wizards 6, Wither Assault 4, Speed Builders Arcade 4, Heroes of Gwen 3. This is a *static missing-trigger indicator*, not a claim that every other award is correct. | Build missing detectors and event-state tracking, then test each with a real-player positive case, a failing negative case and a bot/test exclusion. See the precise IDs below. |
| P1 | `ArcadePortManager.awardCatalogAchievement` does not itself check `testPlayers.isTestPlayer`; several specialist awarders and stats writers do. An unguarded caller can therefore persist a test player's achievement. | Enforce eligible-real-player checks in **every shared award entry point**, with a separate clearly marked simulation/debug route that cannot count toward actual progression or rarity. |
| P1 | Runner Switcher currently requires **all seven catalogued Runner awards plus three legacy `test-achievements.runner` entries**. Adding seven more catalogue achievements would silently make Switcher much harder to earn. Its lobby lore says only “every Runner achievement.” | Freeze the intended stable unlock set or explicitly approve a redesigned mastery criterion. Expose the exact progress as `N / M` in the lobby and stats. |
| P1 | Existing kit selection rules are scattered: Runner/Spleef/Micro Battles/Turf Wars have local predicates, Death Tag/Sheep Quest/Wither Assault use the shared map, and Snap/Plexball/Super Sploor and other purple options have no achievement check in the inspected `selectKit` methods. | Test **all input routes** (NPC click, kit menu, command, reconnect, future round, live-game swap) and replace scattered checks with the shared entitlement service. |
| P2 | **Chris P. Bacon** was formerly treated as removed in the audit, but Josh confirmed the redesigned kit must stay. Its catalogue and ability remain, and `flash-fried` + `raw-pork` are now its approved gate. | Keep the redesigned kit and its history intact; exercise both prerequisites through GUI, command and next-match guards. |
| P2 | Several games maintain more than one achievement-facing view or data set. Runner has three legacy awards outside its seven-entry catalogue; Sneaky Assassins additionally displays an unrelated `the-masters-master` 1/1 counter. Dragon Escape has its own stats branch. | Unify the *visible* catalogue and history without dropping valid legacy records. The separate `/stats` PR owns layout, dynamic rarity sorting and pagination; this PR supplies canonical records and detector tests. |

### Exact IDs without an observed awarding call

These IDs are declared in `ArcadeProgressCatalog` but have no literal runtime award reference after excluding the catalogue itself:

| Game | IDs to wire or verify |
| --- | --- |
| Sheep Quest (6) | `whats-a-sheep`, `cheeky`, `yeet`, `unconventional-but-effective`, `solo-carry`, `the-biggest-loser` |
| Wizards (6) | `youre-a-wizard-harry`, `strong-willed-small-minded`, `ding-dong`, `trifecta`, `one-trick-wizard`, `spell-it-out` |
| Wither Assault (4) | `eye-candy`, `split-up`, `wither-world`, `its-a-mans-world` |
| Speed Builders Arcade (4) | `speedrunner`, `unlucky`, `bad-builds`, `right-on-time` (the manager's `speedrunner` *stat* is not an achievement award) |
| Heroes of Gwen (3) | `moba-master`, `gold-mine`, `know-your-enemy` |

`Monster Maze` is **not** included in this count: the inspected Arcade module exposes `recordMonsterMazeAchievement(id, achievementId)`, which accepts all nine catalogue IDs, but the upstream Monster Maze event producer was not available in the sparse checkout. Its nine source triggers must be inspected before declaring them working or missing. `Snake` likewise accepts achievement IDs from an external module, and requires an end-to-end bridge test.

## Game-by-game review matrix

`Award candidate` means an award path or backend reference exists, **not** a passed gameplay test. `Missing IDs` refers to the precise table above. `Bridge` means a delegated module/backend needs real-client checks. The `+` column is *new achievements still to propose*, not approval to create them.

| Game family | Existing | + | Purple achievement kits in lobby | Current detection / kit audit |
| --- | ---: | ---: | --- | --- |
| Bacon Brawl | 14 | 0 | Brutus Bacon, Chris P. Bacon | Four new approved detectors: Iron Hog (8 guards), Extra Crispy (5 unique ignitions), Variety Pack (wins with all 5 kits), Quadruple Serving (4 knock-off KOs, refreshing 10 seconds after each). Both purple kits have specific noncircular unlocks. Got em’ recovery timing and Mama Piggles blast combo attribution are repaired; needs live-player validation. |
| Bomb Lobbers | 14 | 0 | Constructor | Three-kit redesign: Acrobat (renamed Jumper), Armorer, achievement-locked Constructor (replaces Waller); Pitcher removed. Added `not-on-my-watch` for three distinct enemy TNT blocked. Cleaned up Technical killing-TNT attribution, Over 9000 genuine power-up use and Grenade from the Grave direct/explosion post-death kills. Constructor's **four-achievement gate is approved**; Blast Proof now requires five lifetime Armorer wins. Validate structures and counters with live Paper clients. |
| Castle Assault | 6 | 8 | None | Six award candidates in `CastleAssaultHeroesOfGwenRuntime`; propose one side-grade kit only after balancing with existing five. |
| Death Tag | 10 | 4 | Turtle, Trickster | Both have named shared unlocks (`turtle-shell`, `smoke-and-mirrors`); verify no bypass from menu save or live selection. |
| Dragon Escape | 9 | 5 | Digger | Nine award candidates; **Digger has no enforced shared unlock**. `learn-to-code` requires using Digger, so cannot be Digger's prerequisite. Builder/Disruptor/Adventure changes require independent playtest. |
| Dragons | 11 | 3 | Tamer (planned/needs verification) | Award candidates in custom Dragons awarding paths; **Tamer is the intended achievement kit**, not a request for a new one. Verify its selection restriction and real gameplay. |
| Draw My Thing | 5 | 9 | None | Five award candidates; drawing is a fixed-role game, so choose between a gameplay utility and a prestige cosmetic first. |
| Evolution | 7 | 7 | Health Harvester (legacy; needs verification) | Seven award candidates, but note proposed removal of Spider/Ability Assassin may make `ability-assassin` stale. Verify whether the original Health Harvester achievement kit has been ported, before considering changes. |
| Gladiators | 9 | 5 | None | Award candidates via special Gladiators keys; kitless equipment/bracket format—prefer selectable prestige loadout cosmetic unless competitive side-grade is explicitly wanted. |
| Heroes of Gwen | 6 | 8 | None | **Three missing award IDs**; a new hero is a substantial balance change, so review whether an achievement hero or cosmetic variant is appropriate. |
| Hoops Remastered | 9 | 5 | None | Nine award candidates in `HoopsRemasteredRuntime`; fixed sporting role—consider prestige move effect or carefully balanced ability. |
| Master Builders | 4 | 10 | None | Four award candidates; avoid an achievement tool granting competitive votes/building advantage. Cosmetic prestige may fit. |
| Micro Battles | 9 | 5 | Dasher | Local `speedster` award and in-selector Dasher guard exist; UI/shared map are not the same rule. Test award after a genuine sub-90-second win and no menu preference bypass. |
| Milk the Cow | 7 | 7 | None | Seven award candidates including `slaughterhouse`/`got-the-chops`; fixed asymmetric cow/human roles suggest a visual prestige reward. |
| Monster Maze (Original/Speed/Modern) | 9 | 5 | Repulsor, Maverick | **Bridge** to Monster Maze; neither purple kit has a shared entitlement. Inspect its actual kit ability and award emitters before selecting requirements. |
| One in the Quiver | 8 | 6 | Ninja | Award candidates through Quiver enum and catalogue; **Ninja has no entitlement**. |
| Paintball | 5 | 9 | Sniper | All five award calls found; **Sniper has no entitlement**; choose an earnable Rifle/Shotgun challenge, not a Sniper-only one. |
| Plexball | 9 | 5 | Skyguard | Nine specific award calls in `PlexballGame`, stored in **separate progression file**; no Skyguard entitlement. |
| Runner | 7 (+3 legacy) | 7 | Switcher | Seven catalogue award candidates plus three legacy awards; **all-10 gate** exists in selector but is dynamic for catalogue additions. |
| Sheep Quest | 12 | 2 | Shepherd | **Six missing IDs, including its `cheeky` unlock**; shared lobby gate exists but must not make the kit permanently unobtainable. |
| SkyWars | 3 | 11 | None | Three award candidates; propose a distinctive kit only after comparing five element roles. |
| Snake | 9 | 5 | None | **Bridge** from Snake module; shared `recordSnakeStats` persists supplied flags but rewrites all nine booleans on each call. Test history retention and identity/case of IDs. |
| Snap | 20 | 0 | Understudy | All 20 award calls found; **Understudy has no unlock check**. `method-actor` requires Understudy, so would be a circular prerequisite. Page 2 must show six. |
| Sneaky Assassins | 6 | 8 | Briber | Six award candidates with local-achievements keys; **Briber has no entitlement**. Reconcile extra legacy 1/1 stats counter. |
| Speed Builders Arcade | 4 | 10 | None | **No runtime award of any of its four declared IDs found**; scores/perfects are tracked separately. Prestige cosmetic or neutral build tool preferred. |
| Spleef | 7 | 7 | Gust | `praying-to-the-gods` gate exists in selector, but lobby item says Click to select regardless of progress and menu saves rejections. |
| Super Sploor | 15 | 0 | Dualies | All 15 award calls found; **Dualies has no entitlement**. `master-of-the-palette` requires winning with Dualies, so cannot unlock it. Page 2 must show one. |
| Turf Wars | 9 | 5 | Fortifier | Named `genius-mathematician` gate exists in selector and UI; menu still writes preference after refusal. |
| Wither Assault | 8 | 6 | Warden | **Four missing IDs**; `savior` award candidate exists and Warden shared gate exists. Test 5 revives on eligible opponents. |
| Wizards | 6 | 8 | None | **No runtime awards for any of six declared IDs found** in `WizardsGameManager` or `WizardsProgressStore`; define conditions using spell cast/kill data first. |

## Kit-specific unlock design for review — **not final selections**

Use the following **three predicate types** depending on gameplay, not a universal “complete all 14” requirement:

1. **Signature feat (one named achievement)** for a simple, thematic kit: e.g. Spleef Gust = `praying-to-the-gods` (retain), Micro Battles Dasher = `speedster` (retain), Turf Wars Fortifier = `genius-mathematician` (retain), Wither Assault Warden = `savior` (retain), Paintball Sniper = a Rifle achievement such as `share-your-screen`.
2. **Curated feat bundle (two or three stable IDs)** for versatile or strong kits: e.g. Dragon Escape Digger = `to-hell-and-back` **and** `paralympics` (obtainable as Jumper/Warper/Disruptor); Super Sploor Dualies = `reclaimer` **and** `team-palette` (both attainable before Dualies); Snap Understudy = `camera-shy` **and** `double-take` (not `method-actor`). These are *examples for Josh's approval*, not promised balance.
3. **Stable mastery set** where the established design demands broad achievement mastery (Runner Switcher). Explicitly enumerate the legacy set or a versioned set; new achievements should not silently modify it. Show progress and never require an achievement awarded exclusively with the locked kit.

For **games without traditional kits**, preserve competitive parity by default: propose a distinctive **cosmetic prestige** for creative and fixed-role games (Draw My Thing, Master Builders, Hoops, Gladiators, Milk the Cow, Speed Builders), and discuss genuine new *side-grade* kits for conventional class-based games (SkyWars, Dragons, Castle Assault, Evolution). For Heroes of Gwen, adding an achievement-only playable hero changes the entire hero balance and should be an explicit choice.

### Proposed predicates for **each of the 20 existing purple-labelled options**

This table is a discussion starter. It deliberately favours accomplishments that can be earned using **other, already available** kits. A requirement marked *retain* is the current intended rule; still fix the enforcement path and run its award tests.

| Game / kit | Proposed prerequisite(s) | Reason / risk to resolve |
| --- | --- | --- |
| Bacon Brawl — Brutus Bacon | `king-of-bacon` + `pigs-in-blankets` | Basic mastery and a high-kill match; neither needs Brutus. |
| Bacon Brawl — Chris P. Bacon | `flash-fried` + `raw-pork` (**approved**) | Both earned without Chris; his new `extra-crispy` achievement is intentionally **not** a prerequisite, avoiding a circular lock. |
| Bomb Lobbers — Constructor | **Approved:** `two-birds-one-stone` + `whats-a-double-jump` + `blast-proof` + `jelly-skin` | Four independent feats obtainable without Constructor. Blast Proof requires five wins as Armorer; prior earned records remain true. Decide whether to grandfather prior Waller entitlement before merge. |
| Death Tag — Turtle | `turtle-shell` (*retain*) | Existing obtainable runner-chaser challenge. |
| Death Tag — Trickster | `smoke-and-mirrors` (*retain*) | Existing runner win challenge. |
| Dragon Escape — Digger | `to-hell-and-back` + `paralympics` | Course mastery without Leap, obtainable without Digger; **do not use Digger-only `learn-to-code`**. |
| Micro Battles — Dasher | `speedster` (*retain*) | The one-game, under-90-second goal already named in its catalogue. |
| Sheep Quest — Shepherd | `cheeky` (*retain if detector is fixed*) | Repair the missing award first; otherwise no legitimate player can unlock it. |
| Monster Maze — Repulsor | `blizzard` + `stampede` | Demonstrate Cryo Blitz/Body Rush mastery, provided both source detectors are verified. |
| Monster Maze — Maverick | `perfect-run` | All first-pad mastery using other Maze kits; confirm submode eligibility. |
| One in the Quiver — Ninja | `sharpshooter` + `perfectionist` | Accuracy and survival, neither requiring Ninja. |
| Paintball — Sniper | `share-your-screen` | Accurate Rifle match to demonstrate precision before Sniper. |
| Runner — Switcher | **Freeze existing 7 catalogue + 3 legacy IDs** (*retain pending review*) | Preserve today's all-achievements requirement and acquired unlocks; seven new proposals must not auto-tighten it. |
| Wither Assault — Warden | `savior` (*retain*) | Five revives in one match; prove eligibility and revive attribution. |
| Sneaky Assassins — Briber | `prosecutor` + `idiot` | Show role mastery as Revealer and Escape Artist before Briber. |
| Spleef — Gust | `praying-to-the-gods` (*retain*) | Already guarded on direct selection. |
| Turf Wars — Fortifier | `genius-mathematician` (*retain*) | Already guarded on direct selection. |
| Super Sploor — Dualies | `reclaimer` + `team-palette` | Territory + assists achievable without Dualies; **not `master-of-the-palette`**, which requires Dualies. |
| Plexball — Skyguard | `sticky-gloves-three` + `sweet-spot-75m` | Catching and batting skill, both possible before Skyguard. Resolve separate save backend. |
| Snap — Understudy | `camera-shy` + `double-take` | Survival and quick photos achievable with starting kits; **not `method-actor`**, which requires Understudy. |

Before approving a kit, record `game`, `kit`, `mechanics`, `predicate version`, `prerequisite achievement IDs`, `can-earn-with-base-kit`, `persistence adapter`, `on-join/selection/rejoin guard`, `test cases`, and `unlock UI lore`. The remaining 171-achievement proposal must include exact trigger events/state resets, not just descriptions.

## Acceptance tests required before implementation is called complete

1. **Catalogue integrity:** 30 families; each has >=14 after approved additions; stable IDs; no duplicates; legacy unlock keys recognised; 14 items/page, with every overflow achievement reachable.
2. **Detector contract:** per-achievement positive, negative, disallowed bot, and non-completed-round cases; tests for multi-player credit attribution, self-damage, AFK accounts, same-player farming and team switching where relevant.
3. **Persistence:** award once; reconnect/restart; legacy-key migration/read-through without deleting records; canonical `/stats` and kit entitlement agree, including Plexball/Snake.
4. **Unlock enforcement:** run matrix against NPC, GUI, command, reload, reconnect, spectator, active game, next match and admin test bypass. Rejection must never save preferences or play successful selection feedback.
5. **Circularity scan:** every locked kit must be unlockable using only kits available beforehand; newly proposed awards never create retroactive lockouts for previously earned kits.
6. **Functional playtesting:** multiplayer Paper 26.3 for every gameplay-changing achievement kit, bot parity, no collision/hitbox side effects, and source/modern map coverage.

### Specific source entry points reviewed

- `ArcadeProgressCatalog.java`: 27 families / 208 declarations; `CameraShyProgressCatalog.java` (20), `SuperSploorProgressCatalog.java` (15), `PlexballAchievement.java` (9).
- `ArcadeLobbyKitCatalogue.java`: kit metadata / labels; `ModernArcadeLobbyRuntime.java`: kit item lore and click handler.
- `ArcadePortManager.java`: `selectKit`, specialist selectors, `achievementKitUnlock`, `awardCatalogAchievement`, `arcadeAchievementConfigKey`, `recordSnakeStats`, `recordMonsterMazeAchievement`, match stats and `/stats` rendering.
- `ArcadePortRules.java`: legacy test-achievement key formats and kit normalisers; `CastleAssaultHeroesOfGwenRuntime.java`, `HoopsRemasteredRuntime.java`, `SneakyAssassinsMechanics.java`, `PlexballGame.java`, `CameraShyGame.java`, `SpeedBuildersGame.java`, `WizardsGameManager.java`, `WizardsProgressStore.java`.

**Scope limit:** This is a static code-path audit, not a live Paper server verification. Named award calls do not guarantee a trigger is reachable or correct; missing literal IDs warrant targeted review because dynamically constructed IDs or external modules can evade this search. The test checklist above is the gate to claim verified game behaviour.
