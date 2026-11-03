# Mineplex source map completeness audit — 2026-11-03

Compared this repository's `update/maps` ZIPs with the Mineplex Backup catalog at <https://maps.nixuge.me/mineplex/1/>. The catalog lists maps whose original archive links are not all available. Counts below account for the `Paintball` + `Super Paintball` and `Speef` + `Super Spleef` folder aliases, plus the existing Turf Wars `Double Forts` filename.

## Import results

- Imported 155 source ZIPs into the matching `update/maps/<game>/` folders.
- The import manifest is [MINEPLEX-CATALOG-IMPORT-2026-11-03.csv](../../update/maps/MINEPLEX-CATALOG-IMPORT-2026-11-03.csv).
- 77 listed map ZIP URLs returned HTTP 404. Their names are listed below.
- Camera Shy, Hoops Remastered/Basketball, and Plexball have no map collections in this catalog.

| Game | Catalog maps | Local maps | Catalog ZIPs unavailable |
|---|---:|---:|---|
| Bacon Brawl | 27 | 27 | — |
| Bomb Lobbers | 24 | 24 | — |
| Castle Assault | 12 | 12 | — |
| Death Tag | 12 | 12 | — |
| Dragon Escape | 29 | 29 | — |
| Dragons | 34 | 34 | — |
| Draw My Thing | 15 | 15 | — |
| Evolution | 7 | 6 | Twin Peaks 2.0 |
| Gladiators | 81 | 23 | 58 listed below |
| Heroes of GWEN | 2 | 2 | — |
| Master Builders | 18 | 18 | — |
| Micro Battles | 51 | 51 | — |
| Milk the Cow | 9 | 9 | — |
| Monster Maze | 6 | 6 | — (one additional local map: Eye of Ender) |
| One in the Quiver | 13 | 13 | — |
| Paintball / Super Paintball | 40 | 40 | — |
| Runner | 21 | 21 | — |
| Sheep Quest | 21 | 21 | — |
| Skywars | 51 | 51 | — |
| Snake | 29 | 29 | — |
| Sneaky Assassins | 11 | 11 | — |
| Speed Builders | 10 | 1 | 9 listed below |
| Super Spleef | 30 | 29 | A Piece Of Cake! |
| Sploor / Super Sploor | 9 | 1 | 8 listed below |
| Turf Wars | 42 | 42 | — |
| Wither Assault | 5 | 5 | — |

### Catalog archives unavailable (HTTP 404)

- **Evolution:** Twin Peaks 2.0.
- **Gladiators:** Amphitheatre; Ancient Temple; Arabian Nights; Atlantic; Auto Shop; Aztec Temple; Badlands; Bamboo Forest; Bios Sombre; Breach; Centre Court; City Boulevard; Coloribus Tecta; Colosseum; Concordia; Courtyard; Creeden; Deep Jungle; Deja Vu; Faerie Cavern; Ferragudo; Giza; Gods of Gladiators; Haunted; Hut, Hut, Hike!; Imagine; Inertia; Koi Pond; Leaky Pipes; Lily; Marshes; Museum; Noble Gardens; Ocean Temples; Old Town Road; Origins of the Colossal Squid; Overgrown Cavern; Overgrown Prison; Petra; Polar Realm; Red; Redwood Rocks; Romkastle; Rosea Courtyard; Slicing Board; Sol; Some Lava; Starlight Mayhem; Subway; Sugimura Park; Under Construction; Venice; Volcano; Warren; We All Float Down Here; Winter Wonderland; Wool Mushrooms; Zealess Arena.
- **Speed Builders:** Free Fall; Jungle Tribe; Lost And Found; Rust; Seasons of Time; Snowy Sunset; Tiny You; Unfound Utopia; Westward Trail.
- **Super Spleef:** A Piece Of Cake! (the catalog's main, parsed, and unparsed archive URLs returned 404).
- **Sploor:** Blizzard; Flowery Forest!!; Grasslands; Greenhouse; Metropolis; Splat; Under the Sea; Urban Slums.

## Inferred test metadata

The downloaded catalog ZIPs are WorldDownloader terrain snapshots and omit the Reborn `WorldConfig.dat`. `SourceMapCatalog` now accepts those terrain-only archives, infers bounds and safe surface starts, and labels each affected archive **Inferred test config** in the map picker. The loader does not infer authored objective, plot, or data markers. These maps can launch for testing, but their game-specific spawns/objectives may need adjustment against the source.

Archives that include `WorldConfig.dat` continue to use their authored metadata. See [INFERRED-METADATA-NOTE.md](../../update/maps/INFERRED-METADATA-NOTE.md) beside the map folders.
