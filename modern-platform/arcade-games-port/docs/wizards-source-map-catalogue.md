# Mineplex Wizards source-map catalogue

This records the original Wizards Arcade maps exposed by [maps.nixuge.me/mineplex/1](https://maps.nixuge.me/mineplex/1/). The public catalogue is defined in [MineplexData.ts](https://github.com/Nixuge/McMapsBackup-Website/blob/master/src/ts/servers/mineplex/MineplexData.ts); the website's [download-path implementation](https://github.com/Nixuge/McMapsBackup-Website/blob/master/src/ts/servers/mineplex/MineplexMap.ts) distinguishes the original Wizards pool from Nano Wizards.

| Map | Builder credit | Standard Wizards ZIP | Nano Wizards ZIP |
|---|---|---|---|
| Haven | Wolfey | [haven.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/haven.zip) | [haven.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/haven.zip) |
| Wizard Arena | libraryaddict | [wizardarena.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/wizardarena.zip) | [wizardarena.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/wizardarena.zip) |
| Orbis Harbor | BrandonMWX & Goldeey | [orbisharbor.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/orbisharbor.zip) | [orbisharbor.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/orbisharbor.zip) |
| Aspen | TheDarkWarlord & bribbs | [aspen.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/aspen.zip) | [aspen.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/aspen.zip) |
| Hogwarts | Alfredo & LukeAteDale | [hogwarts.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/hogwarts.zip) | [hogwarts.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/hogwarts.zip) |
| Industrial West | Mineplex Build Team | [industrialwest.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/industrialwest.zip) | [industrialwest.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/industrialwest.zip) |
| Alicudi | btmodz, MrMagicMan & eggdumbhead | [alicudi.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/alicudi.zip) | [alicudi.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/alicudi.zip) |
| Thaumaturge Colony | PhoenixSky | [thaumaturgecolony.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/thaumaturgecolony.zip) | [thaumaturgecolony.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/thaumaturgecolony.zip) |
| Magically Possible | ReindeerClark | [magicallypossible.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/magicallypossible.zip) | [magicallypossible.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/magicallypossible.zip) |
| Kalamit | TheDarkWarLord | [kalamit.zip](https://maps.nixuge.me/static/mineplex/zip/wizards/kalamit.zip) | [kalamit.zip](https://maps.nixuge.me/static/mineplex/zip/nano/nanowizards/kalamit.zip) |

## Import locations

The Testplex importer accepts ZIPs from these server-local folders:

- update/maps/Wizards/
- update/maps/Nano Wizards/

The configured source-maps.directory value is the common root above those folders. It defaults to update/maps relative to the server process working directory; use an absolute path if the existing Reborn map store is elsewhere. Both folders are scanned, but unrelated games' archives are not.

The normal Reborn source-map importer remains strict: it requires a unique world root containing both level.dat and WorldConfig.dat, plus region terrain files (region/*.mca). Wizards has a separate, opt-in adapter for these WorldDownloader snapshots: it accepts a unique level.dat + region/*.mca root, derives conservative test bounds from populated Anvil chunks, searches loaded terrain for safe player starts, and recovers chest block positions or places replacement test chests. It does **not** recreate original Mineplex TEAM_SPAWNS, bounds, or chest inventories. If a selected source archive fails runtime validation, Wizards falls back to the generated test arena instead of failing to start.

## Automated archive inspection

The dedicated GitHub Actions workflow, Validate Wizards Source Maps, downloads all 20 source ZIPs into temporary runner storage and checks ZIP CRCs, safe paths, world-root structure, level.dat NBT, Anvil region headers/chunks, chest block data and SHA-256. It distinguishes directly importable Reborn worlds from **Wizards adapter candidates** (WorldDownloader exports without WorldConfig.dat). It uploads a small JSON/Markdown report as an Actions artifact. Downloaded ZIPs are deleted after inspection; this repository intentionally does not mirror or redistribute map binaries.

Builder credits are copied from the upstream catalogue. No independent map redistribution licence has been verified. A structural pass is not a substitute for loading each world on Paper 26.3 and playing a multiplayer Wizards round; maps with no spawn markers or incomplete bounds are flagged for manual review.


## Inspection results (31 October 2026)

The dedicated CI job downloaded and CRC-checked all 20 archive variants. They are **Minecraft world snapshots**, not schematics: each uses the root-level layout `WorldDownloader.txt`, `level.dat`, `playerdata/`, `region/` and `session.lock`. The archives contain active Anvil `.mca` terrain chunks, but **none includes Mineplex's `WorldConfig.dat`**. They cannot pass the generic Reborn importer; they need the Wizards-specific adapter described below. The level.dat spawn and populated chunk extents are useful evidence, but neither is an original Mineplex arena marker or official map boundary.

| Map | Standard level.dat spawn | Standard active chunks | Nano level.dat spawn | Nano active chunks |
|---|---:|---:|---:|---:|
| Haven | `0, 100, 0` | 93 | `-54, 144, -1` | 405 |
| Wizard Arena | `64, 12, 64` | 60 | `75, 29, 58` | 328 |
| Orbis Harbor | `35, 66, 113` | 211 | `0, 68, 107` | 513 |
| Aspen | `398, 4, -613` | 87 | `-26, 120, 26` | 408 |
| Hogwarts | `0, 100, 0` | 76 | `-20, 84, -14` | 400 |
| Industrial West | `-25, 64, -67` | 87 | `3, 100, 18` | 432 |
| Alicudi | `76, 172, 89` | 100 | `9, 198, 63` | 409 |
| Thaumaturge Colony | `163, 101, -22` | 89 | `-5, 131, 55` | 411 |
| Magically Possible | `0, 100, 0` | 150 | `-2, 61, 14` | 394 |
| Kalamit | `398, 4, -613` | 80 | `12, 98, -18` | 404 |

Spawn coordinates above are the values stored in each archive's `level.dat`; they are recorded as candidates only, **not yet verified as the intended Wizards player spawn markers**. Chunk counts are the populated region-file chunk slots observed in the archive, not playable map dimensions.

### Chest and marker limitations

The follow-up Anvil-section scan found these legacy chest block IDs (54) in the downloaded region data:

| Map | Standard Wizards | Nano Wizards |
|---|---:|---:|
| Haven | 242 | 202 |
| Wizard Arena | 233 | 0 |
| Orbis Harbor | 206 | 0 |
| Aspen | 238 | 0 |
| Hogwarts | 191 | 0 |
| Industrial West | 266 | 0 |
| Alicudi | 262 | 222 |
| Thaumaturge Colony | 253 | 0 |
| Magically Possible | 207 | 0 |
| Kalamit | 227 | 0 |

These are chest **block records**, not proof that usable chest block entities or original inventories survived. The initial NBT inspection found 211 chest tile-entity records in the standard Alicudi archive; the Nano downloader metadata includes `LOAD_TILE_ENTITY=false`, so Nano snapshots in particular cannot be assumed to preserve chest block-entity NBT. The runtime adapter scans only chunks marked populated in the original Anvil region headers, using Paper chunk snapshots rather than generating every chunk in the rectangular bounds just to inspect blocks. It recreates chest block entities before Wizards populates them with test loot. Any map yielding fewer than eight recoverable chests is topped up with terrain-checked replacement positions; the archive is rejected if it still cannot provide eight usable loot chests.

The source-specific `WorldConfig.dat` data is absent throughout. Original map bounds, `TEAM_SPAWNS` groups, and custom chest marker metadata are therefore unavailable. The adapter uses the Anvil location tables to derive a conservative rectangle around populated chunks, then checks the loaded terrain for safe start and replacement-chest positions. `level.dat` spawn coordinates are never silently promoted to original Mineplex starts.

### Engineering decision and acceptance status

A dedicated Wizards-only import path is now implemented in `SourceMapCatalog` and wired into `WizardsGameManager`. It does not weaken the generic importer used by other games. Chest recovery scans only populated chunks from the source region index and requires at least eight usable loot-chest positions. The adapter:

1. accepts only the two approved Wizards source folders and a unique root with `level.dat` plus legacy Anvil region files;
2. converts the legacy region layout to Paper 26.3's custom-world layout;
3. derives test-only horizontal bounds from populated chunk coordinates;
4. checks terrain for clear, safe player starts instead of assuming the saved world spawn is correct;
5. recovers chest block positions from the loaded world, recreates missing chest block entities, and uses safe replacement locations only when the archive contains no chest blocks;
6. logs that these positions are inferred and not original Mineplex WorldConfig metadata.

If the adapter cannot find sufficient safe starts or usable chest locations, Wizards falls back to the generated test arena. The world-import acceptance gate has now passed on a disposable Paper 26.3 server; full player movement, combat, loot-balance and multiplayer acceptance remain outstanding. Maps that have no chest block records (eight Nano variants) use synthesized replacement chest positions and are not fidelity-equivalent to the standard exports.

### Paper 26.3 source-map runtime smoke (1 November 2026)

[Paper 26.3 workflow run 3469](https://github.com/joshbet9/testplex/actions/runs/41214247540) started the actual Arcade plugin on stable Paper 26.3 build 235 and loaded every source archive through the production `WizardsGameManager.createArena()` path. The CI-only smoke plugin also asserted that the selected archive remained active (no generated-arena fallback), that each world had at least four distinct terrain-safe starts, and that every reported chest was a usable Bukkit chest block entity with an inventory.

<badge>20/20 source maps passed</badge>

| Map | Standard starts | Standard usable chests | Nano starts | Nano usable chests |
|---|---:|---:|---:|---:|
| Haven | 16 | 242 | 16 | 202 |
| Wizard Arena | 16 | 233 | 9 | 32 |
| Orbis Harbor | 16 | 206 | 16 | 32 |
| Aspen | 16 | 238 | 16 | 32 |
| Hogwarts | 16 | 191 | 16 | 32 |
| Industrial West | 16 | 266 | 15 | 32 |
| Alicudi | 16 | 262 | 16 | 222 |
| Thaumaturge Colony | 16 | 253 | 16 | 32 |
| Magically Possible | 16 | 207 | 16 | 32 |
| Kalamit | 16 | 227 | 15 | 32 |

The chest counts are usable containers after import, not proof of original inventories. Wizards repopulates them with its test loot. The eight Nano variants with no preserved chest block records received 32 safe replacement chests each. Nano Wizard Arena produced nine safe starts—above the four-start acceptance minimum, but it should receive particular visual review before gameplay acceptance.

The per-map `PASS/FAIL` result sheet and Paper server log are retained in the [Wizards Paper 26.3 smoke artifact](https://github.com/joshbet9/testplex/actions/runs/41214247540/artifacts/13047597481). The complete build also passed all Maven tests, packaging checks and deployment-recovery checks. Its Hyper-V deployment job was skipped; no VM was changed.

The validator continues to report direct Reborn imports separately from Wizards adapter candidates: 0/20 direct imports and 20/20 adapter candidates. Original `WorldConfig.dat`, player-start markers and chest inventories remain unavailable, so inferred starts/bounds and replacement Nano chests must not be described as authentic Mineplex metadata. The detailed NBT/chunk metadata report remains [available as an earlier Actions artifact](https://github.com/joshbet9/testplex/actions/runs/41208131882/artifacts/13045531836). The map ZIPs themselves are not committed or rehosted.
