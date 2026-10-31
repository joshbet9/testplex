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
| Plexball | Modern dedicated arena; no 1.8 source map folder | — |
| **Total archived source assets** | | **221 ZIP files** |

The counts are source archive files, not a claim that every archive has unique binary contents. All original filenames are retained and verified.

## Rename handling

- `Super Paintball` is a legacy rename/archive folder for **Paintball**, not a second game or mode.
- `Super Spleef` is a legacy rename/archive folder for **Spleef**, not a second game or mode.
- Both folders' map archives are included in their parent game's single map selector and use the same game ID, lifecycle and mechanics.

## Next gated map-parity candidates

Runner and Dragons (Solo) are the next two map-backed 1.8 candidates being prepared for acceptance. They remain disabled in `enabled-games`, but the build independently verifies their complete original map ZIP filename sets so archive parity is protected before either readiness promotion.

| Order | Game | 1.8 source folder | ZIP archive files | Queue status |
|---:|---|---|---:|---|
| 1 | Runner | `Runner` | 22 | Gated; acceptance in progress |
| 2 | Dragons (Solo) | `Dragons` | 40 | Gated; acceptance in progress |

Runner's 22 original files are listed in [RUNNER-ACCEPTANCE-PLAN.md](RUNNER-ACCEPTANCE-PLAN.md). The two Clay Plaza filenames are distinct source archives.

Dragons' 40 original files are listed in [DRAGONS-ACCEPTANCE-PLAN.md](DRAGONS-ACCEPTANCE-PLAN.md). The similarly named Balloon Town, Sky Ship and Voyage files are distinct original archives and must not be collapsed by filename normalisation. Only **Dragons (Solo)** is in scope; Dragons Teams remains excluded.

## Build enforcement

The Paper 26.3 build reads the packaged plugin's actual `enabled-games` list and resolves each enabled game's legacy map folders. It compares the sorted ZIP filenames in each source folder with the entries actually packaged under `source-maps/<folder>/` in `ArcadeMechanicsModern.jar`. A missing file, unexpected file, or enabled game without an explicit map-source policy fails CI.

Runner and Dragons receive the same exact filename comparison while they are still gated, using the same source-vs-JAR comparison function. A promotion must add the game to the enabled-game policy and config together; its candidate check can then be consolidated into the allow-list-derived policy.

When a new game is enabled, its source-folder mapping must be added to the build policy and `SourceMapCatalog.mapFoldersFor`. If the game has no 1.8 map archive (as with Plexball's modern arena), that exception must be explicit rather than silently skipping the check.
