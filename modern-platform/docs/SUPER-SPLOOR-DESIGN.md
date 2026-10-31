# Super Sploor — Paper 26.3 Game Design Specification

**Status:** Design baseline for implementation  
**Game ID:** super-sploor  
**Platform:** Testplex Modern Arcade, Paper 26.3 / Java 25  
**Source reference:** Mineplex NANO Sploor  
**Design influence:** Splatoon-style turf combat, ink economy and movement  
**Initial mode:** Turf War (Red vs Blue)

This document defines the intended gameplay before implementation. It is not a claim that Super Sploor is already playable or ready for testing. The original NANO Sploor source remains the reference for the mechanics explicitly retained below; the new weapons, ink economy, abilities, scoring refinements and progression are Super Sploor additions.

## 1. Game identity and design pillars

Super Sploor is a team-based territory shooter. Players use paint weapons to claim neutral ground, overwrite enemy territory, splat opponents and travel rapidly through their own paint. Combat creates openings, but territory wins the match.

The game must deliver four things:

1. **Painting matters.** Every primary weapon and ability has a useful territory-painting role.
2. **Ink changes movement.** Friendly paint is infrastructure: it enables fast travel, wall climbing and ammunition recovery.
3. **Kits create different jobs.** A Roller, Charger and Lobber must feel meaningfully different, not like reskinned projectile launchers.
4. **Objectives beat kill farming.** Kills and assists help a team take space, but are not a substitute for owning territory.

All five kits are available to every player from the beginning. There are no damage, health, fire-rate, ink-capacity or movement upgrades earned through progression. Achievements and milestones are recognition and cosmetic progression only.

## 2. Match format and lifecycle

| Setting | Initial specification |
| --- | --- |
| Mode | Turf War |
| Teams | Red and Blue |
| Team size | 4–8 players per team |
| Maximum players | 16 |
| Minimum players | 4 humans for a normal match |
| Match duration | 3 minutes |
| Respawning | Enabled |
| Splat recovery | 2.5-second respawn |
| Spawn protection | 2 seconds; firing disabled during protection |
| Winner | Team controlling the greater weighted paintable territory at time |
| Exact tie | 30-second overtime; first team to lead by 1 percentage point wins; otherwise draw |
| Map | Dedicated Super Sploor arena; original Excavation map is first import candidate |

The match uses the normal Testplex Arcade lifecycle: queue, kit selection, preparation, match, result, spectator handling and teardown. It must not create a separate Bukkit plugin lifecycle, private lobby, or independent server.

### Match phases

- **Recruit / kit selection:** players choose one of the five kits. Team assignment is balanced by the normal Arcade team manager.
- **Prepare:** 5 seconds, with controls and weapons shown. Players cannot paint or damage opponents.
- **Live:** 180 seconds. Players paint, contest territory, splat opponents and use abilities.
- **Final 30 seconds:** a clear countdown and urgent match announcement; HUD score updates remain live.
- **Overtime:** only when the two weighted team scores are exactly equal at 180 seconds. The first territory update that creates a lead of at least 1 percentage point wins. Overtime is capped at 30 seconds; an exact tie at the cap is a draw.
- **End / cleanup:** announce score and winner, show personal results, restore all modified arena blocks, clear projectiles and temporary objects, then return players through the shared Arcade lifecycle.

A player who disconnects must be removed from the active roster and their team score must remain valid. Rejoining should follow the shared Arcade reconnect policy; do not create duplicate team members or retain stale projectiles, ink effects or player state.

## 3. NANO Sploor mechanics to preserve

The existing Mineplex NANO Sploor implementation is the source reference. Preserve its recognizable identity:

- Two teams, Red and Blue.
- A paint gun fires paint projectiles.
- Paint impacts colour nearby wool and stained-glass surfaces.
- Painting an enemy-coloured block transfers it to the attacker's team.
- The team with the most painted territory wins.
- Hitting an opponent progressively marks their armour; four successful marks splat them.
- Switching away from the paint weapon can activate Squid Mode when standing on friendly paint.
- Squid Mode provides fast movement and lets players climb walls made from their team's paint.
- The original round was two minutes; Super Sploor extends this to three minutes to support the expanded kit and objective loop.

