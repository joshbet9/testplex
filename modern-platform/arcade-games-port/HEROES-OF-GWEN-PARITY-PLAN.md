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

The next source-documented Mage slice is implemented on branch feat/heroes-of-gwen-anath-abilities-batch1-20261101:

- Register Anath's source hotbar layout: Flame Wand, Fire Charge (Burn Beam), Feather (Flame Dash), Nether Star (Meteor Shower), Embers and Gold Shop.
- **Burn Beam:** ten-block-high flame/lava wall, 20-block forward range, approximately 14 damage to enemy players, instantly defeats enemy minions, damages the enemy Wither, and does not damage tower structures. Uses the archived 10-second cooldown.
- **Meteor Shower:** throws the netherrack marker; on landing it creates a team-coloured 10-block radius zone for 7 seconds. It applies the documented 1 HP burn pulse every 0.5 seconds, an additional 2 damage each second, and falling fireballs that can deal up to 20 damage on impact. It also damages an enemy Wither while leaving Wither fire-immune.
- Added deterministic rule tests for both abilities' documented range, height, cooldown, radius, duration and damage cadence.

**Still incomplete for Anath:** Flame Wand basic-attack behaviour and Flame Dash exact movement/damage data are not present in the current accessible archive reference, so their slots are represented but not invented. The other six hero ability sets, source Wither movement/targeting, Pumpkin King sidequest and role-shop/passive verification also remain open. Heroes of Gwen stays `MECHANICS_IN_PROGRESS` and disabled.


### Additional Hunter parity — Rowena and Fletcher

- Add Rowena's documented hotbar layout: Bow, Light Arrows, Combat Slide, Bombardment, Fletched Arrows and Gold Shop.
- Implement Fletcher passive with the archived per-hero limits: Rowena receives one arrow every 2 seconds (maximum 2); Devon receives one arrow every 3 seconds (maximum 3).
- Add deterministic tests for both Hunter reload intervals and ammo caps.

**Remaining:** Rowena's Light Arrows, Combat Slide and Bombardment effects still require their own mechanics references; those slots are shown but are not advertised as implemented.
