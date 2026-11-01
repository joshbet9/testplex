#!/usr/bin/env python3
"""Download the public Wizards map snapshots for a disposable Paper acceptance run.

Archives remain under the caller-provided temporary server directory. They are
never written into the repository or packaged plugin artifacts.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import sys
import tempfile
import time
import zipfile
from pathlib import Path
from urllib.request import Request, urlopen

BASE = "https://maps.nixuge.me/static/mineplex/zip"
MAPS = [
    "haven", "wizardarena", "orbisharbor", "aspen", "hogwarts",
    "industrialwest", "alicudi", "thaumaturgecolony", "magicallypossible", "kalamit",
]
POOLS = [
    ("Wizards", f"{BASE}/wizards"),
    ("Nano Wizards", f"{BASE}/nano/nanowizards"),
]
MAX_DOWNLOAD_BYTES = 1_500_000_000
USER_AGENT = "Testplex-Wizards-Paper-Smoke/1.0 (https://github.com/joshbet9/testplex)"


def download(url: str, destination: Path) -> tuple[int, str]:
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/zip"})
    digest = hashlib.sha256()
    total = 0
    destination.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary_name = tempfile.mkstemp(prefix=destination.name + ".", suffix=".part", dir=destination.parent)
    try:
        with os.fdopen(fd, "wb") as output, urlopen(request, timeout=60) as response:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                total += len(chunk)
                if total > MAX_DOWNLOAD_BYTES:
                    raise RuntimeError(f"Archive exceeds {MAX_DOWNLOAD_BYTES} byte safety limit: {url}")
                output.write(chunk)
                digest.update(chunk)
        if total == 0:
            raise RuntimeError(f"Downloaded an empty archive: {url}")
        with zipfile.ZipFile(temporary_name) as archive:
            bad_member = archive.testzip()
            if bad_member is not None:
                raise RuntimeError(f"ZIP CRC failed for {url}: {bad_member}")
            for entry in archive.infolist():
                normalized = entry.filename.replace("\\", "/")
                if normalized.startswith("/") or re.match(r"^[A-Za-z]:", normalized):
                    raise RuntimeError(f"Unsafe absolute ZIP entry in {url}: {entry.filename}")
                if ".." in Path(normalized).parts:
                    raise RuntimeError(f"Unsafe traversal ZIP entry in {url}: {entry.filename}")
        os.replace(temporary_name, destination)
        return total, digest.hexdigest()
    finally:
        try:
            os.unlink(temporary_name)
        except FileNotFoundError:
            pass


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--map-root", required=True, type=Path,
                        help="Temporary update/maps directory used by the Paper server process")
    args = parser.parse_args()
    args.map_root.mkdir(parents=True, exist_ok=True)

    completed = 0
    total_bytes = 0
    for folder, base in POOLS:
        for slug in MAPS:
            url = f"{base}/{slug}.zip"
            destination = args.map_root / folder / f"{slug}.zip"
            size, sha256 = download(url, destination)
            completed += 1
            total_bytes += size
            print(f"MAP_ARCHIVE|PASS|{folder}/{slug}.zip|bytes={size}|sha256={sha256}", flush=True)
    print(f"Downloaded and CRC-checked {completed}/20 Wizards snapshots ({total_bytes:,} bytes).", flush=True)
    return 0 if completed == 20 else 1


if __name__ == "__main__":
    raise SystemExit(main())
