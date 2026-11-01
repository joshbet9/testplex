# Wither Assault readiness and acceptance plan

Wither Assault remains outside the 22-game enabled queue, but is **READY_FOR_TEST** for isolated controlled mechanics sessions using a generated arena. The original Industrial West, Labyrinth and Sky Ruins archives are not present in the repository, so the generated layout is not authentic source-map parity. The source kit names are Human Archer, Human Medic and Human Editor.

## Implemented in this readiness pass

- Human Archer, Medic and Editor are selectable in the lobby and saved per player.
- Human loadouts and team-coloured leather armour are applied at round start. Human Archer and Editor receive bows; both have Fletcher replenishment to a four-arrow cap.
- Archer and Editor bow shots are tracked as Smoke Arrows. Hitting or approaching a Wither with a Smoke Arrow applies the source four-second Blindness effect.
- Human Medic receives the source 45-second Revival Potion refill behaviour when out of potions.
- The Wither is blinded for its opening 15 seconds but can still move and attack during that time.
- Wither controls now match the source item layout: Gold Sword left-click launches Wither Skull; Diamond Sword left-click launches three Wither Minions; right-click either sword starts the XP-bar-powered Fire Storm; Compass right-click uses Smell Humans.
- The custom Revival Potion splash event revives a nearby downed Human at their Wither-out marker.
- Editor blocks are tracked, temporarily removed for two seconds by any Human right-click, constrained by the four-block floor rule and protected from breaking directly under another player. Editor-placed blocks are restored at round end.

## Remaining parity and live-acceptance work

1. Implement and validate the Human Medic's Repair Master and Healing Hands behaviours; the refill and revival potion paths alone do not complete the Medic kit.
2. Compare Wither Skull proximity/damage scaling, Wither movement/slowdown near the ground, terrain destruction, Wither immunity and minion damage/targeting against the source.
3. Obtain and package the original Industrial West, Labyrinth and Sky Ruins archives, or explicitly continue treating the generated arena as a non-source test harness. Do not describe the generated arena as authentic.
4. Exercise Editor placement, breaking, pickup, the under-player restriction and two-second removal with multiple Humans; verify no map state leaks between rounds.
5. Run repeated human-vs-wither sessions with real clients, including down/revive, void falls, disconnects, role rebalance, scoreboard, rewards and full lobby restoration.

The custom Wither achievement triggers in Testplex are local extensions; the original game reference does not list native achievements. The unit tests cover Human kit IDs, Editor boundaries, Archer/Editor Smoke Arrow registration rules and Wither sword ability controls. Live Paper 26.3 acceptance is still outstanding.