The original source uses a 150 ms firing recharge, a three-block paint radius around projectile impacts, four armour pieces as the visual hit indicator, and a final splat splash. These are useful starting references, not values that must be copied unchanged into the new balance model.

The new implementation must retain the feeling of the NANO original: bright colour ownership, quick paint impacts, a visible four-hit splat state, and the satisfying transition from running to Squid Mode.

## 4. Territory and paint rules

### 4.1 Block ownership

Every map defines an explicit set of paintable blocks. Each eligible block has exactly one state:

- Neutral
- Red-owned
- Blue-owned

A successful paint action changes the ownership of eligible blocks in its impact shape. Painting neutral territory claims it. Painting enemy territory transfers it. Painting a block already owned by the attacking team has no scoring value.

Each eligible block contributes one territory unit by default. The map author controls which blocks are eligible; decorative ceilings, hidden backing blocks, spawn-room interiors, protected structures and inaccessible decoration should not be paintable. Avoid scoring exploits caused by large decorative surfaces or inaccessible blocks.

The map loader must validate that both teams have a fair amount of reachable paintable territory. The denominator is the total number of eligible territory units in the arena, not the number of blocks currently painted.

### 4.2 Scoring

At every point in the match:

- Red percentage = Red-owned territory units / all eligible territory units.
- Blue percentage = Blue-owned territory units / all eligible territory units.
- Neutral percentage = Neutral territory units / all eligible territory units.

The three percentages should total 100%, allowing for display rounding. The leading team is the team with more owned territory units. Neutral territory is not silently awarded to either team.

The HUD must display both team percentages and neutral percentage or a clear remaining-neutral indicator. A kill never directly adds territory points. Repainting a block that your team already owns never adds score, special charge or milestone progress.

### 4.3 Paintable surfaces and block restoration

The first implementation should support the original map palette (wool and stained glass) and modern equivalents (concrete, terracotta and stained glass) through a central material/colour mapping. Do not hard-code paint logic separately into every weapon.

The territory manager must:

- Keep a single authoritative ownership record for every eligible block.
- Update team totals incrementally when ownership changes; do not rescan the entire arena every tick.
- Batch visual block updates where possible to avoid excessive packet and chunk churn.
- Snapshot original block data before the first modification.
- Restore every changed block and original block data at teardown, including after disconnects, forced stops or exceptions.
- Clear only state owned by this game session; never modify the lobby or another game's world.

Each weapon and secondary must call the same paint service so the ownership, scoring, feedback and progression rules are consistent.

## 5. Player combat and splats

Combat uses paint marks rather than ordinary Minecraft health damage. Disable normal melee damage, fall damage and unrelated environmental damage for the match unless a future mode explicitly introduces them.

### Four-mark splat model

- Every player starts with zero paint marks and four clean armour segments.
- A valid hit from an opposing player's primary, secondary or special adds one mark to one armour segment.
- The four segments visibly change to the splatted colour as marks accumulate.
- The fourth mark causes a splat.
- One projectile or one ability activation can add at most one mark to a given player, even if its effect overlaps them multiple times.
- Friendly projectiles do not mark teammates.
- Damage and paint from a kit must not bypass the four-mark base rule. Kit identity comes from range, firing pattern, coverage, mobility and utility—not an unfair one-shot weapon.

A player's marks begin to clear after 6 seconds without receiving an enemy paint hit while standing on friendly territory. One mark clears every 2 seconds while that condition remains true. Taking another hit resets the recovery delay. Standing on neutral or enemy territory does not grant mark recovery. This rewards players who retreat onto their own turf without making escapes instantaneous.

A splat awards the opposing player a kill credit. If another teammate damaged the same target within the previous 8 seconds, they receive an assist. A player can receive only one kill credit per splat.

### Respawn

Splat players return to a valid team spawn after 2.5 seconds with:
- Four clean armour segments.
- A full ink tank.
- No active special charge penalty.
- The selected kit restored.
- 2 seconds of spawn protection, during which the player cannot fire, use abilities or claim territory.

