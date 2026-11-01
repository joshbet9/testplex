# Sneaky Assassins controlled-acceptance plan

Sneaky Assassins remains gated. The Mineplex Reborn repository does not contain an original map archive or authored town-NPC patrol routes, so the Paper 26.3 port uses a generated market-town test arena, authored spawn/power-up locations and a simplified NPC movement controller. This is deliberately a test substitute, not authentic map parity.

## Implemented mechanics to exercise

- The four source kits: Escape Artist, Assassin, Briber and Revealer.
- Villager disguise/reveal cycle and 60-second global reveal.
- Smoke Bomb cloak/blindness, thrown Revealer, power-up beacon progression, weapon/armour upgrades and Master Assassin compass.
- 200 persistent town NPCs; NPCs now leave the active roster only when missing/invalid or actually killed, not through periodic random deletion.
- Bribery with the 8-second cooldown and persistent allegiance until the bribed NPC is removed.
- Kill bonuses, scoreboard, source damage protections and round teardown.

## Required live acceptance

1. Start a two-human match, then a full 16-player test roster. Confirm spawn spacing, disguise state, scoreboard rows and NPC count.
2. Verify global reveals on the exact 60-second cycle; players re-disguise after the four-second global reveal, while Revealer gives a five-second local reveal.
3. Exercise all four kits: Escape Artist's three Smoke Bombs, Assassin's bow and melee damage restrictions, Briber's eight-second cooldown and target allegiance, and Revealer's projectile/radius/distinct-target tracking.
4. Verify first power-up appears after 12 seconds, at most two active beacons, subsequent seven-second cadence, weapon/armour upgrades, the fifth-pick Master Assassin compass and bonus pickups after full progression.
5. Confirm town NPCs persist through a long round, are killable without drops/XP, cannot be traded/interacted with normally, do not burn, and a bribed NPC continues to target enemies until killed/removed.
6. Confirm player quit/respawn, disguise visibility, compass, scoreboard restoration, NPC cleanup, power-up block restoration and repeated rounds.

The unit tests protect kit names, damage/progression thresholds, NPC persistence policy and achievement conditions. Human two-client acceptance and source-map/NPC-route parity are still outstanding; do not enable this game solely because CI passes.
