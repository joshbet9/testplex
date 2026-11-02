#!/usr/bin/env python3
"""Create and apply verified entry-level deltas for Java plugin JARs."""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import sys
import zipfile


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_name(name: str) -> bool:
    path = PurePosixPath(name)
    return bool(name) and not path.is_absolute() and all(part not in ("", ".", "..") for part in path.parts)


def index_archive(path: Path) -> tuple[zipfile.ZipFile, dict[str, zipfile.ZipInfo]]:
    archive = zipfile.ZipFile(path, "r")
    index: dict[str, zipfile.ZipInfo] = {}
    for info in archive.infolist():
        if not safe_name(info.filename):
            archive.close()
            raise ValueError(f"unsafe JAR entry name: {info.filename!r}")
        if info.filename in index:
            archive.close()
            raise ValueError(f"duplicate JAR entry cannot be patched safely: {info.filename!r}")
        index[info.filename] = info
    return archive, index


def entry_sha256(archive: zipfile.ZipFile, name: str) -> str:
    digest = hashlib.sha256()
    with archive.open(name) as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def metadata(info: zipfile.ZipInfo) -> dict[str, object]:
    return {
        "name": info.filename,
        "sha256": None,
        "date_time": list(info.date_time),
        "compress_type": info.compress_type,
        "comment": base64.b64encode(info.comment).decode("ascii"),
        "extra": base64.b64encode(info.extra).decode("ascii"),
        "create_system": info.create_system,
        "create_version": info.create_version,
        "extract_version": info.extract_version,
        "reserved": info.reserved,
        "flag_bits": info.flag_bits,
        "volume": info.volume,
        "internal_attr": info.internal_attr,
        "external_attr": info.external_attr,
    }


