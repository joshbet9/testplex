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
