# Achievement and achievement-kit expansion — design review (not yet approved)

This document is a **proposal and inventory**, not approval to change achievement definitions,
remove existing unlocks, or enable new achievement kits. It belongs to the Dragon Escape
Builder/Disruptor draft PR so the gameplay work can be tested independently.

## Target and source-of-truth

- Target **14 core achievements per game family**. Treat Monster Maze Original, Speed and Modern
  as a single achievement family (as `/stats` already does). Speed Builders Arcade is its own
  family even though it is not a value of `ArcadePortGame`.
- There are **30 game families** represented across four catalogues: `ArcadeProgressCatalog`,
  `PlexballAchievement`, `CameraShyProgressCatalog`, and `SuperSploorProgressCatalog`.
- The snapshot below contains **252 declared achievements**. The target is 420 core slots.
- **175 additions** would fill all under-target catalogues. Snap currently has six extra
  achievements; Super Sploor has one extra. Seven above-cap definitions require a deliberate
  preservation/bonus/retirement decision before the final core count can be exactly 14.
- Catalogue declarations are **not proof that award tracking is complete**. Every proposed or
  existing achievement must be cross-checked against the actual awarding events and persistence.

## Catalogue audit (2026-11-06)

| Game family | Declared | To add for 14 | Above cap |
| --- | ---: | ---: | ---: |
| Bacon Brawl | 10 | 4 | 0 |
| Bomb Lobbers | 13 | 1 | 0 |
| Castle Assault | 6 | 8 | 0 |
| Death Tag | 10 | 4 | 0 |
| Dragon Escape | 9 | 5 | 0 |
| Dragons | 11 | 3 | 0 |
| Draw My Thing | 5 | 9 | 0 |
| Evolution | 7 | 7 | 0 |
| Gladiators | 9 | 5 | 0 |
| Heroes of Gwen | 6 | 8 | 0 |
| Hoops Remastered | 9 | 5 | 0 |
| Master Builders | 4 | 10 | 0 |
| Micro Battles | 9 | 5 | 0 |
| Milk the Cow | 7 | 7 | 0 |
| Monster Maze (all modes) | 9 | 5 | 0 |
| One in the Quiver | 8 | 6 | 0 |
| Paintball | 5 | 9 | 0 |
| Plexball | 9 | 5 | 0 |
| Runner | 7 | 7 | 0 |
| Sheep Quest | 12 | 2 | 0 |
| SkyWars | 3 | 11 | 0 |
| Snake | 9 | 5 | 0 |
| Snap | 20 | 0 | 6 |
| Sneaky Assassins | 6 | 8 | 0 |
| Speed Builders (Arcade) | 4 | 10 | 0 |
| Spleef | 7 | 7 | 0 |
| Super Sploor | 15 | 0 | 1 |
| Turf Wars | 9 | 5 | 0 |
| Wither Assault | 8 | 6 | 0 |
| Wizards | 6 | 8 | 0 |
| **Total** | **252** | **175** | **7** |

## Suggested review methodology

1. **Preserve source achievements.** Identify authentic Mineplex challenges first, then
   distinguish existing user-approved Testplex changes from placeholders or obsolete rules.
   Preserve stable IDs and unlock history whenever possible.
2. **One meaningful 14-slot set per family.** A suggested spread is 3 approachable,
   4 general mastery, 4 kit/role-specific, 2 hard challenges, and 1 prestige challenge.
   Adjust this balance for games without traditional kits; avoid padding with duplicate win totals.
3. **Implementable trigger contract.** Each row needs name, precise success condition,
   event/state required, reset point, anti-farming exclusions, persistence path, regression
   test, and expected rarity. Do not promise a completion condition that gameplay never tracks.
4. **Rarity remains empirical.** The `/stats` PR handles dynamic unlock-percent sorting
   and hover percentages. Do not hard-code achievement ordering based on guessed difficulty.
5. **Bot/test treatment.** Only eligible real players should count toward population
   percentages and competitive achievement awards; AFK test roster and debug triggers must
   not inflate live unlocks.
6. **Compatibility.** Existing milestone order, stages, counts and independent progress
   remain unchanged. Proposed kits must not undermine current modes, map sizes, or bot tests.
7. **Release gate.** A per-game proposal is approved before implementation; then award
   behaviour and kit abilities are independently verified, including multiplayer gameplay.

