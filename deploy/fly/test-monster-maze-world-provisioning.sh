#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROVISION="$SCRIPT_DIR/provision-monster-maze-worlds.sh"
ROOT="$(mktemp -d)"
trap 'rm -rf -- "$ROOT"' EXIT
SOURCE="$ROOT/image/MonsterMazeMaps"
WORLD_ROOT="$ROOT/runtime/server"
RUNTIME_ROOT="$ROOT/runtime/tmp/MonsterMazeMaps"
mkdir -p "$SOURCE" "$WORLD_ROOT/world/playerdata"
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

# Emulate the existing Fly layout: the old provisioner installed image links.
for world in mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido; do
    ln -s "$SOURCE/$world" "$WORLD_ROOT/$world"
done
printf '%s\n' "$revision_one" > "$WORLD_ROOT/.testplex-mm-source-maps"
TESTPLEX_WORLD_OWNER= TESTPLEX_MM_RUNTIME_ROOT="$RUNTIME_ROOT" bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_one" ]]
for world in mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido; do
    [[ -d "$WORLD_ROOT/$world" && ! -L "$WORLD_ROOT/$world" ]]
    [[ "$(cat "$WORLD_ROOT/$world/level.dat")" == "$world:first" ]]
    [[ -s "$WORLD_ROOT/$world/region/r.0.0.mca" ]]
done
[[ "$(cat "$WORLD_ROOT/world/playerdata/preserve.dat")" == 'player-world-data-must-survive' ]]
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

# Paper may migrate a real map folder. The same source revision preserves it.
printf 'paper-runtime-change\n' > "$WORLD_ROOT/mm_void/paper-migration-marker.dat"
mkdir -p "$WORLD_ROOT/mm_void/dimensions/minecraft/overworld"
mv "$WORLD_ROOT/mm_void/region" "$WORLD_ROOT/mm_void/dimensions/minecraft/overworld/region"
TESTPLEX_WORLD_OWNER= TESTPLEX_MM_RUNTIME_ROOT="$RUNTIME_ROOT" bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
grep -Fxq 'paper-runtime-change' "$WORLD_ROOT/mm_void/paper-migration-marker.dat"
[[ -s "$WORLD_ROOT/mm_void/dimensions/minecraft/overworld/region/r.0.0.mca" ]]

# A new image source revision updates only the seven managed map folders.
printf 'preserve-me\n' > "$WORLD_ROOT/world/runtime-marker.txt"
write_source "$revision_two" second
TESTPLEX_WORLD_OWNER= TESTPLEX_MM_RUNTIME_ROOT="$RUNTIME_ROOT" bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
[[ "$(cat "$WORLD_ROOT/mm_void/level.dat")" == 'mm_void:second' ]]
[[ ! -e "$WORLD_ROOT/mm_void/paper-migration-marker.dat" ]]
grep -Fxq 'preserve-me' "$WORLD_ROOT/world/runtime-marker.txt"
[[ "$(cat "$WORLD_ROOT/world/playerdata/preserve.dat")" == 'player-world-data-must-survive' ]]
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

# Validate the full source set before replacing any installed map.
printf 'keep-on-failure\n' > "$WORLD_ROOT/world/runtime-marker.txt"
rm -rf "$SOURCE/mm_swampland/region"
if TESTPLEX_WORLD_OWNER= TESTPLEX_MM_RUNTIME_ROOT="$RUNTIME_ROOT" bash "$PROVISION" "$SOURCE" "$WORLD_ROOT"; then
    echo 'Expected incomplete source maps to fail validation.' >&2
    exit 1
fi
[[ "$(cat "$WORLD_ROOT/.testplex-mm-source-maps")" == "$revision_two" ]]
[[ "$(cat "$WORLD_ROOT/mm_void/level.dat")" == 'mm_void:second' ]]
grep -Fxq 'keep-on-failure' "$WORLD_ROOT/world/runtime-marker.txt"
[[ "$(cat "$WORLD_ROOT/world/playerdata/preserve.dat")" == 'player-world-data-must-survive' ]]
[[ "$(cat "$WORLD_ROOT/player-stats.db")" == 'plugin-stats-must-survive' ]]

# A same-named world with no provisioner marker is user data, not disposable.
FOREIGN_ROOT="$ROOT/foreign-server"
mkdir -p "$FOREIGN_ROOT/mm_void"
printf 'unmanaged-world-data\n' > "$FOREIGN_ROOT/mm_void/keep.dat"
if TESTPLEX_WORLD_OWNER= TESTPLEX_MM_RUNTIME_ROOT="$RUNTIME_ROOT" bash "$PROVISION" "$SOURCE" "$FOREIGN_ROOT"; then
    echo 'Expected untracked world data to be protected.' >&2
    exit 1
fi
[[ "$(cat "$FOREIGN_ROOT/mm_void/keep.dat")" == 'unmanaged-world-data' ]]

echo 'Monster Maze world provisioning checks passed.'
