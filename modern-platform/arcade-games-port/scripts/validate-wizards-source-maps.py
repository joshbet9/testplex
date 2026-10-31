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


def read_nbt_payload(stream, tag_type, depth=0):
    import struct
    if depth > 64:
        raise ValueError("NBT nesting exceeds 64 levels")
    def unpack(fmt):
        size = struct.calcsize(fmt)
        raw = stream.read(size)
        if len(raw) != size:
            raise ValueError("truncated NBT payload")
        return struct.unpack(fmt, raw)[0]
    def read_string():
        length = unpack(">H")
        raw = stream.read(length)
        if len(raw) != length:
            raise ValueError("truncated NBT string")
        return raw.decode("utf-8", errors="replace")
    if tag_type == 1: return unpack(">b")
    if tag_type == 2: return unpack(">h")
    if tag_type == 3: return unpack(">i")
    if tag_type == 4: return unpack(">q")
    if tag_type == 5: return unpack(">f")
    if tag_type == 6: return unpack(">d")
    if tag_type == 7:
        length = unpack(">i")
        raw = stream.read(length)
        if length < 0 or len(raw) != length: raise ValueError("invalid NBT byte array")
        return raw
    if tag_type == 8: return read_string()
    if tag_type == 9:
        child_type = unpack(">B")
        length = unpack(">i")
        if length < 0 or length > 10_000_000: raise ValueError("invalid NBT list length")
        return [read_nbt_payload(stream, child_type, depth + 1) for _ in range(length)]
    if tag_type == 10:
        result = {}
        while True:
            child_type = unpack(">B")
            if child_type == 0: break
            child_name = read_string()
            result[child_name] = read_nbt_payload(stream, child_type, depth + 1)
        return result
    if tag_type == 11:
        length = unpack(">i")
        if length < 0 or length > 10_000_000: raise ValueError("invalid NBT int array")
        return [unpack(">i") for _ in range(length)]
    if tag_type == 12:
        length = unpack(">i")
        if length < 0 or length > 10_000_000: raise ValueError("invalid NBT long array")
        return [unpack(">q") for _ in range(length)]
    raise ValueError("unsupported NBT tag type " + str(tag_type))


def read_nbt(raw):
    import io, struct
    stream = io.BytesIO(raw)
    tag_type_raw = stream.read(1)
    if not tag_type_raw or tag_type_raw[0] != 10:
        raise ValueError("NBT root is not a compound")
    length_raw = stream.read(2)
    if len(length_raw) != 2: raise ValueError("truncated NBT root name")
    name_length = struct.unpack(">H", length_raw)[0]
    root_name = stream.read(name_length)
    if len(root_name) != name_length: raise ValueError("truncated NBT root name")
    return read_nbt_payload(stream, 10)


def decompress_nbt(raw, compression):
    import gzip, zlib
    if compression == 1: return gzip.decompress(raw)
    if compression == 2: return zlib.decompress(raw)
    if compression == 3: return raw
    raise ValueError("unknown Anvil chunk compression " + str(compression))


def inspect_level_dat(zf, path):
    try:
        raw = zf.read(path)
        import gzip
        try: decoded = gzip.decompress(raw)
        except OSError: decoded = raw
        root = read_nbt(decoded)
        data = root.get("Data", root)
        spawn = {key: data.get("Spawn" + key) for key in ("X", "Y", "Z")}
        version = data.get("DataVersion")
        version_tag = data.get("Version")
        if isinstance(version_tag, dict):
            version = version_tag.get("Id", version)
        return {"spawn": spawn, "data_version": version, "level_name": data.get("LevelName")}
    except Exception as error:
        return {"parse_error": str(error)}


