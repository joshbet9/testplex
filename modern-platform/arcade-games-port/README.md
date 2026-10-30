# Modern Arcade mechanics prototypes (Paper 26.3)

This plugin adds first-pass playable mechanics harnesses for the 14 Arcade games outside the dedicated Monster Maze and Snake modules. It is a compatibility/work-in-progress layer intended to expose API and game-loop problems early; it is **not** a claim that all original Mineplex mechanics or maps have been faithfully ported yet.

## Games covered in this first pass

- Bacon Brawl — 16-player source Green `TEAM_SPAWNS`, last-pig-standing, no respawns or fixed timeout, doubled hunger depletion, and faster global knockback escalation on a 20-second maximum cadence as soon as two pigs remain (including if the match starts with two). Combat abilities are damage-metered with no wall-clock cooldown: El Muchacho Pigo spends charge for a stronger Body Slam, Pig spends charge for a meter-scaled invincibility window (up to five seconds), and Mama Piggles spends charge to scale Bacon Blast power. Every successful hit updates the meter and gets visible timing feedback; rapid clicks weaken knockback, while measured 500–900 ms hits get the strongest multiplier. Each player's hotbar meter shows fill, and a match-only sidebar lists surviving pigs with meter percentages, restoring the prior scoreboard at round end. The Pig/Pink Sheep avatar proxy is scoped to match participants. Optional PacketEvents 2.14.0 redirects avatar hit packets to the hidden player with server-side reach and line-of-sight validation; without it, the fallback avatar hit handler is used. Required combat feedback includes a Soul Fire Flame Bacon Blast trail, electric sparks on Body Slam, and glowing/totem-particle feedback during Pig invincibility. The requested achievements are Flash Fried (win within 60 seconds), Raw Pork (win without an ability), and Three for one! (three Body Slam kills in one game); the existing Pigs In Blankets six-kill reward remains separate. Ability meters, hit timing, scoreboard isolation, and the three requested achievements have unit-level coverage; production achievement integration and live-server acceptance remain open. Cosmetics and win-scene customisation are deferred for this game.
- Bomb Lobbers — acceptance candidate: authentic archived map and team spawn centres, 20-minute source timeout, TNT throws with fuse refresh, wall-contact and eight-second detonation, source expanded-box direct hits with note-pling/hurt/pickup feedback, legacy armour damage and knockback, explosion exposure and block conversion, team-side cheating checks, all four kits (Jumper, Armorer, Pitcher and Waller), double-jump recharge, Craftman TNT regeneration, Waller's 5×3 wall, spectator compass, and the 5-damage-per-second water penalty. The isolated stats menu includes Ultimate Killer (5 kills), Two birds, one stone (2 kills from the same TNT, including a direct hit then later blast), Team player! (5 assists), What's a double jump? (win alive without double-jumping), The MVP (win alone against at least 3 opponents), Master Bomber (30 wins), plus the existing Blast Proof and Jelly Skin. Build and unit checks pass; it is not certified at 100% until the in-game mechanics are accepted.
- Death Tag — source-style Alpha opening chaser and 30-second Alpha promotions, death-triggered runner conversion, Chaser regeneration/haste switching to a one-way Runner Speed-I unlock when the strict 25% Chaser threshold is first crossed, Basher Slow III (2 seconds), Archer's two-arrow Fletcher with 2-second replenishment, 3x arrow knockback and remove-on-impact, Traitor's 0.8 knockback with its 400 ms per-target recharge, and flat Iron Skin damage reduction. Defeated Chasers respawn as their existing role; player-caused conversions count as kills. Original `89` light markers flicker between gold blocks and black wool (the original wool data value 15), with their pre-round block data restored after the match. Normal and Alpha Chasers use native Skeleton / Wither Skeleton visual proxies, with hits on the proxy redirected to the real player. This remains a server-side visual approximation rather than a packet-level player disguise.
- Dragon Escape — embedded Reborn terrain and RED/BLACK/GREEN source markers, Douglas's authored-route chase/destruction, map-specific finish checks, and the four Reborn kits. Jumper has 4 charges, Disruptor has 3 charges plus an 8-second/two-mine pickup-triggered TNT supply, Warper has 2 charges and one forward-player pearl, and Digger has 6 block-recovery/build uses with the source 10-second opening lockout, nearby-player restriction and 120-second block restore. Hell maps use source night time (16000); Pirate maps apply 2 water damage on the source 500ms check. The race HUD includes per-map PB delta and a live top-14 sidebar/tab ranking with leader elapsed time, approximate `~+` gaps and eliminated `OUT` rows. The opt-in PB ghost is a genuine client-side player entity using the owner's skin, rendered as a translucent invisible teammate; it has no server hitbox, collision or gameplay interaction and is sent only to its owner. New PBs record player position, rotation, sprint/sneak and grounded pose every server tick; the compressed per-map replay is stored under `dragon-escape-ghosts/<uuid>/<map>.ghost.gz`. Older PBs use their saved checkpoint locations as a lower-accuracy player-model fallback.
- Dragons (solo only) — source RED-marker spawns (one additional dragon every 5 seconds, up to five), source 125 ms dragon targeting/contact cadence and bounding-box hit detection, non-collidable dragon ghosts, PvP disabled, HungerSet=20 with saturation reset to 1, source Coward Leap vector/ground boost, Marksman Fletcher (one instant arrow, replenished to four every four seconds) and six-arrow Barrage (1-second first charge, 200 ms charge cadence, one arrow per tick when fired), Pyrotechnic sparklers (first charge after 25 seconds, two-item cap, source 48-block attraction and five-second lifetime), one-damage fall reduction and four-damage water ticks every 500 ms. The SoloGame alive/dead sidebar, spectator compass/menu, immediate Sparklez stat, survival-time gems and 20/15/10 placement plus 10 participation rewards are represented; gems are explicitly local Testplex ledger values. Dragons Teams is intentionally excluded. Defensive Dragon Flare remains separate: Coward 1, Marksman 3 and Pyrotechnic 2; six-second, 24-block exclusion zone, with six server-visible cosmetic choices.
- Evolution — source-form health/damage progression across Iron Golem, Blaze, Spider, Creeper, Slime and Chicken; kills unlock hold-sneak grounded evolution charge, faster with consecutive life kills; 2.2-second form change plus 7-second total animation; stage persists through death with a four-second respawn. Map-platform illusions and the full 1.8 perk/disguise stack remain to port.
- Gladiators — four source-style loadout tiers, paired tournament rounds, a five-second combat countdown and 60-second poison limited to unresolved 1v1 duels. Arena placement now reads the original map's GREEN/YELLOW/ORANGE/RED centre markers and nearest BLACK player spawns; per-arena doors/trails and full kit polish still remain.
- Micro Battles — original source map/barrier/spectator markers, Archer/Worker/Fighter loadouts, Fletcher replenishment and Fighter Iron Skin, plus MapCrumbleModule's vertical two-tick map scan and three farthest supported-block removals each tick after the one-second live gate.
- One in the Quiver — first-to-20 one-arrow kill loop, kill-earned Super Arrow, one-second respawn and breakable glass/glass panes; Leaper double-jump, Enchanter rebound and Ninja vanish are present, and Slam Shot now uses the source's radial 2D Ground Pound knockback (remaining kit/aesthetic and arena progression polish remains).
- Paintball — selectable Rifle, Sniper, Shotgun and Machine Gun kits; projectile-specific paint counts, scope-charge damage, team-coloured armour, Water Bomb stacks, a 250 ms Water Bomb gate, SAVE ME marker/revival within three blocks, nearby teammate cleanse with armour/health restoration, and the source-style high-pitched hit-confirmation sound. The Machine Gun keeps the Reborn 80 ms fire gate and 250 ms cooling start, but now gains heat at 1/48 of its bar per shot so it fires 48 shots before overheating. Paintball cosmetics use one combined `Weapon & Pellet Identity` line with independent weapon-finish and pellet-style selections: the weapon finish changes the held item model, while the chosen team-coloured pellet trail is broadcast with forced rendering to every player in the Paintball world, including distant spectators and clients using reduced particle settings. Cosmetic visuals do not change projectile velocity, hitboxes or paint damage.
- Runner — source Leaper/Archer/Frosty kit identities, 8-second Leap, 6-second Quickshot, Frost Balls replenished every 500 ms up to 16 with Slow II for two seconds, source-map bounds, trail material tracking, 600 ms decay and 1.2-second falling-block lifetime, and radius-2.5 arrow trail marking, and the source 4-damage water penalty.
- Sheep Quest — source GREEN/BLUE/RED/YELLOW pens, team sheep capture and steals, three-sheep carry limit, and the five-minute winner based on sheep in the pen; a tie at the end is a draw.
- Spleef — source kit selection (Snowballer, Brawler and Archer), snowballer replacement cap, Brawler Leap with a 30-second cooldown, Smasher, two-arrow Fletcher replenishment, source-timed hunger drain/restoration, source material damage transitions, 3D arrow-radius clearing, and the source 4-damage water penalty. Its Snowburst, Ice Shards and Chalk Grid block-break signatures are live renderers broadcast with forced particle delivery to every player and spectator in the dedicated match world.
- Turf Wars — team assignment, bow combat and territory block marking. Standard Turf Wars keeps its 40-second opening build, then 20-second build / 90-second combat cycles. The dedicated Mixed Arcade variant is started with `turf-wars-mixed` (or `mixed-turf-wars`): its opening build is 20 seconds, later build phases are 10 seconds, combat phases are 45 seconds, and each kill advances the frontline by two columns with both newly captured columns repainted. Standard Turf Wars timings and one-column advances are unchanged.

