#!/usr/bin/env python3
"""Validate Mineplex Wizards source ZIPs without retaining or rehosting binaries."""
from __future__ import annotations
import argparse, concurrent.futures, hashlib, json, os, re, stat, tempfile, time
from pathlib import Path, PurePosixPath
from urllib.request import Request, urlopen
import zipfile

BASE = "https://maps.nixuge.me/static/mineplex/zip"
MAPS = [
    ("haven", "Haven", "Wolfey"),
    ("wizardarena", "Wizard Arena", "libraryaddict"),
    ("orbisharbor", "Orbis Harbor", "BrandonMWX & Goldeey"),
    ("aspen", "Aspen", "TheDarkWarlord & bribbs"),
    ("hogwarts", "Hogwarts", "Alfredo & LukeAteDale"),
    ("industrialwest", "Industrial West", "Mineplex Build Team"),
    ("alicudi", "Alicudi", "btmodz, MrMagicMan & eggdumbhead"),
    ("thaumaturgecolony", "Thaumaturge Colony", "PhoenixSky"),
    ("magicallypossible", "Magically Possible", "ReindeerClark"),
    ("kalamit", "Kalamit", "TheDarkWarLord"),
]
POOLS = [
    ("Wizards", "Wizards", f"{BASE}/wizards"),
    ("Nano Wizards", "Nano Wizards", f"{BASE}/nano/nanowizards"),
]
MAX_DOWNLOAD = 1_500_000_000
MAX_EXPANDED = 8_000_000_000


def safe_name(name):
    name = name.replace("\\", "/")
    if name.startswith("/") or re.match(r"^[A-Za-z]:", name):
        raise ValueError("absolute ZIP entry path: " + name)
    parts = PurePosixPath(name).parts
    if ".." in parts:
        raise ValueError("traversal ZIP entry path: " + name)
    return "/".join(part for part in parts if part not in {"", "."})


def count_coordinates(value):
    count = 0
    for token in value.split(":"):
        xyz = token.strip().split(",")
        if len(xyz) < 3:
            continue
        try:
            [float(v.strip()) for v in xyz[:3]]
            count += 1
        except ValueError:
            pass
    return count


def parse_config(raw):
    meta = {"map_name": None, "bounds": {}, "team_spawns": {}, "data_locations": {}, "custom_locations": {}}
    team = data = custom = "Unassigned"
    for line in raw.decode("utf-8-sig", errors="replace").splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        key, value = key.strip().upper(), value.strip()
        if key == "MAP_NAME":
            meta["map_name"] = value or None
        elif key in {"MIN_X", "MAX_X", "MIN_Z", "MAX_Z", "MIN_Y", "MAX_Y"}:
            try:
                meta["bounds"][key.lower()] = int(value)
            except ValueError:
                meta["bounds"][key.lower()] = None
        elif key == "TEAM_NAME":
            team = value or "Unassigned"
            meta["team_spawns"].setdefault(team, 0)
        elif key == "TEAM_SPAWNS":
            meta["team_spawns"][team] = meta["team_spawns"].get(team, 0) + count_coordinates(value)
        elif key == "DATA_NAME":
            data = value or "Unassigned"
            meta["data_locations"].setdefault(data, 0)
        elif key == "DATA_LOCS":
            meta["data_locations"][data] = meta["data_locations"].get(data, 0) + count_coordinates(value)
        elif key == "CUSTOM_NAME":
            custom = value or "Unassigned"
            meta["custom_locations"].setdefault(custom, 0)
        elif key == "CUSTOM_LOCS":
            meta["custom_locations"][custom] = meta["custom_locations"].get(custom, 0) + count_coordinates(value)
    meta["spawn_count"] = sum(meta["team_spawns"].values())
    meta["data_location_count"] = sum(meta["data_locations"].values())
    meta["custom_location_count"] = sum(meta["custom_locations"].values())
    meta["has_horizontal_bounds"] = all(meta["bounds"].get(k) is not None for k in ("min_x", "max_x", "min_z", "max_z"))
    return meta


def inspect_zip(path):
    with zipfile.ZipFile(path) as zf:
        infos = zf.infolist()
        if not infos:
            raise ValueError("empty ZIP")
        entries, expanded = [], 0
        for info in infos:
            name = safe_name(info.filename)
            if not name:
                continue
            if info.flag_bits & 1:
                raise ValueError("encrypted ZIP entries are unsupported")
            if ((info.external_attr >> 16) & 0o170000) == stat.S_IFLNK:
                raise ValueError("symbolic-link ZIP entry rejected: " + info.filename)
            expanded += info.file_size
            if expanded > MAX_EXPANDED:
                raise ValueError("archive expands beyond 8 GB limit")
            entries.append((name, info))
        bad = zf.testzip()
        if bad:
            raise ValueError("ZIP CRC failed: " + bad)

        files = {name for name, info in entries if not info.is_dir()}
        roots = set()
        for name in files:
            if PurePosixPath(name).name != "WorldConfig.dat":
                continue
            parent = str(PurePosixPath(name).parent)
            if parent == ".":
                parent = ""
            level = f"{parent}/level.dat" if parent else "level.dat"
            if level in files:
                roots.add(parent)
        if not roots:
            raise ValueError("no folder contains both level.dat and WorldConfig.dat")
        depth = {root: len(PurePosixPath(root).parts) if root else 0 for root in roots}
        shallowest = min(depth.values())
        candidates = sorted(root for root in roots if depth[root] == shallowest)
        if len(candidates) != 1:
            raise ValueError("ambiguous shallowest world roots: " + ", ".join(candidates))
        root = candidates[0]
        prefix = f"{root}/region/" if root else "region/"
        regions = [name for name in files if name.startswith(prefix) and name.lower().endswith(".mca")]
        if not regions:
            raise ValueError("world root has no region/*.mca terrain files")
        config_path = f"{root}/WorldConfig.dat" if root else "WorldConfig.dat"
        config_info = next(info for name, info in entries if name == config_path and not info.is_dir())
        if config_info.file_size > 2_000_000:
            raise ValueError("WorldConfig.dat exceeds 2 MB")
        meta = parse_config(zf.read(config_info))
        if not meta["map_name"]:
            raise ValueError("WorldConfig.dat has no MAP_NAME")
        notes = []
        if not meta["spawn_count"]:
            notes.append("No valid TEAM_SPAWNS coordinates; review player spawn setup.")
        if not meta["has_horizontal_bounds"]:
            notes.append("Horizontal bounds incomplete; review arena bounds.")
        return {
            "zip_entries": len(infos), "uncompressed_bytes": expanded,
            "world_root": root or ".", "region_files": len(regions),
            "world_config": config_path, "metadata": meta,
            "importer_structure": "PASS", "notes": notes,
        }


