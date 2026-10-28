# Modern Arcade mechanics prototypes (Paper 26.3)

This plugin adds first-pass playable mechanics harnesses for the 14 Arcade games outside the dedicated Monster Maze and Snake modules. It is a compatibility/work-in-progress layer intended to expose API and game-loop problems early; it is **not** a claim that all original Mineplex mechanics or maps have been faithfully ported yet.

## Games covered in this first pass

- Bacon Brawl — knockback and ring-out elimination.
- Bomb Lobbers — snowball throws, direct-hit ding and knockback.
- Death Tag — tagged/chaser tracking and last-runners logic.
- Dragon Escape — parkour platforms that crumble behind the pack, with a dash item.
- Dragons — timed fireball hazards and a Pyro fireball ability.
- Evolution — combat-driven stage/weapon progression and respawn loop.
- Gladiators — compact melee elimination arena.
- Micro Battles — isolated islands and shrinking border.
- One in the Quiver — one-arrow kill loop and arrow regrant on elimination.
- Paintball — snowball hit scoring with teams.
- Runner — disappearing floor tiles and dash item.
- Sheep Quest — captureable sheep and score target.
- Spleef — snow floor and shovel-only tile breaks.
- Turf Wars — team assignment, bow combat and territory block marking.

Each game gets its own separate test world, so arena generation/destruction does not use the live 1.8 Testplex world. These are simple generated test arenas, not the historical original maps.

## Commands

- `/gamepanel` → choose a supported game → **Start / Test**. The coordinator calls `/arcadeport play <game-id>`.
- `/arcadeport play <id>` — joins a quick-start window, giving others a few seconds to join.
- `/arcadeport join <id>` then `/arcadeport start <id>` — queue a multi-player test.
- `/arcadeport status`, `/arcadeport stop` (operator only).

Game IDs are: `bacon-brawl`, `bomb-lobbers`, `death-tag`, `dragon-escape`, `dragons`, `evolution`, `gladiators`, `micro-battles`, `one-in-the-quiver`, `paintball`, `runner`, `sheep-quest`, `spleef`, `turf-wars`.

## Acceptance boundary

CI verifies compilation, plugin metadata and catalogue/ID tests. It does not verify the mechanics in a running server. The first in-game iteration should cover command/GUI entry, each game start/end, weapon interactions, elimination/respawn, per-world cleanup and inventory restoration. Then compare the rough mechanics to the existing Reborn game sources and fix discrepancies one game at a time.

Cosmetic selections and distinct win effects remain intentionally separate, deferred work. They should only become functional after the game-specific mechanics and round lifecycle are stable.
