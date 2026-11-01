#!/usr/bin/env python3
"""Recover missing authentic Mineplex maps from Nixuge's pinned catalogue.

The original Testplex 1.8 repository has no Evolution world archives and only
Gladiators_Caged_In.zip. Never overwrite existing archived worlds, invent
WorldConfig.dat, or present downloaded/unconfigured maps as playable.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from zipfile import BadZipFile, ZipFile

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "modern-platform/arcade-games-port/map-recovery/nixuge-standard-catalog.json"
REPORT = ROOT / "update/map-recovery/nixuge/provenance.json"
MAX_DOWNLOAD = 40 * 1024 * 1024
MAX_UNCOMPRESSED = 500 * 1024 * 1024
EXPECTED = {"Evolution": 7, "Gladiators": 23}
USER_AGENT = "Testplex-Map-Recovery/1.0 (+https://github.com/joshbet9/testplex)"


def destination(game: str, name: str, configured: bool) -> Path:
    safe = re.sub(r"[^a-zA-Z0-9_.-]+", "_", name).strip("_")
    folder = "maps" if configured else "map-recovery/nixuge/unconfigured"
    return ROOT / "update" / folder / game / (game + "_" + safe + ".zip")


def validate(payload: bytes) -> dict:
    """Validate the raw archived world; don't execute or extract untrusted files."""
    if len(payload) > MAX_DOWNLOAD:
        raise ValueError("Archive exceeds maximum download size")
    import io
    try:
        with ZipFile(io.BytesIO(payload)) as source:
            entries = [name.replace("\\", "/").strip("/") for name in source.namelist()]
            files = [name for name in entries if name and not name.endswith("/")]
            if not files:
                raise ValueError("ZIP has no files")
            if any(name.startswith("../") or "/../" in name or re.match(r"(?i)^[a-z]:", name)
                   for name in files):
                raise ValueError("Unsafe archive entry paths")
            total_size = sum(f.file_size for f in source.infolist())
            if total_size > MAX_UNCOMPRESSED:
                raise ValueError("Archive exceeds uncompressed size cap")
            def member(base):
                return any(name == base or name.endswith("/" + base) for name in files)
            def terrain(name):
                return bool(re.search(r"(^|/)region/r\\.-?\\d+\\.-?\\d+\\.mca$", name, re.I))
            has_level = member("level.dat")
            has_regions = any(terrain(name) for name in files)
            has_config = member("WorldConfig.dat")
            if not (has_level and has_regions):
                raise ValueError("Missing original level.dat or Anvil region files")
            # A world needs its original Reborn metadata (spawns, bounds, kit markers)
            # before SourceMapCatalog can safely include it in the playable map list.
            if has_config:
                configs = [x for x in source.infolist()
                           if x.filename.replace("\\", "/").endswith("WorldConfig.dat")]
                config = source.read(configs[0]).decode("utf-8", errors="replace")
                has_config = ("MAP_NAME:" in config and
                              ("TEAM_SPAWNS:" in config or "DATA_LOCS:" in config))
            return {"has_level_dat": has_level, "has_regions": has_regions,
                    "has_reborn_worldconfig": has_config, "entries": len(files),
                    "uncompressed_bytes": total_size}
    except BadZipFile as exc:
        raise ValueError("Not a valid ZIP file") from exc


def fetch(url: str) -> bytes:
    last_error = None
    for attempt in range(3):
        try:
            request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/zip"})
            with urlopen(request, timeout=40) as response:
                length = response.headers.get("Content-Length")
                if length and int(length) > MAX_DOWNLOAD:
                    raise ValueError("Download advertised more than 40 MiB")
                payload = response.read(MAX_DOWNLOAD + 1)
                if len(payload) > MAX_DOWNLOAD:
                    raise ValueError("Download exceeded 40 MiB")
                return payload
        except (HTTPError, URLError, TimeoutError, OSError, ValueError) as exc:
            last_error = exc
            print(f"[download] {url}: attempt {attempt + 1} failed: {exc}", flush=True)
            if attempt < 2:
                time.sleep(1 + attempt)
    raise RuntimeError(f"Cannot fetch original archive: {last_error}")


def main() -> int:
    inventory = json.loads(CATALOG.read_text(encoding="utf-8"))
    maps = inventory["maps"]
    for game, count in EXPECTED.items():
        if sum(m["game"] == game for m in maps) != count:
            raise ValueError(f"Pinned {game} catalogue unexpectedly changed")

    existing = ROOT / "update/maps/Gladiators/Gladiators_Caged_In.zip"
    if not existing.is_file() or existing.stat().st_size < 1024:
        raise ValueError("The repository's known authentic Gladiators Caged In map is missing")

    recovered = []
    errors = []
    for map_data in maps:
        game, name, source_file = map_data["game"], map_data["name"], map_data["file"]
        if game == "Gladiators" and name == "Caged In":
            recovered.append({"game": game, "name": name, "status": "already-present-in-1.8-source",
                              "path": str(existing.relative_to(ROOT))})
            continue
        url = inventory["download_base"] + "/" + game.lower() + "/" + source_file
        try:
            payload = fetch(url)
            details = validate(payload)
            target = destination(game, name, details["has_reborn_worldconfig"])
            if target.exists():
                if hashlib.sha256(target.read_bytes()).hexdigest() != hashlib.sha256(payload).hexdigest():
                    raise ValueError(f"Existing recovered file differs from Nixuge original: {target}")
                status = "existing-identical"
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(payload)
                status = "configured-source-map" if details["has_reborn_worldconfig"] else "raw-world-needs-metadata"
            info = {"game": game, "name": name, "id": map_data["id"],
                    "url": url, "path": str(target.relative_to(ROOT)),
                    "sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload),
                    "status": status, **details}
            recovered.append(info)
            print(f"[recovered] {game} {name}: {len(payload):,} bytes, "
                  f"Reborn metadata={details['has_reborn_worldconfig']}, {status}", flush=True)
        except Exception as exc:
            errors.append({"game": game, "name": name, "url": url, "error": str(exc)})
            print(f"[missing] {game} {name}: {exc}", flush=True)

    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({"source": inventory["source"],
                                  "source_commit": inventory["source_commit"],
                                  "recovered": recovered, "errors": errors}, indent=2) + "\n",
                      encoding="utf-8")
    print(f"[result] recovered={len(recovered)}/{len(maps)}, errors={len(errors)}")
    print(f"[result] report={REPORT.relative_to(ROOT)}", flush=True)
    return 0 if not errors else 2


if __name__ == "__main__":
    sys.exit(main())