Each game gets its own separate modern test world, so arena generation/destruction does not use the live 1.8 Testplex world. Original Reborn map archives are embedded for Bacon Brawl, Bomb Lobbers, Death Tag, Dragon Escape, Dragons, Gladiators, Micro Battles, One in the Quiver, Paintball, Runner, Sheep Quest, Spleef and Turf Wars; these load into separate source-world folders and read WorldConfig.dat spawn groups and map bounds. Evolution remains generated because an Evolution map archive is not present in the source map catalogue.

## Commands

- `/gamepanel` → choose a supported game → **Start / Test**. The coordinator calls `/arcadeport play <game-id>`.
- `/arcadeport play <id>` — joins a quick-start window, giving others a few seconds to join.
- `/arcadeport join <id>` then `/arcadeport start <id>` — queue a multi-player test.
- `/arcadeport play turf-wars-mixed` or `/arcadeport join turf-wars-mixed` then `/arcadeport start turf-wars-mixed` — use the dedicated Mixed Arcade Turf Wars timings and double-column capture. This variant has its own queue and does not change standard `turf-wars`.
- `/arcadeport kit <el-muchacho-pigo|mama-piggles|pig|mama-pig|sheep-pig|jumper|dragon-escape-jumper|dragon-escape-disruptor|dragon-escape-warper|dragon-escape-digger|disruptor|warper|digger|armorer|pitcher|waller|snowballer|brawler|archer|rifle|sniper|shotgun|machine-gun|runner-basher|runner-archer|runner-traitor|runner-null>` — choose a game kit before a test; Bacon Brawl kits lock at round start, so changes apply on the next test; legacy kit aliases remain accepted.
- `/arcadeport ghost <on|off|toggle>` — enable or hide your Dragon Escape translucent PB player ghost.
- `/arcadeport status`, `/arcadeport stop` (operator only).