def create_delta(base_path: Path, target_path: Path, delta_path: Path, manifest_path: Path) -> None:
    base, base_index = index_archive(base_path)
    target, target_index = index_archive(target_path)
    try:
        changed = []
        base_entries = [
            {"name": name, "sha256": entry_sha256(base, name)}
            for name in base_index
        ]
        target_entries = []
        for name, info in target_index.items():
            digest = entry_sha256(target, name)
            target_entry = metadata(info)
            target_entry["sha256"] = digest
            target_entries.append(target_entry)
            if name not in base_index or entry_sha256(base, name) != digest:
                changed.append(name)

        delta_path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(delta_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as delta:
            for name in changed:
                info = target_index[name]
                patch_info = zipfile.ZipInfo(name, info.date_time)
                patch_info.compress_type = info.compress_type
                patch_info.external_attr = info.external_attr
                patch_info.create_system = info.create_system
                delta.writestr(patch_info, target.read(name))

        manifest = {
            "format": 1,
            "base_sha256": sha256_file(base_path),
            "base_entries": base_entries,
            "target_sha256": sha256_file(target_path),
            "target_comment": base64.b64encode(target.comment).decode("ascii"),
            "target_entries": target_entries,
            "changed_entries": changed,
        }
        manifest_path.write_text(json.dumps(manifest, separators=(",", ":")) + "\n", encoding="utf-8")
        print(f"base_bytes={base_path.stat().st_size}")
        print(f"target_bytes={target_path.stat().st_size}")
        print(f"changed_entries={len(changed)}")
        print(f"delta_bytes={delta_path.stat().st_size}")
        print(f"target_sha256={manifest['target_sha256']}")
    finally:
        base.close()
        target.close()


def apply_delta(base_path: Path, delta_path: Path, manifest_path: Path, output_path: Path) -> None:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("format") != 1:
        raise ValueError("unsupported JAR delta format")
    actual_base_sha = sha256_file(base_path)
    base, base_index = index_archive(base_path)
    if actual_base_sha != manifest.get("base_sha256"):
        base_entries = manifest.get("base_entries")
        if not isinstance(base_entries, list):
            raise ValueError("installed JAR differs from the patch base and no entry manifest is available")
        expected_base: dict[str, str] = {}
        for entry in base_entries:
            if not isinstance(entry, dict) or not safe_name(str(entry.get("name", ""))):
                raise ValueError("patch base manifest contains an unsafe entry name")
            name = str(entry["name"])
            if name in expected_base or not isinstance(entry.get("sha256"), str):
                raise ValueError(f"invalid or duplicate base manifest entry: {name}")
            expected_base[name] = str(entry["sha256"])
        if set(base_index) != set(expected_base):
            raise ValueError("installed JAR entry list does not match the deployed base artifact")
        for name, digest in expected_base.items():
            if entry_sha256(base, name) != digest:
                raise ValueError(f"installed JAR entry differs from the deployed base artifact: {name}")
    delta, delta_index = index_archive(delta_path)
    expected_entries = manifest.get("target_entries")
    changed_entries = manifest.get("changed_entries")
    if not isinstance(expected_entries, list) or not isinstance(changed_entries, list):
        raise ValueError("patch manifest is missing entry records")
    expected: dict[str, dict[str, object]] = {}
    for entry in expected_entries:
        if not isinstance(entry, dict) or not safe_name(str(entry.get("name", ""))):
            raise ValueError("patch manifest contains an unsafe entry name")
        name = str(entry["name"])
        if name in expected or not isinstance(entry.get("sha256"), str):
            raise ValueError(f"invalid or duplicate manifest entry: {name}")
        expected[name] = entry

    if set(delta_index) != set(changed_entries):
        raise ValueError("delta archive entries do not match the changed-entry manifest")
    if not set(delta_index).issubset(expected):
        raise ValueError("delta archive contains entries absent from the target manifest")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output_path.with_name(output_path.name + ".assembling")
    try:
        with zipfile.ZipFile(temporary, "w", allowZip64=True) as output:
            output.comment = base64.b64decode(str(manifest.get("target_comment", "")))
            for name, entry in expected.items():
                source = delta if name in delta_index else base
                if name not in delta_index and name not in base_index:
                    raise ValueError(f"target entry has no source bytes: {name}")
                data = source.read(name)
                if hashlib.sha256(data).hexdigest() != entry["sha256"]:
                    raise ValueError(f"entry checksum mismatch: {name}")
                info = zipfile.ZipInfo(name, tuple(entry["date_time"]))
                info.compress_type = int(entry["compress_type"])
                info.comment = base64.b64decode(str(entry["comment"]))
                info.extra = base64.b64decode(str(entry["extra"]))
                info.create_system = int(entry["create_system"])
                info.create_version = int(entry["create_version"])
                info.extract_version = int(entry["extract_version"])
                info.reserved = int(entry["reserved"])
                info.flag_bits = int(entry["flag_bits"])
                info.volume = int(entry["volume"])
                info.internal_attr = int(entry["internal_attr"])
                info.external_attr = int(entry["external_attr"])
                output.writestr(info, data, compress_type=info.compress_type, compresslevel=6)

        with zipfile.ZipFile(temporary, "r") as assembled:
            if assembled.testzip() is not None:
                raise ValueError("assembled JAR failed ZIP CRC validation")
            if set(assembled.namelist()) != set(expected):
                raise ValueError("assembled JAR entry list does not match the target manifest")
            for name, entry in expected.items():
                if entry_sha256(assembled, name) != entry["sha256"]:
                    raise ValueError(f"assembled JAR entry checksum mismatch: {name}")
        os.replace(temporary, output_path)
        print(f"assembled_bytes={output_path.stat().st_size}")
        print(f"verified_entries={len(expected)}")
        print(f"assembled_sha256={sha256_file(output_path)}")
    finally:
        base.close()
        delta.close()
        if temporary.exists():
            temporary.unlink()


def main() -> int:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("create")
    create.add_argument("base", type=Path)
    create.add_argument("target", type=Path)
    create.add_argument("delta", type=Path)
    create.add_argument("manifest", type=Path)
    apply = commands.add_parser("apply")
    apply.add_argument("base", type=Path)
    apply.add_argument("delta", type=Path)
    apply.add_argument("manifest", type=Path)
    apply.add_argument("output", type=Path)
    args = parser.parse_args()
    try:
        if args.command == "create":
            create_delta(args.base, args.target, args.delta, args.manifest)
        else:
            apply_delta(args.base, args.delta, args.manifest, args.output)
    except (OSError, ValueError, zipfile.BadZipFile, KeyError, TypeError) as error:
        print(f"jar delta error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