Spawn protection must end early if the player attacks or deliberately leaves the protected spawn area. Spawn selection should favour safe, spread-out points and avoid placing players directly in enemy line of sight.

## 6. Ink economy and Squid Mode

### 6.1 Ink tank

Every player has a 100-unit ink tank. Primary and secondary weapons consume ink. Specials are charged separately and do not consume tank ink once ready.

Initial regeneration rules:

- While not firing or using a secondary, a player regenerates 6 ink per second after a 1-second quiet delay.
- In Squid Mode on friendly paint, the player regenerates 30 ink per second.
- Squid Mode on neutral paint gives no movement bonus and only normal regeneration.
- Enemy paint cannot activate Squid Mode.
- At zero ink, primary and secondary actions are disabled. The HUD tells the player to enter friendly paint to refill.
- Respawning restores the tank to 100.

Ink values below are initial tuning targets. They must be validated in a live match and may be adjusted without changing kit identity.

### 6.2 Squid Mode activation

The hotbar uses a dedicated Squid slot so that selecting a secondary or special item does not accidentally activate the movement form. Selecting the Squid slot toggles Squid Mode; it activates only when the player is touching friendly-painted ground or a connected friendly-painted wall.

In Squid Mode:

- The player receives a substantial movement-speed increase while travelling over friendly paint.
- The player's visual state clearly indicates Squid Mode to nearby players.
- The ink tank refills rapidly.
- The player cannot use their primary weapon, secondary or special.
- The player can climb an adjacent connected wall of their team's paint.
- Leaving friendly paint ends the speed and wall-climb benefits; the player returns to normal form when no valid friendly paint contact remains.

Wall climbing should use a server-authoritative movement check against the adjacent painted surface. It must not allow climbing through unpainted blocks, enemy paint, ceilings, protected structures or arbitrary vertical teleportation. The player rises smoothly while maintaining contact with the connected friendly-painted wall. The climb should be interruptible by moving away, entering a non-friendly surface or taking a splat.

The wall-climb mechanic is core, not a cosmetic effect. It must be demonstrated in acceptance tests.

### 6.3 Hotbar and controls

Recommended match hotbar:

| Slot | Function |
| --- | --- |
| 1 | Primary weapon |
| 2 | Secondary weapon |
| 3 | Special ability / charge indicator |
| 8 | Squid Mode toggle |
| Other slots | Reserved for clear future utility or left empty |

Controls must be explained in the preparation phase and shown in kit descriptions. Weapon and ability use should rely on ordinary mouse/key interactions where possible. The same action must behave consistently for all five kits.

## 7. Kits and weapon specification

Every kit has one primary, one fixed secondary and one kit-specific special. The five kits are available to all players in the kit selector. There are no kit unlock requirements or kit stat upgrades.

### 7.1 Sprayer — all-rounder

**Role:** reliable combat and balanced territory coverage.  
**Difficulty:** beginner.  
**Primary — Splattershot:** rapid, straight paint projectiles with moderate range and a compact impact radius.

Initial targets:
- 5 shots per second.
- 4 ink per shot.
- Impact radius approximately 1.5 blocks.
- Effective at short-to-medium range.
- One paint mark per valid enemy hit.
- Consistent coverage but less territory per ink unit than a Roller or Lobber.

**Secondary — Burst Bomb:** throw a compact bomb that detonates after a short fuse, painting a radius of approximately 3 blocks. Costs 25 ink. One mark maximum per affected opponent.

**Special — Colourburst:** launch a highly visible paint orb to a targeted location. On impact it creates one large radial paint burst (target radius 5 blocks), repaints enemy territory and paints neutral surfaces. Each enemy caught can receive at most one paint mark from that activation. No multi-hit stacking from the same blast.

**Purpose:** a simple and dependable kit that can fight, paint and support any part of the map without being best at every specialist task.

### 7.2 Roller — territory specialist

**Role:** high-volume ground coverage and frontline creation.  
**Difficulty:** beginner/intermediate.  
**Primary — Paint Roller:** holding the primary attack moves the roller forward and paints a wide path under and immediately ahead of the player. Releasing the attack stops the rolling action. A separate flick action sends a fan of paint forward at shorter range.

