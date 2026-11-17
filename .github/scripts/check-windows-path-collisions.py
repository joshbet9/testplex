#!/usr/bin/env python3
"""Fail when tracked paths collide on case-insensitive filesystems such as default Windows NTFS."""

from __future__ import annotations

import subprocess
import sys
import unicodedata


def windows_key(path: str) -> str:
    return unicodedata.normalize("NFC", path.replace("\\", "/")).casefold()


def main() -> int:
    output = subprocess.run(
        ["git", "ls-files", "-z"],
        check=True,
        stdout=subprocess.PIPE,
    ).stdout
    paths = [entry.decode("utf-8", errors="surrogateescape") for entry in output.split(b"\0") if entry]
    seen: dict[str, str] = {}
    collisions: list[tuple[str, str]] = []

    for path in paths:
        key = windows_key(path)
        previous = seen.get(key)
        if previous is not None and previous != path:
            collisions.append((previous, path))
        else:
            seen[key] = path

    if collisions:
        for first, second in collisions:
            print(f"Windows path collision: {first!r} <> {second!r}", file=sys.stderr)
        return 1

    print(f"Windows path check passed ({len(paths)} tracked paths).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