Game IDs are: `bacon-brawl`, `bomb-lobbers`, `death-tag`, `dragon-escape`, `dragons`, `evolution`, `gladiators`, `micro-battles`, `one-in-the-quiver`, `paintball`, `runner`, `sheep-quest`, `spleef`, `turf-wars`. The separate Mixed Arcade alias is `turf-wars-mixed` (`mixed-turf-wars` is also accepted).

## Acceptance boundary

Install PacketEvents 2.14.0 on the Paper 26.3 test server for both the optional Bacon Brawl packet-hit path and Dragon Escape's genuine translucent player ghost. PacketEvents is a provided/soft dependency and is not bundled into `ArcadeMechanicsModern.jar`; the plugin logs a clear warning and cannot render the player ghost without it.

CI verifies compilation, plugin metadata and catalogue/ID tests. It does not verify the mechanics in a running server. The first in-game iteration should cover command/GUI entry, each game start/end, weapon interactions, elimination/respawn, per-world cleanup and inventory restoration. Then compare the rough mechanics to the existing Reborn game sources and fix discrepancies one game at a time.

Game-specific cosmetic renderers are still deferred for most games, but Paintball weapon/pellet selections, Spleef block-break signatures and the Dragon Escape leader marker are now wired to their live gameplay hooks. Distinct win effects remain deferred until each game's mechanics and round lifecycle are stable.


