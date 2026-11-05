# Arcade game debug framework checklist

This checklist tracks implemented controls, not menu/catalog presence alone. A game is **complete** only after its meaningful variables and feature switches reach gameplay code, its practical event/phase/achievement tools are wired, reset/profile isolation is verified, and automated tests cover the controls.

## Framework foundation

- [x] Debug access uses the existing authorised admin/tester check.
- [x] Profiles and test-bot loadouts are stored separately in `debug-profiles.yml`, outside normal game settings and player stats.
- [x] Named profiles support create, duplicate, rename, load, save and delete; the default profile cannot be deleted.
- [x] Automated coverage saves an isolated bot count, team, kit and cosmetic selection for every game and verifies new named profiles do not inherit another profile's roster.
- [x] Automated coverage verifies common feature switches persist, reset and remain isolated across profiles for every game.
- [x] Reset to Defaults removes only the selected profile's overrides.
- [x] Menus separate profiles, feature switches, variables and bot setup for the currently selected game.
- [x] Numeric shared Arcade rules can be overridden for the active round; the menu marks these as restart-required.
- [x] The variable menu exposes only rules with an explicit production runtime consumer; persisted values are clamped to the rule's safe range and malformed integer conversions cannot crash gameplay.
- [x] Automated coverage walks every exposed game variable, sets it through the typed runtime override boundary, reads it back and verifies clearing restores its default; a source check keeps runtime override keys linked to the profile catalog.
- [x] A source-backed test verifies every catalogued variable has a production gameplay consumer, including the mode-scoped Monster Maze and dynamic Sneaky Assassins rule accessors.
- [x] Automated coverage requires every individual game and Monster Maze mode to expose at least one game-specific runtime variable in addition to shared combat rules.
- [x] Shared feature switches cover kit unlock, invulnerability, fall damage and hunger; Bacon Brawl also has its unlimited-ability switch.
- [x] Achievement previews provide a game-scoped read-only menu entry for each achievement message/sound, while achievement preview mode also runs real conditions through wired Arcade award helpers and Snap; neither path persists unlocks.
- [x] Authorized testers can advance any shared Arcade start countdown, the live Draw My Thing turn, Master Builders build/review phase, Turf Wars phase, any supported shared round timeout, or the current Speed Builders, Plexball, Wizards, Snake, Snap, Monster Maze or Super Sploor phase; bot rosters are configured for the next round.
- [x] Authorized testers can restore only their own active-round health/hunger and clear fire/potion effects without invoking damage, scoring, or achievement paths.
- [x] Automated coverage verifies every event shown in a game menu has a matching production dispatcher, preventing catalog-only debug buttons.
- [x] A source-backed access test protects the authorised-tester gates on debug menus, bot menus, event actions, phase changes and feature mutations.
- [ ] Add explicit per-game control metadata for every meaningful mechanic, beyond primitive `ArcadePortRules` fields.
- [ ] Add per-game event triggers beyond the read-only achievement preview and award-preview hooks; current tools cover Draw My Thing hint reveal, Dragons spawn, Death Tag chaser conversion, One in the Quiver arrow refill, Paintball armour restoration, Spleef hunger restore, Wizards spell access, a normal selected-kit refresh for 17 shared games, and Wither Assault's minion-wave ability. Remaining games still need appropriate event tools.
- [ ] Add automated persistence, permission, reset, live-effect and round-restart tests for profiles.
- [ ] Verify every setting listed below against its gameplay consumer; keep unchecked games out of the **complete** state until done.

Legend: **Partial** means the common profile framework and any listed shared rule hooks exist, but game-specific controls or required tests are still missing. **Pending** means its separate runtime has not yet been connected to profile variables and controls. No game is marked complete yet.

Debug-round persistence guard: configured debug rounds and rounds launched with a debug bot roster do not persist match stats or achievements. Authorized live feature changes, phase advances, vitals resets and active-profile resets also mark the round for isolation. Reset to Defaults clears active rule changes but keeps this guard enabled until the round ends. It covers shared Arcade stats, Snap, Speed Builders (including its standalone match and disconnect writers), Wizards, Plexball, and Monster Maze; newly added direct progression paths must use the same guard.

Bot counts and per-bot team, kit, and cosmetic loadouts are now stored inside each tester's selected game profile. Duplicating or renaming a profile carries its roster configuration with it. Existing `test-bot-loadouts` values remain a fallback for the default profile during migration.

The common tester switches (all kits, invulnerability, no fall damage, and no hunger loss) are exposed in every game's debug menu. Invulnerability, fall, and hunger switches now also have runtime hooks for separately managed games when the authorized tester is in the active game world.

