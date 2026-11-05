# Arcade game debug framework checklist

This checklist tracks implemented controls, not menu/catalog presence alone. A game is **complete** only after its meaningful variables and feature switches reach gameplay code, its practical event/phase/achievement tools are wired, reset/profile isolation is verified, and automated tests cover the controls.

## Framework foundation

- [x] Debug access uses the existing authorised admin/tester check.
- [x] Profiles and test-bot loadouts are stored separately in `debug-profiles.yml`, outside normal game settings and player stats.
- [x] Named profiles support create, duplicate, rename, load, save and delete; the default profile cannot be deleted.
- [x] Reset to Defaults removes only the selected profile's overrides.
- [x] Menus separate profiles, feature switches, variables and bot setup for the currently selected game.
- [x] Numeric shared Arcade rules can be overridden for the active round; the menu marks these as restart-required.
- [x] The variable menu exposes only rules with an explicit production runtime consumer; persisted values are clamped to the rule's safe range and malformed integer conversions cannot crash gameplay.
- [x] Shared feature switches cover kit unlock, invulnerability, fall damage and hunger; Bacon Brawl also has its unlimited-ability switch.
- [x] Achievement preview mode runs real achievement conditions through shared Arcade award helpers and Snap; preview notices do not persist unlocks.
- [x] Authorized testers can advance any shared Arcade start countdown, the live Draw My Thing turn, Master Builders build/review phase, Turf Wars phase, any supported shared round timeout, or the current Speed Builders phase; bot rosters are configured for the next round.
- [x] Authorized testers can restore only their own active-round health/hunger and clear fire/potion effects without invoking damage, scoring, or achievement paths.
- [ ] Add explicit per-game control metadata for every meaningful mechanic, beyond primitive `ArcadePortRules` fields.
- [ ] Add per-game event triggers and non-persistent achievement simulation beyond the award-preview hooks; the current vitals action is a shared tester utility, not a substitute for game event triggers.
- [ ] Add automated persistence, permission, reset, live-effect and round-restart tests for profiles.
- [ ] Verify every setting listed below against its gameplay consumer; keep unchecked games out of the **complete** state until done.

Legend: **Partial** means the common profile framework and any listed shared rule hooks exist, but game-specific controls or required tests are still missing. **Pending** means its separate runtime has not yet been connected to profile variables and controls. No game is marked complete yet.

