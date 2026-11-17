from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

from scripts.testplex_jar_delta import apply_delta, create_delta


class JarDeltaTests(unittest.TestCase):
    def write_jar(self, path: Path, entries: dict[str, bytes]) -> None:
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as jar:
            for name, value in entries.items():
                jar.writestr(name, value)

    def test_delta_reconstructs_added_changed_and_removed_entries(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.jar"
            target = root / "target.jar"
            delta = root / "delta.zip"
            manifest = root / "delta.json"
            output = root / "assembled.jar"
            base_entries = {
                "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\n",
                "unchanged.class": b"unchanged bytecode",
                "changed.class": b"old bytecode",
                "removed.dat": b"remove me",
            }
            target_entries = {
                "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\n",
                "unchanged.class": b"unchanged bytecode",
                "changed.class": b"new bytecode",
                "added.dat": b"new resource",
            }
            self.write_jar(base, base_entries)
            self.write_jar(target, target_entries)

            create_delta(base, target, delta, manifest)
            apply_delta(base, delta, manifest, output)

            with zipfile.ZipFile(output) as assembled:
                self.assertIsNone(assembled.testzip())
                self.assertEqual(set(assembled.namelist()), set(target_entries))
                self.assertEqual(
                    {name: assembled.read(name) for name in assembled.namelist()},
                    target_entries,
                )

    def test_apply_rejects_wrong_deployed_base(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.jar"
            wrong_base = root / "wrong.jar"
            target = root / "target.jar"
            delta = root / "delta.zip"
            manifest = root / "delta.json"
            output = root / "assembled.jar"
            self.write_jar(base, {"plugin.class": b"base"})
            self.write_jar(wrong_base, {"plugin.class": b"tampered"})
            self.write_jar(target, {"plugin.class": b"target"})
            create_delta(base, target, delta, manifest)

            with self.assertRaisesRegex(ValueError, "differs from the deployed base"):
                apply_delta(wrong_base, delta, manifest, output)

    def test_create_rejects_traversal_entry(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.jar"
            target = root / "target.jar"
            self.write_jar(base, {"safe.class": b"safe"})
            self.write_jar(target, {"../outside.class": b"unsafe"})

            with self.assertRaisesRegex(ValueError, "unsafe JAR entry"):
                create_delta(base, target, root / "delta.zip", root / "delta.json")


if __name__ == "__main__":
    unittest.main()
