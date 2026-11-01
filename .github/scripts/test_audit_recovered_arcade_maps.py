#!/usr/bin/env python3
"""Unit and archive regression tests for the read-only Reborn metadata audit."""

import importlib.util
import json
from pathlib import Path
import unittest

SCRIPT = Path(__file__).with_name("audit_recovered_arcade_maps.py")
SPEC = importlib.util.spec_from_file_location("map_audit", SCRIPT)
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)


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

    def test_quiver_twin_peaks_is_not_evolution_configured(self):
        ref = audit.ROOT / "update/maps/One in the Quiver/Quiver_Twin Peaks 2.0.zip"
        report, _, _, _ = audit.scan_archive(ref, False)
        self.assertEqual(report["authored_marker_counts"], {"TEAM:GREEN": 16})
        # Its 16 Quiver spawns are not evidence of the RED/GREEN/YELLOW
        # platform + viewing + storage trio required by Evolution.

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
