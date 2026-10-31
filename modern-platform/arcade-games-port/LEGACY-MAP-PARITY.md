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
| Dragons (Solo) | `Dragons` | 40 |
| Runner | `Runner` | 22 |
| **Total archived source assets** | | **284 ZIP files** |

The counts are source archive files, not a claim that every archive has unique binary contents. All original filenames are retained and verified.

## Rename handling

- `Super Paintball` is a legacy rename/archive folder for **Paintball**, not a second game or mode.
- `Super Spleef` is a legacy rename/archive folder for **Spleef**, not a second game or mode.
- Both folders' map archives are included in their parent game's single map selector and use the same game ID, lifecycle and mechanics.

## Newly promoted games

Super Sploor is already in the enabled queue and contributes the original `Sploor/NanoGames_Excavation.zip` archive. Runner and Dragons (Solo) are now also in the shared controlled test queue. Their original source archives are checked by the same enabled-game-derived CI policy. None of these titles is being introduced as an alias of another game.

Runner's 22 original files are listed in [RUNNER-ACCEPTANCE-PLAN.md](RUNNER-ACCEPTANCE-PLAN.md). The two Clay Plaza filenames are distinct source archives.

Dragons' 40 original files are listed in [DRAGONS-ACCEPTANCE-PLAN.md](DRAGONS-ACCEPTANCE-PLAN.md). The similarly named Balloon Town, Sky Ship and Voyage files are distinct original archives and must not be collapsed by filename normalisation. Only **Dragons (Solo)** is in scope; Dragons Teams remains excluded.
## Build enforcement

The Paper 26.3 build reads the packaged plugin's actual `enabled-games` list and resolves each enabled game's legacy map folders. It compares the sorted ZIP filenames in each source folder with the entries actually packaged under `source-maps/<folder>/` in `ArcadeMechanicsModern.jar`. A missing file, unexpected file, or enabled game without an explicit map-source policy fails CI.

Super Sploor, Runner and Dragons now receive the same exact filename comparison as every other enabled map-backed game, using the enabled-game-derived source-vs-JAR policy.

When a new game is enabled, its source-folder mapping must be added to the build policy and `SourceMapCatalog.mapFoldersFor`. If the game has no 1.8 map archive (as with Plexball's modern arena), that exception must be explicit rather than silently skipping the check.