| Game | Status | Implemented coverage | Remaining work |
|---|---|---|---|
| Bacon Brawl | Partial | Shared rule overrides; common switches; achievement preview in its award helper | Per-kit ability switches/variables, event triggers and stats-isolated match simulation |
| Bomb Lobbers | Partial | Shared rule overrides and common switches; authorised testers can pause future power-up waves and clear loose drops through a profile-backed live switch | TNT/event controls, phase tools and full feature regression tests |
| Castle Assault | Partial | Shared rule overrides plus objective TNT respawn timing is runtime-backed | Objective, tower/crystal/TNT switches, event triggers and regression tests |
| Death Tag | Partial | Shared rule overrides plus chaser promotion, archer knockback and Traitor cooldown are runtime-backed; authorised testers can run the normal Runner-to-Chaser conversion on themselves | Runner/chaser feature controls, tag/respawn tools and achievement simulations |
| Dragon Escape | Partial | Shared rule overrides plus Digger protection radius and Warper grace period are runtime-backed | Dragon/ability/finish feature switches and event controls |
| Dragons | Partial | Shared rule overrides plus sparkler count, lifetime, autonomous flight speed, Tamer flight speed and turn rate are runtime-backed; authorised testers can request the next dragon spawn through the normal map spawn and targeting path; movement vector tests exercise the same steering and flight helpers used by the live dragons | Remaining Tamer/perk controls and live movement acceptance |
| Draw My Thing | Partial | 320-second match budget and per-turn cap can be tuned through runtime-backed rules; authorised testers can reveal the next hint letter during a live round | Drawing/guess/map/round controls and direct tests |
| Evolution | Partial | Shared rules include transformation timing, Quick Evolver multiplier, win level, overkill threshold and Sulphur Bomb range/cooldown | Per-form abilities, pity/evolution controls and event tools |
| Gladiators | Partial | Shared rule overrides and common switches | Bracket/duel controls, forfeit/disconnect simulation and profile acceptance tests |
| Heroes of GWEN | Partial | Shared rule overrides plus Heroes runtime constants | Per-hero/minion/objective switches and phase/event tools |
| Hoops Remastered | Partial | Hoops timing, score and gameplay constants are profile-backed and consumed by the active runtime | Remaining constants, score/possession/shot event controls and direct behavior tests |
| Master Builders | Partial | Shared builder limits and common switches | Plot/map/vote/build controls and achievement/event simulation |
| Micro Battles | Partial | Shared rule overrides and common switches | Kit/bridge/border feature switches and phase/event controls |
| Milk the Cow | Partial | Score goal, milk/death scoring, cow-team ratio, herd size/speed, and common session switches are profile-backed and consumed by the manager | Cow phase/event triggers and direct gameplay behavior tests |
| Monster Maze (all modes) | Partial | Per-mode mob speed, Body Rush duration, Cryo cooldown/freeze/radius and jump strength are profile-backed in the existing in-process engine; debug rounds suppress leaderboard personal-best writes; authorized phase control advances the current Safe Pad through normal stage resolution | Mob/theme/map controls, achievement/event preview paths and direct mode behavior tests |
| One in the Quiver | Partial | Shared rule overrides and common switches; authorised testers can restore the normal one-shot arrow to its configured slot; Leaper double jump can be disabled through a profile-backed live switch | Remaining kit-specific ability switches and kill/round event tools |
| Paintball | Partial | Shared rule overrides include Machine Gun heat capacity, shots-per-cycle and cooling; authorised testers can restore their normal team-coloured armour through the runtime armour renderer | Weapon, reload, map and team-event controls |
| Plexball | Partial | Player limits, match/batting timing, rewind, ball physics, home-run distance and catch windows are profile-backed and consumed by the rule/runtime code; debug controls advance intro, turn-break, and batting timeout phases through normal transitions | Remaining scoring/possession tunables, bot integration and direct match behavior tests |
| Runner | Partial | Shared rule overrides and common switches | Switcher/blocks/kit feature controls and phase/event tools |
| Sheep Quest | Partial | Shared rule overrides and common switches | Sheep/carry/kit/score controls and capture event simulation |
| SkyWars | Partial | Shared rule overrides and common switches | Chest/loot/island/void controls and event simulation |
| Snake | Partial | Hazard density/timing/steering, preparation countdown, segment spacing and speed scaling are profile-backed and consumed by `SnakeGameManager`; debug controls can skip the preparation countdown | Item roll, ability, map and elimination controls; direct runtime behavior tests |
| Sneaky Assassins | Partial | Power-up timing/count, master-assassin threshold, reveal/smoke radii, disguise reveal cycles, NPC count/patrol/cull/recovery, and common session switches are profile-backed and used by `SneakyAssassinsRules` | Disguise/map/role event controls and direct runtime behavior tests |
| Snap | Partial | Photo range, cooldown, protection and match timing rules are profile-backed and consumed by its runtime; timed round can be advanced through the ordinary timeout path; achievement conditions support preview-only notices | Remaining photo/outline/kit event controls and direct behavior tests |
| Speed Builders | Partial | View/build/intermission timing, match rounds, build-time bounds and template-mob visibility are profile-backed and consumed by the round loop; authorized phase advance is available; the standalone queue uses configured bot counts and applies each bot's saved team, kit, and game cosmetic profile; standalone match and disconnect progress writers skip debug rounds | Game-specific event triggers and direct round-loop tests |
| Spleef | Partial | Shared rule overrides and common switches; hunger drain has a profile-backed live switch; authorised testers can restore hunger and reapply their selected kit through normal setup | Block-break and round-phase switches/event tools |
| Super Sploor | Partial | Ink, shot, squid, territory and round constants are profile-backed and consumed by its runtime; debug advances round/overtime deadlines through normal score and finish logic; tester profile can keep the special full between activations | Remaining kit/event controls and direct behavior tests |
| Turf Wars | Partial | Shared rule overrides include build/combat phase duration and melee/sword/shredder damage | Build/combat/territory/respawn switches and phase/event tools |
| Wither Assault | Partial | Minion count and common shared-session switches are profile-backed and consumed by the manager/rules; authorised testers can trigger the normal minion-wave ability while playing the Wither role | Tower/phase controls and direct gameplay behavior tests |
| Wizards | Partial | Player limits, starter-spell rolls, mana conversion, Soul Star mana rate and endgame strike timing are profile-backed and consumed by `WizardsGameManager`/`WizardsRules`; debug controls can start endgame, trigger its next strike, and grant an isolated spell/mana test loadout | Spell/chest feature controls and direct live-round tests |

## Completion gate

Do not mark a game complete based on a saved profile value alone. For each control, record the gameplay consumer and automated test that proves the value or feature switch changes the round. Confirm default-profile behavior matches the intended source mechanics and that testing does not write achievements or stats.
