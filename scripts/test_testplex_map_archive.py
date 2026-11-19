import importlib.util
import pathlib
import tarfile
import tempfile
import unittest
import io


SCRIPT = pathlib.Path(__file__).parents[1] / "deploy" / "testplex-map-archive.py"
SPEC = importlib.util.spec_from_file_location("testplex_map_archive", SCRIPT)
MAP_ARCHIVE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MAP_ARCHIVE)


class MapArchiveTests(unittest.TestCase):
    def make_source(self, path, *, missing_world=None, unsafe_path=None, symlink_world=None):
        with tarfile.open(path, "w:gz") as archive:
            for world in MAP_ARCHIVE.WORLDS:
                if world == missing_world:
                    continue
                root = f"MonsterMaze-abc123/solo/1.21/maps/{world}"
                for name, contents in (("level.dat", b"level"), ("region/r.0.0.mca", b"region"),
                                       ("uid.dat", b"runtime id"), ("session.lock", b"runtime lock")):
                    info = tarfile.TarInfo(f"{root}/{name}")
                    info.size = len(contents)
                    archive.addfile(info, io.BytesIO(contents))
            extra = tarfile.TarInfo("MonsterMaze-abc123/solo/1.21/maps/unrelated/level.dat")
            extra.size = 4
            archive.addfile(extra, io.BytesIO(b"skip"))
            if unsafe_path:
                info = tarfile.TarInfo(unsafe_path)
                info.size = 1
                archive.addfile(info, io.BytesIO(b"x"))
            if symlink_world:
                info = tarfile.TarInfo(f"MonsterMaze-abc123/solo/1.21/maps/{symlink_world}/evil")
                info.type = tarfile.SYMTYPE
                info.linkname = "../../../../outside"
                archive.addfile(info)

    def test_filters_only_the_seven_complete_worlds(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp) / "source.tar.gz"
            output = pathlib.Path(tmp) / "maps.tar.gz"
            self.make_source(source)
            count = MAP_ARCHIVE.filter_source_tarball(source, output)
            self.assertEqual(14, count)
            with tarfile.open(output, "r:gz") as result:
                names = {member.name for member in result}
            self.assertEqual(14, len(names))
            self.assertTrue(all(name.startswith(MAP_ARCHIVE.WORLDS) for name in names))
            self.assertNotIn("unrelated/level.dat", names)
            self.assertFalse(any(name.endswith(("/uid.dat", "/session.lock")) for name in names))

    def test_rejects_traversal(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp) / "source.tar.gz"
            output = pathlib.Path(tmp) / "maps.tar.gz"
            self.make_source(source, unsafe_path="MonsterMaze-abc123/../escape")
            with self.assertRaisesRegex(ValueError, "unsafe source archive path"):
                MAP_ARCHIVE.filter_source_tarball(source, output)

    def test_rejects_missing_world_data(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp) / "source.tar.gz"
            output = pathlib.Path(tmp) / "maps.tar.gz"
            self.make_source(source, missing_world="mm_void")
            with self.assertRaisesRegex(ValueError, "missing mm_void/level.dat"):
                MAP_ARCHIVE.filter_source_tarball(source, output)

    def test_rejects_links_inside_selected_worlds(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp) / "source.tar.gz"
            output = pathlib.Path(tmp) / "maps.tar.gz"
            self.make_source(source, symlink_world="mm_void")
            with self.assertRaisesRegex(ValueError, "unsupported file type"):
                MAP_ARCHIVE.filter_source_tarball(source, output)


if __name__ == "__main__":
    unittest.main()
