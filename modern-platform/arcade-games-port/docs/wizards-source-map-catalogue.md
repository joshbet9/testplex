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

The importer requires a uniquely identifiable world root with both level.dat and WorldConfig.dat. Paper 26.3 conversion also requires region terrain files (region/*.mca). WorldConfig.dat supplies map bounds, player/team spawn groups and data/custom markers. A generated arena is only a fallback when neither source folder contains an archive.

## Automated archive inspection

The dedicated GitHub Actions workflow, Validate Wizards Source Maps, downloads all 20 source ZIPs into temporary runner storage and checks ZIP CRCs, safe paths, unique world-root structure, terrain region files, WorldConfig.dat metadata and SHA-256. It uploads a small JSON/Markdown report as an Actions artifact. Downloaded ZIPs are deleted after inspection; this repository intentionally does not mirror or redistribute map binaries.

Builder credits are copied from the upstream catalogue. No independent map redistribution licence has been verified. A structural pass is not a substitute for loading each world on Paper 26.3 and playing a multiplayer Wizards round; maps with no spawn markers or incomplete bounds are flagged for manual review.


## Inspection results (31 October 2026)

The dedicated CI job downloaded and CRC-checked all 20 archive variants. They are **Minecraft world snapshots**, not schematics: each uses the root-level layout `WorldDownloader.txt`, `level.dat`, `playerdata/`, `region/` and `session.lock`. The archives contain active Anvil `.mca` terrain chunks, but **none includes Mineplex's `WorldConfig.dat`**. As a result, none is directly installable by the current `SourceMapCatalog`, which requires both `level.dat` and `WorldConfig.dat` in the same world root before it accepts a source world.

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

- The initial NBT inspection found 211 chest tile-entity records in the standard Alicudi archive; none were found in the other 19 snapshots.
- Nano Wizards `WorldDownloader.txt` includes `ServerName=eu.mineplex.com` and the downloader setting `LOAD_TILE_ENTITY=false`. That means its exported files cannot be assumed to preserve chest tile-entity NBT or other tile entities.
- The source-specific `WorldConfig.dat` data is absent throughout, so the original map bounds, `TEAM_SPAWNS` groups and custom chest marker list are not available from these ZIPs.
- The validator is also inspecting legacy chunk section block arrays for chest block IDs. A chest block in terrain without its tile-entity data still needs in-server testing before it can be treated as a usable loot chest.

### Engineering decision

Keep these as recorded source assets, but do not mark any as ready for the existing Wizards importer. A proper adapter would need to accept a world export containing `level.dat` plus region files, derive bounded arena extents from populated chunks, verify safe spawn positions from the actual terrain, and recover or reconstruct usable chest locations. It must not silently treat the `level.dat` world spawn as an authoritative Mineplex `TEAM_SPAWNS` marker.

The current implementation therefore keeps the generated test arena as the safe fallback until that adapter and a live Paper 26.3 acceptance pass are implemented. The metadata-only CI report for the initial full archive run is [available as an Actions artifact](https://github.com/joshbet9/testplex/actions/runs/41207742669/artifacts/13045575510). The map ZIPs themselves are not committed or rehosted.
