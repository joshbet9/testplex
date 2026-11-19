#!/usr/bin/env python3
"""Filter the seven pinned Monster Maze worlds from a GitHub source tarball."""

from __future__ import annotations

import argparse
import pathlib
import tarfile


WORLDS = (
    "mm_void",
    "mm_volcano",
    "mm_colombia",
    "mm_sandycoast",
    "mm_siberian",
    "mm_swampland",
    "mm_tesorohundido",
)
SOURCE_PREFIX = ("solo", "1.21", "maps")


def filter_source_tarball(source_path: pathlib.Path, output_path: pathlib.Path) -> int:
    """Write only validated world files into a compact world-rooted tar.gz."""
    selected: dict[str, list[tuple[tarfile.TarInfo, str]]] = {world: [] for world in WORLDS}
    roots: set[str] = set()
    seen_paths: set[str] = set()

    with tarfile.open(source_path, mode="r:gz") as source:
        for member in source:
            name = member.name
            path = pathlib.PurePosixPath(name)
            if path.is_absolute() or ".." in path.parts or not path.parts:
                raise ValueError(f"unsafe source archive path: {name!r}")
            roots.add(path.parts[0])
            if len(path.parts) < 5 or path.parts[1:4] != SOURCE_PREFIX:
                continue
            world = path.parts[4]
            if world not in selected:
                continue
            relative = pathlib.PurePosixPath(*path.parts[4:]).as_posix()
            if pathlib.PurePosixPath(relative).name in {"session.lock", "uid.dat"}:
                continue
            if relative in seen_paths:
                raise ValueError(f"duplicate map archive path: {relative!r}")
            seen_paths.add(relative)
            if not (member.isdir() or member.isfile()):
                raise ValueError(f"unsupported file type in selected map: {name!r}")
            if member.isfile():
                selected[world].append((member, relative))

        if len(roots) != 1:
            raise ValueError(f"expected one source archive root, found {len(roots)}")

        for world, files in selected.items():
            names = {relative for _, relative in files}
            if f"{world}/level.dat" not in names:
                raise ValueError(f"source archive is missing {world}/level.dat")
            if not any(name.startswith(f"{world}/region/") and name.endswith(".mca") for name in names):
                raise ValueError(f"source archive is missing {world} region files")

        output_path.parent.mkdir(parents=True, exist_ok=True)
        with tarfile.open(output_path, mode="w:gz", compresslevel=1) as output:
            for files in selected.values():
                for source_member, relative in files:
                    if source_member.isdir():
                        continue
                    stream = source.extractfile(source_member)
                    if stream is None:
                        raise ValueError(f"could not read source map file: {source_member.name!r}")
                    target = tarfile.TarInfo(relative)
                    target.size = source_member.size
                    target.mode = source_member.mode & 0o755
                    target.mtime = source_member.mtime
                    output.addfile(target, stream)

    return sum(len(files) for files in selected.values())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=pathlib.Path)
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args()
    count = filter_source_tarball(args.source, args.output)
    print(f"Validated and staged {count} files across {len(WORLDS)} pinned Monster Maze worlds.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