Initial targets:
- Rolling path approximately 3 blocks wide.
- Ink drain approximately 7 units per second while rolling.
- Flick costs 8 ink and paints a forward fan.
- Rolling is strongest on open ground and weak at long range.
- A roller does not paint through solid walls.
- Direct contact/flick can mark an enemy once per attack window; simply running into a player must not cause repeated marks every tick.

**Secondary — Ink Mine:** place a small, visible team-coloured mine on a paintable surface. It arms after a short delay, then triggers when an enemy enters its radius. It paints a compact area and adds at most one mark to each affected enemy. Costs 30 ink. It can be destroyed or neutralised by enemy paint before activation.

**Special — Stampede:** for 4 seconds, the roller creates a wider, faster paint path and gains a controlled forward burst. It does not grant invulnerability. Each opponent can be marked at most once by the special. Walls, voids and map boundaries remain authoritative and cannot be bypassed.

**Purpose:** the best way to establish and reclaim ground, but vulnerable when isolated or forced into long-range fights.

### 7.3 Charger — precision specialist

**Role:** long sightlines, precision painting and ranged pressure.  
**Difficulty:** advanced.  
**Primary — Ink Charger:** hold to charge, release to fire a narrow, fast paint shot. Charging increases travel distance and paint-line length, not the number of marks inflicted on a player.

Initial targets:
- Full charge time approximately 0.9 seconds.
- Low charge: shorter range and small impact paint.
- Full charge: maximum range approximately 22–28 blocks and a narrow line of painted surfaces.
- Full-charge ink cost approximately 18 units; partial shots cost less.
- Full-charge shot can mark one opponent once.
- The charger should be accurate but have a meaningful charge window and low close-range coverage.

**Secondary — Tracker Flare:** fire a flare that briefly reveals enemy positions in a defined radius (initial target: 18 blocks) to the user and their teammates. Use a clear temporary outline or equivalent 26.3-compatible visual. It does not deal damage or paint a large area. Costs 25 ink.

**Special — Prism Beam:** charge and release a high-visibility beam that paints a long, narrow corridor through eligible surfaces. It can mark each enemy intersected once. It must have a clear wind-up and firing cue so opponents can dodge or take cover. It cannot pass through protected solid geometry.

**Purpose:** give the team long-range pressure and information, but rely on teammates to cover close-range territory and protect the charger.

### 7.4 Lobber — splash and area denial

**Role:** paint elevated surfaces, pressure cover and dislodge defenders.  
**Difficulty:** intermediate.  
**Primary — Paint Lobber:** throw an arcing paint projectile that bursts on impact. The trajectory should be readable and consistent, allowing players to paint over low cover and onto raised positions.

Initial targets:
- One shot approximately every 1.1 seconds.
- 14 ink per shot.
- Impact radius approximately 3 blocks.
- More area per hit than the Sprayer, but slower and less precise.
- One mark maximum per enemy caught in one projectile impact.

**Secondary — Sprinkler:** deploy a destructible stationary emitter that paints nearby eligible surfaces in small pulses for up to 6 seconds. Initial target radius: 2 blocks. Costs 35 ink. It can be destroyed by enemy attacks and must be removed on expiry, splat, round end or teardown.

**Special — Paintstorm:** designate a visible target area and launch a short sequence of large arcing paint bursts into it. The warning marker and incoming projectiles must be visible to both teams. Each affected enemy can receive no more than one mark from the entire activation.

**Purpose:** create pressure around contested areas and paint awkward vertical routes, trading fire rate and mobility for strong splash coverage.

### 7.5 Dualies — mobile skirmisher

**Role:** close-range combat, flanking and rapid repositioning.  
**Difficulty:** advanced.  
**Primary — Twin Blasters:** fire paired paint projectiles in quick volleys. The weapon has a shorter effective range and tighter close-range pressure than the Sprayer, but less efficient broad-area coverage.

