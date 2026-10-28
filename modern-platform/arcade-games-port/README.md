# Modern Arcade mechanics prototypes (Paper 26.3)

This plugin adds first-pass playable mechanics harnesses for the 14 Arcade games outside the dedicated Monster Maze and Snake modules. It is a compatibility/work-in-progress layer intended to expose API and game-loop problems early; it is **not** a claim that all original Mineplex mechanics or maps have been faithfully ported yet.

## Games covered in this first pass

- Bacon Brawl — a 2-minute free-for-all kill race with respawns, source GREEN-map spawns and two knockback escalations; selected balance change doubles hunger drain. Official kits are El Muchacho Pigo (Body Slam), Mama Piggles (Bacon Blast/Speed II), and Pig (5-second cloak/10-second cooldown plus directional backstab). Old `mama-pig` and `sheep-pig` command aliases remain accepted; Chris P. Bacon is omitted. Bacon Blast hits resolve through one path to prevent duplicate damage. Packet-level player disguises, Mama Piggles' baby companion and cosmetics still need acceptance.
- Bomb Lobbers — timed TNT throws, block/fuse detonation, direct-hit damage with the original note-pling feedback and light direct-hit knockback; team timeouts now use surviving players and team kill totals rather than an arbitrary individual score; the source water penalty is restored at 5 damage per second.
- Death Tag — source-style Normal/Alpha chasers, death-triggered conversion, runner speed threshold, Basher/Archer/Traitor/Null runner loadouts, Archer arrow refresh and flat Iron Skin damage reduction. Original `89` light markers now flicker between gold blocks and white wool, with their pre-round block data restored after the match; disguise visuals remain.
- Dragon Escape — embedded Reborn terrain and RED/BLACK/GREEN source markers, authored-route finish checks, and Jumper (4 leaps), Disruptor (3 leaps + proximity TNT), Warper (2 leaps + one forward-player pearl), and Digger (6 block recovery/build uses) kits. Hell maps use the source night time (16000); Pirate maps apply the original 2-damage water penalty. Runtime course pacing and exact source-kit tuning still need live acceptance.
- Dragons — source RED-marker spawn locations, at most five live dragons spawned at 500 ms intervals, map-aware dragon targeting/contact hits, Marksman arrow replenishment and Pyrotechnic sparklers that attract dragons, and the source 4-damage water penalty.
- Evolution — source-form health/damage progression across Iron Golem, Blaze, Spider, Creeper, Slime and Chicken; kills unlock hold-sneak grounded evolution charge, faster with consecutive life kills; 2.2-second form change plus 7-second total animation; stage persists through death with a four-second respawn. Map-platform illusions and the full 1.8 perk/disguise stack remain to port.
- Gladiators — four source-style loadout tiers, paired tournament rounds, a five-second combat countdown and 60-second poison limited to unresolved 1v1 duels. Arena placement now reads the original map's GREEN/YELLOW/ORANGE/RED centre markers and nearest BLACK player spawns; per-arena doors/trails and full kit polish still remain.
- Micro Battles — original source map/barrier/spectator markers, Archer/Worker/Fighter loadouts, Fletcher replenishment and Fighter Iron Skin, plus MapCrumbleModule's vertical two-tick map scan and three farthest supported-block removals each tick after the one-second live gate.
- One in the Quiver — first-to-20 one-arrow kill loop, kill-earned Super Arrow, one-second respawn and breakable glass/glass panes; Leaper double-jump, Enchanter rebound and Ninja vanish are present, and Slam Shot now uses the source's radial 2D Ground Pound knockback (remaining kit/aesthetic and arena progression polish remains).
- Paintball — selectable Rifle, Sniper, Shotgun and Machine Gun kits; projectile-specific paint counts, scope-charge damage, a heat gauge, team-coloured armour, Water Bomb stacks, 250 ms throw gate, SAVE ME marker/revival within three blocks, nearby teammate cleanse with armour/health restoration, and the source-style high-pitched hit-confirmation sound.
- Runner — source Leaper/Archer/Frosty kit identities, 8-second Leap, 6-second Quickshot, Frost Balls replenished every 500 ms up to 16 with Slow II for two seconds, source-map bounds, trail material tracking, 600 ms decay and 1.2-second falling-block lifetime, and radius-2.5 arrow trail marking, and the source 4-damage water penalty.
- Sheep Quest — source GREEN/BLUE/RED/YELLOW pens, team sheep capture and steals, three-sheep carry limit, and the five-minute winner based on sheep in the pen; a tie at the end is a draw.
- Spleef — source kit selection (Snowballer, Brawler and Archer), snowballer replacement cap, Brawler Leap/Smasher, two-arrow Fletcher replenishment, source-timed hunger drain/restoration, source material damage transitions, 3D arrow-radius clearing, and the source 4-damage water penalty.
- Turf Wars — team assignment, bow combat and territory block marking; source timing now uses the 40-second initial build, followed by 20-second build / 90-second combat cycles.

Each game gets its own separate modern test world, so arena generation/destruction does not use the live 1.8 Testplex world. Original Reborn map archives are embedded for Bacon Brawl, Bomb Lobbers, Death Tag, Dragon Escape, Dragons, Gladiators, Micro Battles, One in the Quiver, Paintball, Runner, Sheep Quest, Spleef and Turf Wars; these load into separate source-world folders and read WorldConfig.dat spawn groups and map bounds. Evolution remains generated because an Evolution map archive is not present in the source map catalogue.

## Commands

- `/gamepanel` → choose a supported game → **Start / Test**. The coordinator calls `/arcadeport play <game-id>`.
- `/arcadeport play <id>` — joins a quick-start window, giving others a few seconds to join.
- `/arcadeport join <id>` then `/arcadeport start <id>` — queue a multi-player test.
- `/arcadeport kit <el-muchacho-pigo|mama-piggles|pig|mama-pig|sheep-pig|jumper|dragon-escape-jumper|dragon-escape-disruptor|dragon-escape-warper|dragon-escape-digger|disruptor|warper|digger|armorer|pitcher|waller|snowballer|brawler|archer|rifle|sniper|shotgun|machine-gun|runner-basher|runner-archer|runner-traitor|runner-null>` — choose a game kit before a test; legacy kit aliases remain accepted.
- `/arcadeport status`, `/arcadeport stop` (operator only).

Game IDs are: `bacon-brawl`, `bomb-lobbers`, `death-tag`, `dragon-escape`, `dragons`, `evolution`, `gladiators`, `micro-battles`, `one-in-the-quiver`, `paintball`, `runner`, `sheep-quest`, `spleef`, `turf-wars`.

## Acceptance boundary

CI verifies compilation, plugin metadata and catalogue/ID tests. It does not verify the mechanics in a running server. The first in-game iteration should cover command/GUI entry, each game start/end, weapon interactions, elimination/respawn, per-world cleanup and inventory restoration. Then compare the rough mechanics to the existing Reborn game sources and fix discrepancies one game at a time.

Cosmetic selections and distinct win effects remain intentionally separate, deferred work. They should only become functional after the game-specific mechanics and round lifecycle are stable.
