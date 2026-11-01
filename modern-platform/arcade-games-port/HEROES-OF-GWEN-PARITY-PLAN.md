# Heroes of Gwen — source parity and acceptance plan

**Catalogue status:** `MECHANICS_IN_PROGRESS`  
**Enabled rotation:** No. This plan does not enable the game or mark it test-ready.  
**Maps present:** Heroes Valley and Tartarus source archives are included in the repository.

## Current implementation inventory

The current Paper 26.3 runtime has a mixed-variant implementation built around the original map markers. The merged port work registers the Heroes of Gwen catalogue entry, imports the original Heroes Valley and Tartarus archives, handles base/tower/beacon markers, implements tower pressure and a Wither objective, adds capture-point ownership/gold handling, and provides four role-specific shop catalogues.

The current explicit skill dispatcher contains six hero skills: Hattori's Shuriken, Ninja Dash and Ender Blade; Devon's TNT Infusion, Hunters Boost and Infinity. Other named hero kits still fall through to the generic “abilities are still being ported” loadout/skill path. The existing merged PR also explicitly leaves role abilities, item passive effects, full Wither movement/attacks and detailed beacon visuals for in-game comparison.

## Canonical hero/ability inventory

The archived community reference lists the following nine playable heroes and their hotbar abilities:

| Role | Hero | Ability set |
|---|---|---|
| Assassin | Hattori | Shuriken; Ninja Dash; Ender Blade |
| Assassin | Bardolf | Summon Wolf; Wolf Pounce; Full Moon |
| Mage | Anath the Burnt | Flame Wand; Burn Beam; Flame Dash; Meteor Shower |
| Mage | Larissa | Aqua Cannon; Dancing Fountain; Water Dash; Storm's Blessing |
| Hunter | Devon | TNT Infusion; Hunters Boost; Infinity; Fletcher |
| Hunter | Rowena | Light Arrows; Combat Slide; Bombardment; Fletcher |
| Warrior | Dana | Pulse Heal; Knock Up; Rally |
| Warrior | Biff | Tether; Battle Leap; Cavalry Charge |
| Warrior | Ivy | Vine Pull; Floral Leap; Thorn Wall |

The current rules helper also normalises `bob-ross` to a Mage role. The community reference identifies Bob Ross as a lobby morph rather than one of the nine playable Heroes of Gwen champions. Confirm whether this is an intentional Testplex variant before treating it as a playable hero.

