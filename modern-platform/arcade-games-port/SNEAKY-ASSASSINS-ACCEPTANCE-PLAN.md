# Sneaky Assassins controlled-acceptance plan

Sneaky Assassins is **READY_FOR_TEST** for controlled sessions in its generated market-town test arena. The Mineplex Reborn repository does not contain an original map archive or authored town-NPC spawn group. NPC selection/cadence, safe destination checks and bribed-NPC recovery follow the source rules, but patrol anchors are generated and movement is velocity-steered rather than the original NMS Creature navigation. This promotion is for mechanics testing only, not authentic map or navigation parity.

## Implemented mechanics to exercise

- The four source kits: Escape Artist, Assassin, Briber and Revealer.
- Villager disguise/reveal cycle and 60-second global reveal.
- Smoke Bomb cloak/blindness, thrown Revealer, power-up beacon progression, weapon/armour upgrades and Master Assassin compass.
- 200 starting town NPCs; source Live cadence removes one random valid town NPC every four seconds with smoke feedback, excluding player disguise proxies. Missing/invalid entity cleanup remains separate.
- Per-NPC 1-in-40-per-tick patrol decisions select a spawn anchor within 16 blocks on each horizontal axis, try up to five safe offsets, and steer toward a supported/two-block-clear destination. This is a generated-arena velocity approximation, not the source NMS navigator.
- Bribery uses the 8-second cooldown, pursues the nearest visible enemy, attacks for one damage each second inside 1.5 blocks, and performs the source 800 ms stuck recovery.
- Kill bonuses, scoreboard, source damage protections and round teardown.

## Required live acceptance

1. Start a two-human match, then a full 16-player test roster. Confirm spawn spacing, disguise state, scoreboard rows and NPC count.
2. Verify global reveals on the exact 60-second cycle; players re-disguise after the four-second global reveal, while Revealer gives a five-second local reveal.
3. Exercise all four kits: Escape Artist's three Smoke Bombs, Assassin's bow and melee damage restrictions, Briber's eight-second cooldown and target allegiance, and Revealer's projectile/radius/distinct-target tracking.
4. Verify first power-up appears after 12 seconds, at most two active beacons, subsequent seven-second cadence, weapon/armour upgrades, the fifth-pick Master Assassin compass and bonus pickups after full progression.
5. Confirm 200 town NPCs at round start; during Live, exactly one random town NPC is removed every four seconds with source smoke feedback, with no drops/XP and no effect on player disguise proxies. Confirm ordinary villager trading/interactions are blocked and NPCs do not burn.
6. Observe independent NPC patrol orders (about 1-in-40 per tick), nearby-anchor filtering, five safe offset attempts and generated-town path steering. Verify bribed NPCs ignore their owner/cloaked targets, chase a visible enemy, attack once per second inside 1.5 blocks and recover from a stationary obstruction after 800 ms.
7. Confirm player quit/respawn, disguise visibility, compass, scoreboard restoration, NPC cleanup, power-up block restoration and repeated rounds.

The unit tests protect kit names, damage/progression thresholds, source NPC spawn/cull timing, patrol selection and safe surfaces, bribed-NPC recovery and achievement conditions. Human two-client acceptance and authentic map/NMS-navigation parity are still outstanding; do not enable this game solely because CI passes.
