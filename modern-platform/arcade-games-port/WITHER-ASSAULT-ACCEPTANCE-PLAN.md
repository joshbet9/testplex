# Wither Assault readiness and acceptance plan

Wither Assault remains MECHANICS_IN_PROGRESS. The Mineplex Reborn source archive is absent from this repository, so Testplex uses an isolated generated arena; a generated layout is not a claim of authentic map parity. The source kit names are Human Archer, Human Medic and Human Editor.

## Implemented in this readiness pass

- Human kit choices are exposed in the lobby and saved as per-player preferences.
- Match loadouts now apply Archer, Medic and Editor inventories and team-coloured armour; the Wither receives its Wither Skull, minion, scent and shield/Fire Storm controls.
- Existing Wither Skull, minion and scent routines are connected to the Wither player's items, respecting the 15-second opening protection.
- The custom Revival Potion now revives a nearby downed Human when its splash reaches their Wither-out marker.
- Editor blocks are tracked, can be temporarily removed by right-click for two seconds, obey the four-block floor limit and cannot be broken directly under another player. Placed blocks are restored to their original state at round end.

## Still required before enabling

1. Port and validate the source Human Archer Smoke Arrow and the remaining Human/Medic/Editor passives and ability timing.
2. Compare Wither flight, immunity, Fire Storm, Wither Skull, minions, scent, down/revive and all role loadouts against Reborn source behaviour.
3. Replace the generated arena only when authentic archived map data becomes available; until then validate the test arena separately and do not describe it as source-map parity.
4. Add meaningful gameplay award paths for Eye Candy and SPLIT UP!, and map-tracked wins for Wither World and It's a man's world only if the source map set is available.
5. Run repeatable multi-human and Wither-vs-Human sessions, then test disconnects, conversions, cleanup and lobby restoration.

The unit tests cover kit normalization and Editor block boundaries; live Paper 26.3 acceptance is still outstanding.
