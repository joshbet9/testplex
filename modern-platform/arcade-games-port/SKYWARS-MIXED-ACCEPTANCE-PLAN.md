# SkyWars Mixed Arcade acceptance plan

SkyWars Mixed is a separate 16-player free-for-all implementation. It does not replace or modify legacy Solo SkyWars or Team SkyWars. The current port contains the fast-round runtime, five source kits, original island/connector/middle loot families, throwable TNT, Zombie Guardians, island crumble and a closing world border. It remains gated until the 50-map archive/marker gate and live two-client/multi-client acceptance pass.

## Automated archive gate

- Confirm the exact 50 original ZIP filenames in `update/maps/Skywars` are also packaged under `source-maps/Skywars`.
- Parse every packaged `WorldConfig.dat` and require authored player `TEAM_SPAWNS` (or the source GREEN start fallback), map bounds and the LIME Throwable TNT generator location.
- Record the presence of RED Zombie Guardian markers and BROWN/GRAY/BLACK chest families exactly as authored. RED is optional in the legacy source; maps with no RED points correctly spawn no Zombie Guardians.
- Do not invent substitute player spawns, generator locations or chest markers when an archive is incomplete.

## Required live acceptance

1. Test at least two clients, then a full 16-player queue. Confirm each player is assigned to a separate island spawn, the selected map/border is isolated from the lobby, and spectators cannot affect the round.
2. Select every one of the 50 maps at least once. Verify authored island spawn placement, island/connector/middle chest locations, guardian locations, world bounds and arena reset between repeated rounds.
3. Verify all three chest families and their source loot pools: Island starter equipment/food/building blocks, Connector mid-tier equipment/projectiles/enchanted books, and Middle high-tier equipment/diamonds/experience/projectiles.
4. Verify the RED Zombie Guardians spawn with 15 health, gold armour, fire resistance, no natural despawn and no drops except one random Middle-loot reward on death. Confirm they remain tethered to their original RED marker and cannot chase players beyond eight blocks.
5. Verify the LIME generator creates the two-item Throwable TNT drop on the fast-mode 15-second cadence; test pickup, recharge, left-click throw, right-click drop, 1.6 throw strength, 60-tick fuse and 500 ms click gate.
6. Exercise each lobby kit (Ice, Fire, Air, Metal and Earth), including bridge/area burst/void save/double jump/armour magnetism/Throwable Dirt and their cooldown or upgrade levels. Confirm no kit state leaks between players or rounds.
7. Verify the fast-round clock, 5-second Prepare, eight-minute round ceiling, crumble starting 60 seconds after Live, block-by-block island/connector/middle crumble priority and 45-second border close to 32 blocks.
8. Confirm PvP, fall/void deaths, TNT deaths, disconnect recovery, spectator compass, winner/placement handling, scoreboard and cleanup across three consecutive matches.

The tests protect archive parity and deterministic timer limits. They do not prove loot randomness, NPC pathing, live combat balance or repeated-round behavior. Keep SkyWars Mixed gated until the live checks are recorded.
