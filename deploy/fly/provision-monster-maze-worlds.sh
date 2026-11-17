#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 ]]; then
    echo "Usage: provision-monster-maze-worlds.sh SOURCE_ROOT WORLD_ROOT" >&2
    exit 2
fi

SOURCE_ROOT="$(realpath -e -- "$1")"
WORLD_ROOT="$2"
EXPECTED_SOURCE="$(tr -d '\r\n[:space:]' < "$SOURCE_ROOT/.testplex-mm-source-maps")"
WORLD_NAMES=(mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido)

if [[ ! "$EXPECTED_SOURCE" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Monster Maze source revision marker is missing or invalid." >&2
    exit 1
fi
if [[ -L "$WORLD_ROOT" ]]; then
    echo "Refusing to provision maps through a symbolic-link world container." >&2
    exit 1
fi
mkdir -p -- "$WORLD_ROOT"
if [[ -z "${MSYSTEM:-}" ]]; then
    chmod 0750 -- "$WORLD_ROOT"
fi
WORLD_ROOT="$(realpath -e -- "$WORLD_ROOT")"

for world in "${WORLD_NAMES[@]}"; do
    source="$SOURCE_ROOT/$world"
    if [[ ! -d "$source" || -L "$source" || ! -s "$source/level.dat" || ! -d "$source/region" ]]; then
        echo "Monster Maze source world is incomplete or unsafe: $world" >&2
        exit 1
    fi
done

has_complete_world_set() {
    local installed_source
    [[ -f "$WORLD_ROOT/.testplex-mm-source-maps" ]] || return 1
    installed_source="$(tr -d '\r\n[:space:]' < "$WORLD_ROOT/.testplex-mm-source-maps")"
    [[ "$installed_source" == "$EXPECTED_SOURCE" ]] || return 1
    local world
    for world in "${WORLD_NAMES[@]}"; do
        local installed_target
        [[ -L "$WORLD_ROOT/$world" ]] || return 1
        installed_target="$(readlink -f -- "$WORLD_ROOT/$world")"
        [[ "$installed_target" == "$SOURCE_ROOT/$world" ]] || return 1
    done
}

if has_complete_world_set; then
    echo "Monster Maze source worlds already match $EXPECTED_SOURCE; preserving the runtime copies."
    exit 0
fi

stage="$(mktemp -d "$WORLD_ROOT/.testplex-mm-stage.XXXXXX")"
cleanup() { rm -rf -- "$stage"; }
trap cleanup EXIT

# Never delete or replace data already present in Paper's persistent world
# container. New map entries are symlinks to this image's assets, keeping their
# large region data outside the player/world-data volume.
for world in "${WORLD_NAMES[@]}"; do
    link="$WORLD_ROOT/$world"
    target="$SOURCE_ROOT/$world"
    if [[ -e "$link" || -L "$link" ]]; then
        if [[ ! -L "$link" || "$(readlink -f -- "$link")" != "$target" ]]; then
            echo "Refusing to overwrite existing world data at $link" >&2
            exit 1
        fi
    else
        ln -s -- "$target" "$stage/$world"
    fi
done

if [[ -n "${TESTPLEX_WORLD_OWNER:-}" ]]; then
    chown -- "$TESTPLEX_WORLD_OWNER" "$stage"
fi
for world in "${WORLD_NAMES[@]}"; do
    if [[ -L "$stage/$world" ]]; then
        mv -- "$stage/$world" "$WORLD_ROOT/$world"
    fi
done
printf '%s\n' "$EXPECTED_SOURCE" > "$stage/.testplex-mm-source-maps"
mv -f -- "$stage/.testplex-mm-source-maps" "$WORLD_ROOT/.testplex-mm-source-maps"
has_complete_world_set || { echo "Monster Maze source worlds failed post-install verification." >&2; exit 1; }
echo "Linked and verified seven Monster Maze source worlds at revision $EXPECTED_SOURCE; persistent player/world data was preserved."