## Achievement-kit proposal policy (requires approval)

- Existing purple kits remain available; keep their legacy names and mechanics unless
  a separate reviewed redesign explicitly replaces them.
- For games with a normal kit system but no purple kit, propose one **distinctive,
  balanced, mechanically testable** new kit; avoid a stronger version of an existing one.
- Default proposal: the purple kit unlocks on **14/14 core achievements** for that family.
  Milestones do not gate the kit. This is a proposal, not an implemented unlock rule.
- For kitless/fixed-role games (for example, Hoops, Draw My Thing, Master Builders, or
  Gladiators), decide separately whether to introduce an actual gameplay kit, a side-grade
  toolset, or an achievement prestige cosmetic. Introducing a kit is a gameplay-design
  decision, not merely a lobby metadata change.

### Decisions needed before the 175-achievement proposal pass

1. **Snap/Super Sploor:** Retain every existing award as a bonus/legacy achievement outside
   the 14 core slots, replace/retire selected definitions while preserving existing earned
   progress, or allow these two games to exceed 14? The first option retains all past work.
2. **Unlock rule:** Complete 14/14 achievements, or unlock a purple kit through a specific
   prestige challenge (possibly more accessible)?
3. **Kitless games:** Should *every* game gain gameplay-changing purple-kit mechanics, or
   should fixed-role/creative games receive side-grade or cosmetic prestige rewards?
4. **Existing achievements:** Should genuine originals always be retained, unless mechanically
   impossible, even if they produce a less uniform difficulty spread?

### Concrete suggestion for the seven above-cap achievements

**Do not delete award flags or remove definitions yet.** The Snap and Super Sploor runtimes
have actual award paths for these existing achievements.

If exactly 14 *active* achievements is the priority, a first shortlist for moving out of
the active completion pool is:

| Game | Candidate to archive as legacy/bonus | Why review it first |
| --- | --- | --- |
| Snap | Out of Focus | Near-miss variation rather than a specific kit mastery task |
| Snap | Now You See Me | Overlaps the escape-focused Smoke Artist/Runner challenges |
| Snap | Double Take | Additional rapid-photography variation |
| Snap | Close Encounter | Standalone shot-distance task |
| Snap | Long Distance Photography | Standalone shot-distance task |
| Snap | Candid Camera | Standalone target-state shot task |
| Super Sploor | First Splash | Automatic first-match participation rather than a mastery achievement |

These are **discussion candidates, not final selections**. Another legitimate option is to
retain 14 *core* achievements plus a separate non-core legacy/bonus collection; that does not
produce an exact 14-total count, but retains each challenge's visibility and honour.
Whichever path is chosen, earned progress should remain readable, and the unlock denominator
for purple kits should be explicit.

## Dragon Escape Adventure-mode acceptance

- Runners enter Dragon Escape in **Adventure** mode, including after rejoining/respawning.
- Jumper, Disruptor, Builder and Warper abilities work on right-click regardless of a block
  target; Digger left-clicks break approved course blocks through the server and give the
  recovered item even though Adventure suppresses native breaking.
- Digger right-clicks a course block with recovered materials to place that block on its
  selected face, consuming exactly one item and keeping its existing 120-second restore.
- Builder right-clicks the target block face or air to create one temporary brick, with no
  vanilla Adventure placement permission, one active block, two stored charges maximum,
  8-second replenishment, a bounded ping-adjusted lifetime and early removal after use.
- Rejected placements cannot consume items; placement cannot appear inside a racer; old
  blocks are restored at expiry and at session end; changing game modes must not permit
  permanent course edits.
- Validate these behaviours with real Paper multiplayer clients; unit-level checks alone
  do not prove Adventure-mode input delivery or parkour collision timing.

## Implementation staging

- **In this initial draft:** Dragon Escape Builder gameplay, source-accurate Disruptor
  item presentation and protection against accidental recharge-rule regressions,
  gameplay tests, and this catalogue/design audit only.
- **After decisions above:** propose each game's missing achievements, fully specified and
  cross-checked against runtime tracking, plus missing achievement-kit designs.
- **After proposal approval:** implement and test the approved 14-achievement catalogues,
  award tracking, purple-kit unlock conditions and actual abilities. This is a separate
  review gate within the same PR; do not merge before it is resolved.
