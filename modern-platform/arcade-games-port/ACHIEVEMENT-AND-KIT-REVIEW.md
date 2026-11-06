# Achievement and achievement-kit expansion — design review (not yet approved)

This document is a **proposal and inventory**, not approval to change achievement definitions,
remove existing unlocks, or enable new achievement kits. It belongs to the Dragon Escape
Builder/Disruptor draft PR so the gameplay work can be tested independently.

## Target and source-of-truth

- Target **at least 14 active achievements per game family**, displaying at most 14 per
  `/stats` page. Treat Monster Maze Original, Speed and Modern
  as a single achievement family (as `/stats` already does). Speed Builders Arcade is its own
  family even though it is not a value of `ArcadePortGame`.
- There are **30 game families** represented across four catalogues: `ArcadeProgressCatalog`,
  `PlexballAchievement`, `CameraShyProgressCatalog`, and `SuperSploorProgressCatalog`.
- The current catalogue contains **257 declared achievements** (including the four approved Bacon Brawl additions and the approved Bomb Lobbers Constructor achievement). The 30-family minimum is 420.
- **170 further additions** would fill all under-target catalogues, resulting in **427 total active
  achievements** if the existing definitions are retained: Snap has 20 and Super Sploor 15.
  **No existing achievements are to be archived, deleted, or retired.** This supersedes the
  earlier exact-14/archiving proposal following the user's decision.
- Catalogue declarations are **not proof that award tracking is complete**. Every proposed or
  existing achievement must be cross-checked against the actual awarding events and persistence.

## Catalogue audit (2026-11-06)

| Game family | Declared | To add for minimum 14 | Already above 14 |
| --- | ---: | ---: | ---: |
| Bacon Brawl | 14 | 0 | 0 |
| Bomb Lobbers | 14 | 0 | 0 |
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
| **Total** | **257** | **170** | **7** |

## Suggested review methodology

1. **Preserve source achievements.** Identify authentic Mineplex challenges first, then
   distinguish existing user-approved Testplex changes from placeholders or obsolete rules.
   Preserve stable IDs and unlock history whenever possible.
2. **At least 14 meaningful achievements per family.** A suggested spread is 3 approachable,
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
- Purple-kit completion requirements are **game- and kit-specific**, not a global
  complete-all or fixed-number rule. Use an obtainable signature feat, a curated
  feat bundle, or a stable mastery set according to each kit's role. Prevent
  circular prerequisites and avoid silently moving the goalposts when the
  catalogue expands. Milestones remain separate. See
  `ACHIEVEMENT-IMPLEMENTATION-AUDIT.md` for the source-backed audit and
  kit-specific examples. Bacon Brawl's Chris P. Bacon gate is now approved and
  implemented; other games' remaining design decisions still require review.
- For kitless/fixed-role games (for example, Hoops, Draw My Thing, Master Builders, or
  Gladiators), decide separately whether to introduce an actual gameplay kit, a side-grade
  toolset, or an achievement prestige cosmetic. Introducing a kit is a gameplay-design
  decision, not merely a lobby metadata change.

### Decisions needed before the remaining 170-achievement proposal pass

**Bomb Lobbers is the current A–Z review target.** Its 14th achievement is
`not-on-my-watch` (intercept three distinct enemy TNT projectiles with Constructor's
placed defenses in a single game). Constructor replaces Waller; the obsolete Pitcher
kit is removed; Jumper is now Acrobat. Legacy Jumper/Waller identifiers remain input
aliases so existing saved kit preferences migrate without erasing achievements.
Constructor's **four approved prerequisites** are `two-birds-one-stone`,
`whats-a-double-jump`, `blast-proof`, and `jelly-skin`. Blast Proof now means
**five separate wins as Armorer**, tracked per kit (and cannot be farmed by
changing kits mid-match). Previously earned Blast Proof records are preserved.
Only the treatment of previously unlocked Waller-kit access remains to discuss.

1. **Individual unlock predicates:** Approve the actual prerequisite achievement IDs
   for each purple kit, using an obtainable signature feat, curated bundle or stable
   mastery set. Confirm how existing unlocks are grandfathered.
2. **Kitless games:** Should *every* game gain gameplay-changing purple-kit mechanics, or
   should fixed-role/creative games receive side-grade or cosmetic prestige rewards?
3. **Existing achievements:** Should genuine originals always be retained, unless mechanically
   impossible, even if they produce a less uniform difficulty spread?

### `/stats` pagination contract

- Show **14 achievements per page**; create a second page only when a family has 15 or more.
- Snap's current 20 therefore occupy **14 on page 1 and 6 on page 2**.
- Super Sploor's 15 occupy **14 on page 1 and 1 on page 2**.
- Apply the separate `/stats` PR's dynamic unlock-rarity sort **before** paginating, so the
  first 14 are the highest-rarity entries at the time of viewing. Percentages remain visible
  on hover. Do not duplicate navigation logic inside this gameplay PR.
- Preserve every existing achievement ID, award trigger, and earned unlock flag.

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
- **After the remaining kit/unlock decisions:** propose each game's missing achievements, fully specified and
  cross-checked against runtime tracking, plus missing achievement-kit designs.
- **After proposal approval:** implement and test the approved 14-achievement catalogues,
  award tracking, purple-kit unlock conditions and actual abilities. This is a separate
  review gate within the same PR; do not merge before it is resolved.
