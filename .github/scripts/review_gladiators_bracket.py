#!/usr/bin/env python3
"""Review reconstructed Gladiators bracket *candidates*, without creating maps.

The original Reborn WorldConfig.dat is absent from Nixuge's WDL archives.
Stage-centre and duel-pair annotations in review/ are hypotheses inferred from
visible terrain, NOT original data points or playable map metadata.
This tool checks the exact nearest-child algorithm used by 1.8 Gladiators
and nearest-BLACK selection, probing actual 1.8 blocks and route connectivity.
It does not establish authentic spawns, PINK points or emerald-ore gates, and
never creates a WorldConfig.dat.
"""

from __future__ import annotations

import argparse
from collections import deque
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

    def reachable_floor_positions(self, x: int, z: int, y: int,
                                  radius: int = 7) -> set[tuple[int, int]]:
        """Local foot positions connected to the candidate centre.

        Uses 4-neighbour horizontal steps at the same elevation. This measures
        immediately usable floor area, not an arena boundary or a gate route.
        """
        if y not in self.safe_feet_levels(x, z, y, y):
            return set()
        seen = {(x, z)}
        pending = deque([(x, z)])
        while pending:
            px, pz = pending.popleft()
            for nx, nz in ((px - 1, pz), (px + 1, pz), (px, pz - 1), (px, pz + 1)):
                if (nx, nz) in seen or (nx - x) ** 2 + (nz - z) ** 2 > radius ** 2:
                    continue
                if y not in self.safe_feet_levels(nx, nz, y, y):
                    continue
                seen.add((nx, nz))
                pending.append((nx, nz))
        return seen

    def reachable_floor_cells(self, x: int, z: int, y: int, radius: int = 7) -> int:
        return len(self.reachable_floor_positions(x, z, y, radius))

    def traversable(self, start: tuple[int, int, int],
                    finish: tuple[int, int, int], margin: int = 12) -> bool:
        """Conservative walking/jumping reachability across an arena transition.

        Nodes are clear two-block-high feet locations, with four-direction
        movement and at most a one-block vertical step. This detects blocked
        round transitions in the WDL capture; it cannot certify gate mechanics.
        """
        if start[1] not in self.safe_feet_levels(start[0], start[2], start[1], start[1]):
            return False
        lowx, highx = min(start[0], finish[0]) - margin, max(start[0], finish[0]) + margin
        lowz, highz = min(start[2], finish[2]) - margin, max(start[2], finish[2]) + margin
        visited = {start}
        pending = deque([start])
        while pending:
            x, y, z = pending.popleft()
            if (x, y, z) == finish:
                return True
            for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
                if not (lowx <= nx <= highx and lowz <= nz <= highz):
                    continue
                for ny in (y - 1, y, y + 1):
                    point = (nx, ny, nz)
                    if ny < 3 or ny > 10 or point in visited:
                        continue
                    if ny > y and self.block(x, y + 2, z) != 0:
                        continue
                    if ny not in self.safe_feet_levels(nx, nz, ny, ny):
                        continue
                    visited.add(point)
                    pending.append(point)
        return False


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


