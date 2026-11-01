#!/usr/bin/env python3
"""Tests for non-playable arena geometry review and bracket linking."""

import importlib.util
import json
from pathlib import Path
import sys
import unittest

SCRIPT = Path(__file__).with_name("review_gladiators_bracket.py")
SPEC = importlib.util.spec_from_file_location("gladiators_review", SCRIPT)
sys.path.insert(0, str(SCRIPT.parent))
review = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(review)


class GladiatorsBracketReviewTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = (review.ROOT / "update/map-recovery/nixuge/review/"
                "gladiators-bliss-stage-centres.json")
        cls.candidate = json.loads(path.read_text())

    def test_recovered_bliss_review_is_geometry_only_and_links_are_unique(self):
        result = review.review_candidate(self.candidate)
        self.assertTrue(result["observation_only_no_playable_metadata"])
        self.assertTrue(result["valid_bracket_topology"], result["problems"])
        self.assertEqual(len(result["links"]), 14)
        self.assertEqual(len(result["stage_probes"]), 15)
        self.assertTrue(all(6 in stage["safe_feet_y_candidates"]
                            for stage in result["stage_probes"]))

    def test_missing_or_duplicated_bracket_child_is_rejected(self):
        stages = [dict(s) for s in self.candidate["stages"]]
        stages[7]["x"], stages[7]["z"] = 500, 500
        topology = review.validate_tournament(stages)
        self.assertFalse(topology["valid_bracket_topology"])
        self.assertTrue(any("parent(s)" in problem for problem in topology["problems"]))

    def test_unverified_status_and_source_hash_are_required(self):
        altered = dict(self.candidate, status="PLAYABLE")
        with self.assertRaisesRegex(ValueError, "unverified"):
            review.review_candidate(altered)
        altered = dict(self.candidate, source_sha256="0" * 64)
        with self.assertRaisesRegex(ValueError, "source hash"):
            review.review_candidate(altered)

    def test_block_probe_distinguishes_download_gaps_from_air(self):
        archive = review.ROOT / self.candidate["source_archive"]
        blocks = review.WorldBlocks(archive)
        self.assertEqual(blocks.block(0, 18, 0), 166)
        self.assertIsNone(blocks.block(100000, 5, 100000))
        self.assertNotIn(19, blocks.safe_feet_levels(0, 0))


if __name__ == "__main__":
    unittest.main()
