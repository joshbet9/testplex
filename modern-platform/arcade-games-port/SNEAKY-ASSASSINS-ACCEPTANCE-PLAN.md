# Sneaky Assassins — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — available in the shared controlled Arcade queue. Live multiplayer and full disguise/NPC acceptance remain outstanding.  
**Map source:** No legacy 1.8 ZIP archive exists for this port; the modern mechanics module generates its own town with 200 source-style villager NPCs.  
**Scope:** One shared Sneaky Assassins game ID with four selectable source kits. The fifth-power-up Master Assassin state is an unlock, not a fifth kit.

The generated-town exception is explicit in `SourceMapCatalog.mapFoldersFor` and the build workflow. The older PR #47 is not merged; the integration branch already contains the newer implementation and test-bot/respawn safeguards. Do not reintroduce the older PR's stale code over the current mechanics.

## Town and disguise

- [ ] Start from the shared lobby and confirm the generated town, 200 NPC villagers, spawn area, movement bounds and spectator boundaries are initialized.
- [ ] All active assassins are disguised as villagers to other players while retaining the correct player combat identity, inventory, collision and damage handling.
- [ ] NPC villagers and player disguises remain distinguishable to the server so hits, attacks, despawns and kill attribution cannot target the wrong entity.
- [ ] The global reveal cycle follows the source 60-second cadence and four-second reveal window. Countdown warnings and scoreboard state agree with the actual reveal window.
- [ ] Revealer kit scans have a five-second player reveal duration and 12-block radius. The reveal must be visible to the intended viewers and expire without leaving the assassin permanently exposed.
- [ ] Smoke cloak lasts 1.2 seconds and affects the 5-block radius around the player; it must not grant a longer hidden state when refreshed rapidly or when the player disconnects.
- [ ] NPCs wander and attack on the source movement cadence. Verify they are culled/replaced when dead or invalid and that their count never grows beyond 200.
- [ ] The generated town and NPC proxies are removed after round end, stop, plugin disable and reconnect cleanup; no persistent lobby bot roster is created.

## Four source kits

- [ ] **Escape Artist:** source starting gear and smoke-bomb access; ability to disengage using the cloak window.
- [ ] **Assassin:** source offensive starting gear/ability and smoke-bomb access.
- [ ] **Briber:** bribe ability uses the eight-second cooldown, affects a valid nearby NPC only, and never converts or targets another player by mistake.
- [ ] **Revealer:** thrown reveal projectile follows its source radius and five-second duration; hit, miss, world change and chunk unload clean up projectile ownership and pending damage.
- [ ] Kit selection accepts the four source names/aliases, rejects arbitrary values (including `master-assassin`) and locks after the round starts.
- [ ] Check that the selected kit persists through the permitted lobby/respawn path but is reset appropriately between independent matches.

## Power-ups and combat progression

- [ ] First power-up appears after the source 20-second opening delay; subsequent spawns follow the ten-second cadence.
- [ ] Power-ups are collectable once, cannot be duplicated by simultaneous pickups, and respect the maximum five-count progression.
- [ ] Weapon progression is Wooden → Stone → Golden → Iron → Diamond with source damage values; no pickup downgrades a weapon or advances beyond Diamond.
- [ ] Armour progression is Leather → Chainmail → Iron, slot by slot. Diamond or unrelated armour does not enter the source upgrade chain.
- [ ] The fifth power-up unlocks Master Assassin and the dedicated compass; it does not appear as a selectable kit. Verify the compass nearest-opponent tracking, action-bar/scoreboard text and update cadence.
- [ ] Eliminations, kill attribution, respawn, power-up state and weapon/armour progression remain coherent when players disconnect, reconnect or are replaced by a new Bukkit Player instance.

## Automated test-harness safety and cleanup

- [ ] Test bots are excluded through the current `isTestPlayer` predicate from NPC targeting, combat, scoreboard player registration and progression.
- [ ] Match teardown waits for FakePlayerPlugin death/respawn hand-off and Bukkit offline state before despawning; orphaned test players have the bounded cleanup fallback.
- [ ] New matches are blocked while a previous bot/player batch is joining or being cleaned. Sneaky's separate player registry is rebound to the replacement Player instance after respawn.
- [ ] Validate no test-bot UUID is recorded as a human assassin, no orphan disguise or NPC remains after teardown, and no leaked scoreboard team survives a rematch.

## Controlled-queue acceptance gate

Sneaky Assassins is promoted for controlled testing because the generated town, 200-NPC loop, four source kits, global reveal, smoke/bribe/revealer abilities and power-up progression are implemented with deterministic parity tests. This is **not** live gameplay certification. Complete two-client and four-client runs, exercise all kits, reveal windows, NPC pressure, fifth-power-up unlock, bot harness teardown and repeated matches on the isolated Paper 26.3 server.