def validate_duel_proposals(stages: list[dict], proposals: list[dict],
                            world: WorldBlocks) -> dict:
    """Validate BLACK-position *proposals*, never claim original authorship."""
    problems = []
    by_name = {s["name"]: s for s in stages}
    if len(by_name) != 15:
        problems.append("Stage names must be unique")
    if len(proposals) != 15 or {p["arena"] for p in proposals} != set(by_name):
        problems.append("Expected exactly one two-spawn proposal for each arena")
    black = []
    for proposal in proposals:
        name = proposal["arena"]
        stage = by_name.get(name)
        if not stage:
            problems.append(f"Unknown arena {name}")
            continue
        if proposal.get("status") != "RECONSTRUCTED_UNVERIFIED":
            problems.append(f"{name}: proposals cannot be labelled authenticated/playable")
        points = proposal.get("points", [])
        if len(points) != 2:
            problems.append(f"{name}: expected two opposite spawns")
            continue
        if any(len(point) != 3 or any(type(v) is not int for v in point)
               for point in points):
            problems.append(f"{name}: coordinates must be integer X,Y,Z triples")
            continue
        a, b = points
        for point in points:
            x, y, z = point
            black.append((name, (x, y, z)))
            if y != 6:
                problems.append(f"{name}: proposed Y must be separately verified")
            if not all(y in world.safe_feet_levels(x + dx, z + dz, y, y)
                       for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
                problems.append(f"{name}: no 3x3 clear floor around {point}")
            if (x, z) not in world.reachable_floor_positions(
                    stage["x"], stage["z"], y, radius=12):
                problems.append(f"{name}: no same-level route from centre to {point}")
        ax, az = a[0] - stage["x"], a[2] - stage["z"]
        bx, bz = b[0] - stage["x"], b[2] - stage["z"]
        if (ax - bx) ** 2 + (az - bz) ** 2 < 144:
            problems.append(f"{name}: proposed duel spawns closer than 12 blocks")
        if ax * bx + az * bz > -0.75 * (
                (ax * ax + az * az) * (bx * bx + bz * bz)) ** 0.5:
            problems.append(f"{name}: duel spawns are not opposing one another")
    if len(black) != 30 or len({coords for _, coords in black}) != 30:
        problems.append("Expected 30 distinct BLACK spawn proposals")
    if len(black) == 30:
        for stage in stages:
            # Arena.setupSpawns clones all BLACK points, chooses its nearest,
            # removes that point, and chooses the second nearest to its mid.
            closest = sorted(black, key=lambda entry:
                             (entry[1][0] - stage["x"]) ** 2
                             + (entry[1][1] - 6) ** 2
                             + (entry[1][2] - stage["z"]) ** 2)[:2]
            if {name for name, _ in closest} != {stage["name"]}:
                problems.append(f"{stage['name']}: legacy closest-BLACK pairing leaks "
                                f"into other arenas")
    return {"valid_duel_proposals": not problems, "problems": problems,
            "proposed_black_count": len(black)}


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
        "connected_safe_floor_cells_at_y6_within_radius7":
            world.reachable_floor_cells(s["x"], s["z"], 6),
        "blocks_in_column": world.column(s["x"], s["z"]),
    } for s in stages]
    if "duel_spawn_proposals" in candidate:
        result["duel_probe"] = validate_duel_proposals(
            stages, candidate["duel_spawn_proposals"], world)
    by_name = {s["name"]: s for s in stages}
    result["transition_routes"] = [
        {"child": link["child"], "stage": link["stage"],
         "walkable_in_captured_world": world.traversable(
             (by_name[link["child"]]["x"], 6, by_name[link["child"]]["z"]),
             (by_name[link["stage"]]["x"], 6, by_name[link["stage"]]["z"]))}
        for link in result["links"]
    ]
    # A geometry-only review with unverified round transitions is NEVER enough
    # to construct a playable source map or certify game-specific markers.
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate", type=Path, help="Geometry-only review JSON")
    parser.add_argument("--image", type=Path,
                        help="Optional preview with clearly marked UNVERIFIED candidate tiers")
    parser.add_argument("--centres-only", action="store_true",
                        help="On a preview, show only the candidate stage centres")
    args = parser.parse_args()
    candidate = json.loads(args.candidate.read_text())
    print(json.dumps(review_candidate(candidate), indent=2))
    if args.image:
        _, top, _, _ = scan_archive(ROOT / candidate["source_archive"], True,
                                    hide_barriers=True)
        marks = [{"kind": "CANDIDATE", "name": stage["tier"],
                  "x": stage["x"], "y": 6, "z": stage["z"]}
                 for stage in candidate["stages"]]
        if not args.centres_only:
            for duel in candidate.get("duel_spawn_proposals", []):
                marks.extend({"kind": "CANDIDATE", "name": "BLACK",
                              "x": x, "y": y, "z": z}
                             for x, y, z in duel["points"])
        preview(args.image, top, [], marks,
                annotation_label=("UNVERIFIED centres (colour = round)"
                                  if args.centres_only
                                  else "UNVERIFIED centres + white duel pairs"))


if __name__ == "__main__":
    main()
