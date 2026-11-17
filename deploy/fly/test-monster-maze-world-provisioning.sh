#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROVISION="$SCRIPT_DIR/provision-monster-maze-worlds.sh"
ROOT="$(mktemp -d)"
trap 'rm -rf -- "$ROOT"' EXIT
SOURCE="$ROOT/image/MonsterMazeMaps"
WORLD_ROOT="$ROOT/runtime/worlds"
mkdir -p "$SOURCE"

write_source() {
    local revision="$1" marker="$2"
    mkdir -p "$SOURCE"
    for world in mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido; do
        mkdir -p "$SOURCE/$world/region"
        printf '%s:%s\n' "$world" "$marker" > "$SOURCE/$world/level.dat"
        printf 'region:%s:%s\n' "$world" "$marker" > "$SOURCE/$world/region/r.0.0.mca"
        rm -f "$SOURCE/$world/session.lock" "$SOURCE/$world/uid.dat"
    done
    printf '%s\n' "$revision" > "$SOURCE/.testplex-mm-source-maps"
}

revision_one=1111111111111111111111111111111111111111
revision_two=2222222222222222222222222222222222222222
write_source "$revision_one" first
TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_one" ]]
[[ "$(find "$WORLD_ROOT" -mindepth 1 -maxdepth 1 -type d | wc -l)" -eq 7 ]]
[[ ! -e "$WORLD_ROOT/mm_void/session.lock" && ! -e "$WORLD_ROOT/mm_void/uid.dat" ]]

# A deployment with the same pinned revision must preserve the live world copy.
printf 'preserve-me\n' > "$WORLD_ROOT/mm_void/runtime-marker.txt"
TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
grep -Fxq 'preserve-me' "$WORLD_ROOT/mm_void/runtime-marker.txt"

# A changed source revision replaces only the seven source worlds.
write_source "$revision_two" second
TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
grep -Fxq 'mm_void:second' "$WORLD_ROOT/mm_void/level.dat"
[[ ! -e "$WORLD_ROOT/mm_void/runtime-marker.txt" ]]

# Validate every source before replacement so a broken image cannot partially
# overwrite the previous complete map set.
printf 'keep-on-failure\n' > "$WORLD_ROOT/mm_void/runtime-marker.txt"
rm -rf "$SOURCE/mm_swampland/region"
if TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"; then
    echo 'Expected incomplete source maps to fail validation.' >&2
    exit 1
fi
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
grep -Fxq 'keep-on-failure' "$WORLD_ROOT/mm_void/runtime-marker.txt"

if TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" /server 2>/dev/null; then
    echo 'Expected persistent /server world paths to be rejected.' >&2
    exit 1
fi

echo 'Monster Maze world provisioning checks passed.'
