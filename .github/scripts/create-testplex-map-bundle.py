#!/usr/bin/env python3
"""Package every Mineplex Reborn map ZIP into a single deployable artifact."""
from __future__ import annotations

import sys
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]
    map_root = repo_root / "update" / "maps"
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else repo_root / "Plugins" / "out" / "testplex-map-assets.zip"
    output.parent.mkdir(parents=True, exist_ok=True)

    if not map_root.is_dir():
        raise SystemExit(f"Map source directory is missing: {map_root}")

    map_files = sorted(path for path in map_root.rglob("*.zip") if path.is_file())
    if not map_files:
        raise SystemExit(f"No map ZIPs were found under {map_root}")

    total_bytes = 0
    with ZipFile(output, "w", compression=ZIP_STORED, allowZip64=True) as archive:
        for path in map_files:
            relative = path.relative_to(map_root)
            if len(relative.parts) < 2 or path.suffix.lower() != ".zip":
                raise SystemExit(f"Unexpected map ZIP layout: {path}")
            archive.write(path, "maps/" + relative.as_posix())
            total_bytes += path.stat().st_size

    print(f"Packaged {len(map_files)} map ZIPs ({total_bytes:,} source bytes) into {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