Reference: [Mineplex Wiki — Heroes of Gwen](https://mineplex.fandom.com/wiki/Heroes_of_Gwen).

## Source mechanics that must be accepted

- **Lane minions:** six minions spawn for each side every 10 seconds. They fight opposing minions, then advance towards enemy towers. Once the second enemy tower is destroyed, the attacking side's later minions receive iron armour.
- **Towers:** each side has an outer and inner Guardian tower. The crystal is the damageable objective; players must enter the 10-block tower area. Towers prioritise minions over players, except where a player entered the area first.
- **Withers:** one per team, patrolling around its base and attacking nearby enemies. Movement, target selection, attack timing, health and death handling need source comparison.
- **Gold/Emerald capture points:** two neutral capture points. A capture awards 25 gold; each owned point adds 5 gold every 5 seconds to its team.
- **Hero skills:** every hero must have its correct primary weapon, hotbar slot items, ability effect, target filters, cooldown, damage/healing, structure/minion interaction and death/reset behaviour.
- **Role shops and passive effects:** verify all four role catalogues, item pricing, purchase effects, armour/passive bonuses, consumables and the mixed-variant half-price rule.
- **Heroes Valley sidequest:** verify the Pumpkin King spawn/respawn timing, cave behaviour, damage, team reward (Regeneration II and Strength I for one minute) and death cancellation.
- **Mixed Arcade:** preserve the intended mixed variant's middle-tower filtering and half-price shop behaviour without changing standard Heroes of Gwen map markers.

Reference: [Mineplex Wiki — Heroes of Gwen gameplay and classes](https://mineplex.fandom.com/wiki/Heroes_of_Gwen).

## Acceptance checklist

### Runtime and lifecycle
- [ ] Queue/start/stop, team assignment, player disconnect/rejoin, death/respawn and match cleanup.
- [ ] No player can be assigned to two heroes on the same team or to a hero that is already occupied.
- [ ] Standard and Mixed Arcade load the correct map variant and objectives.

### Map and objectives
- [ ] Both original maps load their source marker sets without missing or duplicated markers.
- [ ] Both lanes spawn six minions per team every 10 seconds; minions acquire, fight and replace targets correctly.
- [ ] Minions attack towers; tower target priority and player-entry exception match source behaviour.
- [ ] Destroying the second tower upgrades subsequent friendly minions.
- [ ] Gold and Emerald points can be contested/captured and deliver the correct one-time and periodic gold.
- [ ] Withers patrol, acquire valid enemy targets, attack and return home correctly.
- [ ] The Pumpkin King sidequest works on Heroes Valley only and grants/cancels the correct reward.

### Heroes and shop
- [ ] All nine canonical heroes have complete loadouts and working abilities.
- [ ] Ability cooldowns, damage/healing, projectiles, area effects and movement match the source references.
- [ ] Hero-specific passives and shop upgrades apply only to the correct role and player.
- [ ] Purchases deduct the correct gold exactly once; consumables do not refill or heal unexpectedly.
- [ ] Mixed Arcade halves both displayed and charged prices without changing standard prices.

### Visuals and fairness
- [ ] Beacon ownership, capture progress and team colours are readable at a distance.
- [ ] Tower range and target priority are communicated clearly.
- [ ] Objective damage cannot be dealt from outside the valid tower range.
- [ ] No ability damages teammates, friendly structures or invalid targets unless the source explicitly allows it.

**Enablement gate:** keep `heroes-of-gwen` at `MECHANICS_IN_PROGRESS` and outside `enabled-games` until automated tests, both-map acceptance and live multi-player testing pass. The ability gaps and lane-minion gap are blockers, not cosmetic polish.


## Modern Paper 26.3 implementation progress — lane minion slice

The first missing objective-runtime slice is now implemented on branch feat/heroes-of-gwen-minion-lanes-20261101:

- Red baby zombies and blue baby pigmen spawn in six-unit team waves every 10 seconds from the original team spawn markers.
- Minions acquire opposing minions first, then advance towards the next active enemy tower.
- Guardian tower target-entry times are tracked continuously. Minions take priority unless an opposing player entered the 10-block tower radius first; equal-entry ties go to minions.
- Towers damage minions as well as players. Player damage to a tower is rejected outside the 10-block radius.
- Destroying the final tower upgrades subsequent waves for the attacking team with iron armour.
- Passive income is corrected to 5 gold every 5 seconds; each owned capture point adds another 5 gold per interval, and capture awards 25 gold.
- Added deterministic rule tests for the wave/economy constants and tower-entry priority.

**Validation boundary:** this slice is awaiting the Paper 26.3 CI run. It does not make Heroes of Gwen READY_FOR_TEST; seven hero ability sets, Wither/source parity, the Heroes Valley sidequest, shops/passives, both-map checks and live multiplayer acceptance remain open.


## Modern Paper 26.3 implementation progress — Anath the Burnt

Anath's complete four-slot source kit is now represented in the match-scoped runtime:

- **Flame Wand:** Blaze Rod primary, four Embers, one Ember replenished per second, 500 ms firing interval, 1.25 projectile speed, 5 damage and 3 seconds of fire on hit. The ember item is match-owned, cannot be picked up and is removed on hit, timeout, disconnect, hero change or runtime stop.
- **Burn Beam:** the original horizontal beam advances 9 blocks in 0.2-block increments, three increments per tick. It passes through blocks, reaches 2 blocks around the beam path, applies 11 damage and uses the source 2-second per-target recharge. Ability cooldown is 9 seconds.
- **Flame Dash:** 600 ms horizontal movement at the source velocity, stopping at the end. The trail appears after the source 10-tick delay and each fire block is restored after 5 seconds. Ability cooldown is 12 seconds.
- **Meteor Shower:** retained as the netherrack marker / area effect already present in the runtime.

Deterministic rule tests now cover the Flame Wand fire rate, ammo capacity/reload, damage and speed; Burn Beam range, step size, hit radius, damage, per-target recharge and tick rate; and Flame Dash movement and fire-trail timings.

**Validation boundary:** the full Anath integration build and updated tests are running on this branch. No deployment or in-game acceptance is claimed yet.


### Additional Hunter parity — Rowena and Fletcher

- Add Rowena's documented hotbar layout: Bow, Light Arrows, Combat Slide, Bombardment, Fletched Arrows and Gold Shop.
- Implement Fletcher passive with the archived per-hero limits: Rowena receives one arrow every 2 seconds (maximum 2); Devon receives one arrow every 3 seconds (maximum 3).
- Add deterministic tests for both Hunter reload intervals and ammo caps.

**Remaining:** Rowena's Light Arrows, Combat Slide and Bombardment effects still require their own mechanics references; those slots are shown but are not advertised as implemented.


### Mage parity — Larissa

Larissa's four-slot kit is ported from the archived Java implementation into the same match-scoped Heroes of Gwen runtime:

- **Aqua Cannon:** Diamond Hoe primary, 10-block water beam, 3 damage and a 500 ms fire interval; consumes one Water Stone and replenishes one every 3 seconds (source capacity 64).
- **Dancing Fountain:** Water Bucket projectile; on collision it creates a 3-block radius field for 6 seconds. Each second it heals allied players/minions by 4 HP and damages opposing players/minions by 2 HP. Cooldown 14 seconds.
- **Water Dash:** horizontal look-direction movement for 600 ms. Cooldown 10 seconds.
- **Storm's Blessing:** drop-activated ultimate; grants team Speed I and heals active teammates by 2 HP per second for 7 seconds, with source cooldown restarting after the active window.
- Regression tests cover source damage, range, ammo cadence/cap, field duration/radius/effects, dash and ultimate timings.

**Validation boundary:** Larissa's mechanics and source-value tests were included in a passing Paper 26.3 build on this branch. In-game interaction and balance acceptance remain outstanding.


### Assassin parity — Bardolf

Bardolf's source kit is implemented in the match-scoped runtime:

- **Summon Wolf:** five-wolf cap, 8 health per wolf and 5-second summon cooldown. Wolves are owned by Bardolf, follow him, return if more than 10 blocks away and attack the enemy he last damaged.
- **Wolf attacks:** 1 damage with a 500 ms per-target recharge. Allied players cannot damage their team's wolves; wolf deaths produce no drops or experience.
- **Wolf Pounce:** 7-second cooldown. Bardolf leaps in his look direction and redirects the pack to leap from his take-off point.
- **Full Moon:** 10-second active period, with the 60-second source cooldown restarting after the active period. Each living wolf adds 1 damage and 5% movement speed; wolves are healed and strengthened, then removed at the end.
- Pack entities/effects are removed on death, disconnect, hero change and runtime stop. Regression tests cover summon, pounce and ultimate source values.

**Validation boundary:** Bardolf's full implementation and tests passed Paper 26.3 CI. In-game multiplayer acceptance remains outstanding.

Bardolf's source kit is now implemented in the match-scoped runtime:

- **Summon Wolf:** Bone skill with the source five-wolf cap, 8 health per wolf, and 5-second summon cooldown. Wolves are owned by the Bardolf player, show the correct team collar, follow their owner, teleport back when they fall more than 10 blocks away, and attack the target the owner most recently damaged.
- **Wolf attacks:** one damage per bite with the source 500 ms per-owner/per-target recharge; allied players cannot damage their team's wolves, and wolf deaths give no drops or experience.
- **Wolf Pounce:** Feather skill, 7-second cooldown. Bardolf leaps in his look direction with the source minimum upward component; wolves are redirected to his take-off point and leap with the same 1.5 velocity when they get within two blocks.
- **Full Moon:** Nether Star drop-activated ultimate, 10-second active period and 60-second source cooldown restarted after the active period (70 seconds until reuse). Each living wolf adds one damage and 5% walk speed at activation; wolves are restored to 20 health, untamed and made angry, move faster, and are removed when the ultimate ends.
- Pack entities and temporary effects are explicitly removed on player death, disconnect, hero change and runtime stop. Added deterministic tests for summon limits/health/cadence, pounce timing/vector and Full Moon values.

**Validation boundary:** the first post-Bardolf build is running now. This is not whole-game acceptance: Rowena's three active abilities, all three Warrior kits, Anath's Flame Wand/Flame Dash, source Wither patrol/target parity, Pumpkin King sidequest, role shops/passives, both-map checks and live multiplayer acceptance remain outstanding. Keep Heroes of Gwen MECHANICS_IN_PROGRESS and disabled until those checks pass.


### Warrior parity — Dana, Biff and Ivy

**Dana**
- **Pulse Heal:** heals Dana for 2 HP and allied players/minions for 4 HP within a 7-block radius; 8-second cooldown.
- **Knock Up:** horizontal dash for 600 ms; enemy players take 12 damage and enemies/minions take 8 damage with source launch effects; 10-second cooldown.
- **Rally:** jump, then plant a team banner on landing. The landing deals 7 damage within 3 blocks; the 7-second banner heals allies by 3 HP per second within 5 blocks. Source reuse timing is preserved.
- Regression tests cover the archived heal, dash/collision and banner values. Dana's mechanics/tests passed Paper 26.3 CI.

**Biff**
- **Tether:** hooks enemy heroes within 8 blocks, applies the source slow and pulls them toward Biff; tether lasts 5 seconds and uses the 12-second cooldown after its active period.
- **Battle Leap:** upward leap; on landing, enemies within 5 blocks take 4 damage and are knocked upward. 11-second cooldown.
- **Cavalry Charge:** summons an armoured mount for 6 seconds; enemies within 5 blocks are hit for 4 damage and rooted. Horse ownership, no-drop handling and cleanup are match-scoped.
- Regression tests cover tether, leap and cavalry timings/effects. Biff's full implementation/tests passed Paper 26.3 CI.

**Ivy**
- **Vine Pull:** 9-block beam; enemy heroes take 4 damage and are pulled toward Ivy; minions take 6 damage. 10-second cooldown.
- **Floral Leap:** leap and create a 5-second flower bed; enemies within 5 blocks take 2 damage and are slowed each second.
- **Thorn Wall:** 5-second square wall; allies regenerate while enemies are rooted or damaged. Block data is saved and restored without leaving temporary blocks behind.
- Regression tests cover the archived beam, leap, field and wall timings/effects. Ivy's mechanics and tests compile in the successful pre-Anath Paper 26.3 snapshot; the latest combined Anath build remains the current validation run.

### Assassin and Hunter parity — Hattori and Devon

**Hattori**
- **Double Jump:** the source `PerkDoubleJump` is included as a grounded-only flight toggle with controlled 1-block vertical launch and a 3-second recharge. Flight is re-enabled only after landing and when the cooldown is ready.
- **Shuriken:** three snowballs, 3 damage each, 4 ticks apart, 1.5-second cooldown.
- **Ninja Dash:** 10-block teleport dash, 2-block collision radius and 8 damage to enemy living targets, with the source 7-second cooldown.
- **Ender Blade:** 7-second Diamond Sword form, +9 damage, and 60-second source cooldown restarted after the active window.
- Regression tests cover the passive, all three abilities and source timings.

**Devon**
- **TNT Infusion:** three arrows and the source 17-second cooldown after the third shot. Impact splash now applies the archived bow-scaling formula to eligible enemy players/minions and uses the 5-block radius/knockback.
- **Hunters Boost:** Speed II and Jump Boost I for 3 seconds; 10-second cooldown.
- **Infinity:** 7-second arrow-infinity/homing/Wither effect; 60-second source cooldown restarted after the active window. Homing uses the source 6-block target radius.
- Regression tests cover TNT arrow count, radius, bow scaling, boost and ultimate timings.

**Validation boundary:** these final Hattori/Devon mechanics and tests are in the current branch; the new combined Paper 26.3 build is pending.

### Remaining blockers before enabling Heroes of Gwen

The hero kit source ports do not by themselves make the game ready. The following acceptance work remains:

- Verify all nine hero selections, loadouts, cooldown indicators, purchases and respawns in-game.
- Wither patrol, target acquisition, attacks and return-home behaviour still need source comparison and acceptance.
- Verify capture points, shops, role passives/upgrades, purchase charging and Mixed Arcade price scaling.
- Complete both source maps (Heroes Valley and Heroes Ruins) with real players, then run multi-player ability/team/friendly-fire tests.
- Confirm objective lifecycle, team spawns, minion waves and second-tower minion upgrade together with the final hero kit.

Keep the game at `MECHANICS_IN_PROGRESS` and outside `enabled-games` until the remaining automated, both-map and multiplayer acceptance gates pass.


### Wither source-target correction — current implementation slice

The modern runtime now follows the original `MobaAI` target contract more closely:

- Withers retain a valid target and acquire the nearest hostile living entity to their home point inside the team's source boundary, rather than considering only active enemy players.
- Enemy minions and neutral living entities can be targeted; allies, inactive/spectating players, dead entities and entities outside the polygon are rejected.
- Earthquake damage/knockback now applies to eligible hostile living entities in its radius, not just players, while preserving the source distance falloff and the existing four-second runtime cadence.
- Added deterministic target-filter and direct-movement-step tests.

**Validation boundary:** this is a source-parity correction, not Wither acceptance. The Wither is still represented by a native Paper Wither rather than the original disguised 3×5 ArmorStand, and patrol, boundary, boss/tower damage interactions and both-map behaviour still require live comparison.
