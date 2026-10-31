# Spleef — Paper 26.3 controlled acceptance plan

Spleef is promoted to `READY_FOR_TEST`, not declared gameplay-accepted. It uses the shared Testplex Arcade lobby, player snapshots, queue, kits, scoreboard, spectator and teardown lifecycle. `Super Spleef` is an archive-era rename/collection for the same Spleef game; it is not a second mode, game ID or queue entry.

## Source map parity

All individual ZIPs from both legacy 1.8 Testplex folders must be present in the built `ArcadeMechanicsModern.jar` and visible in the same `/arcadeport maps spleef` selector.

| Legacy folder | ZIP archives |
|---|---:|
| `update/maps/Speef` | 32 |
| `update/maps/Super Spleef` | 5 |
| **Total source archives** | **37** |

The Maven resource configuration packages both directories under `source-maps/`. The build workflow compares the sorted source filenames with the actual JAR entries for every enabled game's source folders, including both `Speef` and `Super Spleef`, plus both Paintball folders. A missing or unexpected archive fails CI. Identical legacy ZIP blobs are retained as source assets; the selector must not expose a separate Super Spleef game or variant.

## Implemented source mechanics to verify

- Solo last-player-standing match, no PvP damage, source night vision and compass tracking.
- Three original kits: Snowballer, Brawler and Archer.
- Snowballer: starts with snowballs and receives replenishment when breaking blocks, up to the source cap of 16.
- Brawler: stronger knockback, Smasher multi-block break, and Leap with a 30-second cooldown.
- Archer: Fletcher arrow replenishment every two seconds, capped at two arrows; arrows break the impact tile and a velocity-scaled radius.
- Block breaking is intercepted rather than relying on vanilla drops. Bedrock and lava are protected; source slow-damage transitions apply to wool, terracotta, stone, stone bricks, grass and planks.
- Hunger drains by two every four seconds; breaking blocks restores hunger on the source short cooldown. Water damage remains four per source damage tick.
- Spectator compass and target menu, nearest-living-player tracking, spectator teleport cooldown, blocks-broken stat and match-only scoreboard.
- Block-break signature cosmetics: Snowburst, Ice Shards and Chalk Grid. Effects must be visible to all players and spectators in the match world.
- Match end, player death/disconnect, world unload, block/map cleanup, inventory/scoreboard restoration and return to the shared lobby.

## Manual two-client acceptance

1. Start `/arcadeport play spleef` and confirm the shared lobby launches a normal Spleef session.
2. Open `/arcadeport maps spleef`. Confirm the selector reports all 37 source archives across both legacy folders, without a separate Super Spleef entry or mode. Select at least one map from each folder and verify its WorldConfig spawns and arena bounds.
3. Test Snowballer, Brawler and Archer separately. Confirm kit-specific inventory, abilities, replenishment caps and cooldowns; switching kits must not reset an active Brawler Leap cooldown.
4. Break each supported material family. Confirm the source slow-damage colour/material progression, actual destruction, hunger restoration and block-break stat. Confirm bedrock and lava cannot be removed.
5. Test Snowballer projectiles and Archer arrows against floors and edges. Confirm impact block selection, radius scaling and that arrows/snowballs do not damage players.
6. Verify the 4-second hunger drain and water penalty, then test knockback and Brawler Smasher with two clients.
7. Eliminate one client. Confirm spectator mode, nearest-player compass, target menu and teleport cooldown. Confirm the living player remains in the round.
8. End the round and repeat with a disconnect. Confirm all participants return to the shared lobby, inventories/health/food/flight/scoreboards are restored, temporary world changes are removed and a new match can start cleanly.
9. Repeat the block-break cosmetic visibility check with a second player and a distant spectator for all three signature choices, including multi-block Smasher and arrow-radius breaks.

## Commands

- `/arcadeport play spleef`
- `/arcadeport maps spleef`
- `/arcadeport kit snowballer`
- `/arcadeport kit brawler`
- `/arcadeport kit archer`

CI validates compilation, unit tests, source folder mappings and exact packaged archive parity. Live in-game acceptance remains outstanding until the two-client checklist has been completed on the isolated Paper 26.3 server.
