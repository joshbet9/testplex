# Modern Arcade mechanics prototypes (Paper 26.3)

This plugin adds first-pass playable mechanics harnesses for the 14 Arcade games outside the dedicated Monster Maze and Snake modules. It is a compatibility/work-in-progress layer intended to expose API and game-loop problems early; it is **not** a claim that all original Mineplex mechanics or maps have been faithfully ported yet.

## Games covered in this first pass

- Bacon Brawl — knockback and ring-out elimination.
- Bomb Lobbers — snowball throws, direct-hit ding and knockback.
- Death Tag — source-style Normal/Alpha chasers, death-triggered conversion, runner speed threshold, Basher/Archer/Traitor/Null runner loadouts, Archer arrow refresh, and flat Iron Skin damage reduction (historical map/light parity and disguise visuals remain).
- Dragon Escape — generated parkour course, a visible Douglas chase dragon, radius-based course destruction, catch-up elimination and finish proximity (map-data/waypoint parity still needs original assets).
- Dragons — timed fireball hazards and a Pyro fireball ability.
- Evolution — source-form health/damage progression across Iron Golem, Blaze, Spider, Creeper, Slime and Chicken; kills unlock hold-sneak grounded evolution charge, faster with consecutive life kills; 2.2-second form change plus 7-second total animation; stage persists through death with a four-second respawn. Map-platform illusions and the full 1.8 perk/disguise stack remain to port.
- Gladiators — four source-style loadout tiers, paired tournament rounds, a five-second combat countdown and 60-second poison limited to unresolved 1v1 duels (original arena maps, per-arena doors/trails and full kit polish still remain).
- Micro Battles — isolated islands and shrinking border.
- One in the Quiver — first-to-20 one-arrow kill loop, named Super Arrow regrant, one-second respawn and breakable glass/glass panes (original kit abilities are still to port).
- Paintball — selectable Rifle, Sniper, Shotgun and Machine Gun kits; projectile-specific paint counts, scope-charge damage, a heat gauge and team-coloured armour (kit perks and revive/Save Me flow remain to port).
- Runner — disappearing floor tiles and dash item.
- Sheep Quest — captureable sheep and score target.
- Spleef — snow floor and shovel-only tile breaks.
- Turf Wars — team assignment, bow combat and territory block marking.

Each game gets its own separate test world, so arena generation/destruction does not use the live 1.8 Testplex world. These are simple generated test arenas, not the historical original maps.

## Commands

- `/gamepanel` → choose a supported game → **Start / Test**. The coordinator calls `/arcadeport play <game-id>`.
- `/arcadeport play <id>` — joins a quick-start window, giving others a few seconds to join.
- `/arcadeport join <id>` then `/arcadeport start <id>` — queue a multi-player test.
- `/arcadeport kit <rifle|sniper|shotgun|machine-gun|runner-basher|runner-archer|runner-traitor|runner-null>` — choose a kit before a test (Death Tag runner kit can also be changed while playing as a runner).
- `/arcadeport status`, `/arcadeport stop` (operator only).

Game IDs are: `bacon-brawl`, `bomb-lobbers`, `death-tag`, `dragon-escape`, `dragons`, `evolution`, `gladiators`, `micro-battles`, `one-in-the-quiver`, `paintball`, `runner`, `sheep-quest`, `spleef`, `turf-wars`.

## Acceptance boundary

CI verifies compilation, plugin metadata and catalogue/ID tests. It does not verify the mechanics in a running server. The first in-game iteration should cover command/GUI entry, each game start/end, weapon interactions, elimination/respawn, per-world cleanup and inventory restoration. Then compare the rough mechanics to the existing Reborn game sources and fix discrepancies one game at a time.

Cosmetic selections and distinct win effects remain intentionally separate, deferred work. They should only become functional after the game-specific mechanics and round lifecycle are stable.