Initial targets:
- One paired volley approximately every 0.35 seconds.
- 2 ink per projectile (4 per paired volley).
- Compact spread and rapid falloff at range.
- Both projectiles from the same volley count as one hit event against one opponent.
- One volley can add only one paint mark to the same player.

**Mobility — Dodge Roll:** two charges, each with a short lateral/forward burst; recharge one charge every 5 seconds. The roll must preserve the player's collision and server-authoritative location. It does not grant invulnerability or allow travel through blocks.

**Secondary — Suction Bomb:** attach a visible bomb to a block or surface. It explodes after a readable fuse, paints a small radius and adds at most one mark to each affected enemy. Costs 30 ink.

**Special — Afterimage Dash:** perform three rapid, steerable dashes over a short duration, leaving a broad team-coloured paint trail. It is a mobility and territory special, not an invulnerability state. The player cannot dash through solid blocks or outside the arena. Each enemy can be marked at most once by the full activation.

**Purpose:** take side routes, pressure isolated opponents and create sudden openings, but depend on good positioning and careful ink use.

### 7.6 Kit balance rules

- Every kit uses the same four-mark splat baseline.
- Primary, secondary and special attacks all obey the shared one-mark-per-player-per-activation limit.
- No kit may have a permanent speed, health, damage or ink-capacity advantage.
- Primary weapons must all contribute territory in a way that suits their role.
- Secondary weapons are utility, not a second primary weapon.
- Specials are impactful but have clear warnings, counterplay and bounded duration.
- No special grants unrestricted invulnerability, flight, phasing or block bypass.
- Kit selection is locked at match start to keep stat attribution accurate; a future rematch can provide a fresh selection.

## 8. Special charge

Each player has a separate 0–100 special gauge.

Initial charge rules:
- Earn charge from unique neutral blocks claimed and unique enemy blocks reclaimed.
- Repainting a block already owned by your team gives no charge.
- Repeatedly firing at the same block cannot generate repeated charge.
- A splat contribution and an assist can award a small fixed bonus, but the same event must not pay out twice.
- Charge is capped at 100 and is not earned while protected at spawn or after the match ends.
- The HUD clearly displays percentage and a ready-to-use state.
- Activating a special consumes the full gauge.

Initial tuning target: each newly claimed or reclaimed territory unit contributes a small fraction of a percentage point, with a full gauge requiring meaningful play rather than passive waiting. Final numeric charge rates are a balance item for live play and should be configuration-driven.

## 9. HUD, scoreboard and feedback

The game must be readable without opening a menu.

### Main sidebar

1. Super Sploor title.
2. Match timer.
3. Red territory percentage.
4. Blue territory percentage.
5. Neutral territory percentage or remaining-neutral value.
6. Team indicator and alive roster count.

### Personal HUD

- Ink tank: 0–100.
- Special gauge: 0–100 and ready state.
- Four armour/paint marks.
- Current kit and primary/secondary names.
- Primary cooldown or charge state when relevant.
- Squid Mode state and wall-climb feedback.
- Personal match stats: territory units painted, enemy units reclaimed, splats and assists.

Do not put every statistic into the sidebar. The live match score and timer take priority; personal performance belongs in a compact secondary display or end-of-match summary.

### Feedback

- Every successful paint impact produces a visible colour splash and sound.
- Territory transfer should be visually obvious to both teams.
- A player hit shows a clear armour-mark change.
- The fourth hit triggers a clear splat animation/sound and kill feed.
- Squid Mode activation/deactivation has distinct feedback.
- Wall climbing displays a small cue so the player understands why movement is occurring.
- Secondary and special activation have team-visible visual/audio telegraphs.
- The final 30 seconds has a prominent countdown.
- End-of-match summary shows winner, territory percentages, neutral percentage, and individual contributions.

All essential feedback must work without a custom resource pack. Cosmetics may add alternative visuals but must never hide team colour, hit state, warnings or gameplay effects.

## 10. Achievements

Achievements are one-time unlocks recorded per human player. They must have server-authoritative triggers, reset only the per-match counters they depend on at match start, and unlock at most once. Do not award progress for test bots or fake test players.

The initial achievement set:

| ID | Achievement | Requirement |
| --- | --- | --- |
| first-splash | First Splash | Complete your first Super Sploor match. |
| fresh-canvas | Fresh Canvas | Claim 500 previously neutral territory units in one match. |
| reclaimer | Reclaimer | Reclaim 200 enemy-owned territory units in one match. |
| squid-express | Squid Express | Travel 500 blocks in Squid Mode during one match. |
| wall-to-wall | Wall to Wall | Climb 25 vertical blocks on friendly-painted walls in one match. |
| team-palette | Team Palette | Earn 5 assists in one match. |
| close-shave | Close Shave | Win a match by 2 percentage points or less. |
| clean-sweep | Clean Sweep | Win a match without being splatted. |
| colour-outside-the-lines | Colour Outside the Lines | As Sprayer, claim 350 neutral territory units in one match. |
| road-roller | Road Roller | As Roller, paint 500 territory units using the rolling path in one match. |
| needle-threader | Needle Threader | As Charger, land 5 fully charged primary hits on opponents in one match. |
| splash-zone | Splash Zone | As Lobber, reclaim 150 enemy-owned territory units in one match. |
| blink-and-youll-miss-it | Blink and You'll Miss It | As Dualies, splat 3 different opponents within 5 seconds of a Dodge Roll in one match. |
| special-delivery | Special Delivery | Mark 3 different opponents with one special activation. |
| master-of-the-palette | Master of the Palette | Win at least one match using each of the five kits. |

Achievement criteria are intentionally based on meaningful match events. A single action cannot be counted multiple times because of overlapping splash calculations, repeated projectile callbacks or block-update retries. Achievement progress must be validated using human players in live matches; fake test players must not unlock or advance persistent achievements.

## 11. Long-term milestones

Milestones track lifetime cumulative statistics. They are separate from one-time achievements and use four visible stages, consistent with the modern Arcade progression catalogue.

| Milestone ID | Display name | Stat key | Maximum | Stage thresholds |
| --- | --- | --- | ---: | --- |
| wins | Turf War Wins | wins | 250 wins | 10 / 25 / 100 / 250 |
| territory-painted | Territory Painted | territory-blocks-painted | 250,000 blocks | 1,000 / 10,000 / 50,000 / 250,000 |
| enemy-territory | Enemy Turf Reclaimed | enemy-blocks-reclaimed | 50,000 blocks | 250 / 2,000 / 10,000 / 50,000 |
| splats | Opponents Splatted | splats | 1,000 splats | 10 / 50 / 250 / 1,000 |
| assists | Team Assists | assists | 500 assists | 5 / 25 / 100 / 500 |
| squid-travel | Squid Travel | squid-distance-blocks | 100,000 blocks | 250 / 2,500 / 25,000 / 100,000 |
| specials | Specials Activated | specials-used | 250 specials | 5 / 25 / 100 / 250 |

Stat definitions:
- **wins:** only official match wins; draws do not count.
- **territory-blocks-painted:** count each eligible block only when its ownership changes from neutral or enemy to the player's team. Do not count repeated painting of friendly territory.
- **enemy-blocks-reclaimed:** subset of territory-blocks-painted where ownership changed from the opposing team to the player's team.
- **splats:** credited opponent splats, once per opponent per splat event.
- **assists:** credited assists under the 8-second contribution rule, once per splat.
- **squid-distance-blocks:** actual horizontal movement while in valid Squid Mode; exclude teleports, knockback corrections, respawns and implausible movement.
- **specials-used:** successful special activations only; rejected or interrupted attempts do not count.

Milestones are cumulative across all five kits. Kit-specific mastery is represented by the five kit achievements rather than five extra lifetime win tracks in the first release.

### Progression rewards

Achievement and milestone rewards may grant titles, player-facing badges and cosmetic paint/weapon presentation choices. They must not:
- Unlock the five base kits (all are available from the start).
- Increase weapon damage, paint radius, range or fire rate.
- Increase ink capacity or regeneration.
- Improve movement speed, health or special charge rate.
- Provide a competitive advantage or reveal information unavailable to other players.

