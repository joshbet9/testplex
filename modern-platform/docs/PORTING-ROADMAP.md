# Modern Arcade porting roadmap

## Platform rules

- Keep Paper/Bukkit API usage in the common platform. Do not copy legacy v1_8_R3 packet code into this project.
- Each game owns its own mechanic module, cosmetic line definitions, render hooks, win-effect renderer and test harness.
- Preference keys stay namespaced by player UUID, game ID, line ID and setting ID.
- The win-effect selector is independent of regular cosmetics and independent of other games' win effects.
- Shared infrastructure (game panel, queue/session lifecycle, arena/world management, player profile storage, bot/test tools) is reusable; cosmetic visuals themselves should not be generic recolours that look the same across games.
- Port status must only advance to READY_FOR_TEST when mechanics, lifecycle cleanup, map setup and a repeatable acceptance checklist are in place.

## Per-game acceptance

For each game: confirm lobby and queue entry; round start/reset/end; spectators and elimination; win detection; arena/map cleanup; reconnect and shutdown behaviour; game-specific item/ability handling; persistent cosmetic selections; each cosmetic rendered at the intended trigger; each win effect rendered once to the intended viewers; and a test-bot or multi-client path.

## Order

1. Monster Maze (including per-mob-set mob forms, independently configured beacon and Safe Pad colours, Safe Pad lifecycle, timing, jump/kit rules and PBs).
2. Snake (food spawning, green slimes, physical magma cube item boxes, item roll in the hotbar, escalating effect durations, and tuned drift/negative effects).
3. Only after mechanics acceptance, finalise those two games' cosmetic lines and test their win effects.
4. Port the other 14 games one at a time and design each cosmetic line and win scene alongside the game-specific hooks.
