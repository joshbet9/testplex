# Paintball — shared Paper 26.3 acceptance plan

## Test objective

Paintball joins the shared acceptance queue as a controlled candidate. The port embeds both source map folders (Paintball and Super Paintball), supports the four kits, team-coloured projectiles and arena painting, and mirrors the SAVE ME / Water Bomb revive loop.

Start a match:

```text
/arcadeport play paintball
```

Set a kit before starting or joining:

```text
/arcadeport kit rifle
/arcadeport kit sniper
/arcadeport kit shotgun
/arcadeport kit machine-gun
```

## Source maps and teams

- [ ] Both original map folders, Paintball and Super Paintball, are available from the embedded source archive.
- [ ] Match launches load a dedicated isolated Paintball world and the map's source spawn markers/bounds.
- [ ] Frost/AQUA is assigned first and Nether/RED second; all players spawn in the correct team area.
- [ ] Team-coloured armour is AQUA for Frost and RED for Nether.
- [ ] The team objective and live HUD/sidebar make current armour, team and round status understandable.
- [ ] Teammate damage is rejected for every projectile type; melee hits do not paint players.
- [ ] The round ends when only one team remains, not simply when one player is left alive.
- [ ] The twenty-minute timeout selects the team with the better live standing and produces a draw when standings are tied.

## Kits and combat

- [ ] Rifle fires a fast single Snowball/Ender Pearl projectile, paints three armour pieces, and grants Speed I.
- [ ] Sniper scopes while sneaking, charges the shot, fires a long-range Arrow, applies the intended Slowness while scoped, and paints one through four pieces according to charge.
- [ ] Shotgun fires eight scattered pellets, paints one armour piece per hit and grants Speed II.
- [ ] Machine Gun respects its 80 ms fire gate, 48-shot heat cycle and 250 ms cooling cadence; its experience bar remains a readable heat indicator.
- [ ] Armour damage is consistent: a player is only painted out after all four armour pieces are painted, and health tracks remaining armour.
- [ ] A painted armour piece changes to the eliminated colour; non-paintable blocks are untouched.
- [ ] Wool and terracotta within the paint-impact radius are recoloured for the firing team.
- [ ] Every temporarily recoloured block is restored to its exact original block data after the round, including on an operator stop.
- [ ] Pellet cosmetic trails (Colourburst, Paint Drip and Stencil Stamp) are visible to players and spectators without altering projectile physics or damage.

## Water Bomb, SAVE ME and revival

- [ ] Water Bomb stack is correct for each kit and the 250 ms throw gate prevents accidental rapid double throws.
- [ ] A teammate who is painted out enters spectator state with one visible, team-coloured SAVE ME marker.
- [ ] The marker follows the saved location and remains available for a valid teammate revive.
- [ ] A Water Bomb within three blocks of the marker revives that teammate, restores four armour pieces and health, and returns them to the correct team.
- [ ] A Water Bomb also cleans nearby living teammates without reviving enemies.
- [ ] Repeated or overlapping Water Bombs cannot revive a player more than once or leave duplicate markers.
- [ ] If the out player disconnects or the match ends, the marker and revive state are removed.

## Spectators, stats and cleanup

- [ ] Eliminated players receive a nearest-opponent Tracking Compass and cannot drop it.
- [ ] The compass target/action-bar distance and height values update as opponents move.
- [ ] Natural satiety health regeneration remains disabled; Water Bomb cleaning/revival still restores health intentionally.
- [ ] Killing Spree, Last Stand, Medic, Speedrunner and Flawless Victory counters award only at their exact conditions.
- [ ] Test-roster players do not persist stats or achievements.
- [ ] Wins, kills, deaths, revives and other round stats are written once per match.
- [ ] Ending or restarting a match restores player inventory, armour, effects, health, game mode, flight and lobby state.
- [ ] Tracked projectiles, particle trails, SAVE ME markers, temporary scoreboard state and recoloured blocks are cleaned up.
- [ ] A disconnect, stop command or server restart cannot leave a live session or modified map blocks behind.

## Exit criteria

Keep Paintball enabled in the shared queue while fixing issues discovered. Do not mark it gameplay-accepted until both source maps, all four kits, team combat, block restoration, Water Bomb revive, spectator tools, stats and cleanup have been exercised with multiple players on Paper 26.3.