| Game | Status | Implemented coverage | Remaining work |
|---|---|---|---|
| Bacon Brawl | Partial | Shared rule overrides; common switches; achievement preview in its award helper | Per-kit ability switches/variables, event triggers and stats-isolated match simulation |
| Bomb Lobbers | Partial | Shared rule overrides and common switches | Power-up/TNT feature controls, event triggers and phase controls |
| Castle Assault | Partial | Shared rule overrides plus objective TNT respawn timing is runtime-backed | Objective, tower/crystal/TNT switches, event triggers and regression tests |
| Death Tag | Partial | Shared rule overrides plus chaser promotion, archer knockback and Traitor cooldown are runtime-backed | Runner/chaser feature controls, tag/respawn/event tools and achievements |
| Dragon Escape | Partial | Shared rule overrides plus Digger protection radius and Warper grace period are runtime-backed | Dragon/ability/finish feature switches and event controls |
| Dragons | Partial | Shared rule overrides plus sparkler count and lifetime are runtime-backed | Tamer/dragon/perk controls and phase/event tools |
| Draw My Thing | Partial | 320-second match budget and per-turn cap can be tuned through runtime-backed rules | Drawing/guess/map/round controls, event tools and direct tests |
| Evolution | Partial | Shared rules include transformation timing, Quick Evolver multiplier, win level, overkill threshold and Sulphur Bomb range/cooldown | Per-form abilities, pity/evolution controls and event tools |
| Gladiators | Partial | Shared rule overrides and common switches | Bracket/duel controls, forfeit/disconnect simulation and profile acceptance tests |
| Heroes of GWEN | Partial | Shared rule overrides plus Heroes runtime constants | Per-hero/minion/objective switches and phase/event tools |
| Hoops Remastered | Partial | Hoops timing, score and gameplay constants are profile-backed and consumed by the active runtime | Remaining constants, score/possession/shot event controls and direct behavior tests |
| Master Builders | Partial | Shared builder limits and common switches | Plot/map/vote/build controls and achievement/event simulation |
| Micro Battles | Partial | Shared rule overrides and common switches | Kit/bridge/border feature switches and phase/event controls |
| Milk the Cow | Partial | Score goal, milk/death scoring, cow-team ratio, herd size/speed, and common session switches are profile-backed and consumed by the manager | Cow phase/event triggers and direct gameplay behavior tests |
| Monster Maze (all modes) | Partial | Per-mode mob speed, Body Rush duration, Cryo cooldown/freeze/radius and jump strength are profile-backed in the existing in-process engine | Mob/theme/stage/map controls, achievement/event preview paths, test event/phase actions and direct mode behavior tests |
| One in the Quiver | Partial | Shared rule overrides and common switches | Arrow/kit feature switches and kill/round event tools |
| Paintball | Partial | Shared rule overrides include Machine Gun heat capacity, shots-per-cycle and cooling | Weapon, reload, map and team-event controls |
| Plexball | Partial | Player limits, match/batting timing, rewind, ball physics, home-run distance and catch windows are profile-backed and consumed by the rule/runtime code | Remaining scoring/possession tunables, bot integration and direct match behavior tests |
| Runner | Partial | Shared rule overrides and common switches | Switcher/blocks/kit feature controls and phase/event tools |
| Sheep Quest | Partial | Shared rule overrides and common switches | Sheep/carry/kit/score controls and capture event simulation |
| SkyWars | Partial | Shared rule overrides and common switches | Chest/loot/island/void controls and event simulation |
| Snake | Partial | Hazard density/timing/steering, preparation countdown, segment spacing and speed scaling are profile-backed and consumed by `SnakeGameManager` | Item roll, ability, map and elimination controls; direct runtime behavior tests |
| Sneaky Assassins | Partial | Power-up timing/count, master-assassin threshold, reveal/smoke radii, disguise reveal cycles, NPC count/patrol/cull/recovery, and common session switches are profile-backed and used by `SneakyAssassinsRules` | Disguise/map/role event controls and direct runtime behavior tests |
| Snap | Partial | Photo range, cooldown, protection and match timing rules are profile-backed and consumed by its runtime; achievement conditions support preview-only notices | Remaining photo/outline/kit/phase controls and direct behavior tests |
| Speed Builders | Partial | View/build/intermission timing, match rounds, and build-time bounds are profile-backed and consumed by the round loop; authorized phase advance is available | Bot/profile selection integration, event triggers and direct round-loop tests |
| Spleef | Partial | Shared rule overrides and common switches | Kit, block-break, hunger and round-phase switches/event tools |
| Super Sploor | Partial | Ink, shot, squid, territory and round constants are profile-backed and consumed by its runtime | Remaining kit/event controls and direct behavior tests |
| Turf Wars | Partial | Shared rule overrides include build/combat phase duration and melee/sword/shredder damage | Build/combat/territory/respawn switches and phase/event tools |
| Wither Assault | Partial | Minion count and common shared-session switches are profile-backed and consumed by the manager/rules | Tower/phase/event controls and direct gameplay behavior tests |
| Wizards | Partial | Player limits, starter-spell rolls, mana conversion, Soul Star mana rate and endgame strike timing are profile-backed and consumed by `WizardsGameManager`/`WizardsRules` | Spell/chest event controls, phase tools and direct live-round tests |

## Completion gate

Do not mark a game complete based on a saved profile value alone. For each control, record the gameplay consumer and automated test that proves the value or feature switch changes the round. Confirm default-profile behavior matches the intended source mechanics and that testing does not write achievements or stats.
