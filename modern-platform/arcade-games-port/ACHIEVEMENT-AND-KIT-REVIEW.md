# Achievement and achievement-kit expansion — A–Z review (ongoing)

This document tracks game-by-game approvals and outstanding proposals. Changes to
unreviewed games remain unapproved; approved Bacon Brawl, Bomb Lobbers, Castle Assault
and Death Tag requirements are implemented on this draft branch.

## Target and source-of-truth

- Target **at least 14 active achievements per game family**, displaying at most 14 per
  `/stats` page. Treat Monster Maze Original, Speed and Modern
  as a single achievement family (as `/stats` already does). Speed Builders Arcade is its own
  family even though it is not a value of `ArcadePortGame`.
- There are **30 game families** represented across four catalogues: `ArcadeProgressCatalog`,
  `PlexballAchievement`, `CameraShyProgressCatalog`, and `SuperSploorProgressCatalog`.
- The current catalogue contains **301 declared achievements**. The 30-family minimum is 420.
- **119 further additions** would fill all under-target catalogues, resulting in **427 total active
  achievements** if the existing definitions are retained: Snap has 20 and Super Sploor 15.
  Preserve existing achievements unless Josh explicitly approves a removal. The approved
  One in the Quiver list removes duplicate Perfectionist while keeping its stored historical
  unlock valid for Ninja's prerequisite.
- Catalogue declarations are **not proof that award tracking is complete**. Every proposed or
  existing achievement must be cross-checked against the actual awarding events and persistence.

## Catalogue audit (2026-11-06)

| Game family | Declared | To add for minimum 14 | Already above 14 |
| --- | ---: | ---: | ---: |
| Bacon Brawl | 14 | 0 | 0 |
| Bomb Lobbers | 14 | 0 | 0 |
| Castle Assault | 14 | 0 | 0 |
| Death Tag | 14 | 0 | 0 |
| Dragon Escape | 9 | 5 | 0 |
| Dragons | 11 | 3 | 0 |
| Draw My Thing | 5 | 9 | 0 |
| Evolution | 7 | 7 | 0 |
| Gladiators | 9 | 5 | 0 |
| Heroes of Gwen | 6 | 8 | 0 |
| Hoops Remastered | 9 | 5 | 0 |
| Master Builders | 4 | 10 | 0 |
| Micro Battles | 9 | 5 | 0 |
| Milk the Cow | 14 | 0 | 0 |
| Monster Maze (all modes) | 14 | 0 | 0 |
| One in the Quiver | 7 | 7 | 0 |
| Paintball | 5 | 9 | 0 |
| Plexball | 9 | 5 | 0 |
| Runner | 14 | 0 | 0 |
| Sheep Quest | 14 | 0 | 0 |
| SkyWars | 14 | 0 | 0 |
| Snake | 14 | 0 | 0 |
| Snap | 20 | 0 | 6 |
| Sneaky Assassins | 6 | 8 | 0 |
| Speed Builders (Arcade) | 4 | 10 | 0 |
| Spleef | 7 | 7 | 0 |
| Super Sploor | 15 | 0 | 1 |
| Turf Wars | 9 | 5 | 0 |
| Wither Assault | 8 | 6 | 0 |
| Wizards | 6 | 8 | 0 |
| **Total** | **319** | **101** | **7** |

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

### Decisions needed before the remaining 162-achievement proposal pass

**Bomb Lobbers was signed off; Castle Assault is the current A–Z review target.** Its 14th achievement is
`not-on-my-watch` (intercept three distinct enemy TNT projectiles with Constructor's
placed defenses in a single game). Constructor replaces Waller; the obsolete Pitcher
kit is removed; Jumper is now Acrobat. Legacy Jumper/Waller identifiers remain input
aliases so existing saved kit preferences migrate without erasing achievements.
Constructor's **four approved prerequisites** are `two-birds-one-stone`,
`whats-a-double-jump`, `blast-proof`, and `jelly-skin`. Blast Proof now means
**ten separate wins as Armorer**, tracked per kit (and cannot be farmed by
changing kits mid-match). Previously earned Blast Proof records are preserved.
The server is not live and no player has unlocked Waller, so Constructor needs no grandfathered Waller entitlement. The legacy Waller kit-name alias may remain for configuration compatibility, but it does not bypass the four approved achievements.

Castle Assault's original six challenges are preserved. The eight approved additions are Bad Chemistry (Alchemist), Pin Cushion (Archer), TNT-rrific (Demolitionist), Golden Boy (Fighter), Immovable Object (Tank), Express Delivery (objective TNT crystal within 15 seconds), Royal Guard (three defenses near King), and Against All Odds (win with no friendly crystals). Clutchness is now **King health below 10**, and Multiplicative needs **at least five kills and double the next-highest teammate**. Multiplicative, Blasphemous, and Moral Exercise now allow eligible players from either team to earn them. All five existing kits remain free; this pass creates no Castle Assault purple kit.

**Castle Assault kit passives approved:** Alchemist receives +25% potency on splash potions obtained from original map chests **only**, including extended durations and boosted instant damage/healing. Alchemist kill-streak potions (Slowness, Weakness, Harming) are unchanged and do not receive the passive. Archer arrows are marked at launch and receive +25% base damage on hit. Other kits and ordinary thrown potions keep their original damage and effects.

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
