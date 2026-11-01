#!/usr/bin/env python3
"""Review reconstructed Gladiators bracket *candidates*, without creating maps.

The original Reborn WorldConfig.dat is absent from Nixuge's WDL archives.
Stage-centre annotations in review/ are hypotheses inferred from visible
terrain, NOT original data points, spawn positions, or playable map metadata.
This tool checks the exact nearest-child algorithm used by 1.8 Gladiators
and probes actual 1.8 block columns. It does not infer BLACK spawns, PINK
progression markers, emerald-ore gates, or create a WorldConfig.dat.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
from zipfile import ZipFile

from audit_recovered_arcade_maps import ROOT, chunks, preview, scan_archive

TIERS = ("GREEN", "YELLOW", "ORANGE", "RED")
COUNTS = {"GREEN": 8, "YELLOW": 4, "ORANGE": 2, "RED": 1}

# The below IDs are only a conservative *floor* allow-list; a complete
# gameplay walkthrough and collision/gate review is still mandatory.
FULL_FLOOR_BLOCKS = frozenset({
    1, 2, 3, 4, 5, 7, 17, 20, 22, 24, 35, 41, 42, 43, 45, 49,
    57, 80, 82, 87, 95, 98, 99, 100, 112, 121, 133, 152, 155, 159, 173,
})


class WorldBlocks:
    """Random-access read-only index of the original, unmodified ZIP chunks."""

    def __init__(self, archive: Path):
        self.sections = {}
        self.chunk_keys = set()
        with ZipFile(archive) as source:
            for name in source.namelist():
                # Original Mineplex Caged In has Windows ZIP separators.
                match = re.search(r"(?:^|/)region/r\.(-?\d+)\.(-?\d+)\.mca$",
                                  name.replace("\\", "/"))
                if not match:
                    continue
                for cx, cz, data in chunks(source.read(name), int(match[1]), int(match[2])):
                    self.chunk_keys.add((cx, cz))
                    for section in data.get("Sections", []):
                        block_ids = section.get("Blocks", b"")
                        if len(block_ids) != 4096:
                            raise ValueError("Expected legacy block section")
                        self.sections[(cx, cz, int(section["Y"]))] = block_ids

    def block(self, x: int, y: int, z: int) -> int | None:
        """0=air, None=undownloaded chunk (never count it as air)."""
        if y < 0 or y > 255:
            return None
        cx, cz = x // 16, z // 16
        if (cx, cz) not in self.chunk_keys:
            return None
        section = self.sections.get((cx, cz, y // 16))
        if section is None:
            return 0
        return section[(y % 16) * 256 + (z % 16) * 16 + x % 16]

    def safe_feet_levels(self, x: int, z: int, low: int = 3, high: int = 24) -> list[int]:
        """Candidate feet heights with a full floor and two blocks of air.

        This detects obvious suffocation, not walls/arena access/PvP suitability.
        Barrier blocks (166) are deliberately NOT accepted as safe floors.
        """
        return [y for y in range(low, high + 1)
                if self.block(x, y - 1, z) in FULL_FLOOR_BLOCKS
                and self.block(x, y, z) == 0
                and self.block(x, y + 1, z) == 0]

    def column(self, x: int, z: int, high: int = 24) -> list[list[int]]:
        return [[y, block] for y in range(high + 1)
                if (block := self.block(x, y, z)) not in (None, 0)]


def closest_two(parent: dict, choices: list[dict]) -> list[dict]:
    def distance(candidate):
        return ((parent["x"] - candidate["x"]) ** 2
                + (parent["z"] - candidate["z"]) ** 2)
    # Input order is retained for tied distances, matching the legacy
    # UtilAlg.findClosest behaviour. Ties must be reviewed by a human.
    return sorted(choices, key=distance)[:2]


def validate_tournament(stages: list[dict]) -> dict:
    """Apply the same tier-linking rules as legacy Gladiators.parseArenas()."""
    groups = {color: [s for s in stages if s["tier"] == color] for color in TIERS}
    problems = []
    for color, count in COUNTS.items():
        if len(groups[color]) != count:
            problems.append(f"{color}: expected {count} centres, found {len(groups[color])}")
    if len(stages) != 15:
        problems.append(f"Expected 15 total centres, found {len(stages)}")
    links = []
    for outer, inner in zip(TIERS, TIERS[1:]):
        children = {id(stage): 0 for stage in groups[outer]}
        for parent in groups[inner]:
            matched = closest_two(parent, groups[outer])
            if len(matched) != 2:
                problems.append(f"{parent['name']}: only {len(matched)} children")
            for child in matched:
                children[id(child)] += 1
                links.append({"stage": parent["name"], "child": child["name"]})
        for child in groups[outer]:
            if children[id(child)] != 1:
                problems.append(f"{child['name']}: assigned to {children[id(child)]} "
                                f"{inner} parent(s), expected exactly one")
    return {"valid_bracket_topology": not problems, "problems": problems, "links": links}


def review_candidate(candidate: dict) -> dict:
    if candidate.get("status") != "UNVERIFIED_GEOMETRY_ONLY":
        raise ValueError("Refusing a candidate without explicit unverified status")
    rel = candidate["source_archive"]
    archive = (ROOT / rel).resolve()
    recovery = (ROOT / "update/map-recovery/nixuge/unconfigured").resolve()
    if not archive.is_relative_to(recovery):
        raise ValueError("Review candidate must refer to an unconfigured recovered ZIP")
    if hashlib.sha256(archive.read_bytes()).hexdigest() != candidate["source_sha256"]:
        raise ValueError("WDL source hash changed")

    stages = candidate["stages"]
    result = validate_tournament(stages)
    world = WorldBlocks(archive)
    result["map"] = candidate["map"]
    result["status"] = candidate["status"]
    result["observation_only_no_playable_metadata"] = True
    result["stage_probes"] = [{
        "name": s["name"], "tier": s["tier"], "x": s["x"], "z": s["z"],
        "safe_feet_y_candidates": world.safe_feet_levels(s["x"], s["z"]),
        "blocks_in_column": world.column(s["x"], s["z"]),
    } for s in stages]
    # No condition here claims an arena centre is a suitable duel spawn.
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate", type=Path, help="Geometry-only review JSON")
    parser.add_argument("--image", type=Path,
                        help="Optional preview with clearly marked UNVERIFIED candidate tiers")
    args = parser.parse_args()
    candidate = json.loads(args.candidate.read_text())
    print(json.dumps(review_candidate(candidate), indent=2))
    if args.image:
        _, top, _, _ = scan_archive(ROOT / candidate["source_archive"], True,
                                    hide_barriers=True)
        marks = [{"kind": "CANDIDATE", "name": stage["tier"],
                  "x": stage["x"], "y": 6, "z": stage["z"]}
                 for stage in candidate["stages"]]
        preview(args.image, top, [], marks,
                annotation_label="UNVERIFIED centres (colour = round)")


if __name__ == "__main__":
    main()
