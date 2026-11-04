# In-game scoreboard coverage audit — 2026-11-03

Every playable game needs an in-game sidebar. The shared Arcade session now has an explicit, exhaustive coverage switch: games with a source-shaped scoreboard keep it, and games without one receive a common match scoreboard with the live phase, selected map when available, objective, player count, scores, and clock.

| Game | In-game scoreboard |
| --- | --- |
| Bacon Brawl | Existing game scoreboard |
| Castle Assault | Existing Castle Assault scoreboard |
| Bomb Lobbers | Existing team scoreboard |
| Death Tag | Existing runner/chaser scoreboard |
| Dragon Escape | Existing race scoreboard |
| Dragons | Existing survivor scoreboard |
| Evolution | Shared match scoreboard |
| Gladiators | Existing tournament scoreboard |
| Heroes of GWEN | Existing Heroes of GWEN scoreboard |
| Hoops Remastered | Existing match scoreboard |
| Micro Battles | Shared match scoreboard |
| One in the Quiver | Existing kill scoreboard |
| Paintball | Existing team scoreboard |
| Super Sploor | Existing team scoreboard |
| Plexball | Existing match scoreboard |
| Runner | Shared match scoreboard |
| Sheep Quest | Shared match scoreboard |
| Spleef | Shared match scoreboard |
| Turf Wars | Shared match scoreboard |
| Draw My Thing | Shared match scoreboard |
| Master Builders | Existing build/review scoreboard |
| Monster Maze (Original) | Existing per-player maze scoreboard |
| Monster Maze (Speed) | Existing per-player maze scoreboard |
| Monster Maze (Modern) | Existing per-player maze scoreboard |
| Sneaky Assassins | Existing reveal/player scoreboard |
| Speed Builders | Existing build-phase scoreboard |
| SkyWars | Shared match scoreboard |
| Milk the Cow | Shared match scoreboard |
| Wither Assault | Shared match scoreboard |
| Wizards | Existing match scoreboard |
| Snap (Camera Shy) | New game-specific scoreboard |
| Snake | New Snake match scoreboard |

The Snake scoreboard is scoped to its Arcade-owned game controller. It updates during preparation and live play, restores each player's prior scoreboard on leave/end, and reports map, countdown/elapsed time, survivors, food, and top Snake stats.
