# Bomb Lobbers — controlled acceptance plan

**Status:** Enabled for controlled Paper 26.3 acceptance testing. Not yet gameplay-accepted.

## Existing implementation

The current port documents the authentic archived map and team spawn centres, 20-minute source timeout, TNT throw/fuse/wall-contact lifecycle, eight-second detonation, direct-hit detection and feedback, legacy armour damage/knockback, blast exposure and block conversion, team-side checks, three kits (Acrobat, Armorer and Constructor), double-jump recharge, 4–8-second TNT regeneration for all kits (max three carried), Constructor's three defense shapes, spectator compass, water damage and isolated achievement/stat tracking.

The original map archive is packaged at `maps/bomb-lobbers.zip`.

## Preflight

- [ ] Confirm `bomb-lobbers` appears as available in the Paper 26.3 Game Panel.
- [ ] Confirm the packaged source map imports and loads without a fallback/generated arena.
- [ ] Confirm the first launch and a second launch both reset/load the map cleanly.
- [ ] Confirm the match starts with the expected teams and their archived spawn centres.
- [ ] Confirm the test-player roster fills to 16 total participants and the test players are actual match participants, not lobby-wide NPCs.
- [ ] Confirm a human can leave/stop the game without leaving fake players or a locked match behind.

## Gameplay acceptance

### TNT and combat

- [ ] Throw TNT and confirm the fuse refreshes on throw as intended.
- [ ] Confirm wall contact and the eight-second detonation behaviour.
- [ ] Confirm direct TNT hits register at the source-expanded hitbox boundary and display the expected hit/pickup feedback.
- [ ] Verify armour reduction, damage and knockback against both human and test players.
- [ ] Verify explosion exposure and block conversion, including walls/corners and line-of-sight edge cases.
- [ ] Attempt to exploit the opposing team's side; confirm the team-side restrictions prevent it.
- [ ] Confirm players cannot receive duplicate damage/credit from one detonation.

### Kits

- [ ] Acrobat: stronger double-jump activation, recharge, exhausted feedback and reset between rounds.
- [ ] Armorer: metal armour/damage reduction, weaker double jump and correct inventory.
- [ ] Constructor: weaker double jump, persistent selector, left-click cycles Platforms → Walls → Stairs, right-click **a block** to build the selected geometry. Verify no block is overwritten or placed inside a player and teardown restores original blocks.
- [ ] TNT regeneration: all three kits receive one TNT every 8→4 seconds as the match ages, with three carried maximum.
- [ ] Verify Pitcher no longer appears in selection and saved legacy Jumper/Waller preferences map safely to Acrobat/Constructor.
- [ ] Constructor's four approved prerequisites: Two birds, one stone; What's a double jump?; Blast Proof; Jelly Skin. Confirm previous Waller access is handled intentionally.

### Arena hazards and match flow

- [ ] Confirm water applies the intended five damage per second.
- [ ] Confirm eliminations, alive counts, team standings and win detection.
- [ ] Confirm the 20-minute timeout ends the round correctly.
- [ ] Confirm the spectator compass targets valid living opponents and remains usable after multiple eliminations.
- [ ] Confirm a winner and all losers return to the shared Arcade lobby with inventory, location, health, effects and game mode restored.

### Progress and achievements

- [ ] Ultimate Killer: five kills.
- [ ] Two birds, one stone: two players credited to the same TNT, including direct-hit then later blast credit.
- [ ] Team player!: five assists.
- [ ] What's a double jump?: win alive without double-jumping.
- [ ] The MVP: win alone against at least three opponents.
- [ ] Master Bomber: 30 wins.
- [ ] Blast Proof: complete five **separate wins as Armorer** (track across games; don't count a mid-match switch); previously earned flags remain unlocked. Jelly Skin retains its existing trigger.
- [ ] Not On My Watch: as Constructor, block three **distinct enemy TNT** with structures in one game; friendly TNT and repeated contacts with one projectile do not count.
- [ ] Technical: a close-range TNT only awards its own actual killing blow, with no stale credit for a later, different TNT.
- [ ] Over 9000: a 1v3 powerup-only comeback includes an actual power-up use in the clutch.
- [ ] Grenade From the Grave: two TNT kills after death, including direct-impact kills and blast kills.
- [ ] Test-player actions do not grant or persist player stats, achievements, rewards or personal-best records.

## Lifecycle / regression

- [ ] Normal win, normal loss, manual stop, failed start and consecutive matches.
- [ ] Disconnect and reconnect during queue, live round and spectator state.
- [ ] Server shutdown/restart during an active or queued match.
- [ ] Original map blocks and world state are restored after each outcome.
- [ ] No TNT, walls, tasks, scoreboards, compass targets, fake players or temporary entities remain after teardown.

## Acceptance decision

Mark gameplay acceptance only when every applicable check passes on the live isolated Paper 26.3 server. Record any failures with the exact reproduction steps, logs and expected/actual behaviour; keep Bomb Lobbers enabled while those defects are fixed and retested unless a defect makes the game unsafe to run.
