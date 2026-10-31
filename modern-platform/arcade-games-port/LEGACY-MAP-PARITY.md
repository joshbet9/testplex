# Legacy 1.8 map parity — shared Paper 26.3 acceptance queue

The enabled modern acceptance queue must contain every original 1.8 Testplex map archive for each game that has an archived map collection. The map selector is backed by all individual ZIP files under `update/maps/`, not by a small hand-picked sample or only the convenience `maps/<game>.zip` bundle.

## Current enabled queue

| Game | 1.8 source folder(s) | ZIP archive files |
|---|---|---:|
| Death Tag | `Death Tag` | 1 |
| One in the Quiver | `One in the Quiver` | 17 |
| Bomb Lobbers | `Bomb Lobbers` | 41 |
| Bacon Brawl | `Bacon Brawl` | 28 |
| Gladiators | `Gladiators` | 1 |
| Micro Battles | `Micro Battle` | 50 |
| Paintball | `Paintball` + `Super Paintball` | 46 |
| Spleef | `Speef` + `Super Spleef` | 37 |
| Super Sploor | `Sploor` | 1 |
| Plexball | Modern dedicated arena; no 1.8 source map folder | — |
| Camera Shy | Procedural modern hedge maze; no 1.8 source map folder | — |
| Dragons (Solo) | `Dragons` | 40 |
| Runner | `Runner` | 22 |
| **Total archived source assets** | | **284 ZIP files** |

The counts are source archive files, not a claim that every archive has unique binary contents. All original filenames are retained and verified.

## Rename handling

- `Super Paintball` is a legacy rename/archive folder for **Paintball**, not a second game or mode.
- `Super Spleef` is a legacy rename/archive folder for **Spleef**, not a second game or mode.
- Both folders' map archives are included in their parent game's single map selector and use the same game ID, lifecycle and mechanics.

## Newly promoted games

Super Sploor uses the original `Sploor/NanoGames_Excavation.zip` archive. Dragons (Solo) and Runner are now also in the controlled queue: Dragons contributes 40 original map archives, and Runner contributes 22. Only Dragons (Solo) is in scope; Dragons Teams remains excluded. Their original source archives are checked by the same enabled-game-derived CI policy, with the complete filename checks in `DRAGONS-ACCEPTANCE-PLAN.md` and `RUNNER-ACCEPTANCE-PLAN.md`.

## Build enforcement

The Paper 26.3 build reads the packaged plugin's actual `enabled-games` list and resolves each enabled game's legacy map folders. It compares the sorted ZIP filenames in each source folder with the entries actually packaged under `source-maps/<folder>/` in `ArcadeMechanicsModern.jar`. A missing file, unexpected file, or enabled game without an explicit map-source policy fails CI.

Super Sploor, Dragons (Solo) and Runner receive the same exact filename comparison as every other enabled map-backed game. When a new game is enabled, its source-folder mapping must be added to the build policy and `SourceMapCatalog.mapFoldersFor`. If the game has no 1.8 map archive (as with Plexball's or Camera Shy's modern arena), that exception must be explicit rather than silently skipping the check.