For the first mechanics acceptance pass, implement the unlock tracking and progress display. Cosmetic reward rendering can follow the established Testplex cosmetics pass once core mechanics are accepted. Do not block gameplay acceptance on cosmetic polish.

## 12. Persistent stats and Testplex integration

Use the game ID super-sploor and the modern Arcade's local per-player progression store. Achievement IDs and milestone/stat keys must match the tables above exactly.

Suggested per-game stats:
- matches
- wins
- draws
- territory-blocks-painted
- enemy-blocks-reclaimed
- splats
- deaths
- assists
- squid-distance-blocks
- wall-climb-blocks
- specials-used
- sprayer-territory
- roller-territory
- charger-full-hits
- lobber-enemy-reclaimed
- dualies-post-roll-splats

Per-match counters used by achievements should live in the match session and be discarded after result processing. Lifetime counters should update only once when the match result is committed. Teardown and retry paths must not double-award wins, achievements, milestones or stats.

Only real human players receive persistent stats, achievements and milestones. Testplex AFK/fake test players participate in session lifecycle and can be used to test team balancing, hit targets, respawn and scoreboard handling, but they must be excluded from all account progression and must not be used to fake player contributions.

Keep the new progress catalogue entries and tests aligned with the existing ArcadeProgressCatalog model (achievement id/name/requirement and four-stage milestone definitions). Add dedicated tests for stage thresholds, per-player values, stat-key definitions and idempotent award handling.

## 13. Map plan

The original archived Sploor map, update/maps/Sploor/NanoGames_Excavation.zip, is the first candidate for the port. It should be imported and inspected before creating a replacement map.

Map acceptance requirements:
- At least 12 valid spawn locations per team, with no spawn directly exposed to the opposing spawn.
- A fair quantity of reachable paintable floor and wall surfaces.
- Connected wall routes that can be climbed from friendly-painted ground.
- No paintable blocks behind inaccessible geometry or outside the playable arena.
- No large decorative ceiling or hidden surface that can distort territory percentages.
- Multiple viable routes between each team's side and central territory.
- Elevated firing positions for Charger without making them impossible to contest.
- Narrow routes and cover for Dualies/Roller play.
- At least one alternative vertical path for Squid Mode.
- Team spawn rooms cannot be painted or farmed for special charge.
- The map must be restorable exactly after every match.

If the original archive cannot be imported cleanly into the modern map loader, preserve it as the source reference and build a small modern acceptance arena that reproduces its route, scale and paintable-surface mix. Do not silently substitute an unrelated generated arena and call it source-map parity.

## 14. Implementation plan

### Phase 1 — Core Sploor loop

- Register super-sploor in the modern Arcade game catalogue and route it through the normal session lifecycle.
- Import and validate Excavation or create the bounded acceptance arena.
- Implement teams, match timer, paintable block discovery and block ownership.
- Implement projectile impacts, paint radius, territory transfer and incremental team scoring.
- Implement four-mark player splats, assists, respawns and spawn protection.
- Implement Squid Mode, ink tank, friendly-paint refill and painted-wall climbing.
- Implement HUD, kill feed, result summary and complete arena restoration.
- Add unit tests for territory ownership, score percentages, splat threshold, hit deduplication, cleanup and progress-stat definitions.

**Phase 1 exit:** two human clients can complete a full match, paint territory, splat and respawn, use Squid Mode and wall-climb, see correct score changes, and return to a pristine arena.

### Phase 2 — Five kits and abilities

- Add the Sprayer, Roller, Charger, Lobber and Dualies primary behaviours.
- Add each kit's fixed secondary.
- Add each kit's special gauge and special activation.
- Validate block-paint shapes, ink costs, charge, cooldowns and one-mark-per-activation limits.
- Add kit selection and readable kit descriptions.
- Add per-kit balance and acceptance tests.

**Phase 2 exit:** every kit has a distinct and viable territory role, and no kit can bypass the shared splat or score rules.

### Phase 3 — Achievements and milestones

- Add all 15 achievements and seven lifetime milestone tracks.
- Add exactly-once result commit and achievement unlock handling.
- Add progress summaries and achievement/milestone visibility in the existing Stats UI.
- Verify fake test players never receive persistent progression.
- Add persistence/restart, disconnect, draw, rematch and duplicate-event tests.

