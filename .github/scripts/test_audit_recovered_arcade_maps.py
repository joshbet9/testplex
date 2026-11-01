#!/usr/bin/env python3
"""Unit and archive regression tests for the read-only Reborn metadata audit."""

import importlib.util
import json
from pathlib import Path
import sys
import unittest
from zipfile import ZipFile

SCRIPT = Path(__file__).with_name("audit_recovered_arcade_maps.py")
SPEC = importlib.util.spec_from_file_location("map_audit", SCRIPT)
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)
sys.path.insert(0, str(SCRIPT.parent))
import review_gladiators_bracket as bracket


class AuditRecoveredArcadeMapsTest(unittest.TestCase):
    def test_reborn_marker_parser_tracks_groups_separately(self):
        markers = audit.parse_reborn_config("""MAP_NAME:Example
TEAM_NAME:Green
TEAM_SPAWNS:1,2,3:4,5,6:
DATA_NAME:RED
DATA_LOCS:7,8,9:
DATA_NAME:BLACK
DATA_LOCS:10,11,12:13,14,15:
CUSTOM_NAME:129
CUSTOM_LOCS:16,17,18:
""")
        self.assertEqual([(x["kind"], x["name"]) for x in markers],
                         [("TEAM", "GREEN"), ("TEAM", "GREEN"), ("DATA", "RED"),
                          ("DATA", "BLACK"), ("DATA", "BLACK"), ("CUSTOM", "129")])
        self.assertEqual((markers[0]["x"], markers[0]["y"], markers[0]["z"]), (1, 2, 3))

    def test_real_legacy_gladiators_reference_has_all_bracket_tiers(self):
        ref = audit.ROOT / "update/maps/Gladiators/Gladiators_Caged_In.zip"
        report, _, _, markers = audit.scan_archive(ref, False)
        self.assertTrue(report["worldconfig_present"])
        self.assertGreater(report["chunks"], 0)
        counts = report["authored_marker_counts"]
        self.assertEqual(counts["DATA:RED"], 1)
        self.assertEqual(counts["DATA:ORANGE"], 2)
        self.assertEqual(counts["DATA:YELLOW"], 4)
        self.assertEqual(counts["DATA:GREEN"], 8)
        self.assertEqual(counts["DATA:BLACK"], 30)
        self.assertEqual(counts["TEAM:GREEN"], 1)
        self.assertTrue(markers)

    def test_invisible_barrier_roof_can_be_omitted_from_preview_only(self):
        archive = (audit.ROOT / "update/map-recovery/nixuge/unconfigured/Gladiators/"
                   "Gladiators_Bliss.zip")
        normal, surface, _, _ = audit.scan_archive(archive, True)
        revealed, under_roof, _, _ = audit.scan_archive(archive, True,
                                                       hide_barriers=True)
        self.assertEqual(normal, revealed)
        self.assertEqual(surface[(0, 0)], (18, 166))
        self.assertEqual(under_roof[(0, 0)], (5, 95))

    def test_authentic_caged_in_calibrates_bracket_and_spawn_safety(self):
        archive = audit.ROOT / "update/maps/Gladiators/Gladiators_Caged_In.zip"
        with ZipFile(archive) as original:
            marks = audit.parse_reborn_config(original.read("WorldConfig.dat").decode())
        centres = [
            {"name": f"{m['name']}-{index}", "tier": m["name"],
             "x": m["x"], "z": m["z"]}
            for index, m in enumerate(marks)
            if m["kind"] == "DATA" and m["name"] in bracket.TIERS
        ]
        self.assertTrue(bracket.validate_tournament(centres)["valid_bracket_topology"])
        world = bracket.WorldBlocks(archive)
        black = [m for m in marks if m["kind"] == "DATA" and m["name"] == "BLACK"]
        self.assertEqual(len(black), 30)
        self.assertTrue(all(m["y"] in world.safe_feet_levels(int(m["x"]), int(m["z"]))
                            for m in black))

    def test_bliss_centres_are_only_review_candidates(self):
        path = (audit.ROOT / "update/map-recovery/nixuge/review/"
                "gladiators-bliss-stage-centres.json")
        result = bracket.review_candidate(json.loads(path.read_text()))
        self.assertTrue(result["valid_bracket_topology"], result["problems"])
        self.assertTrue(result["observation_only_no_playable_metadata"])
        self.assertEqual(len(result["links"]), 14)
        self.assertEqual(len(result["stage_probes"]), 15)
        self.assertTrue(all(6 in p["safe_feet_y_candidates"]
                            for p in result["stage_probes"]))

    def test_quiver_twin_peaks_is_not_evolution_configured(self):
        ref = audit.ROOT / "update/maps/One in the Quiver/Quiver_Twin Peaks 2.0.zip"
        report, _, _, _ = audit.scan_archive(ref, False)
        self.assertEqual(report["authored_marker_counts"], {"TEAM:GREEN": 16})
        with ZipFile(ref) as original:
            config = next(name for name in original.namelist()
                          if name.replace("\\", "/").endswith("WorldConfig.dat"))
            markers = audit.parse_reborn_config(original.read(config).decode())
        spawns = [m for m in markers if m["kind"] == "TEAM" and m["name"] == "GREEN"]
        self.assertEqual(len(spawns), 16)
        world = bracket.WorldBlocks(ref)
        # These are *real, authored Quiver* coordinates with safe floor and
        # two-block clearance, so the old map is worth a manual Evo review.
        self.assertTrue(all(m["y"] in world.safe_feet_levels(int(m["x"]), int(m["z"]), 1, 80)
                            for m in spawns))
        # Being collision-safe does NOT demonstrate safe Evolution PvP
        # distances or supply the missing RED/GREEN/YELLOW station trio.

    def test_nixuge_recovery_still_has_no_authored_spawns(self):
        provenance = json.loads(audit.PROVENANCE.read_text())
        entries = [m for m in provenance["recovered"] if m["status"] in
                   ("raw-world-needs-reborn-metadata", "already-verified-unconfigured")]
        self.assertEqual(len(entries), 28)
        self.assertEqual(sum(m["game"] == "Evolution" for m in entries), 6)
        self.assertEqual(sum(m["game"] == "Gladiators" for m in entries), 22)
        # Check every archived WDL world for valid legacy chunks and no
        # missing source metadata without rewriting a single map ZIP.
        for item in entries:
            with self.subTest(map=item["name"], game=item["game"]):
                report, _, _, markers = audit.scan_archive(audit.ROOT / item["path"], False)
                self.assertFalse(report["worldconfig_present"])
                self.assertGreater(report["chunks"], 0)
                self.assertEqual(report["version_formats"], ["legacy-sections"])
                self.assertEqual(markers, [])


if __name__ == "__main__":
    unittest.main()
