#!/usr/bin/env python3
"""Read-only spatial audit of recovered legacy Minecraft worlds.

The Nixuge WorldDownloader backups contain terrain, not Reborn's authored
WorldConfig.dat. This tool reports *observations*, never fabricates spawn
points, generates a WorldConfig, or promotes a map into playable rotation.

Usage: python3 .github/scripts/audit_recovered_arcade_maps.py --json /tmp/map-audit.json
       python3 .github/scripts/audit_recovered_arcade_maps.py --images /tmp/map-preview

Requires only the Python standard library for JSON; PNG previews additionally
require Pillow. All inputs are processed directly from ZIP entries in memory.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import zlib
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
RECOVERY = ROOT / "update/map-recovery/nixuge"
PROVENANCE = RECOVERY / "provenance.json"


def read_exact(stream: io.BytesIO, length: int) -> bytes:
    if length < 0 or length > 8_000_000:
        raise ValueError("Suspicious NBT element length")
    data = stream.read(length)
    if len(data) != length:
        raise ValueError("Truncated NBT element")
    return data


def number(stream: io.BytesIO, fmt: str):
    return struct.unpack(">" + fmt, read_exact(stream, struct.calcsize(fmt)))[0]


def nbt_payload(stream: io.BytesIO, kind: int, depth: int = 0):
    if depth > 32:
        raise ValueError("NBT nesting too deep")
    if kind in (1, 2, 3, 4, 5, 6):
        return number(stream, {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[kind])
    if kind == 7:
        return read_exact(stream, number(stream, "i"))
    if kind == 8:
        return read_exact(stream, number(stream, "H")).decode("utf-8", "replace")
    if kind == 9:
        element = number(stream, "B")
        size = number(stream, "i")
        if size < 0 or size > 1_000_000:
            raise ValueError("Suspicious NBT list size")
        return [nbt_payload(stream, element, depth + 1) for _ in range(size)]
    if kind == 10:
        result = {}
        while True:
            child = number(stream, "B")
            if child == 0:
                break
            name = nbt_payload(stream, 8, depth + 1)
            result[name] = nbt_payload(stream, child, depth + 1)
        return result
    if kind in (11, 12):
        size = number(stream, "i")
        if size < 0 or size > 1_000_000:
            raise ValueError("Suspicious NBT array size")
        fmt = "i" if kind == 11 else "q"
        raw = read_exact(stream, size * struct.calcsize(fmt))
        return list(struct.unpack(">" + fmt * size, raw)) if size else []
    raise ValueError(f"Unknown NBT tag {kind}")


def read_nbt(data: bytes):
    stream = io.BytesIO(data)
    if number(stream, "B") != 10:
        raise ValueError("NBT root is not a compound")
    nbt_payload(stream, 8)  # root name
    return nbt_payload(stream, 10)


def chunks(region: bytes, region_x: int, region_z: int):
    if len(region) < 8192:
        raise ValueError("Truncated region header")
    for slot in range(1024):
        offset = int.from_bytes(region[slot * 4:slot * 4 + 3], "big")
        sectors = region[slot * 4 + 3]
        if not offset or not sectors:
            continue
        start = offset * 4096
        if start + 5 > len(region):
            raise ValueError("Region chunk points beyond archive")
        length = int.from_bytes(region[start:start + 4], "big")
        if length < 2 or length > sectors * 4096 - 4 or start + 4 + length > len(region):
            raise ValueError("Invalid region chunk length")
        encoding = region[start + 4]
        payload = region[start + 5:start + 4 + length]
        if encoding == 2:
            decompressed = zlib.decompress(payload)
        elif encoding == 1:
            decompressed = gzip.decompress(payload)
        elif encoding == 3:
            decompressed = payload
        else:
            raise ValueError(f"Unsupported region chunk compression {encoding}")
        if len(decompressed) > 8_000_000:
            raise ValueError("Oversized uncompressed chunk")
        nbt = read_nbt(decompressed)
        chunk_x = region_x * 32 + slot % 32
        chunk_z = region_z * 32 + slot // 32
        level = nbt.get("Level", nbt)
        if (level.get("xPos", chunk_x), level.get("zPos", chunk_z)) != (chunk_x, chunk_z):
            raise ValueError("Region chunk coordinates do not match their slot")
        yield chunk_x, chunk_z, level


def parse_reborn_config(contents: str):
    """Count authored markers only; parsed data is never reused as a map guess."""
    markers = []
    group = ""
    name = ""
    for raw in contents.splitlines():
        key, sep, value = raw.partition(":")
        if not sep:
            continue
        if key == "TEAM_NAME":
            group, name = "TEAM", value.strip().upper()
        elif key == "DATA_NAME":
            group, name = "DATA", value.strip().upper()
        elif key == "CUSTOM_NAME":
            group, name = "CUSTOM", value.strip().upper()
        elif key in ("TEAM_SPAWNS", "DATA_LOCS", "CUSTOM_LOCS"):
            for triple in value.split(":"):
                coords = triple.strip().split(",")
                if len(coords) != 3:
                    continue
                try:
                    x, y, z = (float(c) for c in coords)
                except ValueError:
                    continue
                markers.append({"kind": group, "name": name, "x": x, "y": y, "z": z})
    return markers


def scan_archive(archive: Path, want_image: bool, hide_barriers: bool = False):
    summary = {"archive": archive.relative_to(ROOT).as_posix(), "worldconfig_present": False,
               "chunks": 0, "chunk_bounds": None, "version_formats": [],
               "emerald_ore_blocks": 0, "emerald_ore_bounds": None,
               "level_spawn_unverified": None, "wdl_player_positions_unverified": []}
    chunk_coords = []
    emerald = []
    top_blocks = {} if want_image else None
    formats = set()
    markers = []
    with ZipFile(archive) as world:
        names = {name.replace("\\", "/"): name for name in world.namelist()}
        summary["worldconfig_present"] = any(name.endswith("WorldConfig.dat") for name in names)
        for name, original in names.items():
            if name.endswith("WorldConfig.dat"):
                markers = parse_reborn_config(world.read(original).decode("utf-8"))
                break
        level_name = next((original for name, original in names.items() if name.endswith("level.dat")), None)
        if level_name:
            level = read_nbt(gzip.decompress(world.read(level_name))).get("Data", {})
            summary["level_spawn_unverified"] = [level.get("SpawnX"), level.get("SpawnY"), level.get("SpawnZ")]
        for name, original in names.items():
            if re.search(r"(^|/)playerdata/[^/]+\.dat$", name):
                try:
                    player = read_nbt(gzip.decompress(world.read(original)))
                    if len(player.get("Pos", [])) == 3:
                        summary["wdl_player_positions_unverified"].append([round(float(n), 2) for n in player["Pos"]])
                except (OSError, ValueError, EOFError):
                    pass
            match = re.search(r"(?:^|/)region/r\.(-?\d+)\.(-?\d+)\.mca$", name)
            if not match:
                continue
            for cx, cz, data in chunks(world.read(original), int(match[1]), int(match[2])):
                chunk_coords.append((cx, cz))
                summary["chunks"] += 1
                sections = data.get("Sections", [])
                if not sections:
                    formats.add("no-legacy-sections")
                    continue
                formats.add("legacy-sections")
                for section in sections:
                    blocks = section.get("Blocks", b"")
                    if len(blocks) != 4096:
                        formats.add("non-legacy-blocks")
                        continue
                    y0 = int(section.get("Y", 0)) * 16
                    extra = section.get("Add", b"")
                    for offset, block in enumerate(blocks):
                        if block != 129:
                            continue
                        if extra and ((extra[offset // 2] >> (4 * (offset % 2))) & 15):
                            continue
                        x = cx * 16 + offset % 16
                        z = cz * 16 + (offset // 16) % 16
                        y = y0 + offset // 256
                        emerald.append((x, y, z))
                if want_image:
                    surfaces = {}
                    for section in sorted(sections, key=lambda item: int(item.get("Y", 0)), reverse=True):
                        blocks = section.get("Blocks", b"")
                        if len(blocks) != 4096:
                            continue
                        y0 = int(section.get("Y", 0)) * 16
                        for local_z in range(16):
                            for local_x in range(16):
                                key = (local_x, local_z)
                                if key in surfaces:
                                    continue
                                for local_y in range(15, -1, -1):
                                    block = blocks[local_y * 256 + local_z * 16 + local_x]
                                    # Minecraft's barrier is invisible to players. WDL
                                    # captures often have a broad barrier roof at Y=18
                                    # that hides all arenas in a naive surface preview.
                                    # Skipping only that block reveals the geometry
                                    # underneath; the blocks remain in the world ZIP.
                                    if block and not (hide_barriers and block == 166):
                                        surfaces[key] = (y0 + local_y, block)
                                        break
                    for (lx, lz), surface in surfaces.items():
                        top_blocks[(cx * 16 + lx, cz * 16 + lz)] = surface
    if chunk_coords:
        summary["chunk_bounds"] = {
            "min_x": min(x for x, _ in chunk_coords) * 16,
            "max_x": max(x for x, _ in chunk_coords) * 16 + 15,
            "min_z": min(z for _, z in chunk_coords) * 16,
            "max_z": max(z for _, z in chunk_coords) * 16 + 15}
    if emerald:
        summary["emerald_ore_bounds"] = {"min_x": min(x for x, _, _ in emerald),
            "max_x": max(x for x, _, _ in emerald), "min_y": min(y for _, y, _ in emerald),
            "max_y": max(y for _, y, _ in emerald), "min_z": min(z for _, _, z in emerald),
            "max_z": max(z for _, _, z in emerald)}
    summary["emerald_ore_blocks"] = len(emerald)
    summary["version_formats"] = sorted(formats)
    if summary["worldconfig_present"]:
        summary["authored_marker_counts"] = {
            group: sum(m["kind"] + ":" + m["name"] == group for m in markers)
            for group in sorted({m["kind"] + ":" + m["name"] for m in markers})}
    return summary, top_blocks, emerald, markers


def preview(path: Path, top_blocks: dict, emerald: list, markers: list,
            annotation_label: str = "marks: authored if present"):
    from PIL import Image, ImageDraw
    if not top_blocks:
        return
    xs, zs = zip(*top_blocks)
    left, right, top, bottom = min(xs), max(xs), min(zs), max(zs)
    scale = max(1, (max(right - left, bottom - top) + 1) // 900 + 1)
    width, height = (right - left) // scale + 1, (bottom - top) // scale + 1
    image = Image.new("RGB", (width, height), (12, 16, 26))
    pixels = image.load()
    color = {1: (94, 99, 105), 2: (65, 130, 55), 3: (105, 78, 49), 5: (157, 125, 82),
             8: (40, 80, 152), 9: (40, 80, 152), 18: (57, 112, 54), 20: (155, 198, 207),
             35: (184, 177, 164), 79: (173, 206, 223), 87: (94, 50, 48), 89: (223, 193, 94),
             159: (184, 127, 88)}
    for (x, z), (y, block) in top_blocks.items():
        xy = ((x - left) // scale, (z - top) // scale)
        base = color.get(block, (104, 100, 93))
        tint = max(-35, min(35, (y - 64) // 2))
        pixels[xy] = tuple(min(255, max(0, value + tint)) for value in base)
    draw = ImageDraw.Draw(image)
    # World coordinates accompany the image; grid spacing uses exact 32-block cells.
    for x in range((left // 32 + 1) * 32, right, 32):
        px = (x - left) // scale
        draw.line([(px, 0), (px, height)], fill=(42, 45, 61), width=1)
    for z in range((top // 32 + 1) * 32, bottom, 32):
        pz = (z - top) // scale
        draw.line([(0, pz), (width, pz)], fill=(42, 45, 61), width=1)
    for x, _, z in emerald:
        px, pz = (x - left) // scale, (z - top) // scale
        draw.point((px, pz), fill=(251, 60, 210))
    marker_colors = {"GREEN": "#36e56d", "YELLOW": "#f5de4e", "ORANGE": "#f89a36",
                     "RED": "#f54949", "BLACK": "#ffffff", "PINK": "#ff76dd"}
    for mark in markers:
        px, pz = (mark["x"] - left) // scale, (mark["z"] - top) // scale
        paint = marker_colors.get(mark["name"], "#37ddfb")
        draw.ellipse((px - 3, pz - 3, px + 3, pz + 3), fill=paint, outline="#111111", width=1)
    # Add explicit X and Z labels so reviewers can record coordinates without
    # treating a coloured dot or a world-download viewpoint as authored metadata.
    labelled = Image.new("RGB", (width + 50, height + 70), (20, 25, 33))
    labelled.paste(image, (46, 32))
    labelled_draw = ImageDraw.Draw(labelled)
    for x in range((left // 32 + 1) * 32, right, 32):
        px = 46 + (x - left) // scale
        labelled_draw.text((px - 10, 8), str(x), fill="#dddddd")
    for z in range((top // 32 + 1) * 32, bottom, 32):
        pz = 32 + (z - top) // scale
        labelled_draw.text((2, pz - 5), str(z), fill="#dddddd")
    labelled_draw.text((46, height + 35),
                       f"X {left}..{right}, Z {top}..{bottom}", fill="#eeeeee")
    labelled_draw.text((46, height + 49), annotation_label, fill="#eeeeee")
    path.parent.mkdir(parents=True, exist_ok=True)
    labelled.save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", type=Path, help="Write audit JSON (else print to stdout)")
    parser.add_argument("--images", type=Path, help="Optional PNG preview output directory")
    parser.add_argument("--hide-barriers", action="store_true",
                        help="In PNG previews, see through invisible barrier blocks (166) to the underlying terrain")
    parser.add_argument("--game", choices=["Evolution", "Gladiators"], help="Audit one game")
    parser.add_argument("--references", action="store_true", help="Include configured Caged In and Quiver Twin Peaks references")
    args = parser.parse_args()
    provenance = json.loads(PROVENANCE.read_text())
    report = {"purpose": "Evidence for manual marker reconstruction; no inferred spawns or gameplay approval",
              "maps": []}
    for item in provenance["recovered"]:
        if item["status"] not in ("raw-world-needs-reborn-metadata", "already-verified-unconfigured"):
            continue
        if args.game and args.game != item["game"]:
            continue
        archive = ROOT / item["path"]
        actual_sha = hashlib.sha256(archive.read_bytes()).hexdigest()
        if actual_sha != item["sha256"]:
            raise ValueError(f"Recovered archive checksum mismatch: {archive}")
        result, top, emerald, markers = scan_archive(archive, args.images is not None,
                                                    hide_barriers=args.hide_barriers)
        result.update({"game": item["game"], "name": item["name"],
                       "sha256": item["sha256"],
                       "marker_status": "UNVERIFIED: never use level/player WDL coordinates as authentic game markers"})
        report["maps"].append(result)
        if args.images:
            preview(args.images / item["game"] / (archive.stem + ".png"), top, emerald, markers)
    if args.references:
        report["configured_references"] = []
        for game, rel in (
                ("Gladiators", "update/maps/Gladiators/Gladiators_Caged_In.zip"),
                ("One in the Quiver", "update/maps/One in the Quiver/Quiver_Twin Peaks 2.0.zip")):
            archive = ROOT / rel
            result, top, emerald, markers = scan_archive(archive, args.images is not None,
                                                        hide_barriers=args.hide_barriers)
            result["game"] = game
            report["configured_references"].append(result)
            if args.images:
                preview(args.images / "reference" / (archive.stem + ".png"), top, emerald, markers)
    report["maps"].sort(key=lambda item: (item["game"], item["name"]))
    content = json.dumps(report, indent=2) + "\n"
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(content)
    else:
        print(content, end="")


if __name__ == "__main__":
    main()
