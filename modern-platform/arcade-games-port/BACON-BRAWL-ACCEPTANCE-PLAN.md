# Bacon Brawl — shared Paper 26.3 acceptance plan

## Test objective

Bacon Brawl is included in the shared Testplex Modern acceptance queue so it can be tested in the same session as Death Tag, One in the Quiver and Bomb Lobbers. This is a controlled test promotion, not a claim that the game has passed live gameplay acceptance.

Start a quick round with:

```text
/arcadeport play bacon-brawl
```

For a multiplayer round, queue testers with `/arcadeport join bacon-brawl` and start with `/arcadeport start bacon-brawl`. Select a Bacon Brawl kit before the round:

```text
/arcadeport kit el-muchacho-pigo
/arcadeport kit mama-piggles
/arcadeport kit pig
```

Kit changes made during an active round should apply to the next round, not immediately.

## Required live checks

### Map and round lifecycle

- [ ] The original Bacon Brawl map archive loads into its own isolated world; it does not alter the lobby or another game world.
- [ ] Source Green `TEAM_SPAWNS` markers are read correctly and the 16-player spawn layout is sensible.
- [ ] All participants spawn as Pig/Pink Sheep avatars and remain scoped to this match only.
- [ ] The round ends when one pig remains. There is no fixed round timer and no respawn loop.
- [ ] Starting with two pigs applies the intended initial knockback pressure immediately.
- [ ] The global knockback escalation continues at the intended cadence when two pigs remain, without accelerating faster than the source rules.
- [ ] Hunger drains at the configured doubled rate; successful hits restore two hunger points, and starvation damage is tracked without allowing fire ticks to damage players.
- [ ] A player disconnecting, the round ending, or an operator stopping the match cleans up the arena, avatars, tasks, scoreboard and player state.

### Kits and combat

- [ ] El Muchacho Pigo: Body Slam consumes meter, applies the expected meter-scaled knockback, and emits the electric-spark feedback.
- [ ] Pig: the defensive/invincibility ability consumes meter, scales up to its configured maximum duration, and shows glowing/totem-particle feedback.
- [ ] Mama Piggles: Bacon Blast consumes meter, scales launch/impact power, and shows the Soul Fire Flame trail.
- [ ] Chris P Bacon: Crispy Bacon flames opponents visually without fire damage; grounded crouch activation dashes about five blocks, leaves real server-side fire blocks that every player can see, and restores those blocks after the short trail expires or the round ends.
- [ ] Ability activation requires the source Iron Axe/right-click interaction and does not trigger from unrelated target-surface clicks.
- [ ] Meter gain is 5% per ordinary hit and 10% per critical hit at 16 alive players, scaling to 10%/20% with two alive players; it clamps at 100% and abilities require at least 50% charge.
- [ ] The hotbar meter fill and swing-reset hit-timing bar remain readable during combat; the bar scales with the current global knockback multiplier.
- [ ] The scaled timing window rewards the run-back-in rhythm as knockback rises; vanilla falling criticals remain critical regardless of timing.
- [ ] Hits show the legacy red hurt flash and impact feedback on the visible pig or sheep avatar.
- [ ] Bacon Blast resolves the first collision along its swept path, has the expected hit radius/expiry, and does not break source arena blocks.
- [ ] The Brutus guard blocks front hits, backstabs bypass it, and Pig Cloak duration/behaviour match the selected kit.
- [ ] Knock-off/fall/water/lava eliminations credit the recent attacker only inside the configured five-second window and do not create duplicate kills.
- [ ] Round kill score credits a human attacker for a test-player victim when that bot is knocked out; test-player progression remains excluded from persistent awards.

### Avatar hit registration

- [ ] With PacketEvents 2.14.0 installed, avatar attack packets resolve against the hidden player only after server-side reach and line-of-sight validation.
- [ ] Out-of-range, obstructed, self, dead-attacker and dead-target hits are rejected.
- [ ] Without PacketEvents, the documented fallback avatar hit handler remains functional and the plugin still starts cleanly.
- [ ] Avatar packet handling does not affect players or entities outside an active Bacon Brawl round.

### HUD, results and stats

- [ ] The match-only sidebar lists only surviving pigs.
- [ ] The previous lobby/sidebar scoreboard is restored when the round ends.
- [ ] The survivor sidebar shows individual names while they fit and uses a count such as "16 players remaining" for the final row when more than 15 are alive.
- [ ] Elimination feedback and the last-pig winner are clear to both participants and spectators.
- [ ] `/stats` → Bacon Brawl displays the local achievement entries: King of Bacon (15 wins), Pigs In Blankets (six kills in one game), Flash Fried (win within 60 seconds), Raw Pork (win without using an ability), and Three for One! (three Body Slam kills in one game).
- [ ] Catalog awards also trigger once and persist for Get Some Pork on Your Fork, GOAT Sty, Mama Get the Camera, Got em’, and Hamished.

## Known implementation gaps — do not silently mark as passed

- Achievement triggers are wired to the isolated test config. Verify the in-game bottle list and confirm each award is delivered once and persists across a restart before marking achievements accepted.
- Bacon Brawl cosmetics and win-scene customisation are deferred.
- PacketEvents is optional and is not bundled with the Arcade plugin. The avatar packet path therefore needs a separate check on a test server where PacketEvents 2.14.0 is installed.

## Exit criteria

Keep Bacon Brawl enabled during the combined test session unless a defect makes the server or other games unsafe. Record each failure against the relevant section above, fix the underlying issue, and rerun the affected check. Only mark gameplay acceptance complete after the map, lifecycle, combat, HUD, cleanup and live achievement behaviour have all been exercised on Paper 26.3.