def inspect_region_chunks(zf, region_paths):
    import struct
    active_chunks = []
    chests = []
    failures = []
    for region_path in region_paths:
        try:
            raw = zf.read(region_path)
            if len(raw) < 8192:
                failures.append(region_path + ": shorter than Anvil header")
                continue
            parts = PurePosixPath(region_path).stem.split(".")
            region_x, region_z = int(parts[1]), int(parts[2])
            for index in range(1024):
                offset = int.from_bytes(raw[index * 4:index * 4 + 3], "big")
                sectors = raw[index * 4 + 3]
                if offset == 0 or sectors == 0:
                    continue
                location = offset * 4096
                if location + 5 > len(raw):
                    failures.append(region_path + ": chunk offset outside file")
                    continue
                length = struct.unpack(">I", raw[location:location + 4])[0]
                compression = raw[location + 4]
                if length < 1 or location + 4 + length > len(raw):
                    failures.append(region_path + ": invalid chunk length")
                    continue
                chunk_x = region_x * 32 + (index % 32)
                chunk_z = region_z * 32 + (index // 32)
                active_chunks.append([chunk_x, chunk_z])
                try:
                    payload = decompress_nbt(raw[location + 5:location + 4 + length], compression)
                    chunk = read_nbt(payload)
                    level = chunk.get("Level", chunk)
                    for tile in level.get("TileEntities", []) if isinstance(level, dict) else []:
                        if not isinstance(tile, dict): continue
                        tile_id = str(tile.get("id", "")).lower()
                        if tile_id in {"chest", "minecraft:chest", "trapped_chest", "minecraft:trapped_chest"}:
                            chests.append({
                                "x": tile.get("x"), "y": tile.get("y"), "z": tile.get("z"),
                                "id": tile_id,
                            })
                except Exception as error:
                    failures.append(region_path + " chunk " + str(chunk_x) + "," + str(chunk_z) + ": " + str(error))
        except Exception as error:
            failures.append(region_path + ": " + str(error))
    return {
        "active_chunk_count": len(active_chunks),
        "active_chunk_coordinates_sample": active_chunks[:100],
        "chest_tile_entity_count": len(chests),
        "chest_tile_entities_sample": chests[:100],
        "region_parse_errors": failures[:20],
    }

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

        files = {name: info for name, info in entries if not info.is_dir()}
        level_entries = [name for name in files if PurePosixPath(name).name.lower() == "level.dat"]
        config_entries = [name for name in files if PurePosixPath(name).name.lower() == "worldconfig.dat"]
        downloader_entries = [name for name in files if PurePosixPath(name).name.lower() == "worlddownloader.txt"]
        suffix_counts = {}
        top_level = set()
        for name in files:
            suffix = PurePosixPath(name).suffix.lower() or "[no extension]"
            suffix_counts[suffix] = suffix_counts.get(suffix, 0) + 1
            parts = PurePosixPath(name).parts
            if parts:
                top_level.add(parts[0])
        relevant_names = [
            name for name in sorted(files)
            if (PurePosixPath(name).suffix.lower() in {".schematic", ".schem", ".mca", ".nbt"}
                or PurePosixPath(name).name.lower() in {"level.dat", "worldconfig.dat"}
                or "config" in PurePosixPath(name).name.lower())
        ]
        level_entries = [name for name in files if PurePosixPath(name).name.lower() == "level.dat"]
        config_entries = [name for name in files if PurePosixPath(name).name.lower() == "worldconfig.dat"]
        region_entries = [
            name for name in files
            if "region" in PurePosixPath(name).parts and name.lower().endswith(".mca")
        ]
        level_meta = inspect_level_dat(zf, level_entries[0]) if len(level_entries) == 1 else {
            "parse_error": "expected exactly one level.dat; found " + str(len(level_entries))
        }
        world_downloader_text = None
        if len(downloader_entries) == 1:
            try:
                world_downloader_text = zf.read(downloader_entries[0]).decode("utf-8", errors="replace")[:4000]
            except Exception as error:
                world_downloader_text = "Could not decode WorldDownloader.txt: " + str(error)
        region_meta = inspect_region_chunks(zf, region_entries)
        schematics = [
            name for name in files
            if PurePosixPath(name).suffix.lower() in {".schematic", ".schem"}
        ]
        inventory = {
            "top_level_entries": sorted(top_level)[:40],
            "extension_counts": dict(sorted(suffix_counts.items())),
            "level_dat_entries": sorted(level_entries)[:10],
            "world_config_entries": sorted(config_entries)[:10],
            "region_mca_entries": sorted(region_entries)[:10],
            "schematic_entries": sorted(schematics)[:20],
            "sample_files": sorted(files)[:30],
        }
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

        format_guess = (
            "schematic archive" if schematics else
            "Minecraft region/world export without Reborn metadata" if region_entries else
            "non-world map asset archive"
        )
        base = {
            "zip_entries": len(infos),
            "uncompressed_bytes": expanded,
            "inventory": inventory,
            "format_guess": format_guess,
            "level_dat": level_meta,
            "world_downloader_txt": world_downloader_text,
            "region_data": region_meta,
        }
        if not roots:
            return {
                **base,
                "importer_structure": "FAIL",
                "error": (
                    "No directory contains both level.dat and WorldConfig.dat. "
                    "This ZIP is not directly installable by SourceMapCatalog."
                ),
            }

        depth = {root: len(PurePosixPath(root).parts) if root else 0 for root in roots}
        shallowest = min(depth.values())
        candidates = sorted(root for root in roots if depth[root] == shallowest)
        if len(candidates) != 1:
            return {
                **base,
                "importer_structure": "FAIL",
                "error": "Ambiguous shallowest world roots: " + ", ".join(candidates),
            }
        root = candidates[0]
        prefix = f"{root}/region/" if root else "region/"
        regions = [
            name for name in files
            if name.startswith(prefix) and name.lower().endswith(".mca")
        ]
        if not regions:
            return {
                **base,
                "world_root": root or ".",
                "importer_structure": "FAIL",
                "error": "The world root has no region/*.mca terrain files.",
            }
        config_path = f"{root}/WorldConfig.dat" if root else "WorldConfig.dat"
        config_info = files[config_path]
        if config_info.file_size > 2_000_000:
            return {**base, "world_root": root or ".", "importer_structure": "FAIL",
                    "error": "WorldConfig.dat exceeds 2 MB."}
        meta = parse_config(zf.read(config_info))
        if not meta["map_name"]:
            return {**base, "world_root": root or ".", "importer_structure": "FAIL",
                    "error": "WorldConfig.dat has no MAP_NAME."}
        notes = []
        if not meta["spawn_count"]:
            notes.append("No valid TEAM_SPAWNS coordinates; review player spawn setup.")
        if not meta["has_horizontal_bounds"]:
            notes.append("Horizontal bounds incomplete; review arena bounds.")
        return {
            **base,
            "world_root": root or ".",
            "region_files": len(regions),
            "world_config": config_path,
            "metadata": meta,
            "importer_structure": "PASS",
            "notes": notes,
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
            inspection = inspect_zip(tmp)
            result.update(inspection)
            result["status"] = "PASS" if inspection.get("importer_structure") == "PASS" else "UNSUPPORTED_LAYOUT"
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
            lines.append(f"  - Finding: {r['error']}")
        inventory = r.get("inventory", {})
        if inventory:
            lines.append(f"  - Format guess: {r.get('format_guess', 'unknown')}")
            lines.append(f"  - Top-level entries: {', '.join(inventory.get('top_level_entries', []))}")
            lines.append(f"  - Extensions: {inventory.get('extension_counts', {})}")
            lines.append(f"  - Relevant files: {', '.join(inventory.get('relevant_names', inventory.get('schematic_entries', []) + inventory.get('region_mca_entries', []) + inventory.get('world_config_entries', []) + inventory.get('level_dat_entries', []))) or 'none'}")
            lines.append(f"  - Sample files: {', '.join(inventory.get('sample_files', []))}")
        if r.get("level_dat") is not None:
            lines.append(f"  - level.dat parse: {r.get('level_dat')}")
        if r.get("region_data") is not None:
            lines.append(f"  - Region/chest scan: {r.get('region_data')}")
        if r.get("world_downloader_txt"):
            lines.append("  - WorldDownloader.txt: " + r["world_downloader_txt"].replace("\n", " / ")[:1000])
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