## Dragon Escape race-timing acceptance

Dragon Escape has a source-comparison checklist for the PB ghost and live race timing:

1. Complete a full map once so the test server saves a map-specific PB and its checkpoint splits. Use `/stats` → Arcade → Dragon Escape to inspect the map PB list.
2. Toggle the personal ghost with `/arcadeport ghost on` (or the PB Ghost option in Dragon Escape stats), then rerun the same map. Confirm a full player model using the owner's skin appears semi-transparent, follows the recorded PB movement and pose continuously, is visible only to its owner, has no collision/hitbox, and disappears when the PB time is reached.
3. Confirm the PB delta remains in the action bar even when the ghost is off, updates through the final three-block finish radius, and changes sign when the current run moves ahead of or behind the PB.
4. Run with multiple racers. The sidebar and player list should agree on ranking, show the leader's elapsed time, show approximate `~+` gaps for trailing racers, and mark eliminated racers `OUT`. The sidebar should restore each player's pre-race scoreboard after the match.
5. Change maps and confirm PBs and checkpoint splits stay isolated per archive. Restart the server and confirm saved PBs, splits, and ghost preference persist.
6. Repeat on a tight switchback map and with Warper to verify checkpoint progress remains monotonic without false shortcut eliminations; separately confirm Douglas's chase and Digger/Disruptor/Leap interactions do not corrupt split timing.

The estimated time gaps use course distance at a fixed display pace; they are presentation estimates, not authoritative race timing. Build/unit checks do not replace live acceptance on the isolated Paper 26.3 server.


#### Death Tag parity continuation

The modern Death Tag port now resolves environmental damage with the legacy Iron Skin exclusions, gives the Null and Archer runner kits their perkless unarmed melee hit, and applies default melee knockback where the source perks do not suppress it. Every Archer arrow now follows the source 300% knockback hit path, including the final arrow in the inventory. Chasers use native Skeleton / Wither Skeleton visual proxies while the real player remains the combat entity; melee and projectile hits on the proxy are forwarded to that player. This is a server-side visual approximation, not a packet-level player disguise. Living players on both sides receive the source-style nearest-opponent Tracking Compass with distance and height feedback.

The Death Tag sidebar shows live Runner and Chaser counts. Forced time-based Alpha promotions happen in place, and converted runners retain their death-location respawn; defeated Chasers respawn at a configured Chaser spawn. Only surviving Runners receive winning placement; if all Runners are gone, the round ends without declaring a Chaser winner, matching the source placement logic.

Death Tag also enforces the source Adventure mode, blocks item-frame interaction, and continually restores food to 20 with saturation 1.0 so sprinting remains available for the full round.

Death Tag also tracks the legacy ComeAtMeBro stat locally: every Chaser defeat from a player's second Chaser kill onward is recorded when the acceptance run completes. This stays in Testplex's isolated local stats and does not write to production Arcade data.

Death Tag Runner trails now read the legacy `cosmetics.yml` choices: all 16 stained-glass colours and the Classic, Soul, Sparks, Smoke, Ender and Hearts styles (defaulting to white/classic). Source-default restrictions are also mirrored: ground pickups, block inventories and crafting-grid clicks are disabled; runner conversions preserve their death location.

