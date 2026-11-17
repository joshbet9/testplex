#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROVISION="$SCRIPT_DIR/provision-monster-maze-worlds.sh"
ROOT="$(mktemp -d)"
trap 'rm -rf -- "$ROOT"' EXIT
SOURCE="$ROOT/image/MonsterMazeMaps"
WORLD_ROOT="$ROOT/runtime/server"
mkdir -p "$SOURCE"
mkdir -p "$WORLD_ROOT/world/playerdata"
printf 'player-world-data-must-survive\n' > "$WORLD_ROOT/world/playerdata/preserve.dat"
printf 'plugin-stats-must-survive\n' > "$WORLD_ROOT/player-stats.db"

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
[[ -L "$WORLD_ROOT/mm_void" && "$(readlink -f "$WORLD_ROOT/mm_void")" == "$SOURCE/mm_void" ]]
[[ "$(cat "$WORLD_ROOT/world/playerdata/preserve.dat")" == 'player-world-data-must-survive' ]]
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

# A deployment with the same pinned revision must preserve unrelated player data.
printf 'preserve-me\n' > "$WORLD_ROOT/world/runtime-marker.txt"
TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
grep -Fxq 'preserve-me' "$WORLD_ROOT/world/runtime-marker.txt"

# A changed image revision updates its marker without altering persistent data.
write_source "$revision_two" second
TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
grep -Fxq 'mm_void:second' "$WORLD_ROOT/mm_void/level.dat"
grep -Fxq 'preserve-me' "$WORLD_ROOT/world/runtime-marker.txt"
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

# Validate every source before replacement so a broken image cannot partially
# overwrite the previous complete map set.
printf 'keep-on-failure\n' > "$WORLD_ROOT/world/runtime-marker.txt"
rm -rf "$SOURCE/mm_swampland/region"
if TESTPLEX_WORLD_OWNER= bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"; then
    echo 'Expected incomplete source maps to fail validation.' >&2
    exit 1
fi
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
grep -Fxq 'keep-on-failure' "$WORLD_ROOT/world/runtime-marker.txt"
[[ "$(cat "$WORLD_ROOT/world/playerdata/preserve.dat")" == 'player-world-data-must-survive' ]]
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

echo 'Monster Maze world provisioning checks passed.'
