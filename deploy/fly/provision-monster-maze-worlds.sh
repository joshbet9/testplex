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
    echo "Refusing to place Monster Maze worlds in a symbolic-link directory." >&2
    exit 1
fi
case "$WORLD_ROOT" in
    /server|/server/*)
        echo "Refusing to place replaceable worlds on the persistent /server data volume." >&2
        exit 1
        ;;
esac
mkdir -p -- "$WORLD_ROOT"
if [[ -z "${MSYSTEM:-}" ]]; then
    chmod 0750 -- "$WORLD_ROOT"
fi
WORLD_ROOT="$(realpath -e -- "$WORLD_ROOT")"
if [[ "$WORLD_ROOT" == /server || "$WORLD_ROOT" == /server/* ]]; then
    echo "Refusing to place replaceable worlds on the persistent /server data volume." >&2
    exit 1
fi

for world in "${WORLD_NAMES[@]}"; do
    source="$SOURCE_ROOT/$world"
    if [[ ! -d "$source" || -L "$source" || ! -s "$source/level.dat" || ! -d "$source/region" ]]; then
        echo "Monster Maze source world is incomplete or unsafe: $world" >&2
        exit 1
    fi
done

has_complete_world_set() {
    [[ -f "$WORLD_ROOT/.testplex-mm-source-maps" ]] \
        && [[ "$(tr -d '\r\n[:space:]' < "$WORLD_ROOT/.testplex-mm-source-maps")" == "$EXPECTED_SOURCE" ]] \
        || return 1
    local world
    for world in "${WORLD_NAMES[@]}"; do
        [[ -s "$WORLD_ROOT/$world/level.dat" && -d "$WORLD_ROOT/$world/region" && ! -L "$WORLD_ROOT/$world" ]] || return 1
    done
}

if has_complete_world_set; then
    echo "Monster Maze source worlds already match $EXPECTED_SOURCE; preserving the runtime copies."
    exit 0
fi

stage="$(mktemp -d "$WORLD_ROOT/.testplex-mm-stage.XXXXXX")"
cleanup() { rm -rf -- "$stage"; }
trap cleanup EXIT
for world in "${WORLD_NAMES[@]}"; do
    cp -a -- "$SOURCE_ROOT/$world" "$stage/$world"
    test -s "$stage/$world/level.dat"
    test -d "$stage/$world/region"
done

if [[ -n "${TESTPLEX_WORLD_OWNER:-}" ]]; then
    chown -R -- "$TESTPLEX_WORLD_OWNER" "$stage"
fi

# Only replace the seven named source maps in the non-persistent world root.
# The /server volume containing stats and plugin data is never touched here.
rm -f -- "$WORLD_ROOT/.testplex-mm-source-maps"
for world in "${WORLD_NAMES[@]}"; do
    rm -rf -- "$WORLD_ROOT/$world"
    mv -- "$stage/$world" "$WORLD_ROOT/$world"
done
printf '%s\n' "$EXPECTED_SOURCE" > "$stage/.testplex-mm-source-maps"
mv -- "$stage/.testplex-mm-source-maps" "$WORLD_ROOT/.testplex-mm-source-maps"
has_complete_world_set || { echo "Monster Maze source worlds failed post-install verification." >&2; exit 1; }
echo "Installed and verified seven Monster Maze source worlds at revision $EXPECTED_SOURCE."