**Phase 3 exit:** achievements and milestone totals are accurate, persistent and auditable across multiple matches.

### Phase 4 — Polish and acceptance

- Tune weapon cadence, ink costs, paint radii, special charge and splat recovery.
- Validate all HUD elements from a second client and a spectator.
- Add sounds, particles and visual telegraphs without requiring a resource pack.
- Verify disconnects, forced stop, server restart and exception cleanup.
- Test the original archive on the modern runtime.
- Keep the game disabled from normal public testing until the full acceptance checklist is passed.

Cosmetics, additional modes (including Splat Zones), ranked rules and win-scene presentation are follow-up work, not blockers for the first playable Turf War.

## 15. Acceptance checklist

### Lifecycle and teams
- [ ] Game appears in the normal Testplex Game Panel with honest development status.
- [ ] Queue, kit selection, prepare, live, overtime, result and teardown all work.
- [ ] Teams balance correctly from 4 to 16 humans.
- [ ] Disconnecting players do not leave stale roster entries or score state.

### Territory
- [ ] Neutral blocks become the correct team colour.
- [ ] Enemy blocks transfer to the attacking team.
- [ ] Friendly repainting does not create score or special charge.
- [ ] Territory percentages are based on all eligible map units and sum with neutral to 100%.
- [ ] Overlapping projectiles or repeated callbacks cannot double-count a block.
- [ ] Arena blocks restore exactly after normal end, draw, forced stop and exception.

### Combat and movement
- [ ] Four valid enemy marks cause a splat; fewer marks do not.
- [ ] Friendly fire is harmless.
- [ ] Mark recovery only occurs under the documented safe-territory condition.
- [ ] Respawn and spawn protection behave consistently.
- [ ] Squid Mode works only on friendly paint.
- [ ] Ink regenerates at the correct rate and does not regenerate through exploit movement.
- [ ] Wall climbing follows connected friendly-painted walls and cannot pass through solid geometry.

### Kits
- [ ] Each primary paints, costs ink and fires at its intended cadence.
- [ ] Each secondary works, costs ink and cleans up.
- [ ] Each special charges, telegraphs and affects each target at most once.
- [ ] No kit has an unintended damage, range, movement or territory exploit.
- [ ] All five kits can contribute to winning through their intended role.

### HUD and progression
- [ ] Red, Blue and Neutral coverage match the authoritative territory state.
- [ ] Ink, special charge, kit, four hit marks and timer are readable.
- [ ] Personal summary and kill feed identify splats/assists correctly.
- [ ] Every achievement unlocks only when its exact condition is met.
- [ ] Milestone thresholds and lifetime counters are correct.
- [ ] Repeated result events do not double-count stats or unlocks.
- [ ] AFK/fake test players are excluded from all persistent progression.

### Multi-client and operational
- [ ] Visual paint, hit, squid and special feedback are visible to other players and spectators.
- [ ] Simultaneous weapon fire does not cause major server stalls or block-update storms.
- [ ] A complete match works with 4, 8 and 16 human participants.
- [ ] Death, disconnect, restart and forced stop leave no active projectiles, mines, sprinklers, temporary effects or modified map blocks.
- [ ] The game remains disabled for public testing until all required live acceptance items pass.

## 16. Source and implementation references

- Original NANO Sploor implementation: Plugins/Mineplex.Game.Nano/src/mineplex/game/nano/game/games/sploor/Sploor.java
- Original map archive: update/maps/Sploor/NanoGames_Excavation.zip
- Modern Arcade mechanics module: modern-platform/arcade-games-port/
- Modern platform architecture: modern-platform/README.md
- Progression catalogue and four-stage milestone model: modern-platform/arcade-games-port/src/main/java/au/com/testplex/arcadeports/ArcadeProgressCatalog.java

**Implementation boundary:** this specification is the design baseline only. No game code, map conversion, runtime registration, achievement unlock, milestone tracking or public test enablement is implied by adding this document. Those changes must be implemented and accepted phase by phase.
