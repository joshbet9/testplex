# Inferred test config note

Mineplex Backup catalog ZIPs imported on 2026-11-03 are WorldDownloader terrain snapshots. These snapshots omit the original Reborn `WorldConfig.dat`, so the modern map picker labels each affected archive **Inferred test config**.

For those archives, the test loader derives map bounds from populated region chunks and selects terrain-checked surface starts. It does not recreate authored source objectives, plot markers, or named data locations. Archives with original `WorldConfig.dat` continue to use that authored config.

The per-game import and missing-link status is recorded in `MINEPLEX-CATALOG-IMPORT-2026-11-03.csv` and `SOURCE-MAP-COMPLETENESS-AUDIT-2026-11-03.md`.