The Runner Speed I threshold is a one-way unlock: once the live Chaser-to-player ratio first rises strictly above 25%, Runners retain Speed I for the rest of the round, even if a later disconnect changes the ratio.

The isolated Death Tag stats page now records lifetime local Kills, Deaths and K/D Ratio, alongside Come At Me Bro. Runner-to-Chaser conversions count as deaths; each time a Chaser is defeated and respawns also counts as a death. These counters remain local to Testplex and do not update production stats.


## Paintball 48-shot / cosmetic acceptance

1. Play Machine Gun and fire continuously at the normal 80 ms cadence. Confirm 48 shots can be fired before the heat bar reaches its limit; the 49th shot must be blocked until the gun cools. Stop firing and verify cooling begins after the 250 ms recharge gate and drops the heat by 0.02 per server tick.
2. Keep the same kit equipped while changing the `Weapon & Pellet Identity` menu. Confirm the menu remains one Paintball cosmetic line, with exactly one selected weapon finish and one selected pellet style at the same time. The selected held-item finish must retain the correct kit display name and weapon behaviour.
3. Test each weapon finish from a second client and as a spectator. Chrome Marker uses the kit's base weapon model; Splatter Cannon uses the Crossbow model; Neon Suppressor uses the Blaze Rod model. The finish must not change which kit is firing.
4. Fire each pellet style with Rifle, Shotgun, Machine Gun and Sniper. All players in the match world, including distant spectators and clients using reduced particle settings, must see the same team-coloured Colourburst, Paint Drip or Stencil Stamp trail. Verify pellet cosmetics do not alter projectile speed, spread, sniper charge/damage, paint-piece count or hit detection.
5. Confirm Water Bomb revival/cleanse, team colours, elimination scoring, and round cleanup still work with each cosmetic selected. Change a cosmetic mid-round and verify the weapon finish refreshes and newly fired pellets use the new selection; already-fired pellets keep the style they were launched with.
6. Restart the server and verify the one-line weapon/pellet selections persist independently for the player.

## Runner — achievements and map-aware trail cosmetics

The modern Runner port includes these local test achievements:

| Achievement | Requirement |
| --- | --- |
| Gettin’ those steps in! | Walk over 400 horizontal blocks in one Runner game. |
| Hop to it | Jump 100 times in one Runner game. |
| Flash | Win a Runner game within 90 seconds. |

Progress is tracked per player and per round. The walk counter accumulates horizontal movement while excluding teleports and implausible position corrections. The jump counter uses Paper's dedicated jump event. Achievement unlocks persist in the isolated mechanics config and appear on the Runner page of `/stats`.

Runner's `/arcadecosmetics` entry now offers 15 non-red trail colours. At the start of a round, the mechanics port samples exposed course surfaces from the selected archived map palette. If a player's chosen colour is already present on that map, it selects an unused alternative; if every colour is represented, it chooses the least represented alternative. The selected trail is applied as actual world-block changes, so players and spectators all see the same colour rather than a client-only particle effect.

Live acceptance still needs to check the three achievement thresholds, the 90-second win boundary, a second client observing another player's trail, and a colour clash on a map containing the requested trail colour.


## Spleef — cooldown and block-break visibility acceptance

1. Equip Brawler and use Leap. Confirm a second use is rejected until 30 seconds have elapsed, with the remaining cooldown shown; changing kits during the cooldown must not reset it.
2. Equip each Block Break Signature option (Snowburst, Ice Shards and Chalk Grid) in `/arcadecosmetics`, then break tiles with a Brawler pickaxe, Snowballer snowball and Archer arrow.
3. Observe each effect from a second player and a spectator positioned far from the break. All viewers in the Spleef match world must receive the selected effect, including during Brawler's multi-block Smasher and arrow-radius damage.
4. Confirm slow-damage colour transitions do not play a break effect until the block is actually destroyed, and the original block material is used for the Ice Shards fragments.

The catalogue and renderer contract are covered by unit tests. A passing build does not replace live two-client acceptance on Paper 26.3; Spleef remains `MECHANICS_IN_PROGRESS` until that check is completed.
