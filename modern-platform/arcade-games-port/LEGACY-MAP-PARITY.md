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

## Next gated map-parity candidate — Runner

Runner is the next map-backed 1.8 candidate being prepared for acceptance. It remains disabled in `enabled-games`, but the build independently verifies all original map ZIP filenames so the archive set is already protected before readiness promotion.

| Game | 1.8 source folder | ZIP archive files | Queue status |
|---|---|---:|---|
| Runner | `Runner` | 22 | Gated; acceptance in progress |

The 22 source files are `Runner_Amazonian Lake.zip`, `Runner_Candyland.zip`, `Runner_Clay Plaza.zip`, `Runner_ClayPlaza.zip`, `Runner_Cloud City.zip`, `Runner_Colloseum.zip`, `Runner_Crescent.zip`, `Runner_Dangerous Sharks.zip`, `Runner_Eclipse.zip`, `Runner_Escape Room.zip`, `Runner_Euphoria.zip`, `Runner_Flower_Field.zip`, `Runner_Frozen.zip`, `Runner_Fruit Salad.zip`, `Runner_Imaginary.zip`, `Runner_Macrocosm.zip`, `Runner_Mesa Canyon.zip`, `Runner_Nu Draconis.zip`, `Runner_Snowball.zip`, `Runner_Tundra.zip`, `Runner_Volcanic Fire.zip` and `Runner_Whirlpool.zip`. The two Clay Plaza archives are distinct files and are both required.

## Build enforcement

The Paper 26.3 build reads the packaged plugin's actual `enabled-games` list and resolves each enabled game's legacy map folders. It compares the sorted ZIP filenames in each source folder with the entries actually packaged under `source-maps/<folder>/` in `ArcadeMechanicsModern.jar`. A missing file, unexpected file, or enabled game without an explicit map-source policy fails CI.

Runner receives the same exact filename comparison while it is still gated, using the same source-vs-JAR comparison function. A Runner promotion must add it to the enabled-game policy and config together; the existing candidate check then becomes redundant but harmless and should be consolidated into the allow-list-derived policy.

When a new game is enabled, its source-folder mapping must be added to the build policy and `SourceMapCatalog.mapFoldersFor`. If the game has no 1.8 map archive (as with Plexball's modern arena), that exception must be explicit rather than silently skipping the check.