def inspect_one(pool, item):
    slug, name, builder = item
    label, folder, prefix = pool
    url = f"{prefix}/{slug}.zip"
    result = {"pool": label, "pool_folder": folder, "slug": slug, "name": name,
              "builder": builder, "url": url, "status": "FAILED"}
    error = None
    for attempt in range(3):
        tmp = None
        try:
            request = Request(url, headers={"User-Agent": "Testplex-Wizards-SourceMap-Validator/1.0",
                                            "Accept": "application/zip,application/octet-stream,*/*"})
            with urlopen(request, timeout=90) as response:
                length = response.headers.get("Content-Length")
                if length and int(length) > MAX_DOWNLOAD:
                    raise ValueError("download exceeds 1.5 GB limit")
                digest, size = hashlib.sha256(), 0
                with tempfile.NamedTemporaryFile(prefix="wizards-map-", suffix=".zip", delete=False) as out:
                    tmp = Path(out.name)
                    while chunk := response.read(1024 * 1024):
                        size += len(chunk)
                        if size > MAX_DOWNLOAD:
                            raise ValueError("download exceeds 1.5 GB limit")
                        digest.update(chunk)
                        out.write(chunk)
            result.update({"download_bytes": size, "sha256": digest.hexdigest()})
            result.update(inspect_zip(tmp))
            result["status"] = "PASS"
            return result
        except Exception as exc:
            error = str(exc)
            if attempt < 2:
                time.sleep(2 ** attempt)
        finally:
            if tmp is not None:
                tmp.unlink(missing_ok=True)
    result["error"] = error or "unknown error"
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    tasks = [(pool, item) for pool in POOLS for item in MAPS]
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        futures = {executor.submit(inspect_one, pool, item): (pool, item) for pool, item in tasks}
        for future in concurrent.futures.as_completed(futures):
            pool, item = futures[future]
            try:
                results.append(future.result())
            except Exception as exc:
                results.append({"pool": pool[0], "pool_folder": pool[1], "slug": item[0],
                                "name": item[1], "builder": item[2], "status": "FAILED",
                                "error": "validator crashed: " + str(exc)})
    pool_order = {p[0]: i for i, p in enumerate(POOLS)}
    map_order = {m[0]: i for i, m in enumerate(MAPS)}
    results.sort(key=lambda r: (pool_order[r["pool"]], map_order[r["slug"]]))
    passed = sum(r["status"] == "PASS" for r in results)
    report = {"generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "source_catalogue": "https://maps.nixuge.me/mineplex/1/",
              "passed": passed, "failed": len(results) - passed, "total": len(results), "maps": results}
    json_path = args.output_dir / "wizards-source-map-validation.json"
    md_path = args.output_dir / "wizards-source-map-validation.md"
    json_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    lines = [
        "# Wizards source-map validation", "",
        f"- Generated: {report['generated_at']}",
        "- Source: https://maps.nixuge.me/mineplex/1/",
        "- ZIPs are downloaded to temporary storage, inspected, then deleted; binaries are not rehosted.",
        "",
        "| Pool | Map | Builder | Result | ZIP bytes | World root | Spawns | Data markers | SHA-256 |",
        "|---|---|---|---|---:|---|---:|---:|---|",
    ]
    for r in results:
        m = r.get("metadata", {})
        lines.append("| {pool} | [{name}]({url}) | {builder} | {status} | {size} | {root} | {spawns} | {data} | {sha} |".format(
            pool=r["pool"], name=r["name"], url=r["url"], builder=r["builder"], status=r["status"],
            size=f"{r.get('download_bytes', 0):,}" if "download_bytes" in r else "—",
            root=r.get("world_root", "—"), spawns=m.get("spawn_count", "—"),
            data=m.get("data_location_count", "—"), sha=r.get("sha256", "—")))
        if r.get("error"):
            lines.append(f"  - Failure: {r['error']}")
        for note in r.get("notes", []):
            lines.append(f"  - Review: {note}")
    lines += ["", "Structural PASS requires ZIP CRC success, one unambiguous shallowest world root with level.dat and WorldConfig.dat, and at least one region/*.mca terrain file. WorldConfig.dat is parsed for MAP_NAME, bounds, TEAM_SPAWNS, DATA_LOCS and CUSTOM_LOCS.",
              "", "This is not live Paper 26.3 loading or multiplayer acceptance. Zero spawn markers or missing bounds are flagged for manual review.",
              "", f"Totals: {passed}/{len(tasks)} archives passed structural validation.", ""]
    markdown = "\n".join(lines)
    md_path.write_text(markdown, encoding="utf-8")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as out:
            out.write(markdown + "\n")
    print(markdown)
    print(f"Reports: {json_path} and {md_path}")
    return 0 if passed == len(tasks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
